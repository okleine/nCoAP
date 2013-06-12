//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import static junit.framework.Assert.*;
//import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
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
//import java.util.Iterator;
//import java.util.SortedMap;
//
//import org.junit.Test;
//
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
//
//
///**
//* Tests if the server sends a new notification when Max-Age ends.
//*
//* @author Stefan Hueske
//*/
//public class ObserveOptionAutoNotificationMaxAgeTest extends AbstractCoapCommunicationTest{
//
//    //registration requests
//    private static CoapRequest coapRequest;
//
//    //notifications
//    private static CoapResponse preparedNotification1;
//    private static CoapResponse preparedNotification2;
//
//    @Override
//    public void createTestScenario() throws Exception {
//        /*
//             testEndpoint                    Server      DESCRIPTION
//                  |                             |
//              (1) |--------GET_OBSERVE--------->|        Register observer
//                  |                             |
//              (2) |<-------1st Notification-----|        Receive first notification
//                  |                             | |
//                  |                             | | 2 seconds until max-age ends
//                  |                             | |
//              (3) |<-------2nd Notification-----|        Auto notification should be send by the server
//                  |                             |        before max-age ends
//              (4) |--------RST----------------->|
//                  |                             |
//                  |                             |
//        */
//
//        //define notifications which the server will use to respond
//        preparedNotification1 = new CoapResponse(Code.CONTENT_205);
//        preparedNotification1.setPayload("testpayload1".getBytes("UTF-8"));
//        preparedNotification1.setMaxAge(2);
//
//        preparedNotification2 = new CoapResponse(Code.CONTENT_205);
//        preparedNotification2.setPayload("testpayload2".getBytes("UTF-8"));
//
//        //create observable web service, add prepared responses and register web service
//        ObservableTestWebService observableTestWebService = new ObservableTestWebService(OBSERVABLE_SERVICE_PATH, true, 0, 0);
//        observableTestWebService.setMaxAge(2);
//        observableTestWebService.addPreparedResponses(preparedNotification1, preparedNotification2);
//        registerObservableTestService(observableTestWebService);
//
//        //create registration request
//        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + OBSERVABLE_SERVICE_PATH);
//        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        coapRequest.getHeader().setMsgID(1111);
//        coapRequest.setToken(new byte[]{0x13, 0x53, 0x34});
//        coapRequest.setObserveOptionRequest();
//
//        //registration
//        testEndpoint.writeMessage(coapRequest, new InetSocketAddress("localhost", testServer.getServerPort()));
//
//        //wait for Max-Age to end and resulting notification
//        Thread.sleep(2500);
//
//        //send reset to remove observer
//        CoapMessage resetMessage = CoapMessage.createEmptyReset(preparedNotification2.getMessageID());
//        resetMessage.getHeader().setMsgType(MsgType.RST);
//
//        CoapMessage updateNotification =
//                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().lastKey());
//
//        resetMessage.setMessageID(updateNotification.getMessageID());
//        testEndpoint.writeMessage(resetMessage, new InetSocketAddress("localhost", testServer.getServerPort()));
//
//        testEndpoint.setReceiveEnabled(false);
//    }
//
//    @Test
//    public void testReceiverReceived2Messages() {
//        String message = "Receiver did not receive 2 messages";
//        assertEquals(message, 2, testEndpoint.getReceivedMessages().values().size());
//    }
//
//    @Test
//    public void testReceivedMessageArrivedIn2secDelay() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        Long msg1time = timeKeys.next();
//        Long msg2time = timeKeys.next();
//        long delay = msg2time - msg1time;
//
//        String message = "Scheduled Max-Age notification did not arrive after 2 seconds but after " + delay;
//        assertTrue(message, Math.abs(2000 - delay) < 200); //200ms tolerance
//    }
//
//    @Test
//    public void testObserveOptionIsSetProperly() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());
//
//        Long observe1 = ((UintOption)recNotification1.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe2 = ((UintOption)recNotification2.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//
//        String message = String.format("ObserveOption sequence is not set properly (1st: %d, 2nd: %d)",
//                observe1, observe2);
//        assertTrue(message, observe1 < observe2);
//    }
//
//    @Test
//    public void testMessageType() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());
//
//        String message = "1st notification should be ACK";
//        assertEquals(message, MsgType.ACK, recNotification1.getMessageType());
//        message = "2nd notification should be CON";
//        assertEquals(message, MsgType.CON, recNotification2.getMessageType());
//    }
//
//    @Test
//    public void testMessagePayload() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());
//
//        String message = "1st notifications payload does not match";
//        assertEquals(message, preparedNotification1.getPayload(), recNotification1.getPayload());
//        message = "2nd notifications payload does not match";
//        assertEquals(message, preparedNotification2.getPayload(), recNotification2.getPayload());
//    }
//}
