///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uzl.itm.ncoap.communication.reliability.outbound;
//
//import com.google.common.collect.HashBasedTable;
//import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
//import de.uzl.itm.ncoap.communication.dispatching.client.Token;
//import de.uzl.itm.ncoap.communication.events.*;
//import de.uzl.itm.ncoap.message.*;
//import org.jboss.netty.channel.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.util.Observable;
//import java.util.Observer;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//
///**
//  * This is the handler to deal with reliability message transfers (e.g. retransmissions of confirmable messages) for
//  * CoAP Endpoints.
//  *
//  * @author Oliver Kleine
// */
//public class OutboundReliabilityHandler extends AbstractCoapChannelHandler implements Observer {
//
//    private static Logger LOG = LoggerFactory.getLogger(OutboundReliabilityHandler.class.getName());
//    private static final TimeUnit MILLIS = TimeUnit.MILLISECONDS;
//
//    private HashBasedTable<InetSocketAddress, Integer, OutboundMessageTransfer> outboundTransfers1;
//    private HashBasedTable<InetSocketAddress, Token, Integer> outboundTransfers2;
//    private ReentrantReadWriteLock lock;
//
//    private final MessageIDFactory messageIDFactory;
//
//    /**
//     * Creates a new instance of {@link OutboundReliabilityHandler}
//     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to process the tasks to ensure
//     *                 reliable message transfer
//     */
//    public OutboundReliabilityHandler(ScheduledExecutorService executor){
//        super(executor);
//        this.outboundTransfers1 = HashBasedTable.create();
//        this.outboundTransfers2 = HashBasedTable.create();
//
//        this.messageIDFactory = new MessageIDFactory(executor);
//        this.messageIDFactory.addObserver(this);
//        this.lock = new ReentrantReadWriteLock();
//    }
//
//
//    @Override
//    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket){
//
//        LOG.debug("HANDLE OUTBOUND MESSAGE: {}", coapMessage);
//
//        // update update notifications (i.e. send as next retransmission)
//        if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()
//                && coapMessage.getMessageTypeName() != MessageType.Name.ACK) {
//
//            if(updateRetransmission(remoteSocket, (CoapResponse) coapMessage)){
//                return false;
//            }
//            else {
//                //There was no update notification (which is very unlikely)
//                coapMessage.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
//            }
//        }
//
//        int messageID = coapMessage.getMessageID();
//
//        // set a new message ID if necessary
//        if(messageID == CoapMessage.UNDEFINED_MESSAGE_ID){
//            messageID = assignMessageID(coapMessage, remoteSocket);
//            Token token = coapMessage.getToken();
//            if(messageID == CoapMessage.UNDEFINED_MESSAGE_ID){
//                triggerEvent(new NoMessageIDAvailableEvent(remoteSocket, token), false);
//                return false;
//            } else {
//                triggerEvent(new MessageIDAssignedEvent(remoteSocket, messageID, token), false);
//            }
//        }
//        this.addMessageTransfer(remoteSocket, coapMessage);
//        return true;
//    }
//
//
//
//    @Override
//    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket){
//
//        LOG.debug("HANDLE INBOUND MESSAGE: {}", coapMessage);
//
//        int messageID = coapMessage.getMessageID();
//        MessageCode.Name messageCode = coapMessage.getMessageCodeName();
//        MessageType.Name messageType = coapMessage.getMessageTypeName();
//
//        if(messageType == MessageType.Name.ACK || messageType == MessageType.Name.RST) {
//
//            OutboundMessageTransfer messageTransfer = terminateMessageTransfer(remoteSocket, messageID, true);
//            if(messageTransfer == null){
//                LOG.warn("No open CON found for ACK or RST from \"{}\" with message ID {}!", remoteSocket, messageID);
//                return false;
//            }
//
//            if (messageCode == MessageCode.Name.EMPTY) {
//                Token token = messageTransfer.getToken();
//                if (messageType == MessageType.Name.ACK) {
//                    // handle empty ACK
//                    LOG.info("Received empty ACK from \"{}\" with message ID {}.", remoteSocket, messageID);
//                    triggerEvent(new EmptyAckReceivedEvent(remoteSocket, messageID, token), false);
//                } else {
//                    // handle empty RST (RST is always empty...)
//                    LOG.info("Received RST from \"{}\" with message ID {}.", remoteSocket, messageID);
//                    triggerEvent(new ResetReceivedEvent(remoteSocket, messageID, token), false);
//                }
//                return false;
//            }
//        } else if (coapMessage instanceof CoapResponse) {
//            OutboundMessageTransfer messageTransfer = terminateMessageTransfer(remoteSocket, messageID, true);
//            if (messageTransfer instanceof OutboundReliableMessageTransfer) {
//                LOG.warn("{}-Response taken to terminate reliable transfer (empty ACK was missing!)", messageType);
//            }
//        }
//
//        return true;
//    }
//
//
//    /**
//     * Assigns the given {@link de.uzl.itm.ncoap.message.CoapMessage} a message ID
//     *
//     * @param coapMessage the {@link de.uzl.itm.ncoap.message.CoapMessage} to be assigned a message ID
//     * @param remoteSocket the {@link java.net.InetSocketAddress} of the remote endpoint (i.e. the recipient of this
//     * {@link de.uzl.itm.ncoap.message.CoapMessage}
//     *
//     * @return the message ID that was assigned to this message (or
//     * {@link de.uzl.itm.ncoap.message.CoapMessage#UNDEFINED_MESSAGE_ID} if no ID could be assigned.
//     */
//    private int assignMessageID(CoapMessage coapMessage, InetSocketAddress remoteSocket){
//        int messageID = this.messageIDFactory.getNextMessageID(remoteSocket, coapMessage.getToken());
//
//        if(!(messageID == CoapMessage.UNDEFINED_MESSAGE_ID)){
//            coapMessage.setMessageID(messageID);
//            LOG.debug("Message ID set to {}.", messageID);
//
//        }
//        return messageID;
//    }
//
//    private void addMessageTransfer(InetSocketAddress remoteEndpoint, CoapMessage coapMessage){
//
//        Token token = coapMessage.getToken();
//        int messageID = coapMessage.getMessageID();
//
//        try{
//            lock.writeLock().lock();
//
//            // CON messages are to be retransmitted up to 4 times
//            if(coapMessage.getMessageTypeName() == MessageType.Name.CON){
//                long delay = OutboundReliableMessageTransfer.provideRetransmissionDelay(1);
//                OutboundReliableMessageTransfer transfer =
//                        new OutboundReliableMessageTransfer(remoteEndpoint, coapMessage);
//
//                RetransmissionTask retransmissionTask = new RetransmissionTask(transfer);
//                ScheduledFuture retransmissionFuture = getExecutor().schedule(retransmissionTask, delay, MILLIS);
//
//                transfer.setRetransmissionFuture(retransmissionFuture);
//
//                this.outboundTransfers1.put(remoteEndpoint, messageID, transfer);
//                if(this.outboundTransfers2.put(remoteEndpoint, token, messageID) != null){
//                    LOG.error("This should never happen!");
//                }
//            }
//
//            // NON requests may receive an answer within the lifetime of the message ID
//            else if (coapMessage instanceof CoapRequest ){
//                OutboundMessageTransfer transfer = new OutboundMessageTransfer(remoteEndpoint, messageID, token);
//                this.outboundTransfers1.put(remoteEndpoint, messageID, transfer);
//                this.outboundTransfers2.put(remoteEndpoint, token, messageID);
//            }
//        }
//
//        finally{
//            lock.writeLock().unlock();
//        }
//    }
//
//
//     private OutboundMessageTransfer terminateMessageTransfer(InetSocketAddress remoteSocket, int messageID,
//            boolean confirm) {
//
//         try{
//            lock.writeLock().lock();
//            OutboundMessageTransfer messageTransfer = this.outboundTransfers1.remove(remoteSocket, messageID);
//            if(messageTransfer != null){
//                this.outboundTransfers2.remove(remoteSocket, messageTransfer.getToken());
//                if(messageTransfer instanceof OutboundReliableMessageTransfer && confirm) {
//                    ((OutboundReliableMessageTransfer) messageTransfer).setConfirmed();
//                    LOG.debug("Confirmed reliable transfer");
//                }
//            } else {
//                LOG.error("This should never happen...");
//            }
//            return messageTransfer;
//         }
//         finally {
//             lock.writeLock().unlock();
//         }
//     }
//
//
//    private boolean updateRetransmission(InetSocketAddress remoteEndpoint, CoapResponse coapResponse){
//        Token token = coapResponse.getToken();
//        try{
//            //update the update notification to be retransmitted
//            lock.readLock().lock();
//            if(this.outboundTransfers2.get(remoteEndpoint, token) == null){
//                return false;
//            }
//        } finally {
//            lock.readLock().unlock();
//        }
//
//        try{
//            lock.writeLock().lock();
//
//            Integer messageID = this.outboundTransfers2.get(remoteEndpoint, token);
//            if(messageID == null){
//                return false;
//            }
//            OutboundMessageTransfer messageTransfer = this.outboundTransfers1.get(remoteEndpoint, messageID);
//            coapResponse.setMessageID(messageID);
//            if(messageTransfer instanceof OutboundReliableMessageTransfer){
//                ((OutboundReliableMessageTransfer) messageTransfer).updateCoapMessage(coapResponse);
//                return true;
//            } else {
//                return false;
//            }
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    @Override
//    public void update(Observable o, Object arg) {
//        if(arg instanceof MessageIDFactory.ReleasedMessageID) {
//            InetSocketAddress remoteSocket = ((MessageIDFactory.ReleasedMessageID) arg).getRemoteSocket();
//            int messageID = ((MessageIDFactory.ReleasedMessageID) arg).getMessageID();
//
//            if(terminateMessageTransfer(remoteSocket, messageID, false) != null) {
//                // there was an ongoing outbound transfer (i.e. CON with no ACK or NON with no response)
//                Token token = ((MessageIDFactory.ReleasedMessageID) arg).getToken();
//                LOG.warn("Transmission timed out (remote socket: \"{}\", token: {}, message ID: {})",
//                        new Object[]{remoteSocket, token, messageID});
//                triggerEvent(new TransmissionTimeoutEvent(remoteSocket, messageID, token), true);
//            } else {
//                LOG.info("Message ID retirement does not lead to a transmission timeout (GOOD!)");
//            }
//        } else {
//            LOG.error("This should never happen...");
//        }
//    }
//
//
//    class RetransmissionTask implements Runnable{
//
//        private OutboundReliableMessageTransfer messageTransfer;
//
//        private RetransmissionTask(OutboundReliableMessageTransfer messageTransfer) {
//            this.messageTransfer = messageTransfer;
//        }
//
//        @Override
//        public synchronized void run() {
//            final InetSocketAddress remoteSocket = this.messageTransfer.getRemoteEndpoint();
//            final CoapMessage coapMessage = this.messageTransfer.getCoapMessage();
//
//            // set the observe value for update notifications
//            if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()){
//                ((CoapResponse) coapMessage).setObserve();
//            }
//
//            // retransmit message
//            ChannelFuture future = Channels.future(getContext().getChannel());
//            Channels.write(getContext(), future, coapMessage, remoteSocket);
//            scheduleNextRetransmission(this.messageTransfer);
//
//            future.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    int messageID = coapMessage.getMessageID();
//                    Token token = coapMessage.getToken();
//                    if(future.isSuccess()) {
//                        triggerEvent(new MessageRetransmittedEvent(remoteSocket, messageID, token), false);
//                    } else {
//                        String desc = "Could not sent retransmission (\"" + future.getCause().getMessage() + "\"";
//                        triggerEvent(new MiscellaneousErrorEvent(remoteSocket, messageID, token, desc), false);
//                    }
//                }
//            });
//        }
//
//        private void scheduleNextRetransmission(OutboundReliableMessageTransfer messageTransfer){
//            if(!messageTransfer.isConfirmed()){
//                int count = messageTransfer.increaseRetransmissions();
//                if (count < OutboundReliableMessageTransfer.MAX_RETRANSMISSIONS) {
//                    long delay = messageTransfer.getNextRetransmissionDelay();
//                    RetransmissionTask retransmissionTask = new RetransmissionTask(messageTransfer);
//                    ScheduledFuture retransmissionFuture = getExecutor().schedule(retransmissionTask, delay, MILLIS);
//                    LOG.debug("Scheduled next retransmission to \"{}\" (Message ID: {})",
//                            messageTransfer.getRemoteEndpoint(), messageTransfer.getCoapMessage().getMessageID());
//                    messageTransfer.setRetransmissionFuture(retransmissionFuture);
//                } else {
//                    LOG.warn("No more retransmissions (remote endpoint: {}, message ID: {})!",
//                            messageTransfer.getRemoteEndpoint(), messageTransfer.getCoapMessage().getMessageID()
//                    );
//                }
//            }
//        }
//    }
//}