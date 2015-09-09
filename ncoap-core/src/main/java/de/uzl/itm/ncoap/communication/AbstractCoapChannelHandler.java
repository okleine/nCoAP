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
import de.uzl.itm.ncoap.communication.events.client.LazyObservationTerminationEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.communication.events.server.ObserverAcceptedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import org.jboss.netty.channel.*;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by olli on 04.09.15.
 */
public abstract class AbstractCoapChannelHandler extends SimpleChannelHandler{

    private ScheduledExecutorService executor;

    protected AbstractCoapChannelHandler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public final void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        Object message = me.getMessage();
        if (message instanceof CoapMessage) {
            if(!handleInboundCoapMessage(ctx, (CoapMessage) message, (InetSocketAddress) me.getRemoteAddress())) {
                return;
            }
        } else if (message instanceof TokenReleasedEvent && this instanceof TokenReleasedEvent.Handler) {
            ((TokenReleasedEvent.Handler) this).handleEvent((TokenReleasedEvent) message);
        } else if (message instanceof EmptyAckReceivedEvent && this instanceof EmptyAckReceivedEvent.Handler) {
            ((EmptyAckReceivedEvent.Handler) this).handleEvent((EmptyAckReceivedEvent) message);
        } else if (message instanceof MessageIDAssignedEvent && this instanceof MessageIDAssignedEvent.Handler) {
            ((MessageIDAssignedEvent.Handler) this).handleEvent((MessageIDAssignedEvent) message);
        } else if (message instanceof MessageRetransmittedEvent && this instanceof MessageRetransmittedEvent.Handler) {
            ((MessageRetransmittedEvent.Handler) this).handleEvent((MessageRetransmittedEvent) message);
        } else if (message instanceof MiscellaneousErrorEvent && this instanceof MiscellaneousErrorEvent.Handler) {
            ((MiscellaneousErrorEvent.Handler) this).handleEvent((MiscellaneousErrorEvent) message);
        } else if (message instanceof RemoteSocketChangedEvent && this instanceof RemoteSocketChangedEvent.Handler) {
            ((RemoteSocketChangedEvent.Handler) this).handleEvent((RemoteSocketChangedEvent) message);
        } else if (message instanceof ResetReceivedEvent && this instanceof ResetReceivedEvent.Handler) {
            ((ResetReceivedEvent.Handler) this).handleEvent((ResetReceivedEvent) message);
        } else if (message instanceof TransmissionTimeoutEvent && this instanceof TransmissionTimeoutEvent.Handler) {
            ((TransmissionTimeoutEvent.Handler) this).handleEvent((TransmissionTimeoutEvent) message);
        } else if (message instanceof LazyObservationTerminationEvent && this instanceof LazyObservationTerminationEvent.Handler) {
            ((LazyObservationTerminationEvent.Handler) this).handleEvent((LazyObservationTerminationEvent) message);
        } else if (message instanceof ObserverAcceptedEvent && this instanceof ObserverAcceptedEvent.Handler) {
            ((ObserverAcceptedEvent.Handler) this).handleEvent((ObserverAcceptedEvent) message);
        }

        ctx.sendUpstream(me);
    }

    public final void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        Object message = me.getMessage();

        if(me.getMessage() instanceof CoapMessage) {
            if (!handleOutboundCoapMessage(ctx, (CoapMessage) message, (InetSocketAddress) me.getRemoteAddress())) {
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
     * @param ctx
     * @param coapMessage
     * @param remoteSocket
     *
     * @return <code>true</code> if this {@link AbstractCoapChannelHandler} is to be
     * further processed by the next handler(s) and <code>false</code> otherwise.
     */
    public abstract boolean handleInboundCoapMessage(ChannelHandlerContext ctx, CoapMessage coapMessage,
            InetSocketAddress remoteSocket);

    /**
     *
     * @param ctx
     * @param coapMessage
     * @param remoteSocket
     *
     * @return <code>true</code> if this {@link AbstractCoapChannelHandler} is to be
     * further processed by the next handler(s) and <code>false</code> otherwise.
     */
    public abstract boolean handleOutboundCoapMessage(ChannelHandlerContext ctx, CoapMessage coapMessage,
            InetSocketAddress remoteSocket);


    protected void triggerEvent(final Channel channel, final AbstractMessageExchangeEvent event) {
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                Channels.fireMessageReceived(channel, event);
            }
        });
    }
}
