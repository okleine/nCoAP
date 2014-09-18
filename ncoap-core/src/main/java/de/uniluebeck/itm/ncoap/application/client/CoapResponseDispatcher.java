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
package de.uniluebeck.itm.ncoap.application.client;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.application.ApplicationShutdownEvent;
import de.uniluebeck.itm.ncoap.communication.observe.client.ClientStopsObservationEvent;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link CoapResponseDispatcher} is responsible for processing incoming {@link CoapResponse}s. Each
 * {@link CoapRequest} needs an associated instance of {@link CoapClientCallback}
 *
 * @author Oliver Kleine
 */
public class CoapResponseDispatcher extends SimpleChannelHandler{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private TokenFactory tokenFactory;

    private HashBasedTable<InetSocketAddress, Token, CoapClientCallback> clientCallbacks;

    private ScheduledExecutorService executorService;


    public CoapResponseDispatcher(ScheduledExecutorService executorService, TokenFactory tokenFactory){
        this.clientCallbacks = HashBasedTable.create();
        this.executorService = executorService;
        this.tokenFactory = tokenFactory;
    }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me){

        if(me.getMessage() instanceof OutgoingCoapMessageWrapper){
            executorService.schedule(new Runnable() {
                @Override
                public void run() {

                    //Extract parameters for message transmission
                    final CoapMessage coapMessage =
                            ((OutgoingCoapMessageWrapper) me.getMessage()).getCoapMessage();

                    final CoapClientCallback coapClientCallback =
                            ((OutgoingCoapMessageWrapper) me.getMessage()).getCoapClientCallback();

//                    //Requests with an observe option need an instance of UpdateNotificationProcessor!
//                    if(coapMessage instanceof CoapRequest
//                            && ((CoapRequest) coapMessage).getObserve() != UintOptionValue.UNDEFINED){
//
//                        if(!(coapClientCallback instanceof UpdateNotificationProcessor)){
//                            log.warn("Given instance of CoapClientCallback is no instance of " +
//                                    "UpdateNotificationProcessor! Remove OBSERVE option from request!");
//                            coapMessage.removeOptions(OptionValue.Name.OBSERVE);
//                        }
//                    }

                    final InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

                    try {
                        //Prepare CoAP request, the response reception and then send the CoAP request
                        Token token = tokenFactory.getNextToken(remoteEndpoint);

                        if(token == null){
                            NoTokenAvailableEvent event = new NoTokenAvailableEvent(remoteEndpoint);
                            coapClientCallback.processMessageExchangeEvent(event);
                        }

                        else{
                            coapMessage.setToken(token);

                            //Add the response callback to wait for the incoming response
                            addResponseCallback(remoteEndpoint, coapMessage.getToken(), coapClientCallback);

                            //Send the request
                            sendCoapMessage(ctx, me.getFuture(), coapMessage, remoteEndpoint);
                        }
                    }
                    catch (Exception ex) {
                        log.error("This should never happen!", ex);
                        removeResponseCallback(remoteEndpoint, coapMessage.getToken());
                    }
                }

            }, 0, TimeUnit.MILLISECONDS);
        }

        else if (me.getMessage() instanceof ApplicationShutdownEvent){
            executorService.schedule(new Runnable(){

                @Override
                public void run() {
                    CoapResponseDispatcher.this.clientCallbacks.clear();
                    ctx.sendDownstream(me);
                }

            }, 0, TimeUnit.MILLISECONDS);
        }


        else if(me.getMessage() instanceof ClientStopsObservationEvent){
            ClientStopsObservationEvent message = (ClientStopsObservationEvent) me.getMessage();

            if(removeResponseCallback(message.getRemoteEndpoint(), message.getToken()) == null){
                log.error("Could not stop observation (remote endpoints: {}, token: {})! No response processor found!",
                        message.getRemoteEndpoint(), message.getToken());

                me.getFuture().setFailure(new Exception("Observation could not be stopped!"));
            }

            else{
                ctx.sendDownstream(me);
            }

        }


        else{
            executorService.schedule(new Runnable(){

                @Override
                public void run() {
                    ctx.sendDownstream(me);
                }

            }, 0, TimeUnit.MILLISECONDS);
        }
    }



    private void sendCoapMessage(ChannelHandlerContext ctx, ChannelFuture future, final CoapMessage coapMessage,
                                 InetSocketAddress remoteEndpoint){

        log.debug("Write CoAP request: {}", coapMessage);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    log.info("Written: {}", coapMessage);
                }

                else{
//                    Throwable cause = future.getCause();
//
//                    if(cause instanceof NoMessageIDAvailableException){
//                        NoMessageIDAvailableException exception = (NoMessageIDAvailableException) cause;
//
//                        InetSocketAddress remoteEndpoint = exception.getRemoteEndpoint();
//                        Token token = exception.getToken();
//
//                        CoapClientCallback callback = removeResponseCallback(remoteEndpoint, token);
//
//                        if(callback != null && callback instanceof NoMessageIDAvailableProcessor){
//                            ((NoMessageIDAvailableProcessor) callback).handleNoMessageIDAvailable(
//                                    remoteEndpoint, exception.getWaitingPeriod()
//                            );
//                        }
//                        else{
//                            log.error("Removed response callback for token {} for {}", token, remoteEndpoint);
//                        }
//                    }

                    log.error("Could not write CoAP Request!", future.getCause());
                }
            }
        });

        Channels.write(ctx, future, coapMessage, remoteEndpoint);
    }


    private synchronized void addResponseCallback(InetSocketAddress remoteEndpoint, Token token,
                                                  CoapClientCallback coapClientCallback){

        if(!(clientCallbacks.put(remoteEndpoint, token, coapClientCallback) == null))
            log.error("Tried to use token {} for {} twice!", token, remoteEndpoint);
        else
            log.debug("Added response processor for token {} from {}.", token,
                    remoteEndpoint);
    }


    private synchronized CoapClientCallback removeResponseCallback(InetSocketAddress remoteEndpoint,
                                                                      Token token){

        CoapClientCallback result = clientCallbacks.remove(remoteEndpoint, token);

        if(result != null)
            log.debug("Removed response processor for token {} from {}.", token,
                    remoteEndpoint);

        log.debug("Number of clients waiting for response: {}. ", clientCallbacks.size());
        return result;
    }

    /**
     * This method is automatically invoked by the framework and relates incoming responses to open requests and invokes
     * the appropriate method of the {@link CoapClientCallback} instance given with the {@link CoapRequest}.
     *
     * The invoked method depends on the message contained in the {@link org.jboss.netty.channel.MessageEvent}. See
     * {@link CoapClientCallback} for more details on possible status updates.
     *
     * @param ctx The {@link org.jboss.netty.channel.ChannelHandlerContext} to relate this handler to the {@link org.jboss.netty.channel.Channel}
     * @param me The {@link org.jboss.netty.channel.MessageEvent} containing the {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Received: {}.", me.getMessage());

        if(me.getMessage() instanceof CoapResponse)
            handleCoapResponse(ctx, (CoapResponse) me.getMessage(), (InetSocketAddress) me.getRemoteAddress());

        else if(me.getMessage() instanceof MessageExchangeEvent)
            handleMessageExchangeEvent((MessageExchangeEvent) me.getMessage());

        else
            log.warn("Could not deal with message: {}", me.getMessage());
    }


    private void handleMessageExchangeEvent(MessageExchangeEvent event) {
        CoapClientCallback responseProcessor;

        //find the response processor for the incoming event
        if(event.isError())
            responseProcessor = clientCallbacks.remove(event.getRemoteEndpoint(), event.getToken());
        else
            responseProcessor = clientCallbacks.get(event.getRemoteEndpoint(), event.getToken());

        //process the event
        if(responseProcessor != null)
            responseProcessor.processMessageExchangeEvent(event);
        else
            log.warn("No response processor found for event: {}!", event);
    }


    private void handleCoapResponse(ChannelHandlerContext ctx, CoapResponse coapResponse,
                                    InetSocketAddress remoteEndpoint){

        log.debug("CoAP response received: {}.", coapResponse);
        Token token = coapResponse.getToken();

        if(!clientCallbacks.contains(remoteEndpoint, token)){

            log.warn("No response processor found for CoAP response from {} ({})", remoteEndpoint , coapResponse);

            //Send RST message
            CoapMessage resetMessage = CoapMessage.createEmptyReset(coapResponse.getMessageID());
            Channels.write(ctx.getChannel(), resetMessage, remoteEndpoint);
            return;
        }

        final CoapClientCallback clientCallback;

        //if the response is a (regular, i.e. no error) update notification, keep the response processor in use
        if(coapResponse.isUpdateNotification() && !MessageCode.isErrorMessage(coapResponse.getMessageCode())){
            clientCallback = clientCallbacks.get(remoteEndpoint, token);

            if(!clientCallback.continueObservation(remoteEndpoint, token)){
                removeResponseCallback(remoteEndpoint, token);

                if(!tokenFactory.passBackToken(remoteEndpoint, token))
                    log.error("Could not pass back token from message: {}", coapResponse);

                //Send internal message to stop the observation
                ClientStopsObservationEvent event = new ClientStopsObservationEvent(remoteEndpoint, token);
                Channels.write(ctx.getChannel(), event);
            }
        }

        //for regular responses, i.e. no update notifications, remove the response processor and pass back the token
        else{
            clientCallback = removeResponseCallback(remoteEndpoint, token);
            if(!tokenFactory.passBackToken(remoteEndpoint, token))
                log.error("Could not pass back token from message: {}", coapResponse);
        }

        //Process the CoAP response
        log.debug("Callback found for token {} from {}.", token, remoteEndpoint);
        clientCallback.processCoapResponse(coapResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee){
        log.error("Exception: ", ee.getCause());
    }

}
