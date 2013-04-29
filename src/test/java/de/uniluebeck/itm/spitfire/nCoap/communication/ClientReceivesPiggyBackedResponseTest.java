package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.SortedMap;

import static junit.framework.Assert.*;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;

/**
* Tests to verify the server functionality related to piggy-backed responses.
*
* @author Stefan Hueske
*/
public class ClientReceivesPiggyBackedResponseTest extends AbstractCoapCommunicationTest {

    //request
    private static CoapRequest coapRequest;
    private static int requestMsgID;
    private static byte[] requestToken;

    //response
    private static CoapResponse expectedCoapResponse;

    @Override
    public void createTestScenario() throws Exception {
        
        /*
             testReceiver                    Server      DESCRIPTION
                  |                             |
              (1) |--------GET----------------->|        send GET-Request to server
                  |                             |
              (2) |<-------ACK-RESPONSE---------|        server responds with piggy-backed response
                  |                             |
                  |                             | 
        */    
        
        //define expected response
        expectedCoapResponse = new CoapResponse(Code.CONTENT_205);
        expectedCoapResponse.setPayload(NOT_OBSERVABLE_RESOURCE_CONTENT.getBytes("UTF-8"));
        expectedCoapResponse.getHeader().setMsgType(MsgType.ACK);

        //register webservice
        registerNotObservableDummyService(0);

        //create request
        requestToken = new byte[]{0x12, 0x24, 0x36};
        requestMsgID = 3334;
        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + NOT_OBSERVABLE_SERVICE_PATH);
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        coapRequest.getHeader().setMsgID(requestMsgID);
        coapRequest.setToken(requestToken);


        //send request to testServer
        testReceiver.writeMessage(coapRequest, new InetSocketAddress("localhost", testServer.getServerPort()));

        //wait for response
        Thread.sleep(150);

        testReceiver.setReceiveEnabled(false);
    }

    @Test
    public void testReceiverReceivedOnlyOneMessage() {
        String message = "Receiver received unexpected number of messages.";
        assertEquals(message, 1, testReceiver.getReceivedMessages().values().size());
    }

    @Test
    public void testReceiverReceivedResponse() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Received message is not a CoapResponse";
        assertTrue(message, receivedMessage instanceof CoapResponse);
    }

    @Test
    public void testReceivedMessageHasSameMsgID() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response Msg ID does not match with request Msg ID";
        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
    }

    @Test
    public void testReceivedMessageHasSameToken() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response token does not match with request token";
        assertTrue(message, Arrays.equals(coapRequest.getToken(), receivedMessage.getToken()));
    }

    @Test
    public void testReceivedMessageHasCodeContent() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response code is not CONTENT 205";
        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
    }

    @Test
    public void testReceivedMessageHasUnmodifiedPayload() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response payload was modified by testServer";
        assertEquals(message, expectedCoapResponse.getPayload(), receivedMessage.getPayload());
    }

    @Test
    public void testReceivedMessageHasMsgTypeACK() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response Msg Type is not ACK";
        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
    }
}
