///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
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
// * Tests if the CON retransmissions use always the latest observable resource.
// *
// * @author Stefan Hueske
// */
//public class ObserveOptionUpdateWhileRetransmissionTest extends AbstractCoapCommunicationTest {
//
//    //registration requests
//    private static CoapRequest regRequest;
//
//    //notifications
//    private static CoapResponse notification1;
//    private static CoapResponse notification2;
//    private static CoapResponse notification3;
//
//    @Override
//    public void createTestScenario() throws Exception {
//
//        /*
//            testReceiver               Server               Service
//                 |                        |                    |
//    (regRequest) |-----GET OBSERVE------->|                    |
//                 |                        |                    |
// (notification1) |<----ACK OBSERVE--------|                    |
//                 |                        |<-----UPDATE()------|
// (notification2) |<----CON OBSERVE--------|                    |  |500ms
//                 |                        |<-----UPDATE()------|
//                 |                        |                    |  |
//                 |                        |                    |  |
// (notification3) |<----retransmission-----|                    |  |2500ms
//                 |                        |                    |  |
//                 |                        |                    |  |
//                 |-----RST message------->|                    |
//                 |                        |                    |
//
//        */
//
//        //create registration request
//        String requestPath = "/testpath";
//        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + requestPath);
//        regRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        regRequest.getHeader().setMsgID(1111);
//        regRequest.setToken(new byte[]{0x12, 0x23, 0x34});
//        regRequest.setObserveOptionRequest();
//
//        //create notifications
//        (notification1 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload1".getBytes("UTF-8"));
//        (notification2 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload2".getBytes("UTF-8"));
//        (notification3 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload3".getBytes("UTF-8"));
//
//        //setup testServer
//
//        ObservableTestWebService observableTestWebService = new ObservableTestWebService(requestPath, true, 0, 0);
//        observableTestWebService.addPreparedResponses(notification1, notification2, notification3);
//        registerObservableTestService(observableTestWebService);
//
//        //run test sequence
//
//        //registration
//        testReceiver.writeMessage(regRequest, new InetSocketAddress("localhost", testServer.getServerPort()));
//        //wait for response
//        Thread.sleep(150);
//
//        //first resource update
//        observableTestWebService.setResourceStatus(true);
//        Thread.sleep(500);
//
//        //second resource update
//        observableTestWebService.setResourceStatus(true);
//        Thread.sleep(2500);
//
//        //send RST message
//
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage lastReceivedMessage = receivedMessages.get(receivedMessages.lastKey());
//        CoapMessage cancelRSTmsg = CoapMessage.createEmptyReset(lastReceivedMessage.getMessageID());
//        testReceiver.writeMessage(cancelRSTmsg, new InetSocketAddress("localhost", testServer.getServerPort()));
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
//
//    @Test
//    public void testNewScheduledRetransmissionHasDifferentMsgID() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification3 = receivedMessages.get(timeKeys.next());
//
//        String message = "Updated CON retransmission should lead to new message ID";
//        assertTrue(message, recNotification2.getMessageID() != recNotification3.getMessageID());
//    }
//
//    @Test
//    public void testRetransmissionUpdatedSuccessfully() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification3 = receivedMessages.get(timeKeys.next());
//
//        String message = "Payload for 2nd notification does not match";
//        assertEquals(message, notification2.getPayload(), recNotification2.getPayload());
//        message = "Payload for 3rd notification does not match / was not updated";
//        assertEquals(message, notification3.getPayload(), recNotification3.getPayload());
//    }
//
//    @Test
//    public void testObserveOptionIsSetProperly() {
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());
//        CoapMessage recNotification3 = receivedMessages.get(timeKeys.next());
//
//        Long observe1 = ((UintOption)recNotification1.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe2 = ((UintOption)recNotification2.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe3 = ((UintOption)recNotification3.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//
//        String message = "Sequential Observe Options are not set properly";
//        assertTrue(message, (observe1 < observe2) && (observe2 < observe3));
//    }
//}
