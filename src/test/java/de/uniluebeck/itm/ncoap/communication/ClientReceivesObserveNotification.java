package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.TestResponseProcessor;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableTestWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;

import static junit.framework.Assert.assertEquals;

/**
* Tests if a client receives notifications.
* @author Stefan Hueske, Oliver Kleine
*/
public class ClientReceivesObserveNotification extends AbstractCoapCommunicationTest{

    private static final String PATH_TO_SERVICE = "/observable";

    private static CoapClientApplication client;
    private static TestResponseProcessor responseProcessor;

    private static CoapServerApplication server;
    private static ObservableTestWebService service;

    //observable request
    private static CoapRequest request;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.client.TestResponseProcessor")
                .setLevel(Level.INFO);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication(0);
        service = new ObservableTestWebService(PATH_TO_SERVICE, 1, 0);
        server.registerService(service);

        client = new CoapClientApplication();
        responseProcessor = new TestResponseProcessor();

        URI targetUri = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        request.setObserveOptionRequest();
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
    }


    @Override
    public void createTestScenario() throws Exception {

//               Client                        Server
//              (1) |------GET-OBSERVE----------->|           send observable request to server
//                  |                             |
//              (2) |<-----ACK-NOTIFICATION-------|           server responds with initial, piggy-backed notification
//                  |                             |
//                  |                             |  <------  status update (new status: 2)
//                  |                             |
//              (3) |<-----CON-NOTIFICATION-------|           server sends 2nd notification,
//                  |                             |
//              (4) |------EMPTY-ACK------------->|
//                  |                             |
//                  |                             |  <------- shutdown server
//                  |                             |
//              (5) |<-----CON-NOTIFICATION-------|           server sends 3rd notification (404 not found)



        //write request
        client.writeCoapRequest(request, responseProcessor);

        Thread.sleep(3000);
        service.setResourceStatus(2);

        Thread.sleep(1000);

        server.shutdown();
        Thread.sleep(1000);
    }

    @Test
    public void testClientReceived3Messages() {
        String message = "Receiver did not receive 3 messages";
        assertEquals(message, 3, responseProcessor.getCoapResponses().size());
    }

    @Test
    public void testFirstMessage(){
        CoapResponse response = responseProcessor.getCoapResponse(0);

        assertEquals("Messagt type is not ACK", MsgType.ACK, response.getMessageType());

        assertEquals("Content does not match.", "Status #1",
                response.getPayload().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testSecondMessage(){
        CoapResponse response = responseProcessor.getCoapResponse(1);

        assertEquals("Messagt type is not CON", MsgType.CON, response.getMessageType());

        assertEquals("Content does not match.", "Status #2",
                response.getPayload().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testThirdMessage(){
        CoapResponse response = responseProcessor.getCoapResponse(2);

        assertEquals("Messagt type is not CON", MsgType.CON, response.getMessageType());

        assertEquals("Code is not 404", Code.NOT_FOUND_404, response.getCode());
    }

//    @Test
//    public void testReceiverReceivedNotification1() {
//        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, expectedNotification1.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification2() {
//        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd notification: MsgType is not ACK";
//        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//        message = "2nd notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "2nd notification: Payload does not match";
//        assertEquals(message, expectedNotification2.getPayload(), receivedMessage.getPayload());
//    }


}
