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

package de.uzl.itm.ncoap.communication.reliability;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.message.*;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This class is the first {@link org.jboss.netty.channel.ChannelUpstreamHandler} to deal with inbound decoded
 * {@link de.uzl.itm.ncoap.message.CoapMessage}s at
 * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication}s. If the inbound message is a confirmable
 * {@link de.uzl.itm.ncoap.message.CoapRequest} it schedules the sending of an empty acknowledgement to the
 * sender if there wasn't a response from the addressed webresource within a period of 1.5 seconds.
 *
 * @author Oliver Kleine
 */
public class InboundReliabilityHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(InboundReliabilityHandler.class.getName());

    private HashBasedTable<InetSocketAddress, Integer, InboundMessageTransfer> conversations;
    private ReentrantReadWriteLock lock;

    private ScheduledExecutorService executor;
    private ChannelHandlerContext ctx;


    /**
     * Creates a new instance of {@link InboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to provide the threads to execute the
     *                 tasks for reliability.
     */
    public InboundReliabilityHandler(ScheduledExecutorService executor){
        this.conversations = HashBasedTable.create();
        this.executor = executor;
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Sets the {@link org.jboss.netty.channel.ChannelHandlerContext} of this handler <b>(for internal use only)</b>
     *
     * @param ctx the {@link org.jboss.netty.channel.ChannelHandlerContext} of this handler
     */
    public void setChannelHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }


    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Received (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapRequest){
            handleInboundCoapRequest(ctx, me);
        }

        else if(me.getMessage() instanceof CoapMessage){
            CoapMessage coapMessage = (CoapMessage) me.getMessage();
            MessageType.Name messageType = coapMessage.getMessageTypeName();
            MessageCode.Name messageCode = coapMessage.getMessageCodeName();

            //CoAP ping
            if(messageType == MessageType.Name.CON && messageCode == MessageCode.Name.EMPTY){
                log.info("CoAP PING received (remote endpoint: {}, message ID: {})", me.getRemoteAddress(),
                        coapMessage.getMessageID());
                InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
                CoapMessage pong = CoapMessage.createEmptyReset(coapMessage.getMessageID());
                Channels.write(ctx.getChannel(), pong, remoteEndpoint);
            }
        }

        else{
            ctx.sendUpstream(me);
        }
    }


    private void handleInboundCoapRequest(ChannelHandlerContext ctx, MessageEvent me){
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapRequest coapRequest = (CoapRequest) me.getMessage();
        MessageType.Name messageType = coapRequest.getMessageTypeName();
        int messageID = coapRequest.getMessageID();

        //duplicate detected!
        if(!this.startConversation(remoteEndpoint, messageID, messageType)){

            if(messageType == MessageType.Name.NON){
                log.warn("Duplicate NON Request (remote endpoint: {}, message ID: {}). IGNORE!", remoteEndpoint, messageID);
            }

            else if(messageType == MessageType.Name.CON){
                log.warn("Duplicate CON Request (remote endpoint: {}, message ID: {}). Schedule ACK!",
                        remoteEndpoint, messageID);
                InboundMessageTransfer transfer = this.conversations.get(remoteEndpoint, messageID);

                if(transfer instanceof InboundReliableMessageTransfer){
                    InboundReliableMessageTransfer reliableTransfer = (InboundReliableMessageTransfer) transfer;

                    //if the message reception was already confirmed, than confirm it again (after default delay)
                    if(reliableTransfer.isConfirmed()){
                        ConfirmationTask confirmationTask = new ConfirmationTask(remoteEndpoint, messageID);
                        ScheduledFuture confirmationFuture = this.executor.schedule(confirmationTask,
                                InboundReliableMessageTransfer.EMPTY_ACK_DELAY, TimeUnit.MILLISECONDS);
                        reliableTransfer.setConfirmationFuture(confirmationFuture);
                        reliableTransfer.setConfirmed(false);
                    }
                }
            }
        }

        //no duplicate detected
        else{
            ctx.sendUpstream(me);
        }
    }


    private boolean startConversation(InetSocketAddress remoteEndpoint, int messageID, MessageType.Name messageType){
        try{
            lock.readLock().lock();
            //duplicate detection
            if(this.conversations.contains(remoteEndpoint, messageID)){
                return false;
            }
        }
        finally{
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();

            //another duplicate detection (just to be safe within the synchronized area)
            if(this.conversations.contains(remoteEndpoint, messageID)){
                return false;
            }

            //this is definitely no duplicate, so add a new conversation
            else if(messageType == MessageType.Name.CON) {
                Runnable confirmationTask = new ConfirmationTask(remoteEndpoint, messageID);
                ScheduledFuture confirmationFuture = this.executor.schedule(confirmationTask,
                        InboundReliableMessageTransfer.EMPTY_ACK_DELAY, TimeUnit.MILLISECONDS);
                InboundReliableMessageTransfer messageExchange = new InboundReliableMessageTransfer(remoteEndpoint,
                        messageID, confirmationFuture);
                this.conversations.put(remoteEndpoint, messageID, messageExchange);
                return true;
            }

            else if(messageType == MessageType.Name.NON){
                this.conversations.put(remoteEndpoint, messageID,
                        new InboundMessageTransfer(remoteEndpoint, messageID));
                return true;
            }

            else{
                log.error("Unexpected message type ({}) to start a conversation!", messageType);
                return false;
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    private InboundMessageTransfer stopReliableTransfer(InetSocketAddress remoteEndpoint, int messageID){
        try{
            lock.readLock().lock();
            if(!this.conversations.contains(remoteEndpoint, messageID)){
                return null;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();
            InboundMessageTransfer messageTransfer = this.conversations.remove(remoteEndpoint, messageID);

            if(messageTransfer != null && messageTransfer instanceof InboundReliableMessageTransfer){
                stopConfirmationTask((InboundReliableMessageTransfer) messageTransfer);
            }

            return messageTransfer;
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    private void stopConfirmationTask(InboundReliableMessageTransfer messageExchange){
        //not yet confirmed
        if(!messageExchange.isConfirmed()){
            if(messageExchange.getConfirmationFuture().cancel(false)){
                log.info("Confirmation task successfully canceled (remote endpoint: {}, message ID: {})",
                        messageExchange.getRemoteEndpoint(), messageExchange.getMessageID());
            }
            else{
                log.error("Could not cancel confirmation task (remote endpoint: {}, message ID: {})",
                        messageExchange.getRemoteEndpoint(), messageExchange.getMessageID());
            }
        }
    }

    /**
     * If the message to be written is a {@link de.uzl.itm.ncoap.message.CoapResponse} this method decides whether the message type is
     * {@link de.uzl.itm.ncoap.message.MessageType.Name#ACK} (if there wasn't an empty acknowledgement sent yet) or {@link de.uzl.itm.ncoap.message.MessageType.Name#CON}
     * (if there was already an empty acknowledgement sent). In the latter case it additionally cancels the sending of
     * an empty acknowledgement (which was scheduled by the <code>messageReceived</code> method when the request
     * was received).
     *
     * @param ctx The {@link org.jboss.netty.channel.ChannelHandlerContext} connecting relating this class (which implements the
     * {@link org.jboss.netty.channel.ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link org.jboss.netty.channel.MessageEvent} containing the actual message
     *
     * @throws Exception if an error occurred
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(me.getMessage() instanceof CoapResponse){
            handleOutboundCoapResponse(ctx, me);
        }
        else{
            ctx.sendDownstream(me);
        }
    }


    private void handleOutboundCoapResponse(ChannelHandlerContext ctx, MessageEvent me){

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapResponse coapResponse = (CoapResponse) me.getMessage();
        int messageID = coapResponse.getMessageID();

        InboundMessageTransfer messageExchange = stopReliableTransfer(remoteEndpoint, messageID);

        if(messageExchange != null && messageExchange instanceof InboundReliableMessageTransfer){
            if(!((InboundReliableMessageTransfer) messageExchange).isConfirmed()){
                coapResponse.setMessageType(MessageType.Name.ACK.getNumber());
            }
            else{
                coapResponse.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
            }
        }

        ctx.sendDownstream(me);
    }


    private class ConfirmationTask implements Runnable{

                  private final InetSocketAddress remoteEndpoint;
                  private final int messageID;

                  public ConfirmationTask(InetSocketAddress remoteEndpoint, int messageID){
                      this.remoteEndpoint = remoteEndpoint;
                      this.messageID = messageID;
                  }

                  @Override
                  public void run(){
                      CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
                      ChannelFuture confirmationFuture = Channels.write(ctx.getChannel(), emptyACK, remoteEndpoint);
                      if(log.isInfoEnabled()){
                          confirmationFuture.addListener(new ChannelFutureListener() {
                              @Override
                              public void operationComplete(ChannelFuture future) throws Exception {
                                log.info("Empty ACK sent (remote endpoint: {}, message ID: {})", remoteEndpoint,
                                        messageID);
                              }
                          });
                      }
                      ((InboundReliableMessageTransfer) conversations.get(remoteEndpoint, messageID))
                              .setConfirmed(true);
                  }
              }
}

