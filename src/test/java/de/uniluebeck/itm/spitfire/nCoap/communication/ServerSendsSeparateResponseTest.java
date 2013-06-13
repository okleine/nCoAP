package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;

import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.SortedMap;

import static junit.framework.Assert.*;


/**
* Tests to verify the client functionality related to separate responses.
*
* @author Stefan Hueske, Oliver Kleine
*/
public class ServerSendsSeparateResponseTest extends AbstractCoapCommunicationTest {

    private static CoapRequest coapRequest;

    @Override
    public void createTestScenario() throws Exception {

        /*
             testClient                    testEndpoint     DESCRIPTION
                  |                             |
              (1) |--------GET----------------->|           testClient sends request to testEndpoint
                  |                             |
              (2) |<-------EMPTY-ACK------------|           testEndpoint responds with empty ack to indicate a separate response
                  |                             | |
                  |                             | | wait 1 second to simulate processing time
                  |                             | |
              (3) |<-------CON-RESPONSE---------|           testEndpoint sends separate response
                  |                             |
              (4) |--------EMPTY-ACK----------->|           testClient confirms arrival
                  |                             |
                  |                             |
        */

        //create request
        URI targetUri = new URI("coap://localhost:" + testEndpoint.getPort());
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri, testClient);

        //write request
        testClient.writeCoapRequest(coapRequest);

        //wait (2000 - epsilon) milliseconds
        Thread.sleep(1800);

        //Get message ID and token from received message
        int messageID =
                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().lastKey()).getMessageID();
        byte[] token =
                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().lastKey()).getToken();

        //create and write empty ACK
        CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
        testEndpoint.writeMessage(emptyACK, new InetSocketAddress("localhost", testClient.getClientPort()));

        //wait another some time to simulate request processing
        Thread.sleep(500);

        //create seperate response to be sent by the message receiver
        CoapResponse coapResponse = new CoapResponse(Code.CONTENT_205);
        coapResponse.setPayload("some arbitrary stuff...".getBytes(Charset.forName("UTF-8")));
        coapResponse.getHeader().setMsgType(MsgType.CON);
        coapResponse.setMessageID(12345);
        coapResponse.setToken(token);

        //send seperate response
        testEndpoint.writeMessage(coapResponse, new InetSocketAddress("localhost", testClient.getClientPort()));

        //wait some time for ACK from client
        Thread.sleep(500);
    }

    @Test
    public void testReceivedRequestEqualsSentRequest() {
        SortedMap<Long, CoapMessage> receivedRequests = testEndpoint.getReceivedMessages();
        String message = "Written and received request do not equal";
        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testEndpointReceivedTwoMessages() {
        String message = "Receiver received wrong number of messages";
        assertEquals(message, 2, testEndpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testClientCallbackInvokedOnce() {
        String message = "Client callback was invoked less or more than once";
        assertEquals(message, 1, testClient.getReceivedResponses().values().size());
    }

    @Test
    public void test2ndReceivedMessageIsEmptyACK() {
        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Second received message is not an EMPTY ACK";
        assertEquals(message, Code.EMPTY, receivedMessage.getCode());
        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
    }
}
