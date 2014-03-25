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
import de.uniluebeck.itm.ncoap.application.InternalApplicationShutdownMessage;
import de.uniluebeck.itm.ncoap.communication.codec.EncodingFailedProcessor;
import de.uniluebeck.itm.ncoap.communication.codec.InternalEncodingFailedMessage;
import de.uniluebeck.itm.ncoap.communication.observe.client.UpdateNotificationProcessor;
import de.uniluebeck.itm.ncoap.communication.observe.client.InternalStopObservationMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.*;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalMessageRetransmittedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link CoapResponseDispatcher} is responsible for processing incoming {@link CoapResponse}s. Each
 * {@link CoapRequest} needs an associated instance of {@link CoapResponseProcessor}
 *
 * @author Oliver Kleine
 */
public class CoapResponseDispatcher extends SimpleChannelHandler{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private TokenFactory tokenFactory;

    private HashBasedTable<InetSocketAddress, Token, CoapResponseProcessor> responseProcessors;

    private ScheduledExecutorService executorService;


    public CoapResponseDispatcher(ScheduledExecutorService executorService, TokenFactory tokenFactory){
        this.responseProcessors = HashBasedTable.create();
        this.executorService = executorService;
        this.tokenFactory = tokenFactory;
    }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me){

        if(me.getMessage() instanceof InternalWrappedOutgoingCoapMessage){
            executorService.schedule(new Runnable() {
                @Override
                public void run() {

                    //Extract parameters for message transmission
                    final CoapMessage coapMessage =
                            ((InternalWrappedOutgoingCoapMessage) me.getMessage()).getCoapMessage();

                    final CoapResponseProcessor coapResponseProcessor =
                            ((InternalWrappedOutgoingCoapMessage) me.getMessage()).getCoapResponseProcessor();

                    //Requests with an observe option need an instance of UpdateNotificationProcessor!
                    if(coapMessage instanceof CoapRequest && ((CoapRequest) coapMessage).isObserveSet()){
                        if(!(coapResponseProcessor instanceof UpdateNotificationProcessor))
                            coapMessage.removeOptions(OptionValue.Name.OBSERVE);
                    }

                    final InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

                    try {
                        //Prepare CoAP request, the response reception and then send the CoAP request
                        coapMessage.setToken(tokenFactory.getNextToken(remoteEndpoint));

                        //Add the response callback to wait for the incoming response
                        addResponseCallback(remoteEndpoint, coapMessage.getToken(), coapResponseProcessor);

                        //Send the request
                        sendCoapMessage(ctx, me.getFuture(), coapMessage, remoteEndpoint);

                    } catch (NoTokenAvailableException e) {
                        if (coapResponseProcessor instanceof NoTokenAvailableProcessor) {
                            NoTokenAvailableProcessor processor = (NoTokenAvailableProcessor) coapResponseProcessor;
                            processor.processNoTokenAvailable(remoteEndpoint);
                        }
                    } catch (Exception e) {
                        log.error("This should never happen!", e);
                        removeResponseCallback(remoteEndpoint, coapMessage.getToken());
                    }
                }

            }, 0, TimeUnit.MILLISECONDS);
        }

        else if (me.getMessage() instanceof InternalApplicationShutdownMessage){
            executorService.schedule(new Runnable(){

                @Override
                public void run() {
                    CoapResponseDispatcher.this.responseProcessors.clear();
                    ctx.sendDownstream(me);
                }

            }, 0, TimeUnit.MILLISECONDS);
        }


        else if(me.getMessage() instanceof InternalStopObservationMessage){
            InternalStopObservationMessage message = (InternalStopObservationMessage) me.getMessage();

            if(removeResponseCallback(message.getRemoteEndpoint(), message.getToken()) == null){
                log.error("Could not stop observation (remote endpoint: {}, token: {})! No response processor found!",
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
                    Throwable cause = future.getCause();

                    if(cause instanceof NoMessageIDAvailableException){
                        NoMessageIDAvailableException exception = (NoMessageIDAvailableException) cause;

                        InetSocketAddress remoteEndpoint = exception.getRemoteEndpoint();
                        Token token = exception.getToken();

                        CoapResponseProcessor callback = removeResponseCallback(remoteEndpoint, token);

                        if(callback != null && callback instanceof NoMessageIDAvailableProcessor){
                            ((NoMessageIDAvailableProcessor) callback).handleNoMessageIDAvailable(
                                    remoteEndpoint, exception.getWaitingPeriod()
                            );
                        }
                        else{
                            log.error("Removed response callback for token {} for {}", token, remoteEndpoint);
                        }
                    }

                    log.warn("Could not write CoAP Request!", cause);
                }
            }
        });

        Channels.write(ctx, future, coapMessage, remoteEndpoint);
    }


    private synchronized void addResponseCallback(InetSocketAddress remoteEndpoint, Token token,
                                                  CoapResponseProcessor coapResponseProcessor){

        if(!(responseProcessors.put(remoteEndpoint, token, coapResponseProcessor) == null))
            log.error("Tried to use token {} for {} twice!", token, remoteEndpoint);
        else
            log.debug("Added response processor for token {} from {}.", token,
                    remoteEndpoint);
    }


    private synchronized CoapResponseProcessor removeResponseCallback(InetSocketAddress remoteEndpoint,
                                                                      Token token){

        CoapResponseProcessor result = responseProcessors.remove(remoteEndpoint, token);

        if(result != null)
            log.debug("Removed response processor for token {} from {}.", token,
                    remoteEndpoint);

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

        if(me.getMessage() instanceof CoapResponse)
            handleCoapResponse(ctx, (CoapResponse) me.getMessage(), (InetSocketAddress) me.getRemoteAddress());

        else if(me.getMessage() instanceof InternalEmptyAcknowledgementReceivedMessage)
            handleEmptyAcknowledgement((InternalEmptyAcknowledgementReceivedMessage) me.getMessage());

        else if(me.getMessage() instanceof InternalResetReceivedMessage)
            handleReset((InternalResetReceivedMessage) me.getMessage());

        else if(me.getMessage() instanceof InternalRetransmissionTimeoutMessage)
            handleRetransmissionTimeoutMessage((InternalRetransmissionTimeoutMessage) me.getMessage());

        else if(me.getMessage() instanceof InternalMessageRetransmittedMessage)
            handleMessageRetransmittedMessage((InternalMessageRetransmittedMessage) me.getMessage());

        else if(me.getMessage() instanceof InternalEncodingFailedMessage)
            handleEncodingFailedMessage((InternalEncodingFailedMessage) me.getMessage());
        else
            log.warn("Could not deal with message: {}", me.getMessage());
    }


    private void handleEncodingFailedMessage(InternalEncodingFailedMessage message) {
        CoapResponseProcessor responseProcessor =
                removeResponseCallback(message.getRemoteEndoint(), message.getToken());

        if(responseProcessor == null){
            log.error("No response processor found for internal encoding failed message...");
            return;
        }

        if(responseProcessor instanceof EncodingFailedProcessor)
            ((EncodingFailedProcessor) responseProcessor).processEncodingFailed(message.getCause());

    }


    private void handleCoapResponse(ChannelHandlerContext ctx, CoapResponse coapResponse,
                                    InetSocketAddress remoteEndpoint){

        log.debug("CoAP response received: {}.", coapResponse);

        final CoapResponseProcessor responseProcessor;

        if(!responseProcessors.contains(remoteEndpoint, coapResponse.getToken())){

            log.warn("No response processor found for CoAP response from {} ({})", remoteEndpoint , coapResponse);

            //Send RST message
            CoapMessage resetMessage = CoapMessage.createEmptyReset(coapResponse.getMessageID());
            Channels.write(ctx.getChannel(), resetMessage, remoteEndpoint);
            return;
        }

        //if the response is a (regular, i.e. no error) update notification, keep the response processor in use
        if(coapResponse.isUpdateNotification() && !MessageCode.isErrorMessage(coapResponse.getMessageCode())){
            responseProcessor = responseProcessors.get(remoteEndpoint, coapResponse.getToken());

            if(!(responseProcessor instanceof UpdateNotificationProcessor)){
                removeResponseCallback(remoteEndpoint, coapResponse.getToken());

                if(!tokenFactory.passBackToken(remoteEndpoint, coapResponse.getToken()))
                    log.error("Could not pass back token from message: {}", coapResponse);
            }
        }

        //for regular responses, i.e. no update notifications, remove the response processor and pass back the token
        else{
            responseProcessor = removeResponseCallback(remoteEndpoint, coapResponse.getToken());
            if(!tokenFactory.passBackToken(remoteEndpoint, coapResponse.getToken()))
                log.error("Could not pass back token from message: {}", coapResponse);
        }

        //Process the CoAP response
        log.debug("Callback found for token {}.", coapResponse.getToken());
        responseProcessor.processCoapResponse(coapResponse);


        //if the observation is not to be continued remove the response processor
        if(responseProcessor instanceof UpdateNotificationProcessor &&
                !((UpdateNotificationProcessor) responseProcessor).continueObservation()){

            //Send internal message to stop the observation
            InternalStopObservationMessage internalMessage =
                    new InternalStopObservationMessage(remoteEndpoint, coapResponse.getToken());

            Channels.write(ctx.getChannel(), internalMessage, remoteEndpoint);
        }
    }


    private void handleEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message){
        //find proper callback
        CoapResponseProcessor callback =
                responseProcessors.get(message.getRemoteEndpoint(), message.getToken());

        if(callback == null){
            log.warn("No response processor found for empty ACK for token {} from {}.", message.getToken(),
                    message.getRemoteEndpoint());
            return;
        }

        if(callback instanceof EmptyAcknowledgementProcessor)
            ((EmptyAcknowledgementProcessor) callback).processEmptyAcknowledgement();

    }


    private void handleReset(InternalResetReceivedMessage message) {
        //find proper callback
        CoapResponseProcessor callback =
                responseProcessors.get(message.getRemoteEndpoint(), message.getToken());

        if(callback == null){
            log.error("No response processor found for RST for token {} from {}.", message.getToken(),
                    message.getRemoteEndpoint());
            return;
        }

        if(callback instanceof ResetProcessor)
            ((ResetProcessor) callback).processReset();
    }


    private void handleMessageRetransmittedMessage(InternalMessageRetransmittedMessage message){

        CoapResponseProcessor callback =
                responseProcessors.get(message.getRemoteAddress(), message.getToken());

        if(callback != null && callback instanceof TransmissionInformationProcessor)
            ((TransmissionInformationProcessor) callback).messageTransmitted(message.getToken(),
                    message.getMessageID(), true);

    }


    private void handleRetransmissionTimeoutMessage(InternalRetransmissionTimeoutMessage message){

        //Find proper callback
        CoapResponseProcessor callback =
                removeResponseCallback(message.getRemoteEndpoint(), message.getToken());

        //Invoke method of callback instance
        if(callback != null && callback instanceof RetransmissionTimeoutProcessor)
            ((RetransmissionTimeoutProcessor) callback).processRetransmissionTimeout();

        //pass the token back
        if(!tokenFactory.passBackToken(message.getRemoteEndpoint(), message.getToken())){
            log.error("Could not pass back token {} for {} from retransmission timeout!",
                    message.getToken(), message.getRemoteEndpoint());
        }
        else{
            log.debug("Passed back token from timeout message!");
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee){
        log.error("Exception: ", ee.getCause());
    }

}
