package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageDecoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageEncoder;
//import de.uniluebeck.itm.spitfire.nCoap.configuration.Configuration;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OpaqueOption;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.StringOption;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests of CoAP request and response messages including reliability,
 * piggy-backed and separate response.
 * 
 * @author Stefan Hueske
 */
public class CoAPRequestResponseTest {
    private static  CoAPTestServer testServer;
    private static  CoAPTestClient testClient;
    private static  CoAPTestReceiver testReceiver = new CoAPTestReceiver();
        
    //All tests are synchronized because JUnit tests could be
    //executed parallel. This is currently not supportet by the
    //nCoAP implementation.
    
    @BeforeClass
    public static void setClientAndServerPorts() {
        //TODO find free ports automatically
//        int clientPort = 25683;
//        Configuration.getInstance().setProperty("client.port", clientPort);
//        CoapClientDatagramChannelFactory.COAP_CLIENT_PORT = clientPort;
        testClient = new CoAPTestClient();
//
//        int serverPort = 5683;
////        Configuration.getInstance().setProperty("server.port", serverPort);
//        CoapServerDatagramChannelFactory.COAP_SERVER_PORT = serverPort;
        testServer = new CoAPTestServer();
    }
    
    /**
     * Tests the timing of all retransmissions for a CON message.
     * Note: This test will take about 46 seconds.
     * Because of this I suggest to comment this test out
     * until modifying a reliability relevant class.
     */
    @Test 
    public synchronized void reliabilityRetransmissionTest() throws Exception {
        System.out.println("Testing reliability...");
        testClient.reset();
        testReceiver.reset();
        
        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, 
                new URI("coap://localhost:" + CoAPTestReceiver.PORT + "/testpath"));
        
        System.out.println("Sending request...");
        long firstRequestSendTime = System.currentTimeMillis();
        long tolerance = 700; //700ms margin of error for processing time
        testClient.writeCoapRequest(coapRequest);
        
        /* 
         * Retransmission intervals (RC = Retransmission Counter)
         * send CON message, set RC = 0
         * wait  2 - 3  sec then send retransmission, set RC = 1
         * wait  4 - 6  sec then send retransmission, set RC = 2
         * wait  8 - 12 sec then send retransmission, set RC = 3
         * wait 16 - 24 sec then send retransmission, set RC = 4
         * wait 32 - 48 sec then fail transmission
         * 
         * Resulting absolute intervals
         * 1st retransmission should be received after  2 - 3  sec
         * 2nd retransmission should be received after  6 - 9  sec
         * 3rd retransmission should be received after 14 - 21 sec
         * 4th retransmission should be received after 30 - 45 sec
         * 
         * (see http://tools.ietf.org/html/draft-ietf-core-coap-08#section-4.1)
         */
        
        Thread.sleep(45000 + tolerance); //see above

        testClient.disableReceiving();
        testReceiver.disableReceiving();
        System.out.println("Messages received:");
        int i = 0;
        for (ReceivedMessage<CoapMessage> msg: testReceiver.receivedMessages) {
            System.out.printf("Msg %d: Received after %6.3f seconds.%n", 
                    ++i, (msg.receivingTime - firstRequestSendTime) / 1000.0);
        }
        
        assertEquals("Unexpected response received by testClient",
                0, testClient.receivedResponses.size());
        //Expect 5 received messages, 1 org. request + 4 retransmissions
        assertEquals("testReceiver did not receive 5 messages",
                5, testReceiver.receivedMessages.size());
        
        
        //Check if receiving times match with the intervals above
        
        long timeDiff = testReceiver.receivedMessages.get(1).receivingTime - firstRequestSendTime;
        assertTrue("1st retransmission did not arrive in time.", 
                timeDiff > 2000 - tolerance &&
                timeDiff < 3000 + tolerance);
        
        timeDiff = testReceiver.receivedMessages.get(2).receivingTime - firstRequestSendTime;
        assertTrue("2nd retransmission did not arrive in time.", 
                timeDiff > 6000 - tolerance &&
                timeDiff < 9000 + tolerance);
        
        timeDiff = testReceiver.receivedMessages.get(3).receivingTime - firstRequestSendTime;
        assertTrue("3rd retransmission did not arrive in time.", 
                timeDiff > 14000 - tolerance &&
                timeDiff < 21000 + tolerance);
        
        timeDiff = testReceiver.receivedMessages.get(4).receivingTime - firstRequestSendTime;
        assertTrue("4th retransmission did not arrive in time.", 
                timeDiff > 30000 - tolerance &&
                timeDiff < 45000 + tolerance);
    }
    
    /**
     * Tests the processing of a piggy-backed response on the client side.
     */
    @Test
    public synchronized void clientSidePiggyBackedTest() throws Exception {
        System.out.println("Testing piggy-backed response on client side...");
        /* Sequence diagram:

        testClient          testReceiver
            |                   |
            +------------------>|     Header: GET (T=CON)
            |    coapRequest    |   Uri-Path: "testpath"
            |                   |   
            |                   |
            |                   |
            |<------------------+     Header: 2.05 Content (T=ACK)
            |  responseMessage  |    Payload: "responsepayload"  
            |                   |    
            |                   |
         */
        
        //reset client and receiver -> delete all received messages
        //                             and enable receiving
        testClient.reset();
        testReceiver.reset();
        
        //create and send request from testClient to testReceiver
        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, 
                new URI("coap://localhost:" + CoAPTestReceiver.PORT + "/testpath"), testClient);
        testClient.writeCoapRequest(coapRequest);
        
        //wait for request message to arrive at testReceiver
        testReceiver.blockUntilMessagesReceivedOrTimeout(500 /*ms timeout*/,1 /*msg count*/);
        testReceiver.disableReceiving();
        assertEquals("testReceiver should receive a single message", 1,
                testReceiver.receivedMessages.size());
        
        //receivedRequest is the actual CoAP request send out by nCoAP via testClient
        CoapMessage receivedRequest = testReceiver.receivedMessages.get(0).message;
        assertEquals("receivedRequest: type should be CON",
                MsgType.CON, receivedRequest.getHeader().getMsgType());
        assertEquals("receivedRequest: code should be GET",
                Code.GET, receivedRequest.getHeader().getCode());
        int messageID = receivedRequest.getMessageID();
        byte[] token = receivedRequest.getToken();
        
        //create response
        Header responseHeader = new Header(MsgType.ACK, Code.CONTENT_205, messageID);
        OptionList responseOptionList = new OptionList();
        if (token.length != 0) {
            responseOptionList.addOption(Code.CONTENT_205, OptionRegistry.OptionName.TOKEN, 
                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, token));
        }
        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
        CoapResponse responseMessage = new CoapResponse(responseHeader, responseOptionList, responsePayload);
        
        //send response from testReceiver to testClient
        Channels.write(testReceiver.channel, responseMessage, 
                new InetSocketAddress("localhost", CoAPTestClient.PORT));
        
        //wait for response message to arrive at testClient (via callback)
        testClient.blockUntilMessagesReceivedOrTimeout(800, 1);
        testClient.disableReceiving();
        assertEquals("testClient should receive a single message", 1,
                testClient.receivedResponses.size());
        CoapResponse receivedResponse = testClient.receivedResponses.get(0).message;
        assertArrayEquals("receivedResponse: token does not match", 
                token, receivedResponse.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue());
        assertEquals("receivedResponse: payload does not match",
                responsePayload, receivedResponse.getPayload());
        
    }
    
    /**
     * Tests the processing of a piggy-backed response on the server side.
     */
    @Test
    public synchronized void serverSidePiggyBackedTest() throws Exception {
        System.out.println("Testing piggy-backed response on server side...");
        /* Sequence diagram:

        testReceiver        testServer
            |                   |
            +------------------>|     Header: GET (T=CON)
            |    coapRequest    |   Uri-Path: "testpath"
            |                   |   
            |                   |
            |                   |
            |<------------------+     Header: 2.05 Content (T=ACK)
            |  responseMessage  |    Payload: "responsepayload"  
            |                   |    
            |                   |
         */
        
        //reset server and receiver -> delete all received messages
        //                             and enable receiving
        testServer.reset();
        testReceiver.reset();
        
        //create coapRequest which will later be sent from testReceiver to testServer
        int requestMessageID = 12345;
        byte[] requestToken = {0x12, 0x34, 0x56};
        String requestUriPath = "testpath";
        Header requestHeader = new Header(MsgType.CON, Code.GET, requestMessageID);
        OptionList requestOptionList = new OptionList();
        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.TOKEN, 
                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.URI_PATH, 
                    StringOption.createStringOption(OptionRegistry.OptionName.URI_PATH, requestUriPath));
        CoapRequest coapRequest = new CoapRequest(requestHeader, requestOptionList, null);
        
        //create response which will later be sent back from testServer to testReceiver
        CoapResponse responseMessage = new CoapResponse(MsgType.CON, Code.CONTENT_205);
        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
        responseMessage.setPayload(responsePayload);
        
        //register response at testServer
        testServer.responsesToSend.add(responseMessage);
        
        //send request from testReceiver to testServer
        Channels.write(testReceiver.channel, coapRequest, 
                new InetSocketAddress("localhost", CoAPTestServer.PORT));
        
        //when the request arrives, testServer will send the registered
        //response 'responseMessage' immediately back to testReceiver
        //-> wait for responseMessage at testReceiver
        testReceiver.blockUntilMessagesReceivedOrTimeout(800, 1);
        testReceiver.disableReceiving();
        assertEquals("testReceiver should receive a single message", 1,
                testReceiver.receivedMessages.size());
        assertEquals("testServer should receive a single message", 1,
                testServer.receivedRequests.size());
        testServer.disableReceiving();
        
        CoapRequest receivedRequest = testServer.receivedRequests.get(0).message; 
        CoapMessage receivedResponse = testReceiver.receivedMessages.get(0).message;
        
        //check the received request from testServer
        assertEquals("receivedRequest: messageID does not match", 
                requestMessageID, receivedRequest.getMessageID());
        assertArrayEquals("receivedRequest: token does not match", 
                requestToken, receivedRequest.getToken());
        assertEquals("receivedRequest: URI_PATH does not match", 
                requestUriPath, ((StringOption) receivedRequest
                .getOption(OptionRegistry.OptionName.URI_PATH).get(0)).getDecodedValue());
        assertEquals("receivedRequest: message type does not match", 
                MsgType.CON, receivedRequest.getMessageType());
        
        //check the received response from testReceiver
        assertEquals("receivedResponse: messageID does not match", 
                requestMessageID, receivedResponse.getMessageID());
        assertArrayEquals("receivedResponse: token does not match", 
                requestToken, receivedResponse.getToken());
        assertEquals("receivedResponse: code does not match", 
                Code.CONTENT_205, receivedResponse.getCode());
        assertEquals("receivedResponse: payload does not match", 
                responsePayload, receivedResponse.getPayload());
        assertEquals("receivedResponse: message type does not match", 
                MsgType.ACK, receivedResponse.getMessageType());
    }
    
    /**
     * Tests the processing of a separate response on the client side.
     */
    @Ignore //TODO fix issue https://github.com/okleine/nCoAP/issues/10
    @Test
    public synchronized void clientSideSeparateTest() throws Exception {
        System.out.println("Testing separate response on client side...");
        
        //TODO uncomment when separate-response bug in IncomingMessageReliabilityHandler is fixed

        //(see http://tools.ietf.org/html/draft-ietf-core-coap-08#page-77)
        testClient.reset();
        testReceiver.reset();
        
        //send request from testClient to testReceiver
        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, 
                new URI("coap://localhost:" + CoAPTestReceiver.PORT + "/testpath"), testClient);
        testClient.writeCoapRequest(coapRequest);
        
        //wait for request message to arrive
        long time = System.currentTimeMillis();
        while (testReceiver.receivedMessages.size() == 0) {
            if (System.currentTimeMillis() - time > 800) {
                fail("testReceiver did not receive the request within time.");
            }
            Thread.sleep(50);
        }
        testReceiver.disableReceiving();
        assertEquals("testReceiver received more than one message", 1,
                testReceiver.receivedMessages.size());
        
        //get values from received request
        CoapMessage receivedRequest = testReceiver.receivedMessages.get(0).message;
        assertEquals(MsgType.CON, receivedRequest.getHeader().getMsgType());
        assertEquals(Code.GET, receivedRequest.getHeader().getCode());
        int requestMessageID = receivedRequest.getMessageID();
        byte[] requestToken = receivedRequest.getToken();
        
        //create emppty response
        CoapResponse emptyResponseMessage = new CoapResponse(Code.EMPTY);
        emptyResponseMessage.setMessageID(requestMessageID);
        
        //send empty response to client
        testReceiver.reset();
        Channels.write(testReceiver.channel, emptyResponseMessage, new InetSocketAddress("localhost", CoAPTestClient.PORT));
        
        //wait 3 seconds then test if retransmissions were send (should not)
        Thread.sleep(3000);
        assertEquals("testReceiver received unexpected messages", 0, testReceiver.receivedMessages.size());
        
        //send (separate) CON response to testClient
        int responseMessageID = 1111;
        Header responseHeader = new Header(MsgType.CON, Code.CONTENT_205, responseMessageID);
        OptionList responseOptionList = new OptionList();
        if (requestToken.length != 0) {
            responseOptionList.addOption(Code.CONTENT_205, OptionRegistry.OptionName.TOKEN, 
                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
        }
        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
        CoapResponse responseMessage = new CoapResponse(responseHeader, responseOptionList, responsePayload);
        
        //send response to client
        Channels.write(testReceiver.channel, responseMessage, new InetSocketAddress("localhost", CoAPTestClient.PORT));
        
        //wait for (separate) CON response to arrive
        time = System.currentTimeMillis();
        while (testClient.receivedResponses.size() == 0) {
            if (System.currentTimeMillis() - time > 500) {
                fail("testClient did not receive the response within time.");
            }
            Thread.sleep(50);
        }
        testClient.disableReceiving();
        assertEquals("testClient received more than one message", 1,
                testClient.receivedResponses.size());
        CoapResponse receivedResponse = testClient.receivedResponses.get(0).message;
        assertArrayEquals(requestToken, receivedResponse.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue());
        assertEquals(responsePayload, receivedResponse.getPayload());
        
        //wait for empty ACK from testClient to testReceiver
        time = System.currentTimeMillis();
        while (testReceiver.receivedMessages.size() == 0) {
            if (System.currentTimeMillis() - time > 500) {
                fail("testReceiver did not receive the empty ACK within time.");
            }
            Thread.sleep(50);
        }
        testReceiver.disableReceiving();
        assertEquals("testReceiver received more than one message", 1,
                testReceiver.receivedMessages.size());
        CoapMessage receivedEmptyACK = testReceiver.receivedMessages.get(0).message;
        assertEquals("received message is not empty", Code.EMPTY, receivedEmptyACK.getCode());
        assertEquals("received message is not ACK", MsgType.ACK, receivedEmptyACK.getMessageType());
        
    }
    
    /**
     * Tests the processing of a separate response on the server side.
     */
    @Test
    public synchronized void serverSideSeparateTest() throws Exception {
        System.out.println("Testing separate response on server side...");
        /* Sequence diagram:
 
        testReceiver        testServer
            |                   |
            |    requestMsg     |            
            +------------------>|     Header: GET (T=CON, Code=1, MID=0x7d38=32056)
            |        GET        |      Token: 0x53
            |                   |   Uri-Path: "temperature"
            |                   |
            |  ackForRequestMsg |
            |<- - - - - - - - - +     Header: (T=ACK, Code=0, MID=0x7d38=32056)
            |                   |
            |    responseMsg    |
            |<------------------+     Header: 2.05 Content (T=CON, Code=69)
            |        2.05       |      Token: 0x53
            |                   |    Payload: "22.3 C"
            |                   |
            | ackForResponseMsg |
            + - - - - - - - - ->|     Header: (T=ACK, Code=0)
            |                   |
         */
        
        //reset server and receiver -> delete all received messages
        //                             and enable receiving
        testServer.reset();
        testReceiver.reset();
        
        //create 'requestMsg'
        int requestMessageID = 32056;
        byte[] requestToken = {0x53};
        String requestUriPath = "temperature";
        Header requestHeader = new Header(MsgType.CON, Code.GET, requestMessageID);
        OptionList requestOptionList = new OptionList();
        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.TOKEN, 
                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.URI_PATH, 
                    StringOption.createStringOption(OptionRegistry.OptionName.URI_PATH, requestUriPath));
        CoapRequest requestMsg = new CoapRequest(requestHeader, requestOptionList, null);     
        
        //create 'responseMsg'
        CoapResponse responseMsgToSend = new CoapResponse(MsgType.CON, Code.CONTENT_205);
        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("22.3 C".getBytes("UTF8"));
        responseMsgToSend.setPayload(responsePayload);
        
        //register response 'responseMsg' at testServer
        testServer.responsesToSend.add(responseMsgToSend);
        
        //set time (in ms) to wait before testServer sends the response
        testServer.waitBeforeSendingResponse = 2500;
        
        //send 'requestMsg' from testReceiver to testServer
        Channels.write(testReceiver.channel, requestMsg, 
                new InetSocketAddress("localhost", CoAPTestServer.PORT));
                
        //wait for two messages at testReceiver
        //('ackForRequestMsg' and 'responseMsg')
        testReceiver.blockUntilMessagesReceivedOrTimeout(3600, 2);
        
        //disable receiving at testServer and testReceiver
        testReceiver.disableReceiving();
        testServer.disableReceiving();
        
        //check amount of messages
        assertEquals("testReceiver should receive two messages", 2,
                testReceiver.receivedMessages.size());
        assertEquals("testServer should receive a single message", 1,
                testServer.receivedRequests.size());
        
        //get received messages
        CoapMessage ackForRequestMsg = testReceiver.receivedMessages.get(0).message;
        CoapMessage responseMsg = testReceiver.receivedMessages.get(1).message;
        
        //send 'ackForResponseMsg' to testServer        
        CoapMessage ackForResponseMsg = new CoapResponse(MsgType.ACK, Code.EMPTY, responseMsg.getMessageID());
        Channels.write(testReceiver.channel, ackForResponseMsg, 
                new InetSocketAddress("localhost", CoAPTestServer.PORT));
        
        //check 'ackForRequestMsg'
        assertEquals("ackForRequestMsg: message type does not match",
                MsgType.ACK, ackForResponseMsg.getMessageType());
        assertEquals("ackForRequestMsg: message code must be zero",
                Code.EMPTY, ackForRequestMsg.getCode());
        assertEquals("ackForRequestMsg: message ID does not match",
                requestMessageID, ackForRequestMsg.getMessageID());
        
        //check 'responseMsg'
        assertEquals("ackForRequestMsg: message type does not match",
                MsgType.CON, responseMsg.getMessageType());
        assertEquals("ackForRequestMsg: message code does not match",
                Code.CONTENT_205, responseMsg.getCode());
        assertArrayEquals("responseMsg: token does not match",
                requestToken, responseMsg.getToken());
        assertEquals("responseMsg: payload does not match", 
                responsePayload, responseMsg.getPayload());
    }
    
}

class CoAPTestClient extends CoapClientApplication {
    public static final int PORT = CoapClientDatagramChannelFactory.COAP_CLIENT_PORT;
            
    //if false receivedResponses will not be modified
    boolean receivingEnabled = true;
    public List<ReceivedMessage<CoapResponse>> receivedResponses = 
            new LinkedList<ReceivedMessage<CoapResponse>>();
    
    @Override
    public synchronized void receiveCoapResponse(CoapResponse coapResponse) {
        if (receivingEnabled) {
            receivedResponses.add(new ReceivedMessage<CoapResponse>(coapResponse));
        }
    }
    
    public synchronized void enableReceiving() {
        receivingEnabled = true;
    }
    
    public synchronized void disableReceiving() {
        receivingEnabled = false;
    }
    
    public synchronized void reset() {
        receivedResponses.clear();
        enableReceiving();
    }
    
    public void blockUntilMessagesReceivedOrTimeout(long timeout, int messagesCount) 
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < timeout) {
            synchronized(this) {
                if (receivedResponses.size() >= messagesCount) {
                    return;
                }
            }
            Thread.sleep(50);
        }
    }
}

class CoAPTestServer extends CoapServerApplication {
    public static final int PORT = CoapServerDatagramChannelFactory.COAP_SERVER_PORT;
            
    //if false receivedRequests will not be modified
    boolean receivingEnabled = true;
    public List<ReceivedMessage<CoapRequest>> receivedRequests = 
            new LinkedList<ReceivedMessage<CoapRequest>>();
    
    public List<CoapResponse> responsesToSend = new LinkedList<CoapResponse>();
    
    //time to block thread in receiveCoapRequest() to force a separate response
    public long waitBeforeSendingResponse = 0;
    
    public synchronized void enableReceiving() {
        receivingEnabled = true;
    }
    
    public synchronized void disableReceiving() {
        receivingEnabled = false;
    }
    
    public synchronized void reset() {
        receivedRequests.clear();
        responsesToSend.clear();
        waitBeforeSendingResponse = 0;
        enableReceiving();
    }
    
    public void blockUntilMessagesReceivedOrTimeout(long timeout, int messagesCount) 
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < timeout) {
            synchronized(this) {
                if (receivedRequests.size() >= messagesCount) {
                    return;
                }
            }
            Thread.sleep(50);
        }
    }

    @Override
    public CoapResponse receiveCoapRequest(CoapRequest coapRequest,
            InetSocketAddress senderAddress) {
        if (receivingEnabled) {
            synchronized(this) {
                receivedRequests.add(new ReceivedMessage<CoapRequest>(coapRequest));
            }
        }
        try {
            Thread.sleep(waitBeforeSendingResponse);
        } catch (InterruptedException ex) {
            fail(ex.toString());
        }
        if(responsesToSend.isEmpty()) {
            fail("responsesToSend is empty. This could be caused by an unexpected request.");
        }
        return responsesToSend.remove(0);
    }
}

class CoAPTestReceiver extends SimpleChannelHandler {
    public static final int PORT = 30431;
    DatagramChannel channel;

    //if false receivedMessages will not be modified
    boolean receivingEnabled = true;
    public List<ReceivedMessage<CoapMessage>> receivedMessages = 
            new LinkedList<ReceivedMessage<CoapMessage>>();
    
    public CoAPTestReceiver() {
        final CoAPTestReceiver testReceiverInstance = this;
        //Create Datagram Channel
        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(Executors.newCachedThreadPool());

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("CoAP Message Encoder", new CoapMessageEncoder());
                pipeline.addLast("CoAP Message Decoder", new CoapMessageDecoder());
                pipeline.addLast("CoAP Test Receiver", testReceiverInstance);
                return pipeline;
            }
        });

        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(PORT));
    }

    @Override
    public synchronized void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof CoapMessage) {
            CoapMessage coapMessage = (CoapMessage) e.getMessage();
            if (receivingEnabled) {
                receivedMessages.add(new ReceivedMessage<CoapMessage>(coapMessage));
            }
        }
    }
    
    public synchronized void enableReceiving() {
        receivingEnabled = true;
    }
    
    public synchronized void disableReceiving() {
        receivingEnabled = false;
    }
    
    public synchronized void reset() {
        receivedMessages.clear();
        enableReceiving();
    }
    
    public void blockUntilMessagesReceivedOrTimeout(long timeout, int messagesCount) 
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < timeout) {
            synchronized(this) {
                if (receivedMessages.size() >= messagesCount) {
                    return;
                }
            }
            Thread.sleep(50);
        }
    }
}

class ReceivedMessage<T> {
    T message;
    long receivingTime;

    public ReceivedMessage(T message) {
        this.message = message;
        receivingTime = System.currentTimeMillis();
    }
}