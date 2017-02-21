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
package de.uzl.itm.ncoap.communication;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uzl.itm.ncoap.communication.events.AbstractMessageExchangeEvent;
import de.uzl.itm.ncoap.communication.events.EmptyAckReceivedEvent;
import de.uzl.itm.ncoap.communication.events.MessageIDAssignedEvent;
import de.uzl.itm.ncoap.communication.events.MessageIDReleasedEvent;
import de.uzl.itm.ncoap.communication.events.MessageRetransmittedEvent;
import de.uzl.itm.ncoap.communication.events.MiscellaneousErrorEvent;
import de.uzl.itm.ncoap.communication.events.NoMessageIDAvailableEvent;
import de.uzl.itm.ncoap.communication.events.ResetReceivedEvent;
import de.uzl.itm.ncoap.communication.events.TransmissionTimeoutEvent;
import de.uzl.itm.ncoap.communication.events.client.ContinueResponseReceivedEvent;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.ResponseBlockReceivedEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.communication.events.server.ObserverAcceptedEvent;
import de.uzl.itm.ncoap.communication.events.server.RemoteClientSocketChangedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapMessageEnvelope;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

//import de.uzl.itm.ncoap.communication.events.client.LazyObservationTerminationEvent;

/**
 * Abstract base class for all {@link ChannelHandler} instances handling inbound or outbound messages
 *
 * @author Oliver Kleine
 */
public abstract class AbstractCoapChannelHandler extends ChannelDuplexHandler{

    private static Logger LOG = LoggerFactory.getLogger(AbstractCoapChannelHandler.class.getName());

    private ScheduledExecutorService executor;
    private ChannelHandlerContext context;

    /**
     * Creates a new instance of {@link AbstractCoapChannelHandler}
     *
     * @param executor the {@link ScheduledExecutorService} used to execute I/O tasks
     */
    protected AbstractCoapChannelHandler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        if (message instanceof CoapMessageEnvelope) {
            CoapMessageEnvelope envelope = (CoapMessageEnvelope)message;
            if (!handleInboundCoapMessage(envelope.content(), envelope.sender())) {
                ReferenceCountUtil.release(message);
                return;
            }
        }
        ctx.fireChannelRead(message);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        setContext(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object message) throws Exception {
        if (message instanceof TokenReleasedEvent && this instanceof TokenReleasedEvent.Handler) {
            ((TokenReleasedEvent.Handler) this).handleEvent((TokenReleasedEvent) message);
        } else if (message instanceof EmptyAckReceivedEvent && this instanceof EmptyAckReceivedEvent.Handler) {
            ((EmptyAckReceivedEvent.Handler) this).handleEvent((EmptyAckReceivedEvent) message);
        } else if (message instanceof MessageIDAssignedEvent && this instanceof MessageIDAssignedEvent.Handler) {
            ((MessageIDAssignedEvent.Handler) this).handleEvent((MessageIDAssignedEvent) message);
        } else if (message instanceof NoMessageIDAvailableEvent && this instanceof NoMessageIDAvailableEvent.Handler) {
            ((NoMessageIDAvailableEvent.Handler) this).handleEvent((NoMessageIDAvailableEvent) message);
        } else if (message instanceof MessageRetransmittedEvent && this instanceof MessageRetransmittedEvent.Handler) {
            ((MessageRetransmittedEvent.Handler) this).handleEvent((MessageRetransmittedEvent) message);
        } else if (message instanceof MiscellaneousErrorEvent && this instanceof MiscellaneousErrorEvent.Handler) {
            ((MiscellaneousErrorEvent.Handler) this).handleEvent((MiscellaneousErrorEvent) message);
        } else if (message instanceof RemoteServerSocketChangedEvent &&
                this instanceof RemoteServerSocketChangedEvent.Handler) {
            ((RemoteServerSocketChangedEvent.Handler) this).handleEvent((RemoteServerSocketChangedEvent) message);
        } else if (message instanceof RemoteClientSocketChangedEvent &&
                this instanceof RemoteClientSocketChangedEvent.Handler) {
            ((RemoteClientSocketChangedEvent.Handler) this).handleEvent((RemoteClientSocketChangedEvent) message);
        } else if (message instanceof ResetReceivedEvent && this instanceof ResetReceivedEvent.Handler) {
            ((ResetReceivedEvent.Handler) this).handleEvent((ResetReceivedEvent) message);
        } else if (message instanceof TransmissionTimeoutEvent && this instanceof TransmissionTimeoutEvent.Handler) {
            ((TransmissionTimeoutEvent.Handler) this).handleEvent((TransmissionTimeoutEvent) message);
        } else if (message instanceof ObserverAcceptedEvent && this instanceof ObserverAcceptedEvent.Handler) {
            ((ObserverAcceptedEvent.Handler) this).handleEvent((ObserverAcceptedEvent) message);
        } else if (message instanceof ResponseBlockReceivedEvent &&
                this instanceof ResponseBlockReceivedEvent.Handler) {
            ((ResponseBlockReceivedEvent.Handler) this).handleEvent((ResponseBlockReceivedEvent) message);
        } else if (message instanceof ContinueResponseReceivedEvent &&
                this instanceof ContinueResponseReceivedEvent.Handler) {
            ((ContinueResponseReceivedEvent.Handler) this).handleEvent((ContinueResponseReceivedEvent) message);
        } else if (message instanceof MessageIDReleasedEvent && this instanceof MessageIDReleasedEvent.Handler) {
            ((MessageIDReleasedEvent.Handler) this).handleEvent((MessageIDReleasedEvent) message);
        }

        ctx.fireUserEventTriggered(message);
    }

    /**
     * Sets the {@link ChannelHandlerContext} of this handler
     *
     * @param context the {@link ChannelHandlerContext} of this handler
     */
    public void setContext(ChannelHandlerContext context) {
        this.context = context;
    }

    /**
     * Returns the {@link ChannelHandlerContext} of this handler
     * @return the {@link ChannelHandlerContext} of this handler
     */
    public ChannelHandlerContext getContext() {
        return this.context;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof CoapMessageEnvelope) {
            CoapMessageEnvelope envelope = (CoapMessageEnvelope) message;
            if (!handleOutboundCoapMessage(envelope.content(), envelope.recipient())) {
                ReferenceCountUtil.release(message);
                return;
            }
        }
        ctx.write(message);
    }

    /**
     * Returns the {@link ScheduledExecutorService} used to execute I/O tasks
     *
     * @return the {@link ScheduledExecutorService} used to execute I/O tasks
     */
    protected ScheduledExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * This method is called by the framework for every inbound {@link CoapMessage}
     *
     * @param coapMessage the {@link CoapMessage} that was received
     * @param remoteSocket the socket address of the remote endpoint (i.e. the sender of the received message)
     *
     * @return <code>true</code> if this {@link AbstractCoapChannelHandler} is to be
     * further processed by the next handler(s) and <code>false</code> otherwise.
     */
    public abstract boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket);


    /**
     *
     * This method is called by the framework for every outbound {@link CoapMessage}
     *
     * @param coapMessage the {@link CoapMessage} that is to be sent
     * @param remoteSocket the recipient of the message
     *
     * @return <code>true</code> if this {@link AbstractCoapChannelHandler} is to be
     * further processed by the next handler(s) and <code>false</code> otherwise.
     */
    public abstract boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket);


    /**
     * Triggers an internal event, i.e. some extension of {@link AbstractMessageExchangeEvent}
     *
     * @param event the event
     * @param bottomUp <code>true</code> if this event is to be handled by all {@link ChannelHandler}s in the
     *                 {@link ChannelPipeline} or <code>false</code> if the event is to be handled by
     *                 {@link ChannelHandler}s that are located "upstream" from the {@link ChannelHandler} that
     *                 triggered this event.
     */
    protected void triggerEvent(final AbstractMessageExchangeEvent event, boolean bottomUp) {
        if (bottomUp) {
            context.pipeline().fireUserEventTriggered(event);
        } else {
            context.fireUserEventTriggered(event);
        }
    }

    /**
     * Continues the internal processing of the {@link CoapMessage} which was previously stopped for some reason
     *
     * @param coapMessage the {@link CoapMessage} to be sent further upstream
     * @param remoteSocket the socket address of the sender of the message to be further processed
     */
    protected void continueMessageProcessing(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        // Channels.fireMessageReceived(this.context, coapMessage, remoteSocket);
        throw new AbstractMethodError("Need to fix this.");
    }

    /**
     * Sends an empty {@link CoapMessage} with {@link de.uzl.itm.ncoap.message.MessageType#ACK}.
     *
     * @param messageID the message ID of this empty ACK
     * @param remoteSocket the recipient of this empty ACK
     */
    protected void sendEmptyACK(int messageID, final InetSocketAddress remoteSocket) {
        final CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
        // Need to encode CoapMessage to UDP with remote socket here.
        ChannelFuture future = context.write(new CoapMessageEnvelope(emptyACK, remoteSocket));
        if (LOG.isDebugEnabled()) {
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    int messageID = emptyACK.getMessageID();
                    LOG.debug("Empty ACK sent to \"{}\" (message ID: {}).", remoteSocket, messageID);
                }
            });
        }
    }

    /**
     * Sends an empty {@link CoapMessage} with {@link de.uzl.itm.ncoap.message.MessageType#RST}.
     *
     * @param messageID the message ID of this empty RST
     * @param remoteSocket the recipient of this empty RST
     */
    protected void sendReset(int messageID, final InetSocketAddress remoteSocket) {
        final CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
        ChannelFuture future = context.writeAndFlush(new CoapMessageEnvelope(resetMessage, remoteSocket));
        if (LOG.isDebugEnabled()) {
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    int messageID = resetMessage.getMessageID();
                    LOG.debug("RST sent to \"{}\" (Message ID: {}).", remoteSocket, messageID);
                }
            });
        }
    }

    /**
     * Sends a {@link CoapMessage}
     *
     * @param coapMessage the {@link CoapMessage} to be sent
     * @param remoteSocket the recipient of the message to be sent
     *
     * @return a {@link ChannelFuture} that will contain the result of the message writing process
     */
    protected ChannelFuture sendCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        return context.writeAndFlush(new CoapMessageEnvelope(coapMessage, remoteSocket));
    }

    /**
     * Sends a {@link CoapMessage}
     *
     * @param coapMessage the {@link CoapMessage} to be sent
     * @param remoteSocket the recipient of the message to be sent
     * @param future the {@link ChannelFuture} that will be set with the result of the message writing process
     */
    protected void sendCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket, ChannelFuture future) {
        context.writeAndFlush(new CoapMessageEnvelope(coapMessage, remoteSocket));
    }

    protected ScheduledFuture scheduleTask(Runnable task, long delay, TimeUnit unit) {
        return this.getExecutor().schedule(task, delay, unit);
    }
}