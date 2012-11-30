//package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalErrorMessage;
//import org.jboss.netty.channel.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 15.11.12
// * Time: 15:11
// * To change this template use File | Settings | File Templates.
// */
//class TimeOutNotifier implements Runnable{
//
//    Logger log = LoggerFactory.getLogger(TimeOutNotifier.class.getName());
//
//    private ChannelHandlerContext ctx;
//    private byte[] token;
//    private InetSocketAddress rcptAddress;
//
//    public TimeOutNotifier(ChannelHandlerContext ctx, byte[] token, InetSocketAddress rcptAddress){
//        this.ctx = ctx;
//        this.token = token;
//        this.rcptAddress = rcptAddress;
//    }
//
//    @Override
//    public void run() {
//        final InternalErrorMessage timeoutMessage =
//                new InternalErrorMessage("No response from server. Gave up retransmitting.", token);
//
//        UpstreamMessageEvent timeoutMessageEvent =
//                new UpstreamMessageEvent(ctx.getChannel(), timeoutMessage, rcptAddress);
//
//        ChannelFuture future = timeoutMessageEvent.getFuture();
//
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                log.info("Successfully sent end error message to application: {}", timeoutMessage);
//            }
//        });
//
//        ctx.sendUpstream(timeoutMessageEvent);
//    }
//}
