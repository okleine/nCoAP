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
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
  * This is the handler to deal with reliability concerns for CoAP Clients. The reliability functionality for
  * inbound messages is within the
  * {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallbackManager}.
  *
  * @author Oliver Kleine
 */
public class OutboundReliabilityHandler extends SimpleChannelHandler{

    private static Logger log = LoggerFactory.getLogger(OutboundReliabilityHandler.class.getName());
    private static final TimeUnit MILLIS = TimeUnit.MILLISECONDS;

    private ChannelHandlerContext ctx;

    //remote socket mapped to message ID and token
    private HashBasedTable<InetSocketAddress, Integer, OutboundMessageTransfer> transfers;
    private ReentrantReadWriteLock lock;

    private final MessageIDFactory messageIDFactory;
    private ScheduledExecutorService executor;


    public OutboundReliabilityHandler(ScheduledExecutorService executor){
        this.executor = executor;
        this.transfers = HashBasedTable.create();
        this.messageIDFactory = new MessageIDFactory(executor);
        this.lock = new ReentrantReadWriteLock();
    }


    public void setChannelHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
        this.messageIDFactory.setChannel(ctx.getChannel());
     }


    private void addTransfer(InetSocketAddress remoteEndpoint, CoapMessage coapMessage, boolean reliable){
        Token token = coapMessage.getToken();
        int messageID = coapMessage.getMessageID();

        try{
            lock.writeLock().lock();

            if(reliable){
                long delay = OutboundReliableMessageTransfer.provideRetransmissionDelay(1);
                RetransmissionTask retransmissionTask = new RetransmissionTask(remoteEndpoint, coapMessage);
                ScheduledFuture retransmissionFuture = this.executor.schedule(retransmissionTask, delay, MILLIS);

                OutboundReliableMessageTransfer transfer = new OutboundReliableMessageTransfer(remoteEndpoint,
                        messageID, token, retransmissionFuture);

                this.transfers.put(remoteEndpoint, coapMessage.getMessageID(), transfer);
            }

            else{
                OutboundMessageTransfer transfer = new OutboundMessageTransfer(remoteEndpoint, messageID, token);
                this.transfers.put(remoteEndpoint, messageID, transfer);
            }
        }

        finally{
            lock.writeLock().unlock();
        }
    }


     private OutboundMessageTransfer removeTransfer(InetSocketAddress remoteEndpoint, int messageID){
         try{
            lock.writeLock().lock();
            return this.transfers.remove(remoteEndpoint, messageID);
         }
         finally {
             lock.writeLock().unlock();
         }
     }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("DOWNSTREAM BEFORE (to {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapMessage){
            handleOutboundCoapMessage(ctx, me);
        }
        else{
            log.debug("DOWNSTREAM AFTER (to {}): {}.", me.getRemoteAddress(), me.getMessage());
            ctx.sendDownstream(me);
        }

    }


    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("UPSTREAM (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapMessage) {
            handleInboundCoapMessage(ctx, me);
        }
        else if(me.getMessage() instanceof MessageIDReleasedEvent){
            handleMessageIDReleasedEvent(ctx, me);
        }
        else{
            ctx.sendUpstream(me);
        }
    }

    
    private void handleMessageIDReleasedEvent(ChannelHandlerContext ctx, MessageEvent me) {
        MessageIDReleasedEvent event = (MessageIDReleasedEvent) me.getMessage();
        InetSocketAddress remoteEndpoint = event.getRemoteEndpoint();
        int messageID = event.getMessageID();

        if(this.transfers.contains(remoteEndpoint, messageID)){
            OutboundMessageTransfer transfer = removeTransfer(remoteEndpoint, messageID);
            if(transfer != null){
                if(transfer instanceof OutboundReliableMessageTransfer){
                    log.info("Removed reliable transfer (remote endpoint: {}, message ID: {})", remoteEndpoint,
                            messageID);
                    Token token = transfer.getToken();
                    Channels.fireMessageReceived(ctx, new TransmissionTimeoutEvent(remoteEndpoint, messageID, token));
                }
                else{
                    log.info("Removed non-reliable transfer (remote endpoint: {}, message ID: {})", remoteEndpoint,
                            messageID);
                    ctx.sendUpstream(me);
                }
            }
        }

        ctx.sendUpstream(me);
    }


    private boolean updateConfirmableUpdateNotification(InetSocketAddress remoteEndpoint, CoapResponse coapResponse){
        int messageID = coapResponse.getMessageID();

        try{
            //update the update notification to be retransmitted
            lock.readLock().lock();

            if(!this.transfers.contains(remoteEndpoint, messageID)){
                return false;
            }
        }
        finally{
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();

            OutboundMessageTransfer transfer = transfers.get(remoteEndpoint, messageID);
            if(transfer instanceof OutboundReliableMessageTransfer){
                ScheduledFuture retransmissionFuture =
                        ((OutboundReliableMessageTransfer) transfer).getRetransmissionFuture();

                //Try to cancel the retransmission
                if(!retransmissionFuture.cancel(true)){
                    log.error("Could not cancel retransmission of update notification (remote endpoint: {}, " +
                        "message ID: {})", remoteEndpoint, messageID);
                }

                long delay = Math.max(retransmissionFuture.getDelay(TimeUnit.MILLISECONDS), 0);

                RetransmissionTask retransmissionTask = new RetransmissionTask(remoteEndpoint, coapResponse);

                retransmissionFuture = this.executor.schedule(retransmissionTask, delay, TimeUnit.MILLISECONDS);

                ((OutboundReliableMessageTransfer) transfer).setRetransmissionFuture(retransmissionFuture);
                return true;
            }

            else{
                return false;
            }
        }
        finally{
            lock.writeLock().unlock();
        }
    }


    private void handleOutboundCoapMessage(ChannelHandlerContext ctx, MessageEvent me){

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        if(coapMessage.getMessageID() != CoapMessage.UNDEFINED_MESSAGE_ID){
            int messageID = coapMessage.getMessageID();

            if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()
                    && coapMessage.getMessageTypeName() != MessageType.Name.ACK){

                if(this.transfers.contains(remoteEndpoint, messageID)){

                    if(updateConfirmableUpdateNotification(remoteEndpoint, (CoapResponse) coapMessage)){
                        return;
                    }

                    else{
                        //There was no update notification (which is very unlikely)
                        coapMessage.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
                    }
                }
            }

            else{
                ctx.sendDownstream(me);
                return;
            }
        }

        int messageID = this.messageIDFactory.getNextMessageID(remoteEndpoint);

        if(messageID == CoapMessage.UNDEFINED_MESSAGE_ID){
            MiscellaneousErrorEvent event = new MiscellaneousErrorEvent(remoteEndpoint, messageID,
            coapMessage.getToken(), "No message ID available for remote endpoint: " + remoteEndpoint);
            Channels.fireMessageReceived(ctx.getChannel(), event);
            return;
        }

        else if(coapMessage.getMessageTypeName() == MessageType.Name.CON){
            coapMessage.setMessageID(messageID);
            this.addTransfer(remoteEndpoint, coapMessage, true);
            log.debug("DOWNSTREAM AFTER (to {}): {}.", me.getRemoteAddress(), me.getMessage());
            ctx.sendDownstream(me);
        }

        else if(coapMessage.getMessageTypeName() == MessageType.Name.NON){
            coapMessage.setMessageID(messageID);
            this.addTransfer(remoteEndpoint, coapMessage, false);
            log.debug("DOWNSTREAM AFTER (to {}): {}.", me.getRemoteAddress(), me.getMessage());
            ctx.sendDownstream(me);
        }

        else{
            coapMessage.setMessageID(messageID);
            log.debug("DOWNSTREAM AFTER (to {}): {}.", me.getRemoteAddress(), me.getMessage());
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
             OutboundMessageTransfer messageExchange = removeTransfer(remoteEndpoint, messageID);

             if(messageExchange != null && messageExchange instanceof OutboundReliableMessageTransfer){

                 if(messageCode == MessageCode.Name.EMPTY){
                     log.info("Received empty ACK (remote endpoint: {}, message ID: {}).", remoteEndpoint,
                             messageID);
                     ((OutboundReliableMessageTransfer) messageExchange).setConfirmed();
                     Token token = messageExchange.getToken();
                     Channels.fireMessageReceived(ctx, new EmptyAckReceivedEvent(remoteEndpoint, messageID, token));

                     me.getFuture().setSuccess();
                 }
                 else{
                     log.info("Received non-empty ACK (remote endpoint: {}, message ID: {}).",
                             remoteEndpoint, messageID);

                     ((OutboundReliableMessageTransfer) messageExchange).setConfirmed();
                     ctx.sendUpstream(me);
                 }


             }

             else{
                 log.warn("No open CON found for ACK (remote endpoint: {}, message ID: {})", remoteEndpoint, messageID);
             }
         }

         else if(messageType == MessageType.Name.RST){
             OutboundMessageTransfer messageExchange = removeTransfer(remoteEndpoint, messageID);

             if(messageExchange != null){

                 if(messageExchange instanceof OutboundReliableMessageTransfer){
                     ((OutboundReliableMessageTransfer) messageExchange).setConfirmed();
                 }

                 Token token = messageExchange.getToken();
                 Channels.fireMessageReceived(ctx, new ResetReceivedEvent(remoteEndpoint, messageID, token));
             }

             else{
                 log.warn("No open CON found for RST (remote endpoint: {}, message ID: {})", remoteEndpoint, messageID);
             }
         }

         else{
             ctx.sendUpstream(me);
         }
     }


//     @Override
//     public void update(Observable o, Object arg) {
//         InetSocketAddress remoteEndpoint = (InetSocketAddress) ((Object[]) arg)[0];
//         int messageID = (Integer) ((Object[]) arg)[1];
//
//         log.debug("Message ID {} is retired for remote endpoint {}!", messageID, remoteEndpoint);
//
//         if(this.transfers.contains(remoteEndpoint, messageID)){
//             Token token = removeTransfer(remoteEndpoint, messageID).getToken();
//             TransmissionTimeoutEvent event = new TransmissionTimeoutEvent(remoteEndpoint, messageID, token);
//             Channels.fireMessageReceived(this.ctx.getChannel(), event);
//         }
//     }


    private class RetransmissionTask implements Runnable{

        private InetSocketAddress remoteEndpoint;
        private CoapMessage coapMessage;

        private RetransmissionTask(InetSocketAddress remoteEndpoint, CoapMessage coapMessage) {
            this.remoteEndpoint = remoteEndpoint;
            this.coapMessage = coapMessage;
        }

        @Override
        public void run() {
            //Set the observe value for update notifications
            if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()){
                ((CoapResponse) coapMessage).setObserve();
            }

            //retransmit message
            ChannelFuture future = Channels.future(ctx.getChannel());
            Channels.write(ctx, future, coapMessage, remoteEndpoint);

            //Fire internal retransmission event
            MessageRetransmittedEvent event = new MessageRetransmittedEvent(remoteEndpoint, coapMessage.getMessageID(),
                    coapMessage.getToken());
            Channels.fireMessageReceived(ctx.getChannel(), event);

            //schedule next transmission
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess()){
                        log.error("Could not sent retransmission!", future.getCause());
                    }

                    int messageID = coapMessage.getMessageID();

                    OutboundMessageTransfer transfer = transfers.get(remoteEndpoint, messageID);

                    if(transfer != null && transfer instanceof OutboundReliableMessageTransfer){
                        OutboundReliableMessageTransfer reliableTransfer = (OutboundReliableMessageTransfer) transfer;

                        int count = reliableTransfer.increaseRetransmissions();

                        log.info("Retransmission #{} completed (remote endpoint: {}, message ID: {})!",
                        new Object[]{count, remoteEndpoint, messageID});

                        if(count < OutboundReliableMessageTransfer.MAX_RETRANSMISSIONS){
                            long delay = reliableTransfer.getNextRetransmissionDelay();
                            RetransmissionTask retransmissionTask = new RetransmissionTask(remoteEndpoint, coapMessage);
                            ScheduledFuture retransmissionFuture = executor.schedule(retransmissionTask, delay, MILLIS);
                            reliableTransfer.setRetransmissionFuture(retransmissionFuture);
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
