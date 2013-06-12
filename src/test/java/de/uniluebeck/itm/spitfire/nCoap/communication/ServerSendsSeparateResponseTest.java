package de.uniluebeck.itm.spitfire.nCoap.communication;

//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.MessageReceiverResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;

import org.junit.Test;

import java.net.URI;
import java.util.SortedMap;

import static junit.framework.Assert.*;


/**
 * Tests to verify the client functionality related to separate responses.
 *
 * @author Stefan Hueske
 */
public class ServerSendsSeparateResponseTest extends AbstractCoapCommunicationTest {

    //request
    private static URI targetUri;
    private static CoapRequest coapRequest;
    private static String requestPath;

    //empty ACK
    private static CoapResponse emptyACK;

    //response
    private static CoapResponse coapResponse;
    private static String responsePayload;
    private static int responseMsgID;

    @Override
    public void createTestScenario() throws Exception {
        
        /*
             testClient                    testReceiver     DESCRIPTION
                  |                             |
              (1) |--------GET----------------->|           testClient sends request to testReceiver
                  |                             |
              (2) |<-------EMPTY-ACK------------|           testReceiver responds with empty ack to indicate a separate response
                  |                             | |
                  |                             | | wait 1 second to simulate processing time
                  |                             | | 
              (3) |<-------CON-RESPONSE---------|           testReceiver sends separate response
                  |                             |        
              (4) |--------EMPTY-ACK----------->|           testClient confirms arrival
                  |                             |
                  |                             | 
        */    
        
        //create request
        requestPath = "/testpath";
        targetUri = new URI("coap://localhost:" + testReceiver.getReceiverPort() + requestPath);
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri, testClient);

        //create empy ack
        emptyACK = new CoapResponse(Code.EMPTY);
        emptyACK.getHeader().setMsgType(MsgType.ACK);
        //testReceiver.addMessageToOutgoingQueue(new MessageReceiverResponse(emptyACK, true, false));

        //create seperate response to be sent by the message receiver
        responsePayload = "testpayload";
        coapResponse = new CoapResponse(Code.CONTENT_205);
        coapResponse.setPayload(responsePayload.getBytes("UTF-8"));
        coapResponse.getHeader().setMsgType(MsgType.CON);

        //write request, disable receiving after 500ms
        testClient.writeCoapRequest(coapRequest);
        Thread.sleep(1000);

        //send empty ack
        //emptyACK.setMessageID(coapRequest.getMessageID());
        //testReceiver.writeMessage(emptyACK, new InetSocketAddress("localhost", testClient.getClientPort()));
        Thread.sleep(1000);

        //send separate response
        responseMsgID = 3333;
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(responseMsgID);
        testReceiver.writeMessage(coapResponse, new InetSocketAddress("localhost", testClient.getClientPort()));
        
        //wait for ack
        Thread.sleep(2500);
        testReceiver.setReceiveEnabled(false);
    }

    @Test
    public void testReceivedRequestEqualsSentRequest() {
        SortedMap<Long, CoapMessage> receivedRequests = testReceiver.getReceivedMessages();
        String message = "Written and received request do not equal";
        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Receiver received wrong number of messages";
        assertEquals(message, 2, testReceiver.getReceivedMessages().values().size());
    }

    @Test
    public void testClientCallbackInvokedOnce() {
        String message = "Client callback was invoked less or more than once";
        assertEquals(message, 1, testClient.getReceivedResponses().values().size());
    }

    @Test
    public void test2ndReceivedMessageIsEmptyACK() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Second received message is not an EMPTY ACK";
        assertEquals(message, Code.EMPTY, receivedMessage.getCode());
        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
    }
}
