//package de.uniluebeck.itm.spitfire.nCoap.communication;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapMessageReceiver;
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
//public class ClientGetsNoAckForConRequestTest {
//
//    private static final int TIMEOUT_MILLIS = 2000;
//    private static Logger log = Logger.getLogger(ClientGetsNoAckForConRequestTest.class.getName());
//
//    private static CoapTestClient testClient = CoapTestClient.getInstance();
//    private static CoapMessageReceiver testMessageReceiver = CoapMessageReceiver.getInstance();
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
//    @BeforeClass
//    public static void init() throws Exception{
//        //Configure Logging
//        String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-30.30c{1} | %-5p | %m%n";
//        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout(pattern)));
//        Logger.getRootLogger().setLevel(Level.INFO);
//
//        //Reset components
//        //testClient.reset();
//        testMessageReceiver.reset();
//        testMessageReceiver.setReceiveEnabled(true);
//        testMessageReceiver.setWriteEnabled(false);
//
//        URI targetUri = new URI("coap://localhost:" + CoapMessageReceiver.RECEIVER_PORT + "/testpath");
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
//        testMessageReceiver.setReceiveEnabled(false);
//
//        //Wait another 50 sec. to let the last retransmission time out before test methods start
//        Thread.sleep(50000);
//    }
//
////    @AfterClass
////    public static void shutdownEverything(){
////        testClient.shutdown();
////        testMessageReceiver.shutdown();
////        log.info("Shutdown of components completed.");
////    }
//
//    @Test
//    public void testNumberOfRequests(){
//        int expected = 5;
//        int actual = testMessageReceiver.getReceivedMessages().size();
//        String message = "Number of received messages: ";
//        assertEquals(message, expected, actual);
//    }
//
//    @Test
//    public void testAllRequestsAreEqual(){
//        SortedMap<Long, CoapMessage> receivedMessages = testMessageReceiver.getReceivedMessages();
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
//        SortedMap<Long, CoapMessage> receivedMessages = testMessageReceiver.getReceivedMessages();
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
//
//
////
////    /**
////     * Tests the processing of a piggy-backed response on the client side.
////     */
////    @Test
////    public synchronized void clientSidePiggyBackedTest() throws Exception {
////        System.out.println("Testing piggy-backed response on client side...");
////        /* Sequence diagram:
////
////        testClient          testMessageReceiver
////            |                   |
////            +------------------>|     Header: GET (T=CON)
////            |    coapRequest    |   Uri-Path: "testpath"
////            |                   |
////            |                   |
////            |                   |
////            |<------------------+     Header: 2.05 Content (T=ACK)
////            |  responseMessage  |    Payload: "responsepayload"
////            |                   |
////            |                   |
////         */
////
////        //reset client and receiver -> delete all received messages
////        //                             and enable receiving
////        testClient.reset();
////        testMessageReceiver.reset();
////
////        //create and send request from testClient to testMessageReceiver
////        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET,
////                new URI("coap://localhost:" + CoapMessageReceiver.RECEIVER_PORT + "/testpath"), testClient);
////        testClient.writeCoapRequest(coapRequest);
////
////        //wait for request message to arrive at testMessageReceiver
////        testMessageReceiver.blockUntilMessagesReceivedOrTimeout(500 /*ms timeout*/, 1 /*msg count*/);
////        testMessageReceiver.disableReceiving();
////        assertEquals("testMessageReceiver should receive a single message", 1,
////                testMessageReceiver.receivedMessages.size());
////
////        //receivedRequest is the actual CoAP request send out by nCoAP via testClient
////        CoapMessage receivedRequest = testMessageReceiver.receivedMessages.get(0).message;
////        assertEquals("receivedRequest: type should be CON",
////                MsgType.CON, receivedRequest.getHeader().getMsgType());
////        assertEquals("receivedRequest: code should be GET",
////                Code.GET, receivedRequest.getHeader().getCode());
////        int messageID = receivedRequest.getMessageID();
////        byte[] token = receivedRequest.getToken();
////
////        //create response
////        Header responseHeader = new Header(MsgType.ACK, Code.CONTENT_205, messageID);
////        OptionList responseOptionList = new OptionList();
////        if (token.length != 0) {
////            responseOptionList.addOption(Code.CONTENT_205, OptionRegistry.OptionName.TOKEN,
////                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, token));
////        }
////        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
////        CoapMessage responseMessage = new CoapMessage(responseHeader, responseOptionList, responsePayload) {};
////
////        //send response from testMessageReceiver to testClient
////        Channels.write(testMessageReceiver.channel, responseMessage,
////                new InetSocketAddress("localhost", CoapTestClient.PORT));
////
////        //wait for response message to arrive at testClient (via callback)
////        testClient.blockUntilMessagesReceivedOrTimeout(800, 1);
////        testClient.disableReceiving();
////        assertEquals("testClient should receive a single message", 1,
////                testClient.receivedResponses.size());
////        CoapResponse receivedResponse = testClient.receivedResponses.get(0).message;
////        assertArrayEquals("receivedResponse: token does not match",
////                token, receivedResponse.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue());
////        assertEquals("receivedResponse: payload does not match",
////                responsePayload, receivedResponse.getPayload());
////
////    }
////
////    /**
////     * Tests the processing of a piggy-backed response on the server side.
////     */
////    @Test
////    public synchronized void serverSidePiggyBackedTest() throws Exception {
////        System.out.println("Testing piggy-backed response on server side...");
////        /* Sequence diagram:
////
////        testMessageReceiver        testServer
////            |                   |
////            +------------------>|     Header: GET (T=CON)
////            |    coapRequest    |   Uri-Path: "testpath"
////            |                   |
////            |                   |
////            |                   |
////            |<------------------+     Header: 2.05 Content (T=ACK)
////            |  responseMessage  |    Payload: "responsepayload"
////            |                   |
////            |                   |
////         */
////
////        //reset server and receiver -> delete all received messages
////        //                             and enable receiving
////        testServer.reset();
////        testMessageReceiver.reset();
////
////        //create coapRequest which will later be sent from testMessageReceiver to testServer
////        int requestMessageID = 12345;
////        byte[] requestToken = {0x12, 0x34, 0x56};
////        String requestUriPath = "testpath";
////        Header requestHeader = new Header(MsgType.CON, Code.GET, requestMessageID);
////        OptionList requestOptionList = new OptionList();
////        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.TOKEN,
////                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
////        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.URI_PATH,
////                    StringOption.createStringOption(OptionRegistry.OptionName.URI_PATH, requestUriPath));
////        CoapMessage coapRequest = new CoapMessage(requestHeader, requestOptionList, null) {};
////
////        //create response which will later be sent back from testServer to testMessageReceiver
////        CoapResponse responseMessage = new CoapResponse(MsgType.CON, Code.CONTENT_205);
////        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
////        responseMessage.setPayload(responsePayload);
////
////        //register response at testServer
////        testServer.responsesToSend.add(responseMessage);
////
////        //send request from testMessageReceiver to testServer
////        Channels.write(testMessageReceiver.channel, coapRequest,
////                new InetSocketAddress("localhost", CoAPTestServer.PORT));
////
////        //when the request arrives, testServer will send the registered
////        //response 'responseMessage' immediately back to testMessageReceiver
////        //-> wait for responseMessage at testMessageReceiver
////        testMessageReceiver.blockUntilMessagesReceivedOrTimeout(800, 1);
////        testMessageReceiver.disableReceiving();
////        assertEquals("testMessageReceiver should receive a single message", 1,
////                testMessageReceiver.receivedMessages.size());
////        assertEquals("testServer should receive a single message", 1,
////                testServer.receivedRequests.size());
////        testServer.disableReceiving();
////
////        CoapRequest receivedRequest = testServer.receivedRequests.get(0).message;
////        CoapMessage receivedResponse = testMessageReceiver.receivedMessages.get(0).message;
////
////        //check the received request from testServer
////        assertEquals("receivedRequest: messageID does not match",
////                requestMessageID, receivedRequest.getMessageID());
////        assertArrayEquals("receivedRequest: token does not match",
////                requestToken, receivedRequest.getToken());
////        assertEquals("receivedRequest: URI_PATH does not match",
////                requestUriPath, ((StringOption) receivedRequest
////                .getOption(OptionRegistry.OptionName.URI_PATH).get(0)).getDecodedValue());
////        assertEquals("receivedRequest: message type does not match",
////                MsgType.CON, receivedRequest.getMessageType());
////
////        //check the received response from testMessageReceiver
////        assertEquals("receivedResponse: messageID does not match",
////                requestMessageID, receivedResponse.getMessageID());
////        assertArrayEquals("receivedResponse: token does not match",
////                requestToken, receivedResponse.getToken());
////        assertEquals("receivedResponse: code does not match",
////                Code.CONTENT_205, receivedResponse.getCode());
////        assertEquals("receivedResponse: payload does not match",
////                responsePayload, receivedResponse.getPayload());
////        assertEquals("receivedResponse: message type does not match",
////                MsgType.ACK, receivedResponse.getMessageType());
////    }
////
////    /**
////     * Tests the processing of a separate response on the client side.
////     */
////    //@Ignore //TODO fix issue https://github.com/okleine/nCoAP/issues/10
////    @Test
////    public synchronized void clientSideSeparateTest() throws Exception {
////        System.out.println("Testing separate response on client side...");
////
////        //TODO uncomment when separate-response bug in IncomingMessageReliabilityHandler is fixed
////
////        //(see http://tools.ietf.org/html/draft-ietf-core-coap-08#page-77)
////        testClient.reset();
////        testMessageReceiver.reset();
////
////        //send request from testClient to testMessageReceiver
////        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET,
////                new URI("coap://localhost:" + CoapMessageReceiver.RECEIVER_PORT + "/testpath"), testClient);
////        testClient.writeCoapRequest(coapRequest);
////
////        //wait for request message to arrive
////        long time = System.currentTimeMillis();
////        while (testMessageReceiver.receivedMessages.size() == 0) {
////            if (System.currentTimeMillis() - time > 800) {
////                fail("testMessageReceiver did not receive the request within time.");
////            }
////            Thread.sleep(50);
////        }
////        testMessageReceiver.disableReceiving();
////        assertEquals("testMessageReceiver received more than one message", 1,
////                testMessageReceiver.receivedMessages.size());
////
////        //get values from received request
////        CoapMessage receivedRequest = testMessageReceiver.receivedMessages.get(0).message;
////        assertEquals(MsgType.CON, receivedRequest.getHeader().getMsgType());
////        assertEquals(Code.GET, receivedRequest.getHeader().getCode());
////        int requestMessageID = receivedRequest.getMessageID();
////        byte[] requestToken = receivedRequest.getToken();
////
////        //create emppty response
////        Header emptyResponseHeader = new Header(MsgType.ACK, Code.EMPTY, requestMessageID);
////        OptionList emptyResponseOptionList = new OptionList();
////        CoapMessage emptyResponseMessage = new CoapMessage(emptyResponseHeader, emptyResponseOptionList, null) {};
////
////        //send empty response to client
////        testMessageReceiver.reset();
////        Channels.write(testMessageReceiver.channel, emptyResponseMessage, new InetSocketAddress("localhost", CoapTestClient.PORT));
////
////        //wait 3 seconds then test if retransmissions were send (should not)
////        Thread.sleep(3000);
////        assertEquals("testMessageReceiver received unexpected messages", 0, testMessageReceiver.receivedMessages.size());
////
////        //send (separate) CON response to testClient
////        int responseMessageID = 1111;
////        Header responseHeader = new Header(MsgType.CON, Code.CONTENT_205, responseMessageID);
////        OptionList responseOptionList = new OptionList();
////        if (requestToken.length != 0) {
////            responseOptionList.addOption(Code.CONTENT_205, OptionRegistry.OptionName.TOKEN,
////                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
////        }
////        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
////        CoapMessage responseMessage = new CoapMessage(responseHeader, responseOptionList, responsePayload) {};
////
////        //send response to client
////        Channels.write(testMessageReceiver.channel, responseMessage, new InetSocketAddress("localhost", CoapTestClient.PORT));
////
////        //wait for (separate) CON response to arrive
////        time = System.currentTimeMillis();
////        while (testClient.receivedResponses.size() == 0) {
////            if (System.currentTimeMillis() - time > 500) {
////                fail("testClient did not receive the response within time.");
////            }
////            Thread.sleep(50);
////        }
////        testClient.disableReceiving();
////        assertEquals("testClient received more than one message", 1,
////                testClient.receivedResponses.size());
////        CoapResponse receivedResponse = testClient.receivedResponses.get(0).message;
////        assertArrayEquals(requestToken, receivedResponse.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue());
////        assertEquals(responsePayload, receivedResponse.getPayload());
////
////        //wait for empty ACK from testClient to testMessageReceiver
////        time = System.currentTimeMillis();
////        while (testMessageReceiver.receivedMessages.size() == 0) {
////            if (System.currentTimeMillis() - time > 2500) {
////                fail("testMessageReceiver did not receive the empty ACK within time.");
////            }
////            Thread.sleep(50);
////        }
////        testMessageReceiver.disableReceiving();
////        assertEquals("testMessageReceiver received more than one message", 1,
////                testMessageReceiver.receivedMessages.size());
////        CoapMessage receivedEmptyACK = testMessageReceiver.receivedMessages.get(0).message;
////        assertEquals("received message is not empty", Code.EMPTY, receivedEmptyACK.getCode());
////        assertEquals("received message is not ACK", MsgType.ACK, receivedEmptyACK.getMessageType());
////
////    }
////
////    /**
////     * Tests the processing of a separate response on the server side.
////     */
////    @Test
////    public synchronized void serverSideSeparateTest() throws Exception {
////        System.out.println("Testing separate response on server side...");
////        /* Sequence diagram:
////
////        testMessageReceiver        testServer
////            |                   |
////            |    requestMsg     |
////            +------------------>|     Header: GET (T=CON, Code=1, MID=0x7d38=32056)
////            |        GET        |      Token: 0x53
////            |                   |   Uri-Path: "temperature"
////            |                   |
////            |  ackForRequestMsg |
////            |<- - - - - - - - - +     Header: (T=ACK, Code=0, MID=0x7d38=32056)
////            |                   |
////            |    responseMsg    |
////            |<------------------+     Header: 2.05 Content (T=CON, Code=69)
////            |        2.05       |      Token: 0x53
////            |                   |    Payload: "22.3 C"
////            |                   |
////            | ackForResponseMsg |
////            + - - - - - - - - ->|     Header: (T=ACK, Code=0)
////            |                   |
////         */
////
////        //reset server and receiver -> delete all received messages
////        //                             and enable receiving
////        testServer.reset();
////        testMessageReceiver.reset();
////
////        //create 'requestMsg'
////        int requestMessageID = 32056;
////        byte[] requestToken = {0x53};
////        String requestUriPath = "temperature";
////        Header requestHeader = new Header(MsgType.CON, Code.GET, requestMessageID);
////        OptionList requestOptionList = new OptionList();
////        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.TOKEN,
////                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
////        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.URI_PATH,
////                    StringOption.createStringOption(OptionRegistry.OptionName.URI_PATH, requestUriPath));
////        CoapMessage requestMsg = new CoapMessage(requestHeader, requestOptionList, null) {};
////
////        //create 'responseMsg'
////        CoapResponse responseMsgToSend = new CoapResponse(MsgType.CON, Code.CONTENT_205);
////        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("22.3 C".getBytes("UTF8"));
////        responseMsgToSend.setPayload(responsePayload);
////
////        //register response 'responseMsg' at testServer
////        testServer.responsesToSend.add(responseMsgToSend);
////
////        //set time (in ms) to wait before testServer sends the response
////        testServer.waitBeforeSendingResponse = 2500;
////
////        //send 'requestMsg' from testMessageReceiver to testServer
////        Channels.write(testMessageReceiver.channel, requestMsg,
////                new InetSocketAddress("localhost", CoAPTestServer.PORT));
////
////        //wait for two messages at testMessageReceiver
////        //('ackForRequestMsg' and 'responseMsg')
////        testMessageReceiver.blockUntilMessagesReceivedOrTimeout(3600, 2);
////
////        //disable receiving at testServer and testMessageReceiver
////        testMessageReceiver.disableReceiving();
////        testServer.disableReceiving();
////
////        //check amount of messages
////        assertEquals("testMessageReceiver should receive two messages", 2,
////                testMessageReceiver.receivedMessages.size());
////        assertEquals("testServer should receive a single message", 1,
////                testServer.receivedRequests.size());
////
////        //get received messages
////        CoapMessage ackForRequestMsg = testMessageReceiver.receivedMessages.get(0).message;
////        CoapMessage responseMsg = testMessageReceiver.receivedMessages.get(1).message;
////
////        //send 'ackForResponseMsg' to testServer
////        CoapMessage ackForResponseMsg = new CoapResponse(MsgType.ACK, Code.EMPTY, responseMsg.getMessageID());
////        Channels.write(testMessageReceiver.channel, ackForResponseMsg,
////                new InetSocketAddress("localhost", CoAPTestServer.PORT));
////
////        //check 'ackForRequestMsg'
////        assertEquals("ackForRequestMsg: message ID does not match",
////                requestMessageID, ackForRequestMsg.getMessageID());
////        assertEquals("ackForRequestMsg: message code must be zero",
////                Code.EMPTY, ackForRequestMsg.getCode());
////
////        //check 'responseMsg'
////        assertArrayEquals("responseMsg: token does not match",
////                requestToken, responseMsg.getToken());
////        assertEquals("responseMsg: payload does not match",
////                responsePayload, responseMsg.getPayload());
////
////    }
//
//    private static String timeToString(DateTime time){
//        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
//        return time.toString(pattern);
//    }
//}
//
