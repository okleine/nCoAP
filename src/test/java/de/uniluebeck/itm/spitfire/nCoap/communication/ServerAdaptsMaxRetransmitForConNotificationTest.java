//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableTestWebService;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
//import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.util.*;
//
//import static junit.framework.Assert.*;
//
///**
//* Tests if the server adapts MAX_RETRANSMIT to avoid CON timeout before Max-Age ends.
//* (Only for observe notifications)
//*
//* @author Stefan Hueske
//*/
//public class ServerAdaptsMaxRetransmitForConNotificationTest extends AbstractCoapCommunicationTest {
//
//    //registration requests
//    private static CoapRequest observationRequest;
//    private static long observationRequestSent;
//
//
//    @BeforeClass
//    public static void setLogLevels(){
//        Logger.getRootLogger().setLevel(Level.ERROR);
//        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.observe").setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver").setLevel(Level.DEBUG);
//    }
//
//    @Override
//    public void createTestScenario() throws Exception {
//
//        /*
//               testEndpoint               Server               Service
//                     |                        |                    |
//(observationRequest) |-----GET OBSERVE------->|                    |
//                     |                        |                    |
//     (notification1) |<----ACK OBSERVE--------|                    |
//                     |                        |<-----UPDATE()------|
//     (notification2) |<-CON Not.,MAX-AGE:100--|                    |      time: 0
//                     |                        |                    | |
//                     |<----CON RETR.1---------|                    | |    time: 11.25 sec
//                     |                        |                    | |
//                     |<----CON RETR.2---------|                    | |    time: 22.5 sec
//                     |                        | <---UPDATE()-------| |    time: ~35 sec.
//                     |<----CON RETR.3---------|                    | |    time: 45 sec
//                     |                        |                    | |
//                     |<----CON RETR.4---------|                    | |    time: 90 sec. (>= MAX AGE)
//
//        */
//
//        //Create observable service
//        ObservableTestWebService webService = new ObservableTestWebService(OBSERVABLE_SERVICE_PATH, 0, 0);
//        webService.setMaxAge(90);
//        testServer.registerService(webService);
//
//        //create observation request
//        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + OBSERVABLE_SERVICE_PATH);
//        observationRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//        observationRequest.getHeader().setMsgID(12345);
//        observationRequest.setToken(new byte[]{0x12, 0x23, 0x34});
//        observationRequest.setObserveOptionRequest();
//
//        //send observation request to server
//        observationRequestSent = System.currentTimeMillis();
//        testEndpoint.writeMessage(observationRequest, new InetSocketAddress("localhost", testServer.getServerPort()));
//
//        //wait 2.5 seconds (until first retransmission was sent and update observed resource
//        Thread.sleep(2000);
//        webService.setResourceStatus(1);
//
//        //wait for update notification and 2 retransmissions and change status then
//        Thread.sleep(35000);
//        webService.setResourceStatus(2);
//
//        //wait another 65 seconds to finish the other 2 retransmissions
//        Thread.sleep(65000);
//        testEndpoint.setReceiveEnabled(false);
//    }
//
//    @Test
//    public void testEndpointReceived6Messages() {
//        String message = "Receiver did not receive 6 messages";
//        assertEquals(message, 6, testEndpoint.getReceivedMessages().values().size());
//    }
//
//    @Test
//    public void testFirstMessageIsAck(){
//        CoapMessage firstMessage =
//                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().firstKey());
//
//        assertEquals("First received message was no ACK.", firstMessage.getMessageType(), MsgType.ACK);
//        assertTrue("First message is no update notification.", ((CoapResponse) firstMessage).isUpdateNotification());
//        assertTrue("Token does not match.", Arrays.equals(firstMessage.getToken(), observationRequest.getToken()));
//    }
//
//    @Test
//    public void testLastMessageWasNotSentBeforeMaxAgeEnded(){
//        long delay = testEndpoint.getReceivedMessages().lastKey() - observationRequestSent;
//
//        assertTrue("Last retransmission was to early (after (" + delay +") millis. But max-age is 90 seconds!",
//                delay >= 90000);
//    }
//
//    @Test
//    public void testUpdateOfStatusDuringRetransmission(){
//        CoapMessage[] receivedMessages = new CoapMessage[6];
//        receivedMessages = testEndpoint.getReceivedMessages().values().toArray(receivedMessages);
//
//        ByteArrayWrapper[] payloads = new ByteArrayWrapper[5];
//
//        for(int i = 1; i < 6; i++){
//            payloads[i-1] = new ByteArrayWrapper(receivedMessages[i].getPayload().copy().array());
//        }
//
//        assertEquals("Original message and 1st retransmission do not match", payloads[0], payloads[1]);
//        assertEquals("1st and 2nd retransmission do not match", payloads[1], payloads[2]);
//        assertFalse("2nd and 3rd retransmission do match!", payloads[2].equals(payloads[3]));
//        assertEquals("3rd and 4th retransmission do not match", payloads[3], payloads[4]);
//    }
//
//    @Test
//    public void testNotificationCount(){
//        CoapResponse[] receivedMessages = new CoapResponse[6];
//        receivedMessages = testEndpoint.getReceivedMessages().values().toArray(receivedMessages);
//
//        long[] notificationCounts = new long[5];
//
//        for(int i = 1; i < 6; i++){
//            notificationCounts[i-1] =
//                    (Long) receivedMessages[i]
//                           .getOption(OptionRegistry.OptionName.OBSERVE_RESPONSE)
//                           .get(0)
//                           .getDecodedValue();
//        }
//
//        for(int i = 1; i < 5; i++){
//            long actual = notificationCounts[i];
//            long previous = notificationCounts[i-1];
//            assertTrue("Notification count (" + actual + ") was not larger than previous (" + previous + ")!",
//                    notificationCounts[i] > notificationCounts[i-1]);
//        }
//    }
//}
