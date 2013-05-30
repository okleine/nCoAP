//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//
//import org.junit.Test;
//
//import java.net.URI;
//import java.util.Iterator;
//import java.util.SortedMap;
//
//import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.CONTENT_205;
//import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.GET;
//import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.CON;
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.assertEquals;
//import static junit.framework.Assert.assertEquals;
//
///**
//* Tests if a client receives notifications.
//* @author Stefan Hueske
//*/
//public class ClientReceivesObserveNotification extends AbstractCoapCommunicationTest{
//
//    //observable request
//    private static CoapRequest request;
//
//    //notifications
//    private static CoapResponse expectedNotification1;
//    private static CoapResponse expectedNotification2;
//
//    @Override
//    public void createTestScenario() throws Exception {
//        //define expected responses
//        expectedNotification1 = new CoapResponse(CONTENT_205);
//        expectedNotification1.setPayload("testpayload1".getBytes("UTF-8"));
//
//        expectedNotification2 = new CoapResponse(CONTENT_205);
//        expectedNotification2.setPayload("testpayload2".getBytes("UTF-8"));
//
//        //setup testserver
//        registerObservableTestService(0, 3000);
//
//        //create CoAP request
//        URI serviceUri = new URI("coap://localhost:" + testServer.getServerPort() + OBSERVABLE_SERVICE_PATH);
//        request = new CoapRequest(CON, GET, serviceUri, testClient);
//        request.setObserveOptionRequest();
//
//        //run test sequence
//        testClient.writeCoapRequest(request);
//
//        //wait for 2 notifications (first immediate, second after 3 seconds)
//        Thread.sleep(4000);
//
//        testClient.setReceiveEnabled(false);
//
//        //delete service from server to stop observability
//        testServer.removeService(OBSERVABLE_SERVICE_PATH);
//    }
//
//    @Test
//    public void testReceiverReceived2Messages() {
//        String message = "Receiver did not receive 2 messages";
//        assertEquals(message, 2, testClient.getReceivedResponses().size());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification1() {
//        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, expectedNotification1.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification2() {
//        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd notification: MsgType is not ACK";
//        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//        message = "2nd notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "2nd notification: Payload does not match";
//        assertEquals(message, expectedNotification2.getPayload(), receivedMessage.getPayload());
//    }
//
//
//}
