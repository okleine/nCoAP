//package de.uniluebeck.itm.spitfire.nCoap.communication.observe;
//
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
//import org.jboss.netty.channel.ChannelFuture;
//import org.jboss.netty.channel.ChannelFutureListener;
//import org.jboss.netty.channel.Channels;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.util.concurrent.Callable;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 23.05.13
// * Time: 16:48
// * To change this template use File | Settings | File Templates.
// */
//public class UpdateNotificationSender implements Callable {
//
//    private static Logger log = LoggerFactory.getLogger(UpdateNotificationSender.class.getName());
//
//    private InetSocketAddress observerAddress;
//    private ObservationParameter observationStatus;
//
//    public UpdateNotificationSender(InetSocketAddress observerAddress, ObservationParameter observationStatus){
//        this.observerAddress = observerAddress;
//        this.observationStatus = observationStatus;
//    }
//
//    @Override
//    public ChannelFuture call() throws Exception {
//
//        CoapRequest coapRequest = new CoapRequest(MsgType.NON, Code.GET, -1, new URI());
//
//            CoapResponse coapResponse =
//                    webService.processMessage(observationStatus.getInitialRequest(), observerAddress);
//
//            observationStatus.increaseNotificationCount();
//            try {
//                coapResponse.setObserveOptionResponse(observationStatus.getNotificationCount());
//                coapResponse.setMessageID(observationStatus.getInitialRequest().getMessageID());
//                coapResponse.setToken(observationStatus.getInitialRequest().getToken());
//            } catch (ToManyOptionsException e) {
//                log.error("This should never happen.", e);
//            } catch (InvalidOptionException e) {
//                log.error("This should never happen.", e);
//            } catch (InvalidHeaderException e) {
//                log.error("This should never happen.", e);
//            }
//
//            ChannelFuture future = Channels.future(ctx.getChannel());
//            Channels.write(ctx, future, coapResponse, observerAddress);
//
//            notificationFutures.add(future);
//
//            future.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    log.info("Update notification for {} sent to {}", webService.getPath(),
//                            observerAddress);
//                }
//            });
//        }
//        return null;
//    }
//}
