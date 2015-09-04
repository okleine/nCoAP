package de.uzl.itm.ncoap.communication;

import de.uzl.itm.ncoap.communication.events.*;
import de.uzl.itm.ncoap.communication.events.client.ObservationCancelledEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.net.InetSocketAddress;

/**
 * Created by olli on 04.09.15.
 */
public abstract class AbstractCoapChannelHandler extends SimpleChannelHandler{

    public final void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        Object msg = me.getMessage();
        if (msg instanceof CoapMessage) {
            if(!handleInboundCoapMessage(ctx, (CoapMessage) msg, (InetSocketAddress) me.getRemoteAddress())) {
                return;
            }
        } else if (msg instanceof MessageExchangeFinishedEvent) {
            handleConversationFinishedEvent(ctx, (MessageExchangeFinishedEvent) msg);
        } else if (msg instanceof EmptyAckReceivedEvent) {
            handleEmptyAckReceivedEvent(ctx, (EmptyAckReceivedEvent) msg);
        } else if (msg instanceof MessageIDAssignedEvent) {
            handleMessageIDAssignedEvent(ctx, (MessageIDAssignedEvent) msg);
        } else if (msg instanceof MessageIDReleasedEvent && this instanceof MessageIDReleasedEvent.Handler) {
            ((MessageIDReleasedEvent.Handler) this).handleMessageIDReleasedEvent((MessageIDReleasedEvent) msg);
        } else if (msg instanceof MessageRetransmittedEvent) {
            handleMessageRetransmittedEvent(ctx, (MessageRetransmittedEvent) msg);
        } else if (msg instanceof MiscellaneousErrorEvent) {
            handleMiscellaneousErrorEvent(ctx, (MiscellaneousErrorEvent) msg);
        } else if (msg instanceof RemoteSocketChangedEvent && this instanceof RemoteSocketChangedEvent.Handler) {
            ((RemoteSocketChangedEvent.Handler) this).handleRemoteSocketChangedEvent((RemoteSocketChangedEvent) msg);
        } else if (msg instanceof ResetReceivedEvent && this instanceof ResetReceivedEvent.Handler) {
            ((ResetReceivedEvent.Handler) this).handleResetReceivedEvent((ResetReceivedEvent) msg);
        } else if (msg instanceof TransmissionTimeoutEvent) {
            handleTransmissionTimeoutEvent(ctx, (TransmissionTimeoutEvent) msg);
        } else if (msg instanceof ObservationCancelledEvent) {
            handleObservationCancelledEvent(ctx, (ObservationCancelledEvent) msg);
        }

        ctx.sendUpstream(me);
    }

    public final void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        Object msg = me.getMessage();

        if(me.getMessage() instanceof CoapMessage){
            if(!handleOutboundCoapMessage(ctx, (CoapMessage) msg, (InetSocketAddress) me.getRemoteAddress())){
                return;
            }
        } else if (msg instanceof ObserverAcceptedEvent && this instanceof ObserverAcceptedEvent.Handler) {
            ((ObserverAcceptedEvent.Handler) this).handleObserverAcceptedEvent((ObserverAcceptedEvent) msg);
        } else if (msg instanceof ObservableWebresourceRegistrationEvent  &&
                this instanceof ObservableWebresourceRegistrationEvent.Handler) {
            ((ObservableWebresourceRegistrationEvent.Handler) this).handleObservableWebresourceRegistrationEvent(
                    (ObservableWebresourceRegistrationEvent) msg
            );
        }

        ctx.sendDownstream(me);
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

    public void handleConversationFinishedEvent(ChannelHandlerContext ctx, MessageExchangeFinishedEvent event){}

    public void handleEmptyAckReceivedEvent(ChannelHandlerContext ctx, EmptyAckReceivedEvent event){}

    public void handleMessageIDAssignedEvent(ChannelHandlerContext ctx, MessageIDAssignedEvent event){}



    public void handleMessageRetransmittedEvent(ChannelHandlerContext ctx, MessageRetransmittedEvent event){}

    public void handleMiscellaneousErrorEvent(ChannelHandlerContext ctx, MiscellaneousErrorEvent event){}

    public void handleObservationCancelledEvent(ChannelHandlerContext ctx, ObservationCancelledEvent event){}


    public void handleTransmissionTimeoutEvent(ChannelHandlerContext ctx, TransmissionTimeoutEvent event){}

}
