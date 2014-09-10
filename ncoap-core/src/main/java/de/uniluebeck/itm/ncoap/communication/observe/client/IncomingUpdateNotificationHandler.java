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
package de.uniluebeck.itm.ncoap.communication.observe.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by olli on 20.03.14.
 */
public class IncomingUpdateNotificationHandler extends SimpleChannelHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private static final String ERROR = "There is no running observation to cancel for remote endpoints %s and token %s";

    private Multimap<InetSocketAddress, Token> observations;


    public IncomingUpdateNotificationHandler(){
        this.observations = Multimaps.synchronizedMultimap(HashMultimap.<InetSocketAddress, Token>create());
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapRequest)
            handleOutgoingCoapRequest(ctx, me);

        else if(me.getMessage() instanceof ClientStopsObservationEvent)
            handleInternalStopObservationMessage(me);

        else
            ctx.sendDownstream(me);
    }

    private void handleInternalStopObservationMessage(MessageEvent me) {
        ClientStopsObservationEvent internalMessage = (ClientStopsObservationEvent) me.getMessage();

        if(observations.remove(internalMessage.getRemoteEndpoint(), internalMessage.getToken())){
            me.getFuture().setSuccess();

            log.info("Observation stopped! Future update notifications from {} with token {} will cause a RST.",
                    internalMessage.getRemoteEndpoint(), internalMessage.getToken());
        }

        else{
            String errorMessage = String.format(ERROR, internalMessage.getRemoteEndpoint().toString(),
                    internalMessage.getToken().toString());

            me.getFuture().setFailure(new Exception(errorMessage));

            log.error(errorMessage);
        }
    }

    private void handleOutgoingCoapRequest(ChannelHandlerContext ctx, MessageEvent me) {
        CoapRequest coapRequest = (CoapRequest) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        if(coapRequest.isObserveSet() && !observations.containsEntry(remoteEndpoint, coapRequest.getToken()))
            observations.put(remoteEndpoint, coapRequest.getToken());

        ctx.sendDownstream(me);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapResponse)
            handleIncomingCoapResponse(ctx, me);

        else
            ctx.sendUpstream(me);
    }


    private void handleIncomingCoapResponse(ChannelHandlerContext ctx, MessageEvent me) {
        CoapResponse coapResponse = (CoapResponse) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        if(coapResponse.isUpdateNotification() && !observations.containsEntry(remoteEndpoint, coapResponse.getToken()))
            sendResetMessage(ctx, remoteEndpoint, coapResponse.getMessageID());

        else
            ctx.sendUpstream(me);
    }


    private void sendResetMessage(ChannelHandlerContext ctx, InetSocketAddress remoteEndoint, int messageID) {
        final CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);

        ChannelFuture future = Channels.future(ctx.getChannel());
        Channels.write(ctx, future , resetMessage, remoteEndoint);

        if(log.isErrorEnabled()){
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess())
                        log.info("RST message sent ({})", resetMessage);

                    else
                        log.error("Could not send RST message ({})", resetMessage);
                }
            });
        }
    }

}
