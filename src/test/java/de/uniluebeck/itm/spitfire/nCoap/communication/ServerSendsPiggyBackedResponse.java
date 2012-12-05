package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapMessageReceiver;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.SortedMap;

import static junit.framework.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 30.11.12
 * Time: 22:55
 * To change this template use File | Settings | File Templates.
 */
public class ServerSendsPiggyBackedResponse {

    private static CoapTestClient testClient= CoapTestClient.getInstance();
    private static CoapMessageReceiver testReceiver = CoapMessageReceiver.getInstance();

    private static URI targetUri;
    private static CoapRequest coapRequest;

    @BeforeClass
    public static void init() throws Exception{
        testClient.reset();
        testReceiver.reset();
        testReceiver.setReceiveEnabled(true);
        testReceiver.setWriteEnabled(true);

        targetUri = new URI("coap://localhost:" + CoapMessageReceiver.RECEIVER_PORT + "/testpath");
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri, testClient);

        testClient.writeCoapRequest(coapRequest);
        Thread.sleep(1000);

        //testReceiver.setReceiveEnabled(false);
    }

    @Test
    public void testReceivedRequestEqualsSentRequest(){
        SortedMap<Long, CoapMessage> receivedRequests = testReceiver.getReceivedMessages();
        String message = "Sent request and received request dont equal";
        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testReceiverReceivedOnlyOneRequest(){
        //TODO
    }

    @Test
    public void testClientDoesNotSendAnotherRequestAfterACKReception(){
        //TODO
    }

    @Test
    public void ResponseCallbackHandlerDeletedOpenRequestAfterACKReception(){
        //TODO
    }

    @Test
    public void TestServerDoesNotSendEmptyACKAfterPiggyBackedResponse(){
        //TODO
    }



//    //
//    /**
//     * Tests the processing of a piggy-backed response on the client side.
//     */
//    @Test
//    public synchronized void clientSidePiggyBackedTest() throws Exception {
//        System.out.println("Testing piggy-backed response on client side...");
//        /* Sequence diagram:
//
//        testClient          testMessageReceiver
//            |                   |
//            +------------------>|     Header: GET (T=CON)
//            |    coapRequest    |   Uri-Path: "testpath"
//            |                   |
//            |                   |
//            |                   |
//            |<------------------+     Header: 2.05 Content (T=ACK)
//            |  responseMessage  |    Payload: "responsepayload"
//            |                   |
//            |                   |
//         */
//
//        //reset client and receiver -> delete all received messages
//        //                             and enable receiving
//        testClient.reset();
//        testMessageReceiver.reset();
//
//        //create and send request from testClient to testMessageReceiver
//        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET,
//                new URI("coap://localhost:" + CoapMessageReceiver.RECEIVER_PORT + "/testpath"), testClient);
//        testClient.writeCoapRequest(coapRequest);
//
//        //wait for request message to arrive at testMessageReceiver
//        testMessageReceiver.blockUntilMessagesReceivedOrTimeout(500 /*ms timeout*/, 1 /*msg count*/);
//        testMessageReceiver.disableReceiving();
//        assertEquals("testMessageReceiver should receive a single message", 1,
//                testMessageReceiver.receivedMessages.size());
//
//        //receivedRequest is the actual CoAP request send out by nCoAP via testClient
//        CoapMessage receivedRequest = testMessageReceiver.receivedMessages.get(0).message;
//        assertEquals("receivedRequest: type should be CON",
//                MsgType.CON, receivedRequest.getHeader().getMsgType());
//        assertEquals("receivedRequest: code should be GET",
//                Code.GET, receivedRequest.getHeader().getCode());
//        int messageID = receivedRequest.getMessageID();
//        byte[] token = receivedRequest.getToken();
//
//        //create response
//        Header responseHeader = new Header(MsgType.ACK, Code.CONTENT_205, messageID);
//        OptionList responseOptionList = new OptionList();
//        if (token.length != 0) {
//            responseOptionList.addOption(Code.CONTENT_205, OptionRegistry.OptionName.TOKEN,
//                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, token));
//        }
//        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
//        CoapMessage responseMessage = new CoapMessage(responseHeader, responseOptionList, responsePayload) {};
//
//        //send response from testMessageReceiver to testClient
//        Channels.write(testMessageReceiver.channel, responseMessage,
//                new InetSocketAddress("localhost", CoapTestClient.PORT));
//
//        //wait for response message to arrive at testClient (via callback)
//        testClient.blockUntilMessagesReceivedOrTimeout(800, 1);
//        testClient.disableReceiving();
//        assertEquals("testClient should receive a single message", 1,
//                testClient.receivedResponses.size());
//        CoapResponse receivedResponse = testClient.receivedResponses.get(0).message;
//        assertArrayEquals("receivedResponse: token does not match",
//                token, receivedResponse.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue());
//        assertEquals("receivedResponse: payload does not match",
//                responsePayload, receivedResponse.getPayload());
//
//    }
//
//    /**
//     * Tests the processing of a piggy-backed response on the server side.
//     */
//    @Test
//    public synchronized void serverSidePiggyBackedTest() throws Exception {
//        System.out.println("Testing piggy-backed response on server side...");
//        /* Sequence diagram:
//
//        testMessageReceiver        testServer
//            |                   |
//            +------------------>|     Header: GET (T=CON)
//            |    coapRequest    |   Uri-Path: "testpath"
//            |                   |
//            |                   |
//            |                   |
//            |<------------------+     Header: 2.05 Content (T=ACK)
//            |  responseMessage  |    Payload: "responsepayload"
//            |                   |
//            |                   |
//         */
//
//        //reset server and receiver -> delete all received messages
//        //                             and enable receiving
//        testServer.reset();
//        testMessageReceiver.reset();
//
//        //create coapRequest which will later be sent from testMessageReceiver to testServer
//        int requestMessageID = 12345;
//        byte[] requestToken = {0x12, 0x34, 0x56};
//        String requestUriPath = "testpath";
//        Header requestHeader = new Header(MsgType.CON, Code.GET, requestMessageID);
//        OptionList requestOptionList = new OptionList();
//        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.TOKEN,
//                    OpaqueOption.createOpaqueOption(OptionRegistry.OptionName.TOKEN, requestToken));
//        requestOptionList.addOption(Code.GET, OptionRegistry.OptionName.URI_PATH,
//                    StringOption.createStringOption(OptionRegistry.OptionName.URI_PATH, requestUriPath));
//        CoapMessage coapRequest = new CoapMessage(requestHeader, requestOptionList, null) {};
//
//        //create response which will later be sent back from testServer to testMessageReceiver
//        CoapResponse responseMessage = new CoapResponse(MsgType.CON, Code.CONTENT_205);
//        ChannelBuffer responsePayload = ChannelBuffers.wrappedBuffer("responsepayload".getBytes("UTF8"));
//        responseMessage.setPayload(responsePayload);
//
//        //register response at testServer
//        testServer.responsesToSend.add(responseMessage);
//
//        //send request from testMessageReceiver to testServer
//        Channels.write(testMessageReceiver.channel, coapRequest,
//                new InetSocketAddress("localhost", CoAPTestServer.PORT));
//
//        //when the request arrives, testServer will send the registered
//        //response 'responseMessage' immediately back to testMessageReceiver
//        //-> wait for responseMessage at testMessageReceiver
//        testMessageReceiver.blockUntilMessagesReceivedOrTimeout(800, 1);
//        testMessageReceiver.disableReceiving();
//        assertEquals("testMessageReceiver should receive a single message", 1,
//                testMessageReceiver.receivedMessages.size());
//        assertEquals("testServer should receive a single message", 1,
//                testServer.receivedRequests.size());
//        testServer.disableReceiving();
//
//        CoapRequest receivedRequest = testServer.receivedRequests.get(0).message;
//        CoapMessage receivedResponse = testMessageReceiver.receivedMessages.get(0).message;
//
//        //check the received request from testServer
//        assertEquals("receivedRequest: messageID does not match",
//                requestMessageID, receivedRequest.getMessageID());
//        assertArrayEquals("receivedRequest: token does not match",
//                requestToken, receivedRequest.getToken());
//        assertEquals("receivedRequest: URI_PATH does not match",
//                requestUriPath, ((StringOption) receivedRequest
//                .getOption(OptionRegistry.OptionName.URI_PATH).get(0)).getDecodedValue());
//        assertEquals("receivedRequest: message type does not match",
//                MsgType.CON, receivedRequest.getMessageType());
//
//        //check the received response from testMessageReceiver
//        assertEquals("receivedResponse: messageID does not match",
//                requestMessageID, receivedResponse.getMessageID());
//        assertArrayEquals("receivedResponse: token does not match",
//                requestToken, receivedResponse.getToken());
//        assertEquals("receivedResponse: code does not match",
//                Code.CONTENT_205, receivedResponse.getCode());
//        assertEquals("receivedResponse: payload does not match",
//                responsePayload, receivedResponse.getPayload());
//        assertEquals("receivedResponse: message type does not match",
//                MsgType.ACK, receivedResponse.getMessageType());
//    }

}
