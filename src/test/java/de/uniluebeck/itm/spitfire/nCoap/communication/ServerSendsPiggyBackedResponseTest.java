//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapMessageReceiver;
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import java.net.InetSocketAddress;
//
//import static de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication.DEFAULT_COAP_SERVER_PORT;
//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.net.URI;
//import java.util.Arrays;
//import java.util.SortedMap;
//
//import static junit.framework.Assert.*;
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
//
//
///**
// * Tests to verify the client functionality related to piggy-backed responses.
// *
// * @author Oliver Kleine, Stefan Hueske
// */
//public class ServerSendsPiggyBackedResponseTest {
//
//    private static CoapTestClient testClient= CoapTestClient.getInstance();
//    private static CoapMessageReceiver testReceiver = CoapMessageReceiver.getInstance();
//
//    //request
//    private static URI targetUri;
//    private static CoapRequest coapRequest;
//    private static String requestPath;
//    private static int requestMsgID;
//    private static byte[] requestToken;
//
//    //response
//    private static CoapResponse coapResponse;
//    private static String responsePayload;
//
//    @BeforeClass
//    public static void init() throws Exception {
//        //init
//        testClient.reset();
//        testReceiver.reset();
//        testReceiver.setReceiveEnabled(true);
//        testReceiver.setWriteEnabled(true);
//
//        //create request
//        requestToken = new byte[]{0x12, 0x23, 0x34};
//        requestPath = "/testpath";
//        requestMsgID = 3333;
//        targetUri = new URI("coap://localhost:" + CoapMessageReceiver.RECEIVER_PORT + requestPath);
//        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri, testClient);
//        coapRequest.getHeader().setMsgID(requestMsgID);
//        coapRequest.setToken(requestToken);
//
//        //create response
//        responsePayload = "testpayload";
//        coapResponse = new CoapResponse(Code.CONTENT_205);
//        coapResponse.setPayload(responsePayload.getBytes("UTF-8"));
//        coapResponse.getHeader().setMsgType(MsgType.ACK);
//
//        //register respone, allow to set msg id and token
//        testReceiver.addResponse(new CoapMessageReceiver.MessageReceiverResponse(coapResponse, true, true));
//
//        //write request, disable receiving after 500ms
//        testClient.writeCoapRequest(coapRequest);
//
//        Thread.sleep(300);
//
//        //send message again to see if callback was removed
//        testReceiver.writeMessage(coapResponse, new InetSocketAddress("localhost",
//                DEFAULT_COAP_SERVER_PORT));
//
//        Thread.sleep(300);
//
//        testReceiver.setReceiveEnabled(false);
//    }
//
//    @Test
//    public void testReceivedRequestEqualsSentRequest() {
//        SortedMap<Long, CoapMessage> receivedRequests = testReceiver.getReceivedMessages();
//        String message = "Written and received request do not equal";
//        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
//    }
//
//    @Test
//    public void testReceiverReceivedOnlyOneRequest() {
//        String message = "Receiver received more than one message";
//        assertEquals(message, 1, testReceiver.getReceivedMessages().values().size());
//    }
//
//    @Test
//    public void testClientCallbackInvokedOnce() {
//        String message = "Client callback was invoked less or more than once";
//        assertEquals(message, 1, testClient.getReceivedResponses().values().size());
//    }
//}
