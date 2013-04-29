package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapMessageReceiver;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.MessageReceiverResponse;
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
* Tests to verify the server functionality related to separate responses.
*
* @author Stefan Hueske
*/
public class ClientReceivesSeparateResponseTest extends AbstractCoapCommunicationTest{

    //request
    private static CoapRequest coapRequest;
    private static int requestMsgID;
    private static byte[] requestToken;

    //response
    private static CoapResponse expectedCoapResponse;

    @Override
    public void createTestScenario() throws Exception{

        /*
             testReceiver                    Server      DESCRIPTION
                  |                             |
              (1) |--------GET----------------->|        send GET-Request to server
                  |                             |
              (2) |<-------EMPTY-ACK------------|        server responds with empty ack to indicate a separate response
                  |                             | |
                  |                             | | 2,5 seconds until message is processed
                  |                             | | 
              (3) |<-------CON-RESPONSE---------|        server sends separate response
                  |                             |        
              (4) |--------EMPTY-ACK----------->|        client confirms arrival
                  |                             |
                  |                             | 
        */    
        
        //define expected response
        expectedCoapResponse = new CoapResponse(Code.CONTENT_205);
        expectedCoapResponse.setPayload(NOT_OBSERVABLE_RESOURCE_CONTENT.getBytes("UTF-8"));
        expectedCoapResponse.getHeader().setMsgType(MsgType.ACK);

        //setup test server
        registerNotObservableDummyService(2500);

        //create request
        requestToken = new byte[]{0x12, 0x23, 0x34};
        requestMsgID = 3333;
        URI serviceURI = new URI("coap://localhost:" + testServer.getServerPort() + NOT_OBSERVABLE_SERVICE_PATH);
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, serviceURI);
        coapRequest.getHeader().setMsgID(requestMsgID);
        coapRequest.setToken(requestToken);

        //send request to testServer
        testReceiver.writeMessage(coapRequest, new InetSocketAddress("localhost", testServer.getServerPort()));

        //wait for response (processing time on server to create seperate response is 2500ms)
        Thread.sleep(3000);

        //let testReceiver write empty ACK to acknowledge seperate response
        CoapResponse emptyACK = new CoapResponse(Code.EMPTY);
        emptyACK.setMessageID(testReceiver.getReceivedMessages().get(testReceiver.getReceivedMessages().lastKey()).getMessageID());
        emptyACK.getHeader().setMsgType(MsgType.ACK);
        testReceiver.writeMessage(emptyACK, new InetSocketAddress("localhost", testServer.getServerPort()));
        Thread.sleep(3000);
        testReceiver.setReceiveEnabled(false);
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Receiver did not receive two messages";
        assertEquals(message, 2, testReceiver.getReceivedMessages().values().size());
    }

    @Test
    public void testReceiverReceivedEmptyAck() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "First received message is not an EMPTY ACK";
        assertEquals(message, Code.EMPTY, receivedMessage.getCode());
    }

    @Test
    public void test2ndReceivedMessageIsResponse() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Receiver received more than one message";
        assertTrue(message, receivedMessage instanceof CoapResponse);
    }

    @Test
    public void test2ndReceivedMessageHasSameMsgID() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response Msg ID does not match with request Msg ID";
        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
    }

    @Test
    public void test2ndReceivedMessageHasSameToken() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response token does not match with request token";
        assertTrue(message, Arrays.equals(coapRequest.getToken(), receivedMessage.getToken()));
    }

    @Test
    public void test2ndReceivedMessageHasCodeContent() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response code is not CONTENT 205";
        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
    }

    @Test
    public void test2ndReceivedMessageHasUnmodifiedPayload() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response payload was modified by testServer";
        assertEquals(message, expectedCoapResponse.getPayload(), receivedMessage.getPayload());
    }

    @Test
    public void test2ndReceivedMessageHasMsgTypeCON() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response Msg Type is not CON";
        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
    }
}
