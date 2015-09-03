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

package de.uzl.itm.ncoap.communication.dispatching.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.webresource.Webresource;
import de.uzl.itm.ncoap.application.server.webresource.WellKnownCoreResource;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.MessageTransferEvent;
import de.uzl.itm.ncoap.communication.events.ConversationFinishedEvent;
import de.uzl.itm.ncoap.communication.identification.EndpointID;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.options.OptionValue;
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
* The {@link WebresourceManager} is the topmost {@link ChannelHandler} of the {@link ChannelPipeline} returned
* by {@link de.uzl.itm.ncoap.application.server.ServerChannelPipelineFactory}. It is responsible to dispatch inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s, i.e.
*
* <ul>
*     <li>invoke the method {@link de.uzl.itm.ncoap.application.server.webresource.Webresource#processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)} of
*     the addressed {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance (if it exists) or</li>
*     <li>invoke the method
*     {@link NotFoundHandler#processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)} if the
*     inbound {@link de.uzl.itm.ncoap.message.CoapRequest} addresses a service that does not (yet) exist.
*     </li>
* </ul>
*
* Upon invocation of the method it awaits a proper {@link de.uzl.itm.ncoap.message.CoapResponse} and sends that response downstream, i.e.
* in the direction of the local socket, i.e. to the client that sent the {@link de.uzl.itm.ncoap.message.CoapRequest}.
*
* However, the {@link WebresourceManager} is aware of all registered {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instances and is thus to be
* used to register new {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instances, e.g. while processing an inbound {@link de.uzl.itm.ncoap.message.CoapRequest} with
* {@link de.uzl.itm.ncoap.message.MessageCode.Name#POST}. That is why all {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instances can reference their
* {@link WebresourceManager} via {@link de.uzl.itm.ncoap.application.server.webresource.Webresource#getWebresourceManager()}.
*
* Last but not least it checks whether the {@link de.uzl.itm.ncoap.message.options.OptionValue.Name#IF_NONE_MATCH} is set on inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s
* and sends a {@link de.uzl.itm.ncoap.message.CoapResponse} with {@link de.uzl.itm.ncoap.message.MessageCode.Name#PRECONDITION_FAILED_412} if the option was set but the
* addressed {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} already exists.
*
* @author Oliver Kleine
*/
public class WebresourceManager extends SimpleChannelUpstreamHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //This map holds all registered webservices (key: URI path, value: Webservice instance)
    private Map<String, Webresource> registeredServices;

    private HashBasedTable<InetSocketAddress, Token, ObservableWebresource> observations;
    private ReentrantReadWriteLock observationsLock;

    private ScheduledExecutorService executor;
    private NotFoundHandler webServiceNotFoundHandler;
    private Channel channel;
    private boolean shutdown;

    /**
     * @param webServiceNotFoundHandler Instance of {@link NotFoundHandler} to deal with inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s with
     *                          {@link de.uzl.itm.ncoap.message.MessageCode.Name#PUT} if the addresses {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} does not exist.
     *
     * @param executor the {@link ScheduledExecutorService} to process the task to send a {@link de.uzl.itm.ncoap.message.CoapResponse}
     *                        and
     */
    public WebresourceManager(NotFoundHandler webServiceNotFoundHandler, ScheduledExecutorService executor){

        this.registeredServices = Collections.synchronizedMap(new HashMap<String, Webresource>());
        this.executor = executor;
        this.webServiceNotFoundHandler = webServiceNotFoundHandler;
        this.shutdown = false;
        this.observations = HashBasedTable.create();
        this.observationsLock = new ReentrantReadWriteLock();

        registerWebresource(new WellKnownCoreResource(registeredServices, executor));
    }


    /**
     * This method is called by the framework to enable the {@link WebresourceManager} to send messages
     * to other handlers in the {@link ChannelPipeline}.
     *
     * @param channel the {@link Channel} to be used for messages to be sent to other handlers in that channel
     */
    public void setChannel(Channel channel){
        this.channel = channel;
    }


    /**
     * This method is called by the Netty framework whenever a new message is received to be processed. If the
     * received messages was a {@link de.uzl.itm.ncoap.message.CoapRequest}, this method deals with the handling of that request,
     * e.g. by invoking the method
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource#processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)} of the addressed
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance.
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
                ObservableWebresource webservice = this.observations.remove(remoteEndpoint, token);
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
                ObservableWebresource webservice = this.observations.get(remoteEndpoint, token);
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
        final Webresource webresource = registeredServices.get(coapRequest.getUriPath());

        if(coapRequest.getObserve() == 1 && webresource instanceof ObservableWebresource){
            Token token = coapRequest.getToken();
            if(((ObservableWebresource) webresource).removeObservation(remoteEndpoint, token)){
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
                        if(webresource instanceof ObservableWebresource && coapRequest.getObserve() == 0){
                            ObservableWebresource observableWebservice = (ObservableWebresource) webresource;
                            observableWebservice.addObservation(remoteEndpoint, coapResponse.getToken(),
                                    coapResponse.getContentFormat(), coapRequest.getEndpointID1());
                            sendUpdateNotification(ctx, remoteEndpoint, coapResponse, observableWebservice);
                        }
                        else{
                            coapResponse.removeOptions(OptionValue.Name.OBSERVE);
                            log.warn("Removed observe option from response!");
                            sendCoapResponse(ctx, remoteEndpoint, coapResponse);
                            sendConversationFinished(ctx.getChannel(), remoteEndpoint, coapResponse.getToken(),
                                coapResponse.getMessageID(), coapRequest.getEndpointID2());
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

                    sendConversationFinished(ctx.getChannel(), remoteEndpoint, errorResponse.getToken(),
                            errorResponse.getMessageID(), coapRequest.getEndpointID2());
                }
            }
        }, executor);

        try{
            //The requested Webservice does not exist
            if(webresource == null) {
                webServiceNotFoundHandler.processCoapRequest(responseFuture, coapRequest, remoteEndpoint);
            }

            //The IF-NON-MATCH option indicates that the request is only to be processed if the webresource does not
            //(yet) exist. But it does. So send an error response
            else if(coapRequest.isIfNonMatchSet()) {
                sendPreconditionFailed(coapRequest.getMessageTypeName(), coapRequest.getUriPath(), responseFuture);

                sendConversationFinished(ctx.getChannel(), remoteEndpoint, coapRequest.getToken(),
                    coapRequest.getMessageID(), coapRequest.getEndpointID2());
            }

            //The inbound request is to be handled by the addressed service
            else {
                webresource.processCoapRequest(responseFuture, coapRequest, remoteEndpoint);
            }

        }
        catch (Exception e) {
            log.error("This should never happen.", e);
            responseFuture.setException(e);
        }
    }

    // requests, update notifications and any (blockwise) messages not containing a last block are final messages
    private void sendConversationFinished(Channel channel, InetSocketAddress remoteSocket, Token token,
            int messageID, byte[] endpointID) {

        ConversationFinishedEvent event = new ConversationFinishedEvent(remoteSocket, messageID, token, endpointID);
        Channels.write(channel, event);
    }



    private void sendCoapResponse(final ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
                                  final CoapResponse coapResponse){

        //Write response
        log.info("Write response to {}: {}", remoteAddress, coapResponse);
        Channels.write(ctx.getChannel(), coapResponse, remoteAddress);
    }


    private void sendUpdateNotification(ChannelHandlerContext ctx, InetSocketAddress remoteAddress,
                                        CoapResponse updateNotification, ObservableWebresource webservice){

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
     * Classes extending {@link de.uzl.itm.ncoap.application.server.CoapServerApplication} may override this method to, e.g. deal with different
     * types of exceptions differently. This implementation only logs an the error message (if logging is
     * enabled).
     *
     * @param ctx the {@link ChannelHandlerContext} relating the {@link de.uzl.itm.ncoap.application.server.CoapServerApplication} and the
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
     * Prior to doing so this methods removes all registered {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instances from the server, i.e.
     * invokes the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource#shutdown()} method of all registered services.
     */
    public void shutdownAllServices() {

        String[] webservices = registeredServices.keySet().toArray(new String[registeredServices.size()]);

        for(String servicePath : webservices){
            shutdownWebresource(servicePath);
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
     * Shut down the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance registered at the given path from the server
     *
     * @param uriPath the path of the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance to be shut down
     *
     * @return <code>true</code> if the service was removed succesfully, <code>false</code> otherwise.
     */
    public synchronized boolean shutdownWebresource(String uriPath) {
        Webresource removedService = registeredServices.remove(uriPath);

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
     * @param webresource A {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance to be registered at the server
     *
     * @throws java.lang.IllegalArgumentException if there was already a
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} registered with the same path
     */
    public final void registerWebresource(final Webresource webresource) throws IllegalArgumentException{
        if(registeredServices.containsKey(webresource.getUriPath())){
            throw new IllegalArgumentException("Service " + webresource.getUriPath() + " is already registered");
        }

        webresource.setWebresourceManager(this);
        registeredServices.put(webresource.getUriPath(), webresource);
        log.info("Registered new service at " + webresource.getUriPath());
    }

    /**
     * Removes the resource from the list of registered resources but DOES NOT invoke
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource#shutdown()}
     *
     * @param webresource the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} to be unregistered.
     */
    public final void unregisterWebresource(Webresource webresource){
        registeredServices.remove(webresource.getUriPath());
    }

    public Channel getChannel(){
        return this.channel;
    }
}
