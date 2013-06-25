package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.communication.observe.InternalUpdateNotificationRetransmissionMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import org.jboss.netty.channel.UpstreamMessageEvent;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 15.11.12
 * Time: 15:09
 * To change this template use File | Settings | File Templates.
 */
class MessageRetransmitter implements Runnable {

    private Logger log = LoggerFactory.getLogger(MessageRetransmitter.class.getName());

    private InetSocketAddress rcptAddress;
    private RetransmissionSchedule retransmissionSchedule;
    private int retransmitNo;
    private ChannelHandlerContext ctx;

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

            //increment OBSERVE notification count before retransmissionSchedule
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
