//package de.uniluebeck.itm.spitfire.nCoap.application.server;
//
//import com.google.common.util.concurrent.ListenableFuture;
//import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalNullResponseFromWebserviceMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
//import org.jboss.netty.channel.ChannelFuture;
//import org.jboss.netty.channel.ChannelFutureListener;
//import org.jboss.netty.channel.socket.DatagramChannel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.net.InetSocketAddress;
//import java.nio.charset.Charset;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 25.05.13
// * Time: 15:54
// * To change this template use File | Settings | File Templates.
// */
//class CoapResponseSender implements Runnable{
//
//    private static Logger log = LoggerFactory.getLogger(CoapResponseSender.class.getName());
//
//    private ListenableFuture<CoapResponse> executionFuture;
//    private int messageID;
//    private byte[] token;
//    private InetSocketAddress remoteAddress;
//    //private String webServicePath;
//    private boolean observeResponseOption;
//    private DatagramChannel channel;
//
//    CoapResponseSender(DatagramChannel channel, int messageID, byte[] token, ListenableFuture<CoapResponse> executionFuture,
//                               InetSocketAddress remoteAddress, boolean observeResponseOption) {
//        this.executionFuture = executionFuture;
//        this.messageID = messageID;
//        this.token = token;
//        this.remoteAddress = remoteAddress;
//        this.observeResponseOption = observeResponseOption;
//        this.channel = channel;
//    }
//
//    @Override
//    public void run() {
//        CoapResponse coapResponse;
//        try {
//            coapResponse = executionFuture.get();
//
//            if(coapResponse == null){
//                handleNullResponse(remoteAddress, messageID);
//                return;
//            }
//        }
//        catch (Exception ex) {
//            coapResponse = new CoapResponse(Code.INTERNAL_SERVER_ERROR_500);
//            StringWriter errors = new StringWriter();
//            ex.printStackTrace(new PrintWriter(errors));
//            try {
//                coapResponse.setPayload(errors.toString().getBytes(Charset.forName("UTF-8")));
//            } catch (MessageDoesNotAllowPayloadException e) {
//                log.error("This should never happen.", e);
//            }
//        }
//
//        try{
//            //Set message ID and token to match the request
//            coapResponse.setMessageID(messageID);
//            if(token.length > 0){
//                coapResponse.setToken(token);
//            }
//
//            //Set observe response option if requested
//            if(observeResponseOption){
//                coapResponse.setObserveOptionResponse(0);
//            }
//
//            //coapResponse.setServicePath(webServicePath);
//
//        } catch (Exception ex) {
//            log.error("This should never happen.", ex);
//        }
//
//        //Write response
//        ChannelFuture future = channel.write(coapResponse, remoteAddress);
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                log.info("Response for token {} successfully sent to recipient {}.", new ByteArrayWrapper(token), remoteAddress);
//            }
//        });
//    }
//
//
//
//}
