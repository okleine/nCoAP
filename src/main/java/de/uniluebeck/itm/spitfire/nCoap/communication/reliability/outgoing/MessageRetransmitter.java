package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler.ScheduledRetransmission;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.logging.Level;

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
    ScheduledRetransmission retransmission;
    private int retransmitNo;
    private ChannelHandlerContext ctx;

    public MessageRetransmitter(ChannelHandlerContext ctx, InetSocketAddress rcptAddress,
            ScheduledRetransmission retransmission, int retransmitNo){
        this.rcptAddress = rcptAddress;
        this.retransmitNo = retransmitNo;
        this.retransmission = retransmission;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        CoapMessage coapMessage = retransmission.getCoapMessage();
        if (!coapMessage.getOption(OptionRegistry.OptionName.OBSERVE_RESPONSE).isEmpty()) {
            //increment OBSERVE option (see http://tools.ietf.org/html/draft-ietf-core-observe-06#page-13)
            long observeOption = ((UintOption)coapMessage.getOption(OptionRegistry
                    .OptionName.OBSERVE_RESPONSE).get(0)).getDecodedValue();
            observeOption++;
            try {
                ((CoapResponse)coapMessage).setObserveOptionResponse(observeOption);
            } catch (ToManyOptionsException ex) {
                log.error("Error while trying to update OBSERVE option in MessageRetransmitter!");
            }
        }
        
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
        CoapMessage coapMessage = retransmission.getCoapMessage();
        return "{[" + this.getClass().getName() + "] " +
                "RetransmitNo " + retransmitNo +
                (coapMessage == null ? "" : (", MsgID " + coapMessage.getMessageID())) +
                ", RcptAddress " + rcptAddress + "}";
    }
    
    
}
