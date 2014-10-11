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
package de.uniluebeck.itm.ncoap.communication.dispatching.client;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.communication.events.AbstractMessageTransferEvent;
import de.uniluebeck.itm.ncoap.communication.events.client.ObservationCancelledEvent;
import de.uniluebeck.itm.ncoap.message.*;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>The {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallbackManager} is responsible for
 * processing inbound {@link de.uniluebeck.itm.ncoap.message.CoapResponse}s. That is why each
 * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} needs an associated instance of
 * {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback} to be called upon reception
 * of a related {@link de.uniluebeck.itm.ncoap.message.CoapResponse}.</p>
 *
 * <p>Besides the response dispatching the
 * {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallbackManager} also deals with
 * the reliability of inbound {@link de.uniluebeck.itm.ncoap.message.CoapResponse}s, i.e. sends RST or ACK
 * messages if necessary.</p>
 *
 * @author Oliver Kleine
 */
public class ClientCallbackManager extends SimpleChannelHandler{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private TokenFactory tokenFactory;

    private HashBasedTable<InetSocketAddress, Token, ClientCallback> clientCallbacks;
    private ReentrantReadWriteLock lock;

    private ScheduledExecutorService executor;

    /**
     * Creates a new instance of {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallbackManager}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to execute the tasks, e.g. send,
     *                 receive and process {@link de.uniluebeck.itm.ncoap.message.CoapMessage}s.
     *
     * @param tokenFactory the {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.TokenFactory} to
     *                     provide {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.Token}
     *                     instances for outbound {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s
     */
    public ClientCallbackManager(ScheduledExecutorService executor, TokenFactory tokenFactory){
        this.clientCallbacks = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
        this.executor = executor;
        this.tokenFactory = tokenFactory;
    }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me){

        if(me.getMessage() instanceof OutboundMessageWrapper){

            //Extract parameters for message transmission
            final CoapMessage coapMessage = ((OutboundMessageWrapper) me.getMessage()).getCoapMessage();
            final ClientCallback clientCallback = ((OutboundMessageWrapper) me.getMessage()).getClientCallback();

            final InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

            try {
                //request to stop an ongoing observation
                if(coapMessage instanceof CoapRequest && coapMessage.getObserve() == 1){
                    Token token = coapMessage.getToken();

                    if(!clientCallbacks.contains(remoteEndpoint, token)){
                        String description = "No ongoing observation on remote endpoint " + remoteEndpoint
                                + " and token " + token + "!";
                        clientCallback.processMiscellaneousError(description);
                    }

                    executor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            Token token = coapMessage.getToken();
                            ObservationCancelledEvent event = new ObservationCancelledEvent(remoteEndpoint, token,
                                    ObservationCancelledEvent.Reason.ACTIVE_CANCELLATION_BY_CLIENT);
                            Channels.write(ctx.getChannel(), event);
                        }
                    }, 0, TimeUnit.SECONDS);
                }

                //CoAP ping
                else if(coapMessage.getMessageTypeName() == MessageType.Name.CON &&
                        coapMessage.getMessageCodeName() == MessageCode.Name.EMPTY){

                    Token emptyToken = new Token(new byte[0]);
                    if(this.clientCallbacks.contains(remoteEndpoint, emptyToken)){
                        String description = "Empty token for remote endpoint " + remoteEndpoint + " not available.";
                        clientCallback.processMiscellaneousError(description);
                        return;
                    }

                    else{
                        coapMessage.setToken(emptyToken);
                    }
                }

                else{
                    //Prepare CoAP request, the response reception and then send the CoAP request
                    Token token = tokenFactory.getNextToken(remoteEndpoint);

                    if(token == null){
                        String description = "No token available for remote endpoint " + remoteEndpoint + ".";
                        clientCallback.processMiscellaneousError(description);
                        return;
                    }

                    else{
                        coapMessage.setToken(token);
                    }
                }

                //Add the response callback to wait for the inbound response
                addResponseCallback(remoteEndpoint, coapMessage.getToken(), clientCallback);

                //Send the request
                sendCoapMessage(ctx, me.getFuture(), coapMessage, remoteEndpoint);
                return;
            }
            catch (Exception ex) {
                log.error("This should never happen!", ex);
                removeClientCallback(remoteEndpoint, coapMessage.getToken());
            }
        }


//        else if (me.getMessage() instanceof ApplicationShutdownEvent){
//            this.clientCallbacks.clear();
//        }


        else if(me.getMessage() instanceof ObservationCancelledEvent){
            ObservationCancelledEvent message = (ObservationCancelledEvent) me.getMessage();

            if(removeClientCallback(message.getRemoteEndpoint(), message.getToken()) == null){
                log.error("Could not stop observation (remote endpoints: {}, token: {})! No callback found!",
                        message.getRemoteEndpoint(), message.getToken());
            }
        }

        ctx.sendDownstream(me);

    }


    private void sendCoapMessage(ChannelHandlerContext ctx, ChannelFuture future, final CoapMessage coapMessage,
                                 final InetSocketAddress remoteEndpoint){

        log.debug("Write CoAP request: {}", coapMessage);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(!future.isSuccess()){
                    removeClientCallback(remoteEndpoint, coapMessage.getToken());
                    log.error("Could not write CoAP Request!", future.getCause());
                }
            }
        });

        Channels.write(ctx, future, coapMessage, remoteEndpoint);
    }


    private void addResponseCallback(InetSocketAddress remoteEndpoint, Token token,
                                                  ClientCallback clientCallback){
        try{
            this.lock.readLock().lock();
            if(this.clientCallbacks.contains(remoteEndpoint, token)){
                log.error("Tried to use token twice (remote endpoint: {}, token: {})", remoteEndpoint, token);
                return;
            }
        }
        finally {
            this.lock.readLock().unlock();
        }

        try{
            this.lock.writeLock().lock();
            if(this.clientCallbacks.contains(remoteEndpoint, token)){
                log.error("Tried to use token twice (remote endpoint: {}, token: {})", remoteEndpoint, token);
            }
            else{
                clientCallbacks.put(remoteEndpoint, token, clientCallback);
                log.debug("Added callback (remote endpoint: {}, token: {})", remoteEndpoint, token);
            }
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }


    private ClientCallback removeClientCallback(InetSocketAddress remoteEndpoint, Token token){
        try{
            this.lock.readLock().lock();
            if(!this.clientCallbacks.contains(remoteEndpoint, token)){
                log.warn("No callback found to be removed (remote endpoint: {}, token: {})", remoteEndpoint, token);
                return null;
            }
        }
        finally {
            this.lock.readLock().unlock();
        }

        try{
            this.lock.writeLock().lock();
            ClientCallback callback = clientCallbacks.remove(remoteEndpoint, token);
            if(callback == null){
                log.warn("No callback found to be removed (remote endpoint: {}, token: {})", remoteEndpoint, token);
            }
            else{
                log.info("Removed callback (remote endpoint: {}, token: {}). Remaining: {}",
                        new Object[]{remoteEndpoint, token, this.clientCallbacks.size()});
            }
            return callback;
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * This method is automatically invoked by the framework and relates inbound responses to open requests and invokes
     * the appropriate method of the {@link ClientCallback} instance given with the {@link CoapRequest}.
     *
     * The invoked method depends on the message contained in the {@link org.jboss.netty.channel.MessageEvent}. See
     * {@link ClientCallback} for more details on possible status updates.
     *
     * @param ctx The {@link org.jboss.netty.channel.ChannelHandlerContext} to relate this handler to the {@link org.jboss.netty.channel.Channel}
     * @param me The {@link org.jboss.netty.channel.MessageEvent} containing the {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Received: {}.", me.getMessage());

        //regular response
        if(me.getMessage() instanceof CoapResponse){
            handleCoapResponse(ctx, (CoapResponse) me.getMessage(), (InetSocketAddress) me.getRemoteAddress());
        }

        //some internal event related to a message exchange
        else if(me.getMessage() instanceof AbstractMessageTransferEvent){
            handleMessageExchangeEvent((AbstractMessageTransferEvent) me.getMessage());
        }

        else if(me.getMessage() instanceof CoapMessage){
            CoapMessage coapMessage = ((CoapMessage) me.getMessage());

            //CoAP ping
            if(coapMessage.getMessageTypeName() == MessageType.Name.CON
                    && coapMessage.getMessageCodeName() == MessageCode.Name.EMPTY){

                InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
                int messageID = coapMessage.getMessageID();
                log.info("CoAP PING received (remote endpoint: {}, message ID: {}", remoteEndpoint, messageID);
                CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
                Channels.write(ctx.getChannel(), resetMessage, remoteEndpoint);
            }

            else{
                log.warn("Don't know what to do with received CoAP message: {}", coapMessage);
            }
        }

        else
            log.warn("Could not deal with message: {}", me.getMessage());
    }


    private void handleMessageExchangeEvent(AbstractMessageTransferEvent event) {
       ClientCallback clientCallback;

       //find the response processor for the inbound events
       if(event.stopsMessageExchange())
           clientCallback = clientCallbacks.remove(event.getRemoteEndpoint(), event.getToken());
       else
           clientCallback = clientCallbacks.get(event.getRemoteEndpoint(), event.getToken());

       //process the events
       if(clientCallback != null)
           clientCallback.processMessageExchangeEvent(event);
       else
           log.warn("No callback found for event: {}!", event);
   }


    private void handleCoapResponse(ChannelHandlerContext ctx, CoapResponse coapResponse,
                                    InetSocketAddress remoteEndpoint){

        log.debug("CoAP response received: {}.", coapResponse);
        Token token = coapResponse.getToken();

        //send RST if the received response could not be related to an open request
        if(!clientCallbacks.contains(remoteEndpoint, token)){
            log.warn("No callback found for CoAP response (from {}): {}", remoteEndpoint , coapResponse);

            //Send RST message
            CoapMessage resetMessage = CoapMessage.createEmptyReset(coapResponse.getMessageID());
            Channels.write(ctx.getChannel(), resetMessage, remoteEndpoint);
            return;
        }

        //send empty ACK if the response was confirmable and an appropriate callback was found
        else if(coapResponse.getMessageTypeName() == MessageType.Name.CON){
            //Send empty ACK
            CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(coapResponse.getMessageID());
            Channels.write(ctx.getChannel(), emptyACK, remoteEndpoint);
        }

        final ClientCallback clientCallback = clientCallbacks.get(remoteEndpoint, token);

        //observation callback found
        if(clientCallback != null && clientCallback.isObserving()){

            if(MessageCode.isErrorMessage(coapResponse.getMessageCode()) || !coapResponse.isUpdateNotification()){
                if(log.isInfoEnabled()){
                    if(MessageCode.isErrorMessage(coapResponse.getMessageCode())){
                        log.info("Observation callback removed because of error response!");
                    }
                    else{
                        log.info("Observation callback removed because inbound response was no update notification!");
                    }
                }

                removeClientCallback(remoteEndpoint, token);
                tokenFactory.passBackToken(remoteEndpoint, token);
            }

            //ask the callback if the observation is to be continued
            else if(!clientCallback.continueObservation()){
                //Send internal message to stop the observation
                ObservationCancelledEvent event = new ObservationCancelledEvent(remoteEndpoint, token,
                        ObservationCancelledEvent.Reason.LAZY_CANCELLATION_BY_CLIENT);
                log.debug("Send observation cancelation event!");
                Channels.write(ctx.getChannel(), event);
            }
        }

        //non-observation callback found
        else{
            removeClientCallback(remoteEndpoint, token);
            tokenFactory.passBackToken(remoteEndpoint, token);
        }


        if(clientCallback != null){
            //Process the CoAP response
            log.debug("Callback found for token {} from {}.", token, remoteEndpoint);
            clientCallback.processCoapResponse(coapResponse);
        }
        else{
            log.warn("No callback found for CoAP response (from {}): {}", remoteEndpoint , coapResponse);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee){
        log.error("Exception: ", ee.getCause());
    }

}
