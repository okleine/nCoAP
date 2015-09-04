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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.webresource.Webresource;
import de.uzl.itm.ncoap.application.server.webresource.WellKnownCoreResource;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.MessageExchangeFinishedEvent;
import de.uzl.itm.ncoap.communication.events.ObservableWebresourceRegistrationEvent;
import de.uzl.itm.ncoap.communication.events.ObserverAcceptedEvent;
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
public class WebresourceManager extends AbstractCoapChannelHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //This map holds all registered webresources (key: URI path, value: Webservice instance)
    private Map<String, Webresource> registeredServices;

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
        registerWebresource(new WellKnownCoreResource(registeredServices, executor));
    }

    @Override
    public boolean handleInboundCoapMessage(final ChannelHandlerContext ctx, CoapMessage coapMessage,
            final InetSocketAddress remoteSocket) {

        if(!(coapMessage instanceof CoapRequest)) {
            return true;
        }

        final CoapRequest coapRequest = (CoapRequest) coapMessage;

        //Create settable future to wait for response
        final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();

        //Look up web service instance to handle the request
        final Webresource webresource = registeredServices.get(coapRequest.getUriPath());


        if(webresource == null) {
            // the requested Webservice DOES NOT exist
            webServiceNotFoundHandler.processCoapRequest(responseFuture, coapRequest, remoteSocket);
        } else if (coapRequest.isIfNonMatchSet()) {
                createPreconditionFailed(coapRequest.getMessageTypeName(), coapRequest.getUriPath(), responseFuture);
        } else {
            // the requested Webservice DOES exist
            try {
                webresource.processCoapRequest(responseFuture, coapRequest, remoteSocket);
            } catch (Exception ex){
                responseFuture.setException(ex);
            }
        }

        Futures.addCallback(responseFuture, new FutureCallback<CoapResponse>() {
            @Override
            public void onSuccess(CoapResponse coapResponse) {
                coapResponse.setMessageID(coapRequest.getMessageID());
                coapResponse.setToken(coapRequest.getToken());

                if (coapResponse.isUpdateNotification()) {
                    if (webresource instanceof ObservableWebresource && coapRequest.getObserve() == 0) {
                        // send new observer accepted event
                        ObserverAcceptedEvent event = new ObserverAcceptedEvent(
                                remoteSocket, coapResponse.getMessageID(), coapResponse.getToken(),
                                (ObservableWebresource) webresource, coapResponse.getContentFormat()
                        );
                        Channels.write(ctx.getChannel(), event);
                    } else {
                        // the observe option is useless here (remove it)...
                        coapResponse.removeOptions(OptionValue.Name.OBSERVE);
                        log.warn("Removed observe option from response!");
                    }
                }

                sendCoapResponse(ctx, remoteSocket, coapResponse);

                if (!coapResponse.isUpdateNotification()) {
                    sendConversationFinished(
                            ctx.getChannel(), remoteSocket, coapResponse.getToken(), coapResponse.getMessageID(),
                            coapRequest.getEndpointID2()
                    );
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Exception while processing inbound request", throwable);
                CoapResponse errorResponse = CoapResponse.createErrorResponse(coapRequest.getMessageTypeName(),
                        MessageCode.Name.INTERNAL_SERVER_ERROR_500, throwable.getMessage());

                errorResponse.setMessageID(coapRequest.getMessageID());
                errorResponse.setToken(coapRequest.getToken());

                sendCoapResponse(ctx, remoteSocket, errorResponse);

                sendConversationFinished(ctx.getChannel(), remoteSocket, errorResponse.getToken(),
                        errorResponse.getMessageID(), coapRequest.getEndpointID2());
            }
        }, this.executor);

        return true;
    }

    @Override
    public boolean handleOutboundCoapMessage(ChannelHandlerContext ctx, CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        return true;
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


    // requests, update notifications and any (blockwise) messages not containing a last block are final messages
    private void sendConversationFinished(Channel channel, InetSocketAddress remoteSocket, Token token,
            int messageID, byte[] endpointID) {

        MessageExchangeFinishedEvent event = new MessageExchangeFinishedEvent(remoteSocket, messageID, token, endpointID);
        Channels.write(channel, event);
    }



    private void sendCoapResponse(final ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
                                  final CoapResponse coapResponse){

        //Write response
        log.error("Write response to {}: {}", remoteAddress, coapResponse);
        Channels.write(ctx.getChannel(), coapResponse, remoteAddress);
    }


    private void createPreconditionFailed(MessageType.Name messageType, String servicePath,
            SettableFuture<CoapResponse> responseFuture) {

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
     * Shuts the server down by closing the datagramChannel which includes to unbind the datagramChannel from a
     * listening port and by this means free the port. All blocked or bound external resources are released.
     *
     * Prior to doing so this methods removes all registered
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instances from the server, i.e.
     * invokes the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource#shutdown()} method of all registered services.
     */
    public void shutdown() {
        this.shutdown = true;
        String[] webservices = registeredServices.keySet().toArray(new String[registeredServices.size()]);
        for(String servicePath : webservices){
            shutdownWebresource(servicePath);
        }

        // some time to send possible update notifications (404_NOT_FOUND) to observers
        try{
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("Interrupted while shutting down Webresource Manager!", e);
        }
    }


    /**
     * Shut down the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance registered at the
     * given path from the server
     *
     * @param uriPath the path of the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance to
     * be shut down
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

        if(webresource instanceof ObservableWebresource){
            Channels.write(
                this.channel, new ObservableWebresourceRegistrationEvent((ObservableWebresource) webresource)
            );
        }
    }

    public Channel getChannel(){
        return this.channel;
    }


}
