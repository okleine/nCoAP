package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.NotObservableTestWebService;
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
import java.util.Arrays;
import java.util.SortedMap;

import static junit.framework.Assert.*;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;

/**
* Tests to verify the server functionality related to piggy-backed responses.
*
* @author Stefan Hueske, Oliver Kleine
*/
public class ClientSendsNONRequest extends AbstractCoapCommunicationTest {

    private static CoapServerApplication server;
    private static NotObservableTestWebService service;
    private static String PATH_TO_SERVICE = "/could/be/any/path";
    private static String PAYLOAD = "some arbitrary payload";

    private static CoapTestEndpoint endpoint;
    private static CoapRequest request;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.application.endpoint").setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.DEBUG);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication(0);
        service = new NotObservableTestWebService(PATH_TO_SERVICE, PAYLOAD, 0);
        server.registerService(service);

        endpoint = new CoapTestEndpoint();
        URI targetUri = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MsgType.NON, Code.GET, targetUri);
        request.getHeader().setMsgID(54321);
    }

    @Override
    public void shutdownComponents() throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createTestScenario() throws Exception {

//             testEndpoint                    Server      DESCRIPTION
//                  |                             |
//              (1) |--------GET (NON)----------->|        send GET-Request to server
//                  |                             |
//              (2) |<-------NON-RESPONSE---------|        server responds with NON response
//                  |                             |
//                  |                             |

        //send request to testServer
        endpoint.writeMessage(request, new InetSocketAddress("localhost", server.getServerPort()));

        //wait some time for response from server
        Thread.sleep(150);
    }

    @Test
    public void testReceiverReceivedOnlyOneMessage() {
        String message = "Receiver received unexpected number of messages.";
        assertEquals(message, 1, endpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testReceiverReceivedResponse() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Received message is not a CoapResponse";
        assertTrue(message, receivedMessage instanceof CoapResponse);
    }

//    @Test
//    public void testReceivedMessageHasSameMsgID() {
//        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "Response Msg ID does not match with request Msg ID";
//        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
//    }

    @Test
    public void testReceivedMessageHasSameToken() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response token does not match with request token";
        assertTrue(message, Arrays.equals(request.getToken(), receivedMessage.getToken()));
    }

    @Test
    public void testReceivedMessageHasCodeContent() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response code is not CONTENT 205";
        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
    }

    @Test
    public void testReceivedMessageHasUnmodifiedPayload() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response payload was modified by testServer";
        assertEquals(message, PAYLOAD, receivedMessage.getPayload().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testReceivedMessageHasMsgTypeNON() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response Msg Type is not NON";
        assertEquals(message, MsgType.NON, receivedMessage.getMessageType());
    }
}
