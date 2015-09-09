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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.MessageIDAssignedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageType;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This class is the {@link org.jboss.netty.channel.ChannelUpstreamHandler} to deal with inbound decoded
 * {@link de.uzl.itm.ncoap.message.CoapMessage}s at
 * {@link de.uzl.itm.ncoap.application.server.CoapServer}s. If the inbound message is a confirmable
 * {@link de.uzl.itm.ncoap.message.CoapRequest} it schedules the sending of an empty acknowledgement to the
 * sender if there wasn't a response from the addressed webresource within a period of 1.5 seconds.
 *
 * @author Oliver Kleine
 */
public class InboundReliabilityHandler extends AbstractCoapChannelHandler implements MessageIDAssignedEvent.Handler{

    /**
     * Minimum delay in milliseconds (1500) between the reception of a confirmable request and an empty ACK
     */
    public static final int EMPTY_ACK_DELAY = 1500;

    private static Logger LOG = LoggerFactory.getLogger(InboundReliabilityHandler.class.getName());

    private Multimap<InetSocketAddress, Token> awaitedResponses;
    private ReentrantReadWriteLock responsesLock;

    private boolean expectsRequests;
    private Table<InetSocketAddress, Integer, Token> unprocessedRequests;
    private Table<InetSocketAddress, Integer, ScheduledFuture> scheduledEmptyAcknowledgements;
    private ReentrantReadWriteLock requestsLock;



    /**
     * Creates a new instance of {@link InboundReliabilityHandler}
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to provide the threads to execute the
     *                 tasks for reliability.
     */
    public InboundReliabilityHandler(ScheduledExecutorService executor, boolean expectsRequests){
        super(executor);
        this.expectsRequests = expectsRequests;
        this.unprocessedRequests = HashBasedTable.create();
        this.awaitedResponses = HashMultimap.create();
        this.scheduledEmptyAcknowledgements = HashBasedTable.create();

        this.responsesLock = new ReentrantReadWriteLock();
        this.requestsLock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleInboundCoapMessage(ChannelHandlerContext ctx, CoapMessage coapMessage,
            final InetSocketAddress remoteSocket) {

        LOG.debug("HANDLE INBOUND MESSAGE: {}", coapMessage);

        final MessageType.Name messageType = coapMessage.getMessageTypeName();
        Token token = coapMessage.getToken();
        int messageID = coapMessage.getMessageID();

        if(coapMessage instanceof CoapResponse) {
            if(!this.isResponseAwaited(remoteSocket, token)) {
                if(messageType == MessageType.Name.CON) {
                    // response was unexpected (send RST)
                    final CoapMessage resetMessage = CoapMessage.createEmptyReset(coapMessage.getMessageID());
                    ChannelFuture future = Channels.future(ctx.getChannel());
                    Channels.write(ctx, future, resetMessage, remoteSocket);
                    if(LOG.isDebugEnabled()) {
                        future.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                int messageID = resetMessage.getMessageID();
                                LOG.debug("RST sent to \"{}\" (Message ID: {}).", remoteSocket, messageID);
                            }
                        });
                    }
                }
                LOG.debug("Received unexpected response from \"{}\" (token: {})", remoteSocket, token);
                return false;
            } else if(!((CoapResponse) coapMessage).isUpdateNotification()) {
                // this is the only expected response within this message transfer (i.e. no update notification)
                removeFromAwaitedResponses(remoteSocket, token);
            }

            if((messageType == MessageType.Name.CON)){
                // send empty ACK
                final CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(coapMessage.getMessageID());
                ChannelFuture future = Channels.future(ctx.getChannel());
                Channels.write(ctx, future, emptyACK, remoteSocket);
                if(LOG.isDebugEnabled()) {
                    future.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            int messageID = emptyACK.getMessageID();
                            LOG.debug("Empty ACK sent to \"{}\" (message ID: {}).", remoteSocket, messageID);
                        }
                    });
                }
            }
        } else if (coapMessage instanceof CoapRequest) {
            if(this.expectsRequests) {
                if (messageType == MessageType.Name.CON) {
                    scheduleEmptyAcknowledgementForRequest(ctx, remoteSocket, coapMessage.getMessageID());
                }
                if(!addUnprocessedRequest(remoteSocket, messageID, token)) {
                    LOG.info("Duplicate Request received from \"{}\" (message ID: {})", remoteSocket, messageID);
                    return false;
                }

            } else {
                LOG.warn("Client received request from \"{}\": {}", remoteSocket, coapMessage);
                return false;
            }
        } else if (coapMessage.isPingMessage()) {
            // CoAP PING
            final CoapMessage resetMessage = CoapMessage.createEmptyReset(coapMessage.getMessageID());
            ChannelFuture future = Channels.future(ctx.getChannel());
            Channels.write(ctx, future, resetMessage, remoteSocket);
            if(LOG.isDebugEnabled()) {
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        int messageID = resetMessage.getMessageID();
                        LOG.debug("RST sent to \"{}\" (Message ID: {}).", remoteSocket, messageID);
                    }
                });
            }
            return false;
        }

        return true;
    }


    @Override
    public boolean handleOutboundCoapMessage(ChannelHandlerContext ctx, CoapMessage coapMessage,
            InetSocketAddress remoteSocket) {

        LOG.info("HANDLE OUTBOUND MESSAGE: {}", coapMessage);
        if (coapMessage instanceof CoapRequest) {
            addToAwaitedResponses(remoteSocket, coapMessage.getToken());
            return true;
        } else if (coapMessage instanceof CoapResponse) {
            if (!cancelEmptyAcknowledgementForRequest(remoteSocket, coapMessage.getMessageID())) {
                // will be set by the next handler
                coapMessage.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
            } else {
                coapMessage.setMessageType(MessageType.Name.ACK);
                LOG.info("Changed message type to ACK!");
            }
        }
        return true;
    }


    private boolean addUnprocessedRequest(InetSocketAddress remoteSocket, int messageID, Token token) {
        try {
            this.requestsLock.readLock().lock();
            if (this.unprocessedRequests.contains(remoteSocket, messageID)) {
                return false;
            }
        } finally {
            this.requestsLock.readLock().unlock();
        }

        try {
            this.requestsLock.writeLock().lock();
            if (this.unprocessedRequests.contains(remoteSocket, messageID)) {
                return false;
            } else {
                this.unprocessedRequests.put(remoteSocket, messageID, token);
                return true;
            }
        } finally {
            this.requestsLock.writeLock().unlock();
        }
    }

    private void scheduleEmptyAcknowledgementForRequest(ChannelHandlerContext ctx, InetSocketAddress remoteSocket,
            int messageID){

        try {
            this.requestsLock.readLock().lock();
            if(this.scheduledEmptyAcknowledgements.contains(remoteSocket, messageID)) {
                LOG.debug("Empty ACK was already scheduled (RCPT: \"{}\", message ID: {}", remoteSocket, messageID);
                return;
            }
        } finally {
            this.requestsLock.readLock().unlock();
        }

        try {
            this.requestsLock.writeLock().lock();
            RequestConfirmationTask confirmationTask = new RequestConfirmationTask(ctx, remoteSocket, messageID);
            ScheduledFuture future = getExecutor().schedule(confirmationTask, EMPTY_ACK_DELAY, TimeUnit.MILLISECONDS);
            this.scheduledEmptyAcknowledgements.put(remoteSocket, messageID, future);
            LOG.debug("Scheduled empty ACK (RCPT: \"{}\", message ID: {}", remoteSocket, messageID);
        } finally {
            this.requestsLock.writeLock().unlock();
        }
    }

    private boolean cancelEmptyAcknowledgementForRequest(InetSocketAddress remoteSocket, int messageID){
        try {
            this.requestsLock.readLock().lock();
            ScheduledFuture future = this.scheduledEmptyAcknowledgements.get(remoteSocket, messageID);
            if (future == null || future.isDone()) {
                return false;
            }
        } finally {
            this.requestsLock.readLock().unlock();
        }

        ScheduledFuture future = removeFromScheduledEmptyAcknowledgements(remoteSocket, messageID);
        if(future != null && !future.isDone()) {
            if (future.cancel(false)) {
                LOG.info("Canceled empty ACK to \"{}\" (message ID: {})", remoteSocket, messageID);
                return true;
            } else {
                LOG.warn("Could NOT cancel empty ACK to \"{}\" (message ID: {})", remoteSocket, messageID);
                return false;
            }
        } else {
            return false;
        }
    }


    private ScheduledFuture removeFromScheduledEmptyAcknowledgements(InetSocketAddress remoteSocket, int messageID) {
        try {
            this.requestsLock.writeLock().lock();
            ScheduledFuture future = this.scheduledEmptyAcknowledgements.remove(remoteSocket, messageID);
            if(LOG.isDebugEnabled() && future != null) {
                LOG.debug("Removed scheduled empty ACK (Remaining: {})", this.scheduledEmptyAcknowledgements.size());
            }
            return future;
        } finally {
            this.requestsLock.writeLock().unlock();
        }
    }

    private boolean isResponseAwaited(InetSocketAddress remoteSocket, Token token){
        try {
            this.responsesLock.readLock().lock();
            return awaitedResponses.get(remoteSocket).contains(token);
        } finally {
            this.responsesLock.readLock().unlock();
        }
    }

    private void addToAwaitedResponses(InetSocketAddress remoteSocket, Token token) {
        try {
            this.responsesLock.writeLock().lock();
            this.awaitedResponses.put(remoteSocket, token);
            LOG.debug("Added to \"Expected Responses\" (Now: {})", this.awaitedResponses.size());
        } finally {
            this.responsesLock.writeLock().unlock();
        }
    }

    private boolean removeFromAwaitedResponses(InetSocketAddress remoteSocket, Token token){
        try {
            this.responsesLock.readLock().lock();
            if(!awaitedResponses.get(remoteSocket).contains(token)){
                return false;
            }
        } finally {
            this.responsesLock.readLock().unlock();
        }

        try {
            this.responsesLock.writeLock().lock();
            if(this.awaitedResponses.remove(remoteSocket, token)) {
                LOG.debug("Removed from \"Awaited Responses\" (Remaining: {})", this.awaitedResponses.size());
                return true;
            } else {
                return false;
            }
        } finally {
            this.responsesLock.writeLock().unlock();
        }
    }


    @Override
    public void handleEvent(MessageIDAssignedEvent event) {

    }


    private class RequestConfirmationTask implements Runnable{

        private ChannelHandlerContext ctx;
        private final InetSocketAddress remoteEndpoint;
        private final int messageID;

        public RequestConfirmationTask(ChannelHandlerContext ctx, InetSocketAddress remoteEndpoint, int messageID){
            this.ctx = ctx;
            this.remoteEndpoint = remoteEndpoint;
            this.messageID = messageID;
        }

        @Override
        public void run(){
            CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
            ChannelFuture future = Channels.future(ctx.getChannel());
            Channels.write(ctx, future, emptyACK, remoteEndpoint);
            removeFromScheduledEmptyAcknowledgements(remoteEndpoint, messageID);

            if(LOG.isInfoEnabled()){
              future.addListener(new ChannelFutureListener() {
                  @Override
                  public void operationComplete(ChannelFuture future) throws Exception {
                      LOG.info("Empty ACK sent (remote endpoint: {}, message ID: {})", remoteEndpoint, messageID);
                  }
              });
            }
        }
    }
}

