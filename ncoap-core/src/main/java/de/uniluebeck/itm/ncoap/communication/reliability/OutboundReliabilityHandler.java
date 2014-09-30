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
package de.uniluebeck.itm.ncoap.communication.reliability;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.communication.events.*;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * This is the handler to deal with reliability concerns for CoAP Clients. The reliability functionality for
 * inbound messages is within the
 * {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallbackManager}.
 *
 * @author Oliver Kleine
*/
public class OutboundReliabilityHandler extends SimpleChannelHandler implements Observer{

    private static Logger log = LoggerFactory.getLogger(OutboundReliabilityHandler.class.getName());
    private static final TimeUnit MILLIS = TimeUnit.MILLISECONDS;

    private ChannelHandlerContext ctx;

    //remote socket mapped to message ID and token
    private HashBasedTable<InetSocketAddress, Integer, OutboundMessageExchange> conversations;
    private final MessageIDFactory messageIDFactory;
    private ScheduledExecutorService executor;


    public OutboundReliabilityHandler(ScheduledExecutorService executorService){
        this.executor = executorService;
        this.conversations = HashBasedTable.create();
        this.messageIDFactory = new MessageIDFactory(executorService);
    }


    public void setChannelHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }


    private synchronized void addReliableConversation(InetSocketAddress remoteEndpoint, CoapMessage coapMessage){

        long delay = OutboundReliableMessageExchange.provideRetransmissionDelay(1);
        RetransmissionTask retransmissionTask = new RetransmissionTask(remoteEndpoint, coapMessage);
        ScheduledFuture retransmissionFuture = this.executor.schedule(retransmissionTask, delay, MILLIS);

        Token token = coapMessage.getToken();
        int messageID = coapMessage.getMessageID();
        OutboundMessageExchange replaced = this.conversations.put(remoteEndpoint, coapMessage.getMessageID(),
                new OutboundReliableMessageExchange(remoteEndpoint, messageID, token, retransmissionFuture));

        if(replaced != null){
            log.error("Old token {} for conversation with {} and message ID {} was overridden!",
                    new Object[]{replaced.getToken(), remoteEndpoint, replaced.getMessageID()});
        }
    }


    private synchronized OutboundMessageExchange removeReliableConversation(InetSocketAddress remoteEndpoint,
                                                                                    int messageID){
        return this.conversations.remove(remoteEndpoint, messageID);
    }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Outgoing (to {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapMessage){
            handleOutboundCoapMessage(ctx, me);
        }
        else{
            ctx.sendDownstream(me);
        }
    }


    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapMessage) {
            handleInboundCoapMessage(ctx, me);
        }
        else{
            ctx.sendUpstream(me);
        }
    }


    private void handleOutboundCoapMessage(ChannelHandlerContext ctx, MessageEvent me){

        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        if(coapMessage.getMessageID() != CoapMessage.UNDEFINED_MESSAGE_ID){
            ctx.sendDownstream(me);
            return;
        }

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        int messageID = this.messageIDFactory.getNextMessageID(remoteEndpoint);

        if(messageID == CoapMessage.UNDEFINED_MESSAGE_ID){
            MiscellaneousErrorEvent event = new MiscellaneousErrorEvent(remoteEndpoint, messageID,
                    coapMessage.getToken(), "No message ID available for remote endpoint: " + remoteEndpoint);
            Channels.fireMessageReceived(ctx.getChannel(), event);
            return;
        }

        else if(coapMessage.getMessageTypeName() == MessageType.Name.CON){
            coapMessage.setMessageID(messageID);
            this.addReliableConversation(remoteEndpoint, coapMessage);
            ctx.sendDownstream(me);
        }

        else{
            coapMessage.setMessageID(messageID);
            ctx.sendDownstream(me);
        }

        MessageIDAssignedEvent event = new MessageIDAssignedEvent(remoteEndpoint, messageID, coapMessage.getToken());
        Channels.fireMessageReceived(ctx.getChannel(), event);
    }


    private void handleInboundCoapMessage(ChannelHandlerContext ctx, MessageEvent me){

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        MessageCode.Name messageCode = coapMessage.getMessageCodeName();
        MessageType.Name messageType = coapMessage.getMessageTypeName();

        int messageID = coapMessage.getMessageID();

        if(messageType == MessageType.Name.ACK){
            OutboundMessageExchange messageExchange = removeReliableConversation(remoteEndpoint, messageID);

            if(messageExchange != null && messageExchange instanceof OutboundReliableMessageExchange){

                ((OutboundReliableMessageExchange) messageExchange).setConfirmed();

                if(messageCode == MessageCode.Name.EMPTY){
                    log.info("Received empty ACK for open CON (remote endpoint: {}, message ID: {}).", remoteEndpoint,
                            messageID);
                    Token token = messageExchange.getToken();
                    Channel channel = ctx.getChannel();
                    Channels.fireMessageReceived(channel, new EmptyAckReceivedEvent(remoteEndpoint, messageID, token));
                    return;
                }
                else{
                    log.info("Received non-empty ACK for open CON (remote endpoint: {}, message ID: {}).",
                            remoteEndpoint, messageID);
                }
            }

            else{
                log.warn("No open CON found for ACK (remote endpoint: {}, message ID: {})", remoteEndpoint, messageID);
                return;
            }
        }

        else if(messageType == MessageType.Name.RST){
            OutboundMessageExchange messageExchange = removeReliableConversation(remoteEndpoint, messageID);

            if(messageExchange != null){

                if(messageExchange instanceof OutboundReliableMessageExchange){
                    ((OutboundReliableMessageExchange) messageExchange).setConfirmed();
                }

                Token token = messageExchange.getToken();
                Channel channel = ctx.getChannel();
                Channels.fireMessageReceived(channel, new ResetReceivedEvent(remoteEndpoint, messageID, token));
                return;
            }

            else{
                log.warn("No open CON found for RST (remote endpoint: {}, message ID: {})", remoteEndpoint, messageID);
            }
        }

        ctx.sendUpstream(me);
    }


    @Override
    public void update(Observable o, Object arg) {
        InetSocketAddress remoteEndpoint = (InetSocketAddress) ((Object[]) arg)[0];
        int messageID = (Integer) ((Object[]) arg)[1];

        log.debug("Message ID {} is retired for remote endpoint {}!", messageID, remoteEndpoint);

        if(this.conversations.contains(remoteEndpoint, messageID)){
            Token token = removeReliableConversation(remoteEndpoint, messageID).getToken();
            ConversationTimeoutEvent event = new ConversationTimeoutEvent(remoteEndpoint, messageID, token);
            Channels.fireMessageReceived(this.ctx.getChannel(), event);
        }
    }


    private class RetransmissionTask implements Runnable{

        private InetSocketAddress remoteEndpoint;
        private CoapMessage coapMessage;

        private RetransmissionTask(InetSocketAddress remoteEndpoint, CoapMessage coapMessage) {
            this.remoteEndpoint = remoteEndpoint;
            this.coapMessage = coapMessage;
        }

        @Override
        public void run() {
            //retransmit message
            ChannelFuture future = Channels.future(ctx.getChannel());
            Channels.write(ctx, future, coapMessage, remoteEndpoint);

            //schedule next transmission
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess()){
                        log.error("Could not sent retransmission!", future.getCause());
                    }

                    int messageID = coapMessage.getMessageID();

                    OutboundMessageExchange messageExchange = conversations.get(remoteEndpoint, messageID);

                    if(messageExchange != null && messageExchange instanceof OutboundReliableMessageExchange){
                        OutboundReliableMessageExchange reliableMessageExchange =
                                (OutboundReliableMessageExchange) messageExchange;

                        int count = reliableMessageExchange.increaseRetransmissions();

                        log.info("Retransmission #{} completed (remote endpoint: {}, message ID: {})!",
                                new Object[]{count, remoteEndpoint, messageID});

                        if(count < OutboundReliableMessageExchange.MAX_RETRANSMISSIONS){
                            long delay = reliableMessageExchange.getNextRetransmissionDelay();
                            RetransmissionTask retransmissionTask = new RetransmissionTask(remoteEndpoint, coapMessage);
                            ScheduledFuture retransmissionFuture = executor.schedule(retransmissionTask, delay, MILLIS);
                            reliableMessageExchange.setRetransmissionFuture(retransmissionFuture);
                        }
                        else{
                            log.warn("No more retransmissions (remote endpoint: {}, message ID: {})!",
                                    remoteEndpoint, messageID);
                        }
                    }
                }
            });
        }
    }
}
