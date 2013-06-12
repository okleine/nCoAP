//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapEndpoint;
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
//import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
//import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;
//import org.joda.time.DateTime;
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.net.URI;
//import java.util.SortedMap;
//
//import static org.junit.Assert.*;
//import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
//
//
///**
// * Tests of CoAP request and response messages including reliability,
// * piggy-backed and separate response.
// *
// * @author Oliver Kleine, Stefan Hüske
// */
//public class ClientGetsNoAckForConRequestTest extends AbstractCoapCommunicationTest {
//
//    private static final int TIMEOUT_MILLIS = 2000;
//    private static Logger log = Logger.getLogger(ClientGetsNoAckForConRequestTest.class.getName());
//
//    private static long timeRequestSent;
//
//    /**
//     * Retransmission intervals (RC = Retransmission Counter):
//     * immediately send CON message, set RC = 0
//     * wait  2 - 3  sec then send retransmission, set RC = 1
//     * wait  4 - 6  sec then send retransmission, set RC = 2
//     * wait  8 - 12 sec then send retransmission, set RC = 3
//     * wait 16 - 24 sec then send retransmission, set RC = 4
//     * wait 32 - 48 sec then fail transmission
//     *
//     * @throws Exception
//     */
//    @Override
//    public void createTestScenario() throws Exception {
//
//        /*
//             testClient                    testReceiver     DESCRIPTION
//                  |                             |
//              (1) |----CON-GET----------------->|           Client sends confirmable request
//                  |                             |
//              (2) |----1st RETRANSMISSION------>|           (Client should send four retransmissions)
//                  |                             |
//              (3) |----2nd RETRANSMISSION------>|
//                  |                             |
//              (4) |----3rd RETRANSMISSION------>|
//                  |                             |
//              (5) |----4th RETRANSMISSION------>|
//                  |                             |
//                  |                             |
//        */
//
//        testReceiver.setWriteEnabled(false);
//
//        URI targetUri = new URI("coap://localhost:" + testReceiver.getReceiverPort() + "/testpath");
//        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri, testClient);
//
//        //Send request
//        log.info("Send request...");
//        timeRequestSent = System.currentTimeMillis();
//        testClient.writeCoapRequest(coapRequest);
//
//        //Maximum time to pass before last retransmission is 45 sec.
//        Thread.sleep(45000);
//
//        log.info("Disable message reception after 45 seconds.");
//        testReceiver.setReceiveEnabled(false);
//
//        //Wait another 50 sec. to let the last retransmission time out before test methods start
//        Thread.sleep(50000);
//    }
//
//    @Test
//    public void testNumberOfRequests(){
//        int expected = 5;
//        int actual = testReceiver.getReceivedMessages().size();
//        String message = "Number of received messages: ";
//        assertEquals(message, expected, actual);
//    }
//
//    @Test
//    public void testAllRequestsAreEqual(){
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        CoapMessage firstMessage = receivedMessages.get(receivedMessages.firstKey());
//
//        for(CoapMessage message : receivedMessages.values()){
//            assertEquals(firstMessage, message);
//        }
//    }
//
//    /**
//     * Resulting absolute intervals
//     * 1st retransmission should be received after  2 - 3  sec
//     * 2nd retransmission should be received after  6 - 9  sec
//     * 3rd retransmission should be received after 14 - 21 sec
//     * 4th retransmission should be received after 30 - 45 sec
//     */
//    @Test
//    public void testRetransmissionsWereReceivedInTime(){
//        //Get times of received messages
//        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
//        Object[] receptionTimes = receivedMessages.keySet().toArray();
//
//        long minDelay = 0;
//        long maxDelay = 0;
//        for(int i = 1; i < receptionTimes.length; i++){
//            minDelay += Math.pow(2, i-1) * TIMEOUT_MILLIS;
//            maxDelay += Math.pow(2, i-1) * TIMEOUT_MILLIS * 1.5;
//            long actualDelay = (Long) receptionTimes[i] - timeRequestSent;
//
//            String message = "Retransmission " + i
//                           + " (expected delay between " + minDelay + " and " + maxDelay + "ms,"
//                           + " actual delay " + actualDelay + "ms).";
//
//            log.info(message);
//            assertTrue(message, minDelay <= actualDelay);
//            assertTrue(message, maxDelay >= actualDelay);
//        }
//    }
//
//   /**
//    * Test if the notification was to early. Minimum delay is the sum of minimal timeouts, maximum delay
//    * is the sum of maximum timeouts (93000) plus a tolerance of 2000
//    */
//    @Test
//    public void testClientReceivesTimeoutNotification(){
//        //Test if the client received a timeout notifiaction
//        assertFalse("Client did not receive a timeout notification at all.",
//                    testClient.getTimeoutNotificationTimes().isEmpty());
//
//        long minDelay = 62000;
//        long maxDelay = 95000;
//
//        long actualDelay = testClient.getTimeoutNotificationTimes().first() - timeRequestSent;
//
//        String message = "Retransmition timeout notification"
//                + " (expected delay between " + minDelay + " and " + maxDelay + "ms,"
//                + " actual delay " + actualDelay + "ms).";
//
//        log.info(message);
//        assertTrue(message, minDelay <= actualDelay);
//        assertTrue(message, maxDelay >= actualDelay);
//    }
//
//    private static String timeToString(DateTime time){
//        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
//        return time.toString(pattern);
//    }
//
//}
//
