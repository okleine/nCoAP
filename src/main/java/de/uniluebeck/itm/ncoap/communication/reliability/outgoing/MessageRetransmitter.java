package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import de.uniluebeck.itm.ncoap.communication.observe.InternalUpdateNotificationRetransmissionMessage;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import org.jboss.netty.channel.UpstreamMessageEvent;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;

/**
 * Implementation of {@link Runnable} to retransmit a confirmable message in exponentially increasing
 * intervals.
 */
class MessageRetransmitter implements Runnable {

    private Logger log = LoggerFactory.getLogger(MessageRetransmitter.class.getName());

    private InetSocketAddress rcptAddress;
    private RetransmissionSchedule retransmissionSchedule;
    private int retransmitNo;
    private ChannelHandlerContext ctx;

    /**
     * @param ctx the {@link ChannelHandlerContext} to get the {@link DatagramChannel} to be used to send the
     *            outgoing {@link CoapMessage}s
     * @param rcptAddress the address of the recipient
     * @param retransmissionSchedule the {@link RetransmissionSchedule} providing the times of retransmission
     * @param retransmitNo the number of previously sent retransmitions plus 1
     */
    public MessageRetransmitter(ChannelHandlerContext ctx, InetSocketAddress rcptAddress,
            RetransmissionSchedule retransmissionSchedule, int retransmitNo){
        this.rcptAddress = rcptAddress;
        this.retransmitNo = retransmitNo;
        this.retransmissionSchedule = retransmissionSchedule;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        final CoapMessage coapMessage = retransmissionSchedule.getCoapMessage();
        if (!coapMessage.getOption(OBSERVE_RESPONSE).isEmpty()) {

            CoapResponse coapResponse = (CoapResponse) coapMessage;

            //increment OBSERVE notification count before retransmission
            long notificationCount = (Long) coapResponse.getOption(OBSERVE_RESPONSE).get(0).getDecodedValue() + 1;

            try {
                coapResponse.setObserveOptionValue(notificationCount);
                UpstreamMessageEvent upstreamEvent = new UpstreamMessageEvent(ctx.getChannel(),
                        new InternalUpdateNotificationRetransmissionMessage(rcptAddress,
                                coapResponse.getServicePath()), null);

                ctx.sendUpstream(upstreamEvent);
            }
            catch (ToManyOptionsException ex) {
                log.error("This should never happen.", ex);
            }
        }
        
        ChannelFuture future = Channels.future(ctx.getChannel());

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Retransmition completed {}", MessageRetransmitter.this);
                UpstreamMessageEvent upstreamEvent = new UpstreamMessageEvent(ctx.getChannel(),
                        new InternalMessageRetransmissionMessage(rcptAddress, coapMessage.getToken()), null);

                ctx.sendUpstream(upstreamEvent);
            }
        });

       Channels.write(ctx, future, coapMessage, rcptAddress);
    }

    @Override
    public String toString() {
        CoapMessage coapMessage = retransmissionSchedule.getCoapMessage();
        return  "RetransmitNo " + retransmitNo +
                (coapMessage == null ? "" : (", MsgID " + coapMessage.getMessageID())) +
                ", RcptAddress " + rcptAddress + "}";
    }

}
