/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.communication.reliability.outbound;

import com.google.common.collect.*;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.*;
import de.uzl.itm.ncoap.message.*;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
  * This is the handler to deal with reliability message transfers (e.g. retransmissions of confirmable messages) for
  * CoAP Endpoints.
  *
  * @author Oliver Kleine
 */
public class ClientOutboundReliabilityHandler extends AbstractOutboundReliabilityHandler implements Observer {

    private static Logger LOG = LoggerFactory.getLogger(ClientOutboundReliabilityHandler.class.getName());

    private Table<InetSocketAddress, Integer, Token> transfers1;
    private Table<InetSocketAddress, Token, Integer> transfers2;

    private ReentrantReadWriteLock lock;


    /**
     * Creates a new instance of {@link de.uzl.itm.ncoap.communication.reliability.outbound.ClientOutboundReliabilityHandler}
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to process the tasks to ensure
     *                 reliable message transfer
     */
    public ClientOutboundReliabilityHandler(ScheduledExecutorService executor, MessageIDFactory factory) {
        super(executor, factory);
        this.transfers1 = HashBasedTable.create();
        this.transfers2 = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapRequest) {
            return handleOutboundCoapMessage2(coapMessage, remoteSocket);
        } else if (coapMessage.isPing()) {
            return handleOutboundCoapMessage2(coapMessage, remoteSocket);
        } else {
            return true;
        }
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapResponse) {
            return handleInboundCoapResponse((CoapResponse) coapMessage, remoteSocket);
        } else if (coapMessage.getMessageCode() == MessageCode.EMPTY) {
            return handleInboundEmptyMessage(coapMessage, remoteSocket);
        } else {
            return true;
        }
    }


    private boolean handleOutboundCoapMessage2(CoapMessage coapRequest, InetSocketAddress remoteSocket) {
        LOG.debug("HANDLE OUTBOUND MESSAGE: {}", coapRequest);

        int messageID = assignMessageID(coapRequest, remoteSocket);
        Token token = coapRequest.getToken();
        if (messageID == CoapMessage.UNDEFINED_MESSAGE_ID) {
            LOG.info("No message ID available for \"{}\" (ID pool exhausted).", remoteSocket);
            triggerEvent(new NoMessageIDAvailableEvent(remoteSocket, token), false);
            return false;
        } else {
            LOG.info("Set message ID to {}", messageID);
            triggerEvent(new MessageIDAssignedEvent(remoteSocket, messageID, token), false);
        }

        addTransfer(remoteSocket, coapRequest.getMessageID(), coapRequest.getToken());
        if (coapRequest.getMessageType() == MessageType.CON) {
            scheduleRetransmission(coapRequest, remoteSocket, 1);
        }
        return true;
    }


    private boolean handleInboundEmptyMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        int messageType = coapMessage.getMessageType();
        if (messageType == MessageType.CON) {
            // incoming PINGs are handled by the inbound reliability handler
            return true;
        } else {
            int messageID = coapMessage.getMessageID();
            Token token = removeTransfer(remoteSocket, messageID);
            if (token == null) {
                return true;
            } else if (messageType == MessageType.ACK) {
                LOG.info("Received empty ACK from \"{}\" for token {} (Message ID: {}).",
                    new Object[]{remoteSocket, messageID, token});
                triggerEvent(new EmptyAckReceivedEvent(remoteSocket, messageID, token), false);
                return false;
            } else if (messageType == MessageType.RST) {
                LOG.info("Received RST from \"{}\" for token {} (Message ID: {}).",
                        new Object[]{remoteSocket, messageID, token});
                triggerEvent(new ResetReceivedEvent(remoteSocket, messageID, token), false);
                return false;
            } else {
                LOG.error("Could not handle empty message from \"{}\": {}", remoteSocket, coapMessage);
            }
            return false;
        }
    }


    private boolean handleInboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {

        int messageType = coapResponse.getMessageType();

        if (messageType == MessageType.ACK) {
            Token token = removeTransfer(remoteSocket, coapResponse.getMessageID());
            if (token == null) {
                LOG.warn("Received ACK response for unknown request (remote socket \"{}\", message ID: {})",
                    remoteSocket, coapResponse.getMessageID()
                );
            }
        } else {
            Token token = coapResponse.getToken();
            Integer messageID = removeTransfer(remoteSocket, token);
            if (messageID != null) {
                LOG.info("Received response for request (remote socket \"{}\", message ID: {})",
                        remoteSocket, messageID
                );
            }
        }
        return true;
    }

//    private int assignMessageID(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
//        int messageID = this.messageIDFactory.getNextMessageID(remoteSocket, coapMessage.getToken());
//
//        if (!(messageID == CoapMessage.UNDEFINED_MESSAGE_ID)) {
//            coapMessage.setMessageID(messageID);
//            LOG.debug("Message ID set to {}.", messageID);
//
//        }
//        return messageID;
//    }

    private void addTransfer(InetSocketAddress remoteSocket, int messageID, Token token) {
        try {
            this.lock.writeLock().lock();
            this.transfers1.put(remoteSocket, messageID, token);
            this.transfers2.put(remoteSocket, token, messageID);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private boolean needsRetransmission(InetSocketAddress remoteSocket, int messageID) {
        try {
            this.lock.readLock().lock();
            return !(this.transfers1.get(remoteSocket, messageID) == null);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private Integer removeTransfer(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            if (this.transfers2.get(remoteSocket, token) == null) {
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            Integer messageID = this.transfers2.remove(remoteSocket, token);
            this.transfers1.remove(remoteSocket, messageID);
            return messageID;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private Token removeTransfer(InetSocketAddress remoteSocket, int messageID) {
        try {
            this.lock.readLock().lock();
            if (this.transfers1.get(remoteSocket, messageID) == null) {
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            Token token = this.transfers1.remove(remoteSocket, messageID);
            this.transfers2.remove(remoteSocket, token);
            return token;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void scheduleRetransmission(CoapMessage coapMessage, InetSocketAddress remoteSocket, int retransmissionNo) {
        long delay = provideRetransmissionDelay(retransmissionNo);
        RequestRetransmissionTask task = new RequestRetransmissionTask(coapMessage, remoteSocket, retransmissionNo);
        getExecutor().schedule(task, delay, TimeUnit.MILLISECONDS);
    }


    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof MessageIDFactory.ReleasedMessageID) {
            InetSocketAddress remoteSocket = ((MessageIDFactory.ReleasedMessageID) arg).getRemoteSocket();
            int messageID = ((MessageIDFactory.ReleasedMessageID) arg).getMessageID();

            if (removeTransfer(remoteSocket, messageID) != null) {
                // there was an ongoing outbound transfer (i.e. CON with no ACK or NON with no response)
                Token token = ((MessageIDFactory.ReleasedMessageID) arg).getToken();
                LOG.warn("Transmission timed out (remote socket: \"{}\", token: {}, message ID: {})",
                        new Object[]{remoteSocket, token, messageID});
                triggerEvent(new TransmissionTimeoutEvent(remoteSocket, messageID, token), true);
            } else {
                LOG.info("Message ID retirement does not lead to a transmission timeout (GOOD!)");
            }
        } else {
            LOG.error("This should never happen...");
        }
    }


    class RequestRetransmissionTask implements Runnable{

        private final CoapMessage coapRequest;
        private final InetSocketAddress remoteSocket;
        private final int retransmissionNo;

        private RequestRetransmissionTask(CoapMessage coapRequest, InetSocketAddress remoteSocket, int retransmissionNo) {
            this.coapRequest = coapRequest;
            this.remoteSocket = remoteSocket;
            this.retransmissionNo = retransmissionNo;
        }

        @Override
        public void run() {
            if (needsRetransmission(remoteSocket, coapRequest.getMessageID())) {
                // retransmit message
                ChannelFuture future = Channels.future(getContext().getChannel());
                Channels.write(getContext(), future, coapRequest, remoteSocket);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        int messageID = coapRequest.getMessageID();
                        Token token = coapRequest.getToken();
                        if (future.isSuccess()) {
                            triggerEvent(new MessageRetransmittedEvent(remoteSocket, messageID, token), false);
                        } else {
                            String desc = "Could not sent retransmission (\"" + future.getCause().getMessage() + "\"";
                            triggerEvent(new MiscellaneousErrorEvent(remoteSocket, messageID, token, desc), false);
                        }
                    }
                });

                if (retransmissionNo < MAX_RETRANSMISSIONS) {
                    scheduleRetransmission(coapRequest, remoteSocket, retransmissionNo + 1);
                    LOG.debug("Scheduled next retransmission to \"{}\" (Message ID: {})",
                        remoteSocket, coapRequest.getMessageID()
                    );
                } else {
                    LOG.warn("No more retransmissions (remote endpoint: {}, message ID: {})!",
                           remoteSocket, coapRequest.getMessageID()
                    );
                }
            }
        }
    }
}