/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uniluebeck.itm.ncoap.communication.dispatching.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.communication.events.MessageTransferEvent;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* The {@link WebserviceManager} is the topmost {@link ChannelHandler} of the {@link ChannelPipeline} returned
* by {@link de.uniluebeck.itm.ncoap.application.server.ServerChannelPipelineFactory}. It is responsible to dispatch inbound {@link CoapRequest}s, i.e.
*
* <ul>
*     <li>invoke the method {@link Webservice#processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} of
*     the addressed {@link Webservice} instance (if it exists) or</li>
*     <li>invoke the method
*     {@link NotFoundHandler#processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} if the
*     inbound {@link CoapRequest} addresses a service that does not (yet) exist.
*     </li>
* </ul>
*
* Upon invocation of the method it awaits a proper {@link CoapResponse} and sends that response downstream, i.e.
* in the direction of the local socket, i.e. to the client that sent the {@link CoapRequest}.
*
* However, the {@link WebserviceManager} is aware of all registered {@link Webservice} instances and is thus to be
* used to register new {@link Webservice} instances, e.g. while processing an inbound {@link CoapRequest} with
* {@link MessageCode.Name#POST}. That is why all {@link Webservice} instances can reference their
* {@link WebserviceManager} via {@link Webservice#getWebserviceManager()}.
*
* Last but not least it checks whether the {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#IF_NONE_MATCH} is set on inbound {@link CoapRequest}s
* and sends a {@link CoapResponse} with {@link MessageCode.Name#PRECONDITION_FAILED_412} if the option was set but the
* addressed {@link Webservice} already exists.
*
* @author Oliver Kleine
*/
public class WebserviceManager extends SimpleChannelUpstreamHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //This map holds all registered webservices (key: URI path, value: Webservice instance)
    private Map<String, Webservice> registeredServices;

    private HashBasedTable<InetSocketAddress, Token, ObservableWebservice> observations;
    private ReentrantReadWriteLock observationsLock;

    private ScheduledExecutorService executor;
    private NotFoundHandler webServiceNotFoundHandler;
    private Channel channel;
    private boolean shutdown;

    /**
     * @param webServiceNotFoundHandler Instance of {@link NotFoundHandler} to deal with inbound {@link CoapRequest}s with
     *                          {@link MessageCode.Name#PUT} if the addresses {@link Webservice} does not exist.
     *
     * @param executor the {@link ScheduledExecutorService} to process the task to send a {@link CoapResponse}
     *                        and
     */
    public WebserviceManager(NotFoundHandler webServiceNotFoundHandler, ScheduledExecutorService executor){

        this.registeredServices = Collections.synchronizedMap(new HashMap<String, Webservice>());
        this.executor = executor;
        this.webServiceNotFoundHandler = webServiceNotFoundHandler;
        this.shutdown = false;
        this.observations = HashBasedTable.create();
        this.observationsLock = new ReentrantReadWriteLock();

        registerService(new WellKnownCoreResource(registeredServices, executor));
    }


    /**
     * This method is called by the framework to enable the {@link WebserviceManager} to send messages
     * to other handlers in the {@link ChannelPipeline}.
     *
     * @param channel the {@link Channel} to be used for messages to be sent to other handlers in that channel
     */
    public void setChannel(Channel channel){
        this.channel = channel;
    }


    /**
     * This method is called by the Netty framework whenever a new message is received to be processed. If the
     * received messages was a {@link CoapRequest}, this method deals with the handling of that request,
     * e.g. by invoking the method
     * {@link Webservice#processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} of the addressed
     * {@link Webservice} instance.
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     *
     * @param me the {@link MessageEvent} containing the actual message
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Inbound (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapRequest)
            handleCoapRequest(ctx, (CoapRequest) me.getMessage(), (InetSocketAddress) me.getRemoteAddress());

        else if(me.getMessage() instanceof MessageTransferEvent){
            handleMessageTransferEvent((MessageTransferEvent) me.getMessage());
        }

        else{
            log.warn("IGNORE MESSAGE OF UNKNOWN TYPE: {}", me.getMessage());
        }


        me.getFuture().setSuccess();
    }

    private void handleMessageTransferEvent(MessageTransferEvent event) {
        log.debug("Inbound: {}", event);
        InetSocketAddress remoteEndpoint = event.getRemoteEndpoint();
        Token token = event.getToken();

        try{
            observationsLock.readLock().lock();
            if(!this.observations.contains(remoteEndpoint, token)){
                return;
            }
        }
        finally {
            observationsLock.readLock().unlock();
        }

        if(event.stopsMessageExchange()){
            try{
                observationsLock.writeLock().lock();
                ObservableWebservice webservice = this.observations.remove(remoteEndpoint, token);
                if(webservice != null){
                    log.info("Stopped observation of \"{}\" (remote endpoint: {}, token: {}) due to: {}",
                            new Object[]{webservice.getUriPath(), remoteEndpoint, token, event});
                    webservice.handleMessageTransferEvent(event);
                }
            }
            finally {
                observationsLock.writeLock().unlock();
            }
        }

        else{
            try{
                observationsLock.readLock().lock();
                ObservableWebservice webservice = this.observations.get(remoteEndpoint, token);
                if(webservice != null){
                    webservice.handleMessageTransferEvent(event);
                }
            }
            finally {
                observationsLock.readLock().unlock();
            }
        }
    }


    private void handleCoapRequest(final ChannelHandlerContext ctx, final CoapRequest coapRequest,
                                   final InetSocketAddress remoteEndpoint){

        //Create settable future to wait for response
        final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();

        //Look up web service instance to handle the request
        final Webservice webservice = registeredServices.get(coapRequest.getUriPath());

        if(coapRequest.getObserve() == 1 && webservice instanceof ObservableWebservice){
            Token token = coapRequest.getToken();
            if(((ObservableWebservice) webservice).removeObservation(remoteEndpoint, token)){
                log.info("Stopped observation due to GET request with observe = 1 (remote endpoint: {}, token: {})",
                        remoteEndpoint, token);
            }
            else{
                log.warn("No observation found to be stopped due to GET request with observe = 1 (remote endpoint:" +
                        "{}, token: {}", remoteEndpoint, token);
            }
        }

        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try{
                    CoapResponse coapResponse = responseFuture.get();
                    coapResponse.setMessageID(coapRequest.getMessageID());
                    coapResponse.setToken(coapRequest.getToken());

                    if(coapResponse.isUpdateNotification()){
                        if(webservice instanceof ObservableWebservice && coapRequest.getObserve() == 0){
                            ObservableWebservice observableWebservice = (ObservableWebservice) webservice;
                            observableWebservice.addObservation(remoteEndpoint, coapResponse.getToken(),
                                    coapResponse.getContentFormat());
                            sendUpdateNotification(ctx, remoteEndpoint, coapResponse, observableWebservice);
                        }
                        else{
                            coapResponse.removeOptions(OptionValue.Name.OBSERVE);
                            log.warn("Removed observe option from response!");
                            sendCoapResponse(ctx, remoteEndpoint, coapResponse);
                        }
                    }
                    else{
                        sendCoapResponse(ctx, remoteEndpoint, coapResponse);
                    }
                }
                catch (Exception e) {
                    log.error("Exception while processing inbound request", e);
                    CoapResponse errorResponse = CoapResponse.createErrorResponse(coapRequest.getMessageTypeName(),
                                    MessageCode.Name.INTERNAL_SERVER_ERROR_500, e.getMessage());

                    errorResponse.setMessageID(coapRequest.getMessageID());
                    errorResponse.setToken(coapRequest.getToken());

                    sendCoapResponse(ctx, remoteEndpoint, errorResponse);
                }
            }
        }, executor);

        try{
            //The requested Webservice does not exist
            if(webservice == null)
                webServiceNotFoundHandler.processCoapRequest(responseFuture, coapRequest, remoteEndpoint);

            //The IF-NON-MATCH option indicates that the request is only to be processed if the webservice does not
            //(yet) exist. But it does. So send an error response
            else if(coapRequest.isIfNonMatchSet())
                sendPreconditionFailed(coapRequest.getMessageTypeName(), coapRequest.getUriPath(), responseFuture);

            //The inbound request is to be handled by the addressed service
            else
                webservice.processCoapRequest(responseFuture, coapRequest, remoteEndpoint);

        }
        catch (Exception e) {
            log.error("This should never happen.", e);
            responseFuture.setException(e);
        }
    }


    private void sendCoapResponse(final ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
                                  final CoapResponse coapResponse){

        //Write response
        log.info("Write response to {}: {}", remoteAddress, coapResponse);
        Channels.write(ctx.getChannel(), coapResponse, remoteAddress);
    }


    private void sendUpdateNotification(ChannelHandlerContext ctx, InetSocketAddress remoteAddress,
                                        CoapResponse updateNotification, ObservableWebservice webservice){

        try{
            this.observationsLock.writeLock().lock();
            this.observations.put(remoteAddress, updateNotification.getToken(), webservice);
            log.info("Added new observation of \"{}\" (remote endpoint: {}, token: {})",
                    new Object[]{webservice.getUriPath(), remoteAddress, updateNotification.getToken()});
        }
        finally{
            this.observationsLock.writeLock().unlock();
        }

        sendCoapResponse(ctx, remoteAddress, updateNotification);
    }


    private void sendPreconditionFailed(MessageType.Name messageType, String servicePath,
                                        SettableFuture<CoapResponse> responseFuture) throws Exception{

        CoapResponse coapResponse = new CoapResponse(messageType, MessageCode.Name.PRECONDITION_FAILED_412);
        String message = "IF-NONE-MATCH option was set but service \"" + servicePath + "\" exists.";
        coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        responseFuture.set(coapResponse);
    }


    /**
     * This method is invoked by the framework if an exception occured during the reception or sending of a
     * {@link CoapMessage}.
     *
     * Classes extending {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication} may override this method to, e.g. deal with different
     * types of exceptions differently. This implementation only logs an the error message (if logging is
     * enabled).
     *
     * @param ctx the {@link ChannelHandlerContext} relating the {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication} and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} on which the exception occured
     *
     * @param exceptionEvent the {@link ExceptionEvent} containing, e.g. the exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent exceptionEvent){
        if(!shutdown){
            Throwable cause = exceptionEvent.getCause();
            log.error("Unsupported exception caught! Don't know what to do...", cause);
        }
    }


    /**
     * Shuts the server down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     *
     * Prior to doing so this methods removes all registered {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances from the server, i.e.
     * invokes the {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice#shutdown()} method of all registered services.
     */
    public void shutdownAllServices() {

        String[] webservices = registeredServices.keySet().toArray(new String[registeredServices.size()]);

        for(String servicePath : webservices){
            shutdownService(servicePath);
        }

        //some time to send possible update notifications (404_NOT_FOUND) to observers
        try{
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
            log.error("Interrupted while shutting down CoapServerApplication!", e);
        }
    }


    /**
     * Shut down the {@link Webservice} instance registered at the given path from the server
     *
     * @param uriPath the path of the {@link Webservice} instance to be shut down
     *
     * @return <code>true</code> if the service was removed succesfully, <code>false</code> otherwise.
     */
    public synchronized boolean shutdownService(String uriPath) {
        Webservice removedService = registeredServices.remove(uriPath);

        if(removedService != null){
            log.info("Service {} removed from server.", uriPath);
            removedService.shutdown();
        }

        else{
            log.error("Service {} could not be removed. Does not exist.", uriPath);
        }

        return removedService == null;
    }

    public void setShutdown(){
        this.shutdown = true;
    }

    /**
     * Registers a Webservice instance at the server. After registration the service will be available at the path
     * given as <code>service.getUriPath()</code>.
     *
     * It is not possible to register multiple webServices at a single path. If a new service is registered at the
     * server with a path from another already registered service, then the new service replaces the old one.
     *
     * @param webservice A {@link Webservice} instance to be registered at the server
     */
    public final void registerService(final Webservice webservice) {
        webservice.setWebserviceManager(this);
        registeredServices.put(webservice.getUriPath(), webservice);
        log.info("Registered new service at " + webservice.getUriPath());

//        if(webservice instanceof ObservableWebservice){
//            ObservableWebserviceRegistrationEvent registrationEvent =
//                    new ObservableWebserviceRegistrationEvent((ObservableWebservice) webservice);
//
//            ChannelFuture future = Channels.write(channel, registrationEvent);
//            future.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    log.info("Registered {} at observable resource handler.", webservice.getUriPath());
//                }
//            });
//        }
    }

    public Channel getChannel(){
        return this.channel;
    }
}
