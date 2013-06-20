package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapTestClient;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.NotObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 18.06.13
 * Time: 14:58
 * To change this template use File | Settings | File Templates.
 */
public class TestParallelRequests extends AbstractCoapCommunicationTest {

    private static CoapTestClient[] clients = new CoapTestClient[3];
    private static CoapRequest[] requests = new CoapRequest[3];

    private static CoapTestServer server;

    @Override
    public void setupComponents() throws Exception {

        server = new CoapTestServer(0);

        //Add 3 different webservices to server
        server.registerService(new NotObservableTestWebService("/service1", "Status of Webservice 1", 3000));
        server.registerService(new NotObservableTestWebService("/service2", "Status of Webservice 2", 3000));
        server.registerService(new NotObservableTestWebService("/service3", "Status of Webservice 3", 3000));

        //Create clients and requests
        for(int i = 0; i < 3; i++){
            clients[i] = new CoapTestClient();
            requests[i] =  new CoapRequest(MsgType.CON, Code.GET,
                    new URI("coap://localhost:" + server.getServerPort() + "/service" + (i+1)));
            requests[i].setResponseCallback(clients[i]);
        }
    }

    @Override
    public void shutdownComponents() throws Exception {
        for(CoapTestClient client : clients)
            client.shutdown();

        server.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.callback").setLevel(Level.DEBUG);
    }

    @Override
    public void createTestScenario() throws Exception {

        for(int i = 0; i < 3; i++){
            clients[i].writeCoapRequest(requests[i]);
        }

        //await responses
        Thread.sleep(6000);
    }



    @Test
    public void testAllClientsReceivedACKandCONResponse(){
        for(int i = 0; i < 3; i++){
            assertEquals("Client " + (i + 1) + " received wrong number of empty ACKs.",
                    1, clients[i].getEmptyAckNotificationTimes().size());

            long emptyAckTime = clients[i].getEmptyAckNotificationTimes().first();

            assertEquals("Client " + (i + 1) + " received wrong number of CON responses.",
                    1, clients[i].getReceivedResponses().size());

            long conResponseTime = clients[i].getReceivedResponses().firstKey();

            assertTrue("Client " + (i+1) + " received CON response was before empty ACK!",
                    conResponseTime > emptyAckTime);
        }

    }

    @Test
    public void testClientsReceivedCorrectResponses(){
        for (int i = 0; i < 3; i++){
            CoapResponse coapResponse = clients[i].getReceivedResponses()
                    .get(clients[i].getReceivedResponses().firstKey());

            assertEquals("Client " + (i+1) + " received wrong message type!",
                    MsgType.CON, coapResponse.getMessageType());

            assertEquals("Client " + (i+1) + " received wrong message content",
                    "Status of Webservice " + (i+1), coapResponse.getPayload().toString(Charset.forName("UTF-8")));
        }
    }
}
