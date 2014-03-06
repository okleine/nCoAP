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
import de.uniluebeck.itm.ncoap.application.NoTokenAvailableException;
import de.uniluebeck.itm.ncoap.application.NoTokenAvailableProcessor;
import de.uniluebeck.itm.ncoap.application.Token;
import de.uniluebeck.itm.ncoap.application.TokenFactory;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.*;
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
 * Created by olli on 08.02.14.
 */
public class CoapResponseDispatcher extends SimpleChannelHandler{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private TokenFactory tokenFactory;

    private HashBasedTable<InetSocketAddress, Token, CoapResponseProcessor> responseProcessors;

    private ScheduledExecutorService scheduledExecutorService;


    public CoapResponseDispatcher(ScheduledExecutorService scheduledExecutorService, TokenFactory tokenFactory){
        this.responseProcessors = HashBasedTable.create();
        this.scheduledExecutorService = scheduledExecutorService;
        this.tokenFactory = tokenFactory;
    }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me){

        if(!(me.getMessage() instanceof InternalSendCoapRequestMessage)){
            ctx.sendDownstream(me);
            return;
        }

        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                //Extract parameters for message transmission
                final CoapRequest coapRequest =
                        ((InternalSendCoapRequestMessage) me.getMessage()).getCoapRequest();

                final CoapResponseProcessor coapResponseProcessor =
                        ((InternalSendCoapRequestMessage) me.getMessage()).getCoapResponseProcessor();

                final InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();

                try {
                    //Prepare CoAP request, the response reception and then send the CoAP request
                    coapRequest.setToken(tokenFactory.getNextToken(remoteSocketAddress));

                    //Add the response callback to wait for the incoming response
                    addResponseCallback(remoteSocketAddress, coapRequest.getToken(), coapResponseProcessor);

                    //Send the request
                    sendCoapRequest(ctx, me.getFuture(), coapRequest, remoteSocketAddress);

                }
                catch (NoTokenAvailableException e) {
                    if(coapResponseProcessor instanceof NoTokenAvailableProcessor){
                        NoTokenAvailableProcessor processor = (NoTokenAvailableProcessor) coapResponseProcessor;
                        processor.processNoTokenAvailable(remoteSocketAddress);
                    }
                }
                catch(Exception e){
                    log.error("This should never happen!", e);
                    removeResponseCallback(remoteSocketAddress, coapRequest.getToken());
                }
            }

        }, 0, TimeUnit.MILLISECONDS);
    }



    private void sendCoapRequest(ChannelHandlerContext ctx, ChannelFuture future, CoapRequest coapRequest,
                                 InetSocketAddress remoteSocketAddress){

        log.info("Write CoAP request: {}", coapRequest);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(!future.isSuccess()){
                    Throwable cause = future.getCause();

                    if(cause instanceof NoMessageIDAvailableException){
                        NoMessageIDAvailableException exception = (NoMessageIDAvailableException) cause;

                        InetSocketAddress remoteSocketAddress = exception.getRemoteSocketAddress();
                        Token token = exception.getToken();

                        CoapResponseProcessor callback = removeResponseCallback(remoteSocketAddress, token);

                        if(callback != null && callback instanceof NoMessageIDAvailableProcessor){
                            ((NoMessageIDAvailableProcessor) callback).handleNoMessageIDAvailable(
                                    remoteSocketAddress, exception.getWaitingPeriod()
                            );
                        }
                        else{
                            log.error("Removed response callback for token {} for {}", token, remoteSocketAddress);
                        }

                    }
                    else{
                        log.error("Unexpected Exception:", cause);
                    }
                }
            }
        });

        Channels.write(ctx, future, coapRequest, remoteSocketAddress);
    }


    private synchronized void addResponseCallback(InetSocketAddress remoteSocketAddress, Token token,
                                                  CoapResponseProcessor coapResponseProcessor){

        if(!(responseProcessors.put(remoteSocketAddress, token, coapResponseProcessor) == null))
            log.error("Tried to use token {} for {} twice!", token, remoteSocketAddress);
        else
            log.debug("Added response processor for token {} from {}.", token,
                    remoteSocketAddress);
    }


    private synchronized CoapResponseProcessor removeResponseCallback(InetSocketAddress remoteSocketAddress,
                                                                      Token token){

        CoapResponseProcessor result = responseProcessors.remove(remoteSocketAddress, token);

        if(result != null)
            log.debug("Removed response processor for token {} from {}.", token,
                    remoteSocketAddress);

        log.debug("Number of clients waiting for response: {}. ", responseProcessors.size());
        return result;
    }

    /**
     * This method is automatically invoked by the framework and relates incoming responses to open requests and invokes
     * the appropriate method of the {@link CoapResponseProcessor} instance given with the {@link CoapRequest}.
     *
     * The invoked method depends on the message contained in the {@link org.jboss.netty.channel.MessageEvent}. See
     * {@link CoapResponseProcessor} for more details on possible status updates.
     *
     * @param ctx The {@link org.jboss.netty.channel.ChannelHandlerContext} to relate this handler to the {@link org.jboss.netty.channel.Channel}
     * @param me The {@link org.jboss.netty.channel.MessageEvent} containing the {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Received: {}.", me.getMessage());

        if(me.getMessage() instanceof InternalEmptyAcknowledgementReceivedMessage){
            InternalEmptyAcknowledgementReceivedMessage message =
                    (InternalEmptyAcknowledgementReceivedMessage) me.getMessage();

            //find proper callback
            CoapResponseProcessor callback =
                    responseProcessors.get(me.getRemoteAddress(), message.getToken());

            if(callback != null && callback instanceof EmptyAcknowledgementProcessor){
                log.debug("Found processor for {}", message);
                ((EmptyAcknowledgementProcessor) callback).processEmptyAcknowledgement(message);
            }
            else{
                log.debug("No processor found for {}", message);
            }

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalRetransmissionTimeoutMessage){
            InternalRetransmissionTimeoutMessage timeoutMessage = (InternalRetransmissionTimeoutMessage) me.getMessage();

            //Find proper callback
            CoapResponseProcessor callback =
                    removeResponseCallback(timeoutMessage.getRemoteAddress(), timeoutMessage.getToken());

            //Invoke method of callback instance
            if(callback != null && callback instanceof RetransmissionTimeoutProcessor)
                ((RetransmissionTimeoutProcessor) callback).processRetransmissionTimeout(timeoutMessage);

            //pass the token back
            if(!tokenFactory.passBackToken(timeoutMessage.getRemoteAddress(), timeoutMessage.getToken())){
                log.error("Could not pass back token {} for {} from retransmission timeout!",
                        timeoutMessage.getToken(), timeoutMessage.getRemoteAddress());
            }
            else{
                log.error("Passed back token from timeout message!");
            }

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalMessageRetransmittedMessage){
            InternalMessageRetransmittedMessage retransmissionMessage =
                    (InternalMessageRetransmittedMessage) me.getMessage();

            CoapResponseProcessor callback =
                    responseProcessors.get(retransmissionMessage.getRemoteAddress(), retransmissionMessage.getToken());

            if(callback != null && callback instanceof TransmissionInformationProcessor)
                ((TransmissionInformationProcessor) callback).messageTransmitted(true);

            return;
        }


        if(me.getMessage() instanceof CoapResponse){

            final CoapResponse coapResponse = (CoapResponse) me.getMessage();
            log.info("Response received: {}.", coapResponse);

            InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();

            final CoapResponseProcessor responseProcessor;

            if(coapResponse.isUpdateNotification() && !MessageCode.isErrorMessage(coapResponse.getMessageCode())){
                responseProcessor = responseProcessors.get(me.getRemoteAddress(), coapResponse.getToken());
            }
            else{
                responseProcessor = removeResponseCallback(remoteSocketAddress, coapResponse.getToken());
                if(!tokenFactory.passBackToken(remoteSocketAddress, coapResponse.getToken()))
                    log.error("Could not pass back token from message: {}", coapResponse);
            }

            if(responseProcessor != null){
                log.debug("Callback found for token {}.", coapResponse.getToken());
                responseProcessor.processCoapResponse(coapResponse);
            }
            else{
                log.error("No response processor found for token {}. (Response: {})",
                        coapResponse.getToken(), coapResponse);

                log.debug("...");
            }

            me.getFuture().setSuccess();
        }
        else{
            me.getFuture().setFailure(new RuntimeException("Could not deal with message "
                    + me.getMessage().getClass().getName()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee){
        log.error("Exception: ", ee.getCause());
    }

}
