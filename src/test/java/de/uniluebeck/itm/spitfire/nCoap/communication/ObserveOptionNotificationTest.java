//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapMessageReceiver;
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.SortedMap;
//
//import de.uniluebeck.itm.spitfire.nCoap.testtools.InitializeLoggingForTests;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import static junit.framework.Assert.*;
//import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
//import static de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication.DEFAULT_COAP_SERVER_PORT;
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableDummyWebService;
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
//
//
///**
// * Tests for Observe Option notifications.
// *
// * @author Stefan Hueske
// */
//public class ObserveOptionNotificationTest {
//
//    private static CoapTestServer testServer = CoapTestServer.getInstance();
//    private static CoapMessageReceiver testReceiver = CoapMessageReceiver.getInstance();
//
//    //request
//    private static URI targetUri;
//    private static CoapRequest coapRequest;
//    private static String requestPath;
//    private static int requestMsgID;
//    private static byte[] requestToken;
//
//    //notifications
//    private static CoapResponse notification1;
//    private static CoapResponse notification2;
//    private static CoapResponse notification3;
//
//    @BeforeClass
//    public static void init() throws Exception {
//        InitializeLoggingForTests.init();
//
//        //Wireshark: https://dl.dropbox.com/u/10179177/Screenshot_2013.04.11-19.41.51.png
//
//        //init
//        testReceiver.reset();
//        testServer.reset();
//        testReceiver.setReceiveEnabled(true);
//
//        //create request
//        requestToken = new byte[]{0x12, 0x23, 0x34};
//        requestPath = "/testpath";
//        requestMsgID = 4567;
//        targetUri = new URI("coap://localhost:" + CoapServerApplication.DEFAULT_COAP_SERVER_PORT + requestPath);
//        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        coapRequest.getHeader().setMsgID(requestMsgID);
//        coapRequest.setToken(requestToken);
//        coapRequest.setObserveOptionRequest();
//
//        //create notifications
//        (notification1 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload1".getBytes("UTF-8"));
//        (notification2 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload2".getBytes("UTF-8"));
//        (notification3 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload3".getBytes("UTF-8"));
//
//        //setup testServer
////        testServer.registerDummyService(requestPath);
//        //resource payload: String payload = getResourceStatus() ? "testpayload1" : "testpayload2";
//        ObservableDummyWebService observableDummyWebService = new ObservableDummyWebService(requestPath, true, 0, 0);
//        observableDummyWebService.addPreparedResponses(notification1, notification2, notification3);
//        testServer.registerService(observableDummyWebService);
//
////        testServer.addResponse(notification1, notification2, notification3);
//
//        //send request to testServer
//        testReceiver.writeMessage(coapRequest, new InetSocketAddress("localhost",
//                CoapServerApplication.DEFAULT_COAP_SERVER_PORT));
//
//        //response sequence
//        //wait for first response
//        Thread.sleep(150);
//        //notify
////        testServer.notifyCoapObservers();
//        observableDummyWebService.setResourceStatus(true);
//        //wait for 2nd response
//        Thread.sleep(150);
//        //send empty ack
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage lastReceivedMessage = receivedMessages.get(receivedMessages.lastKey());
//        CoapMessage emptyACK = new CoapResponse(Code.EMPTY);
//        emptyACK.getHeader().setMsgID(lastReceivedMessage.getMessageID());
//        emptyACK.getHeader().setMsgType(MsgType.ACK);
//        testReceiver.writeMessage(emptyACK, new InetSocketAddress("localhost", DEFAULT_COAP_SERVER_PORT));
//        //notify
////        testServer.notifyCoapObservers();
//        observableDummyWebService.setResourceStatus(true);
//        //wait for 3rd response
//        Thread.sleep(150);
//        //send RST
//        receivedMessages = testReceiver.getReceivedMessages();
//        lastReceivedMessage = receivedMessages.get(receivedMessages.lastKey());
//        emptyACK = new CoapResponse(Code.EMPTY);
//        emptyACK.getHeader().setMsgID(lastReceivedMessage.getMessageID());
//        emptyACK.getHeader().setMsgType(MsgType.RST);
//        testReceiver.writeMessage(emptyACK, new InetSocketAddress("localhost", DEFAULT_COAP_SERVER_PORT));
//
//        testReceiver.setReceiveEnabled(false);
//    }
//
//    @Test
//    public void testReceiverReceived3Messages() {
//        String message = "Receiver did not receive 3 messages";
//        assertEquals(message, 3, testReceiver.getReceivedMessages().values().size());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification1() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, notification1.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification2() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, notification2.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification3() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, notification3.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testObserveOptionIsSetProperly() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage receivedMessage1 = receivedMessages.get(timeKeys.next());
//        CoapMessage receivedMessage2 = receivedMessages.get(timeKeys.next());
//        CoapMessage receivedMessage3 = receivedMessages.get(timeKeys.next());
//        Long observe1 = ((UintOption)receivedMessage1.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe2 = ((UintOption)receivedMessage2.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe3 = ((UintOption)receivedMessage3.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//
//        String message = "ObserveOption sequence is not set properly";
//        assertTrue(message, observe1 < observe2);
//        assertTrue(message, observe2 < observe3);
//    }
//
//    @Test
//    public void testObserveOptionExists() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage receivedMessage1 = receivedMessages.get(timeKeys.next());
//        CoapMessage receivedMessage2 = receivedMessages.get(timeKeys.next());
//        CoapMessage receivedMessage3 = receivedMessages.get(timeKeys.next());
//
//        String message = "At least one notification has no Observe Option";
//        assertFalse(message, receivedMessage1.getOption(OBSERVE_RESPONSE).isEmpty());
//        assertFalse(message, receivedMessage2.getOption(OBSERVE_RESPONSE).isEmpty());
//        assertFalse(message, receivedMessage3.getOption(OBSERVE_RESPONSE).isEmpty());
//    }
//
//    @Test
//    public void testFirstNotificationHasSameMsgID() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "First notification Msg ID does not match with request Msg ID";
//        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
//    }
//
//    @Test
//    public void testAllNotificationsHaveTheSameToken() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage receivedMessage1 = receivedMessages.get(timeKeys.next());
//        CoapMessage receivedMessage2 = receivedMessages.get(timeKeys.next());
//        CoapMessage receivedMessage3 = receivedMessages.get(timeKeys.next());
//
//        String message = "At least one notification has a wrong token";
//        assertTrue(message, Arrays.equals(coapRequest.getToken(), receivedMessage1.getToken()));
//        assertTrue(message, Arrays.equals(coapRequest.getToken(), receivedMessage2.getToken()));
//        assertTrue(message, Arrays.equals(coapRequest.getToken(), receivedMessage3.getToken()));
//    }
//
//}
