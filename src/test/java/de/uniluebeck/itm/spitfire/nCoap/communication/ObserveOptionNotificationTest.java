//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.SortedMap;
//
//import org.junit.Test;
//
//import static junit.framework.Assert.*;
//import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableTestWebService;
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
//
//
///**
// * Tests for Observe Option notifications.
// *
// * @author Stefan Hueske
// */
//public class ObserveOptionNotificationTest extends AbstractCoapCommunicationTest {
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
//    @Override
//    public void createTestScenario() throws Exception {
//
//        //Wireshark: https://dl.dropbox.com/u/10179177/Screenshot_2013.04.11-19.41.51.png
//
//        /*
//             testEndpoint                   testServer      DESCRIPTION
//                  |                             |
//              (1) |------GET-OBSERVE----------->|           send observable request to server
//                  |                             |
//              (2) |<-----ACK-NOTIFICATION-------|           server responds with initial, piggy-backed notification
//                  |                             |
//              (3) |<-----CON-NOTIFICATION-------|           server sends 2nd notification, invoked by
//                  |                             |           a status update in observableTestWebService
//              (4) |------EMPTY-ACK------------->|
//                  |                             |
//              (5) |<-----CON-NOTIFICATION-------|           server sends 3rd notification, invoked by
//                  |                             |           a status update in observableTestWebService
//              (6) |------RST------------------->|
//
//        */
//
//        //create request
//        requestToken = new byte[]{0x12, 0x23, 0x34};
//        requestPath = "/testpath";
//        requestMsgID = 4567;
//        targetUri = new URI("coap://localhost:" + testServer.getServerPort() + requestPath);
//        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri); // (1)
//        coapRequest.getHeader().setMsgID(requestMsgID);
//        coapRequest.setToken(requestToken);
//        coapRequest.setObserveOptionRequest();
//
//        //create notifications
//        (notification1 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload1".getBytes("UTF-8")); // (2)
//        (notification2 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload2".getBytes("UTF-8")); // (3)
//        (notification3 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload3".getBytes("UTF-8")); // (5)
//
//        //create dummy web service and add prepared responses
//        ObservableTestWebService observableTestWebService = new ObservableTestWebService(requestPath, true, 0, 0);
//        observableTestWebService.addPreparedResponses(notification1, notification2, notification3);
//        registerObservableTestService(observableTestWebService);
//
//        //send request to testServer
//        testEndpoint.writeMessage(coapRequest, new InetSocketAddress("localhost", testServer.getServerPort()));
//
//        //response sequence
//        //wait for first response
//        Thread.sleep(150);
//        //notify
////        testServer.notifyCoapObservers();
//        observableTestWebService.setResourceStatus(true);
//        //wait for 2nd response
//        Thread.sleep(150);
//        //send empty ack
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        CoapMessage lastReceivedMessage = receivedMessages.get(receivedMessages.lastKey());
//        CoapMessage emptyACK = new CoapResponse(Code.EMPTY);
//        emptyACK.getHeader().setMsgID(lastReceivedMessage.getMessageID());
//        emptyACK.getHeader().setMsgType(MsgType.ACK);
//        testEndpoint.writeMessage(emptyACK, new InetSocketAddress("localhost", testServer.getServerPort()));
//        //notify
////        testServer.notifyCoapObservers();
//        observableTestWebService.setResourceStatus(true);
//        //wait for 3rd response
//        Thread.sleep(150);
//        //send RST
//        receivedMessages = testEndpoint.getReceivedMessages();
//        lastReceivedMessage = receivedMessages.get(receivedMessages.lastKey());
//        emptyACK = new CoapResponse(Code.EMPTY);
//        emptyACK.getHeader().setMsgID(lastReceivedMessage.getMessageID());
//        emptyACK.getHeader().setMsgType(MsgType.RST);
//        testEndpoint.writeMessage(emptyACK, new InetSocketAddress("localhost", testServer.getServerPort()));
//
//        testEndpoint.setReceiveEnabled(false);
//    }
//
//    @Test
//    public void testReceiverReceived3Messages() {
//        String message = "Receiver did not receive 3 messages";
//        assertEquals(message, 3, testEndpoint.getReceivedMessages().values().size());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification1() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
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
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
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
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
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
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
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
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
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
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "First notification Msg ID does not match with request Msg ID";
//        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
//    }
//
//    @Test
//    public void testAllNotificationsHaveTheSameToken() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
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
