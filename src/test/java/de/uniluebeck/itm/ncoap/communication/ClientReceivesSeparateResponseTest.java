package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableTestWebService;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import java.net.InetSocketAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.SortedMap;


import static junit.framework.Assert.*;


/**
* Tests to verify the server functionality related to separate responses.
*
* @author Stefan Hueske
*/
public class ClientReceivesSeparateResponseTest extends AbstractCoapCommunicationTest{

    private static String PATH_TO_SERVICE = "/path/to/service";
    private static String PAYLOAD = "some arbitrary payload...";
    private static CoapServerApplication server;
    private static NotObservableTestWebService service;

    private static CoapTestEndpoint endpoint;
    private static CoapRequest request;

    private static long requestSentTime;

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap").setLevel(Level.DEBUG);
        //Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.outgoing").setLevel(Level.INFO);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication(0);
        service = new NotObservableTestWebService(PATH_TO_SERVICE, PAYLOAD, 3000);
        server.registerService(service);

        endpoint = new CoapTestEndpoint();
        URI targetUri = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        request.getHeader().setMsgID(12345);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        endpoint.shutdown();
    }

    @Override
    public void createTestScenario() throws Exception{

//             testEndpoint                    Server      DESCRIPTION
//                  |                             |
//              (1) |--------GET----------------->|        send GET-Request to server
//                  |                             |
//              (2) |<-------EMPTY-ACK------------|        server responds with empty ACK (after ~ 2 sec.)
//                  |                             |
//              (3) |<-------CON-RESPONSE---------|        server sends separate response (after ~ 3 sec.)
//                  |                             |
//              (4) |--------EMPTY-ACK----------->|        client confirms arrival
//                  |                             |


        //send request to testServer
        endpoint.writeMessage(request, new InetSocketAddress("localhost", server.getServerPort()));
        requestSentTime = System.currentTimeMillis();

        //wait for responses from server (one empty ACK and one CON response afterwards)
        Thread.sleep(3100);

        //let testEndpoint write empty ACK to acknowledge seperate response
        int messageID = endpoint.getReceivedMessages()
                                .get(endpoint.getReceivedMessages().lastKey())
                                .getMessageID();

        CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);
        endpoint.writeMessage(emptyACK, new InetSocketAddress("localhost", server.getServerPort()));

        //Wait some time to let the server receive the ACK and ensure there is no retransmission
        Thread.sleep(3000);
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Receiver did not receive two messages";
        assertEquals(message, 2, endpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testReceiverReceivedEmptyAck() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();

        long emptyAckReceptionTime = receivedMessages.firstKey();
        long delay = requestSentTime - emptyAckReceptionTime;

        assertTrue("Empty ACK was too late (delay: " + delay + "ms.", delay < 2000);

        CoapMessage receivedMessage = receivedMessages.get(emptyAckReceptionTime);
        String message = "First received message is not an EMPTY ACK";

        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
        assertEquals(message, Code.EMPTY, receivedMessage.getCode());
    }

    @Test
    public void test2ndReceivedMessageIsResponse() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());

        String message = "Endpoint received more than one message";
        assertTrue(message, receivedMessage instanceof CoapResponse);
    }

    @Test
    public void test2ndReceivedMessageHasSameToken() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response token does not match with request token";
        assertTrue(message, Arrays.equals(request.getToken(), receivedMessage.getToken()));
    }

    @Test
    public void test2ndReceivedMessageHasCodeContent() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response code is not CONTENT 205";
        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
    }

    @Test
    public void test2ndReceivedMessageHasUnmodifiedPayload() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response payload was modified by testServer";
        assertEquals(message, PAYLOAD, receivedMessage.getPayload().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void test2ndReceivedMessageHasMsgTypeCON() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.lastKey());
        String message = "Response Msg Type is not CON";
        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
    }
}
