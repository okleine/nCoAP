package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapMessageReceiver;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.BeforeClass;
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
public class ClientReceivesSeparateResponseTest {

    private static CoapTestServer testServer = CoapTestServer.getInstance();
    private static CoapMessageReceiver testReceiver = CoapMessageReceiver.getInstance();

    //request
    private static URI targetUri;
    private static CoapRequest coapRequest;
    private static String requestPath;
    private static int requestMsgID;
    private static byte[] requestToken;
    
    //response
    private static CoapResponse coapResponse;
    private static String responsePayload;
    
    //time
    private static long sendingTime;
    
    @BeforeClass
    public static void init() throws Exception {
        //init
        testReceiver.reset();
        testServer.reset();
        testReceiver.setReceiveEnabled(true); 
        
        //create request
        requestToken = new byte[]{0x12, 0x23, 0x34};
        requestPath = "/testpath";
        requestMsgID = 3333;
        targetUri = new URI("coap://localhost:" + CoapServerDatagramChannelFactory.COAP_SERVER_PORT + requestPath);
        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        coapRequest.getHeader().setMsgID(requestMsgID);
        coapRequest.setToken(requestToken);
        
        //create response
        responsePayload = "testpayload";
        coapResponse = new CoapResponse(Code.CONTENT_205);
        coapResponse.setPayload(responsePayload.getBytes("UTF-8"));
        coapResponse.getHeader().setMsgType(MsgType.ACK);
        
        //setup testServer
        testServer.registerDummyService(requestPath);
        testServer.addResponse(coapResponse);
        testServer.setWaitBeforeSendingResponse(2000);
        
        //setup testReceiver
        CoapResponse emptyACK = new CoapResponse(Code.EMPTY);
//        testReceiver.addResponse(new CoapMessageReceiver.MsgReceiverResponse(coapResponse, true, false));
        
        //send request to testServer
        testReceiver.writeMessage(coapRequest, new InetSocketAddress("localhost", 
                CoapServerDatagramChannelFactory.COAP_SERVER_PORT));
        sendingTime = System.currentTimeMillis();
        
        //wait for response
        //TODO list time intervals (wait for CON response ACK)
                
        long responseProcessing = 300; //time in ms
        Thread.sleep(2000 + responseProcessing);
        emptyACK.setMessageID(coapResponse.getMessageID());
        emptyACK.getHeader().setMsgType(MsgType.ACK);
        testReceiver.writeMessage(emptyACK, new InetSocketAddress("localhost", 
                CoapServerDatagramChannelFactory.COAP_SERVER_PORT));
        Thread.sleep(3000 - responseProcessing);
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
        assertEquals(message, coapResponse.getPayload(), receivedMessage.getPayload());
    }
    
    @Test
    public void test2ndReceivedMessageHasMsgTypeCON() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response Msg Type is not CON";
        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
    }
}
