package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.client.TestResponseProcessor;
import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

    private static CoapClientApplication client;
    private static TestResponseProcessor responseProcessor;
    private static CoapRequest request;

    private static CoapTestEndpoint endpoint;
    private static CoapResponse seperateResponse;

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint").setLevel(Level.DEBUG);
    }

    @Override
    public void setupComponents() throws Exception {
        endpoint = new CoapTestEndpoint();

        seperateResponse = new CoapResponse(Code.CONTENT_205);
        seperateResponse.setPayload("some arbitrary stuff...".getBytes(Charset.forName("UTF-8")));
        seperateResponse.getHeader().setMsgType(MsgType.CON);
        seperateResponse.setMessageID(12345);

        client = new CoapClientApplication();

        URI targetUri = new URI("coap://localhost:" + endpoint.getPort());
        request = new CoapRequest(MsgType.CON, Code.GET, targetUri);

        responseProcessor = new TestResponseProcessor();
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        endpoint.shutdown();
    }


    @Override
    public void createTestScenario() throws Exception {


//             testClient                    testEndpoint     DESCRIPTION
//                  |                             |
//              (1) |--------GET----------------->|           testClient sends request to testEndpoint
//                  |                             |
//              (2) |<-------EMPTY-ACK------------|           testEndpoint responds with empty ack to indicate a separate response
//                  |                             | |
//                  |                             | | wait 1 second to simulate processing time
//                  |                             | |
//              (3) |<-------CON-RESPONSE---------|           testEndpoint sends separate response
//                  |                             |
//              (4) |--------EMPTY-ACK----------->|           testClient confirms arrival
//                  |                             |
//                  |                             |


        //write request
        client.writeCoapRequest(request, responseProcessor);

        //wait (2000 - epsilon) milliseconds
        Thread.sleep(1800);

        //create and write empty ACK
        int messageID =
                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getMessageID();
        CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
        endpoint.writeMessage(emptyACK, new InetSocketAddress("localhost", client.getClientPort()));

        //wait another some time to simulate request processing
        Thread.sleep(500);

        //create seperate response to be sent by the message receiver
        byte[] token =
                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getToken();
        seperateResponse.setToken(token);

        //send seperate response
        endpoint.writeMessage(seperateResponse, new InetSocketAddress("localhost", client.getClientPort()));

        //wait some time for ACK from client
        Thread.sleep(500);
    }



    @Test
    public void testReceivedRequestEqualsSentRequest() {
        SortedMap<Long, CoapMessage> receivedRequests = endpoint.getReceivedMessages();
        String message = "Written and received request do not equal";
        assertEquals(message, request, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testEndpointReceivedTwoMessages() {
        String message = "Receiver received wrong number of messages";
        assertEquals(message, 2, endpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testClientCallbackInvokedOnce() {
        String message = "Client callback was invoked less or more than once";
        assertEquals(message, 1, responseProcessor.getCoapResponses().size());
    }

    @Test
    public void test2ndReceivedMessageIsEmptyACK() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Second received message is not an EMPTY ACK";
        assertEquals(message, Code.EMPTY, receivedMessage.getCode());
        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
    }
}
