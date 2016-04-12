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
package de.uzl.itm.ncoap.communication;

import de.uzl.itm.ncoap.communication.events.*;
//import de.uzl.itm.ncoap.communication.events.client.LazyObservationTerminationEvent;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.ResponseBlockReceivedEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.communication.events.server.ObserverAcceptedEvent;
import de.uzl.itm.ncoap.communication.events.server.RemoteClientSocketChangedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by olli on 04.09.15.
 */
public abstract class AbstractCoapChannelHandler extends SimpleChannelHandler{

    private static Logger LOG = LoggerFactory.getLogger(AbstractCoapChannelHandler.class.getName());

    private ScheduledExecutorService executor;
    private ChannelHandlerContext context;

    protected AbstractCoapChannelHandler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public final void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        Object message = me.getMessage();
        if (message instanceof CoapMessage) {
            if(!handleInboundCoapMessage((CoapMessage) message, (InetSocketAddress) me.getRemoteAddress())) {
                return;
            }
        } else if (message instanceof TokenReleasedEvent && this instanceof TokenReleasedEvent.Handler) {
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
        }

        ctx.sendUpstream(me);
    }

    public void setContext(ChannelHandlerContext context) {
        this.context = context;
    }

    public ChannelHandlerContext getContext() {
        return this.context;
    }

    public final void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        Object message = me.getMessage();

        if(me.getMessage() instanceof CoapMessage) {
            if (!handleOutboundCoapMessage((CoapMessage) message, (InetSocketAddress) me.getRemoteAddress())) {
                me.getFuture().cancel();
                return;
            }
        }
        ctx.sendDownstream(me);
    }

    protected ScheduledExecutorService getExecutor() {
        return this.executor;
    }

    /**
     *
     * @param coapMessage
     * @param remoteSocket
     *
     * @return <code>true</code> if this {@link AbstractCoapChannelHandler} is to be
     * further processed by the next handler(s) and <code>false</code> otherwise.
     */
    public abstract boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket);


    /**
     *
     * @param coapMessage
     * @param remoteSocket
     *
     * @return <code>true</code> if this {@link AbstractCoapChannelHandler} is to be
     * further processed by the next handler(s) and <code>false</code> otherwise.
     */
    public abstract boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket);


    protected void triggerEvent(final AbstractMessageExchangeEvent event, boolean bottomUp) {
        if(bottomUp) {
            Channels.fireMessageReceived(context.getChannel(), event);
        } else {
            Channels.fireMessageReceived(this.context, event);
        }
    }

    protected void continueMessageProcessing(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        Channels.fireMessageReceived(this.context, coapMessage, remoteSocket);
    }


    protected void sendEmptyACK(int messageID, final InetSocketAddress remoteSocket) {
        final CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
        ChannelFuture future = Channels.future(getContext().getChannel());
        Channels.write(getContext(), future, emptyACK, remoteSocket);
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

    protected void sendReset(int messageID, final InetSocketAddress remoteSocket) {
        final CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
        ChannelFuture future = Channels.future(getContext().getChannel());
        Channels.write(getContext(), future, resetMessage, remoteSocket);
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

    protected ChannelFuture sendCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        ChannelFuture future = Channels.future(getContext().getChannel());
        sendCoapMessage(coapMessage, remoteSocket, future);
        return future;
    }

    protected void sendCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket, ChannelFuture future) {
        Channels.write(getContext(), future, coapMessage, remoteSocket);
    }
}
