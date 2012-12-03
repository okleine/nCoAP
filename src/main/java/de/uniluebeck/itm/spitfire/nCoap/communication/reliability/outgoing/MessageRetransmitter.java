package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

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
    private CoapMessage coapMessage;
    private int retransmitNo;
    private ChannelHandlerContext ctx;

    public MessageRetransmitter(ChannelHandlerContext ctx, InetSocketAddress rcptAddress,CoapMessage coapMessage,
                                int retransmitNo){
        this.rcptAddress = rcptAddress;
        this.retransmitNo = retransmitNo;
        this.coapMessage = coapMessage;
        this.ctx = ctx;
    }

    @Override
    public void run() {

        ChannelFuture future = Channels.future(ctx.getChannel());

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Retransmition completed {}", MessageRetransmitter.this);
            }
        });

       Channels.write(ctx, future, coapMessage, rcptAddress);
    }

    @Override
    public String toString() {
        return "{[" + this.getClass().getName() + "] " +
                "RetransmitNo " + retransmitNo +
                ", MsgID " + coapMessage.getMessageID() +
                ", RcptAddress " + rcptAddress + "}";
    }
}
