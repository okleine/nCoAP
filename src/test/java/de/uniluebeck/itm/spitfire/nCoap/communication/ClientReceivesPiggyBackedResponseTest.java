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


/**
 * Tests to verify the server functionality related to piggy-backed responses.
 * 
 * @author Stefan Hueske
 */
public class ClientReceivesPiggyBackedResponseTest {

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
    
    
    @BeforeClass
    public static void init() throws Exception {
        //init
        testReceiver.reset();
        testServer.reset();
        testReceiver.setReceiveEnabled(true); 
        
        //create request
        requestToken = new byte[]{0x12, 0x24, 0x36};
        requestPath = "/testpath";
        requestMsgID = 3334;
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
        
        //send request to testServer
        testReceiver.writeMessage(coapRequest, new InetSocketAddress("localhost", 
                CoapServerDatagramChannelFactory.COAP_SERVER_PORT));
        
        //wait for response
        Thread.sleep(300);
        
        testReceiver.setReceiveEnabled(false);
    }
    
    @Test
    public void testReceiverReceivedOnlyOneMessage() {
        String message = "Receiver received more than one message";
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
        assertEquals(message, coapResponse.getPayload(), receivedMessage.getPayload());
    }
    
    @Test
    public void testReceivedMessageHasMsgTypeACK() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response Msg Type is not ACK";
        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
    }
}
