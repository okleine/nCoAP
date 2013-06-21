package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.spitfire.nCoap.application.client.TestCoapReponseProcessor;
import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;

//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.MessageReceiverResponse;
import org.junit.Test;

import java.net.URI;
import java.util.SortedMap;

import static junit.framework.Assert.*;


/**
* Tests to verify the client functionality related to piggy-backed responses.
*
* @author Oliver Kleine, Stefan Hueske
*/
public class ServerSendsPiggyBackedResponseTest extends AbstractCoapCommunicationTest {

    private static final String PAYLOAD = "Some arbitrary content!";

    private static CoapClientApplication client;
    private static TestCoapReponseProcessor responseProcessor;
    private static CoapRequest coapRequest;

    private static CoapTestEndpoint endpoint;
    static{
        try{

        } catch (Exception e) {
            fail("This should never happen!");
        }
    }

    @Override
    public void setupComponents() throws Exception {

        //Create endpoint
        endpoint = new CoapTestEndpoint();

        //Create client and response processor
        client = new CoapClientApplication();
        responseProcessor = new TestCoapReponseProcessor();

        URI targetUri =  new URI("coap://localhost:" + endpoint.getPort());
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        endpoint.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {

    }

    @Override
    public void createTestScenario() throws Exception {

        /*
             testClient                    testEndpoint     DESCRIPTION
                  |                             |
              (1) |--------GET----------------->|           client sends GET-Request to testEndpoint
                  |                             |
              (2) |<-------ACK-RESPONSE---------|           testEndpoint responds
                  |                             |
              (3) |<-------ACK-RESPONSE---------|           testEndpoint sends the response again,
                  |                             |           nothing should happen here when the client
                  |                             |           removed the callback as expected
        */

        //write request
        client.writeCoapRequest(coapRequest, responseProcessor);

        //Wait some time
        Thread.sleep(300);

        //Get message ID and token from received message
        int messageID =
                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getMessageID();
        byte[] token =
                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getToken();

        //write response #1
        CoapResponse response = createResponse(messageID, token);
        endpoint.writeMessage(response, new InetSocketAddress("localhost", client.getClientPort()));

        //Wait some time
        Thread.sleep(300);

        //write response #2
        CoapResponse response2 = createResponse(messageID, token);
        endpoint.writeMessage(response2, new InetSocketAddress("localhost", client.getClientPort()));

        //Wait some time
        Thread.sleep(300);
        endpoint.setReceiveEnabled(false);
    }



    private static CoapResponse createResponse(int messageID, byte[] token) throws Exception {
        CoapResponse coapResponse = new CoapResponse(Code.CONTENT_205);
        coapResponse.getHeader().setMsgType(MsgType.ACK);
        coapResponse.setMessageID(messageID);
        coapResponse.setToken(token);
        coapResponse.setPayload(PAYLOAD.getBytes("UTF-8"));

        return coapResponse;
    }

    @Test
    public void testReceivedRequestEqualsSentRequest() {
        SortedMap<Long, CoapMessage> receivedRequests = endpoint.getReceivedMessages();
        String message = "Written and received request do not equal";
        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testReceiverReceivedOnlyOneRequest() {
        String message = "Receiver received more than one message";
        assertEquals(message, 1, endpoint.getReceivedMessages().size());
    }

    @Test
    public void testClientCallbackInvokedOnce() {
        String message = "Client callback was invoked less or more than once";
        assertEquals(message, 1, responseProcessor.getCoapResponses().size());
    }
}
