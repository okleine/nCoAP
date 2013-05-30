//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableTestWebService;
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
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
//
///**
// * Tests for the removal of observers.
// *
// * @author Stefan Hueske
// */
//public class ObserveOptionCancellationTest extends AbstractCoapCommunicationTest {
//
//    //registration requests
//    private static CoapRequest reg1Request;
//    private static CoapRequest reg2Request;
//
//    //cancellation messages
//    private static CoapRequest cancelGETrequest;
//    private static CoapMessage cancelRSTmsg;
//
//    //notifications
//    private static CoapResponse notification;
//
//    @Override
//    public void createTestScenario() throws Exception {
//        //create registration requests
//        String requestPath = "/testpath";
//        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + requestPath);
//        reg1Request = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        reg1Request.getHeader().setMsgID(1111);
//        reg1Request.setToken(new byte[]{0x12, 0x23, 0x34});
//        reg1Request.setObserveOptionRequest();
//
//        reg2Request = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        reg2Request.getHeader().setMsgID(2222);
//        reg2Request.setToken(new byte[]{0x23, 0x43, 0x43});
//        reg2Request.setObserveOptionRequest();
//
//        //create notification
//        (notification = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload".getBytes("UTF-8"));
//
//        //create cancellation messages
//        cancelGETrequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        cancelGETrequest.getHeader().setMsgID(3333);
//        cancelGETrequest.setToken(new byte[]{0x54, 0x43, 0x43});
//
//        //setup testServer/Observable WebService
//        ObservableTestWebService observableTestWebService = new ObservableTestWebService(requestPath, true, 0, 0);
//        observableTestWebService.addPreparedResponses(6, notification);
//        registerObservableTestService(observableTestWebService);
//
//        //test sequence, test GET and RST cancellation
//        //Wireshark: https://dl.dropbox.com/u/10179177/Screenshot_2013.04.11-22.16.33.png
//        /*
//             testReceiver                    Server        DESCRIPTION
//                  |                             |
//              (1) |--------GET_OBSERVE--------->|          Register observer
//                  |                             |
//              (2) |<-------1st Notification-----|          Receive first notification
//                  |                             |
//              (3) |--------GET----------------->|          Remove observer using a GET request without observe option
//                  |                             |
//              (4) |<-------Simple ACK response--|          Receive ACK response (without observe option)
//                  |                             |              (call testServer.notifyCoapObservers() here
//                  |                             |               to test if removal was successful)
//              (5) |--------GET_OBSERVE--------->|          Register observer
//                  |                             |
//              (6) |<-------1st Notification-----|          Receive first notification (ACK)
//                  |                             |
//              (7) |<-------2nd Notification-----|          Receive second notification (CON)
//                  |                             |
//              (8) |--------RST----------------->|          Respond with reset to the 2nd notification
//                  |                             |              (call testServer.notifyCoapObservers() here
//                  |                             |               to test if removal was successful)
//        */
//        //first registration
//  /*1*/ testReceiver.writeMessage(reg1Request, new InetSocketAddress("localhost", testServer.getServerPort()));
//        //wait for first response
//  /*2*/ Thread.sleep(150);
//        //send GET for same resource without Observe Option
//  /*3*/ testReceiver.writeMessage(cancelGETrequest, new InetSocketAddress("localhost", testServer.getServerPort()));
//        //wait for Simple ACK response (without Observe Option)
//  /*4*/ Thread.sleep(150);
//        //if cancellation was successful, nothing should happen here
//
//        observableTestWebService.setResourceStatus(true);
////        testServer.notifyCoapObservers();
//        Thread.sleep(150);
//
//        //second registration
//  /*5*/ testReceiver.writeMessage(reg2Request, new InetSocketAddress("localhost", testServer.getServerPort()));
//        //wait for first response
//  /*6*/ Thread.sleep(150);
//        //get second CON response
//        observableTestWebService.setResourceStatus(true);
////        testServer.notifyCoapObservers();
//  /*7*/ Thread.sleep(150);
//        //respond with RST message
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage lastReceivedMessage = receivedMessages.get(receivedMessages.lastKey());
//        cancelRSTmsg = CoapMessage.createEmptyReset(lastReceivedMessage.getMessageID());
//  /*8*/ testReceiver.writeMessage(cancelRSTmsg, new InetSocketAddress("localhost", testServer.getServerPort()));
//        Thread.sleep(150);
//        //if cancellation was successful, nothing should happen here
//        observableTestWebService.setResourceStatus(true);
////        testServer.notifyCoapObservers();
//        Thread.sleep(2000);
//        testReceiver.setReceiveEnabled(false);
//    }
//
//    @Test
//    public void testReceiverReceived4Messages() {
//        String message = "Receiver did not receive 4 messages";
//        assertEquals(message, 4, testReceiver.getReceivedMessages().values().size());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration1Notification1() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration1ACKresponse() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "Simple ACK response: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "Simple ACK response: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "Simple ACK response: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration2Notification1() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd registration, 1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "2nd registration, 1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "2nd registration, 1st notification: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration2Notification2() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd registration, 2nd notification: MsgType is not ACK";
//        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//        message = "2nd registration, 2nd notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "2nd registration, 2nd notification: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReg2ObserveOptionSequenceIsSetProperly() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage reg2notification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage reg2notification2 = receivedMessages.get(timeKeys.next());
//
//        Long observe1 = ((UintOption)reg2notification1.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe2 = ((UintOption)reg2notification2.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//
//        String message = String.format("ObserveOption sequence in second "
//                + "registration is not set properly (1st: %d, 2nd: %d)",
//                observe1, observe2);
//        assertTrue(message, observe1 < observe2);
//    }
//
//    @Test
//    public void testObserveOptions() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        //reg1 notification1
//        CoapMessage receivedMessage1 = receivedMessages.get(timeKeys.next());
//        //simple ack
//        CoapMessage receivedMessage2 = receivedMessages.get(timeKeys.next());
//        //reg2 notification1
//        CoapMessage receivedMessage3 = receivedMessages.get(timeKeys.next());
//        //reg2 notification2
//        CoapMessage receivedMessage4 = receivedMessages.get(timeKeys.next());
//
//
//        String message = "Observe Option should be set for reg1 notification1";
//        assertFalse(message, receivedMessage1.getOption(OBSERVE_RESPONSE).isEmpty());
//        message = "Observe Option should NOT be set for simple ack response";
//        assertTrue(message, receivedMessage2.getOption(OBSERVE_RESPONSE).isEmpty());
//        message = "Observe Option should be set for reg2 notification1";
//        assertFalse(message, receivedMessage3.getOption(OBSERVE_RESPONSE).isEmpty());
//        message = "Observe Option should be set for reg2 notification2";
//        assertFalse(message, receivedMessage4.getOption(OBSERVE_RESPONSE).isEmpty());
//    }
//
//    @Test
//    public void testFirstNotificationHasSameMsgID() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "First notification Msg ID does not match with request Msg ID";
////        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
//    }
//
//    @Test
//    public void testReg2NotificationsHaveTheSameToken() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage reg1notification1 = receivedMessages.get(timeKeys.next());
//        timeKeys.next();
//        CoapMessage reg2notification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage reg2notification2 = receivedMessages.get(timeKeys.next());
//
//        String message = "At least one notification has a wrong token";
//        assertTrue(message, Arrays.equals(reg1Request.getToken(), reg1notification1.getToken()));
//        assertTrue(message, Arrays.equals(reg2Request.getToken(), reg2notification1.getToken()));
//        assertTrue(message, Arrays.equals(reg2Request.getToken(), reg2notification2.getToken()));
//    }
//
//}
