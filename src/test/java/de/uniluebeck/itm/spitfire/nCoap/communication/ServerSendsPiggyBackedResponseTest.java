package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;

//import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.MessageReceiverResponse;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.SortedMap;

import static junit.framework.Assert.*;


/**
 * Tests to verify the client functionality related to piggy-backed responses.
 *
 * @author Oliver Kleine, Stefan Hueske
 */
public class ServerSendsPiggyBackedResponseTest extends AbstractCoapCommunicationTest {

    private static CoapRequest coapRequest;
    static{
        try{
            URI targetUri =  new URI("coap://localhost:" + testEndpoint.getPort());
            coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri, testClient);
        } catch (Exception e) {
            fail("This should never happen!");
        }
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
        testClient.writeCoapRequest(coapRequest);

        //Wait some time
        Thread.sleep(300);

        //Get message ID and token from received message
        int messageID =
                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().lastKey()).getMessageID();
        byte[] token =
                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().lastKey()).getToken();

        //write response #1
        testEndpoint.writeMessage(createResponse(messageID, token),
                new InetSocketAddress("localhost", testClient.getClientPort()));

        //Wait some time
        Thread.sleep(300);

        //write response #2
        testEndpoint.writeMessage(createResponse(messageID, token), new InetSocketAddress("localhost", testClient.getClientPort()));

        //Wait some time
        Thread.sleep(300);
        testEndpoint.setReceiveEnabled(false);
    }

    private static CoapResponse createResponse(int messageID, byte[] token) throws Exception {
        CoapResponse coapResponse = new CoapResponse(Code.CONTENT_205);
        coapResponse.setMessageID(messageID);
        coapResponse.setToken(token);
        coapResponse.setPayload(NOT_OBSERVABLE_RESOURCE_CONTENT.getBytes("UTF-8"));
        coapResponse.getHeader().setMsgType(MsgType.ACK);

        return coapResponse;
    }

    @Test
    public void testReceivedRequestEqualsSentRequest() {
        SortedMap<Long, CoapMessage> receivedRequests = testEndpoint.getReceivedMessages();
        String message = "Written and received request do not equal";
        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testReceiverReceivedOnlyOneRequest() {
        String message = "Receiver received more than one message";
        assertEquals(message, 1, testEndpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testClientCallbackInvokedOnce() {
        String message = "Client callback was invoked less or more than once";
        assertEquals(message, 1, testClient.getReceivedResponses().values().size());
    }
}
