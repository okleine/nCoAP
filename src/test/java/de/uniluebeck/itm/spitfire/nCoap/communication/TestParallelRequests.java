package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapTestClient;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.NotObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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

    private static Map<CoapTestClient, CoapRequest> clients = new HashMap<CoapTestClient, CoapRequest>();
    private static CoapTestServer server;

    @Override
    public void setupComponents() throws Exception {

        server = new CoapTestServer(0);


        clients.put(new CoapTestClient(),
                    new CoapRequest(MsgType.CON, Code.GET,
                            new URI("coap://localhost:" + testServer.getServerPort() + "/service1"));
        request1.setResponseCallback(clients[0]);
        clients[0].writeCoapRequest(request1);


        clients[1] = new CoapTestClient();
        CoapRequest request2 = new CoapRequest(MsgType.CON, Code.GET,
                new URI("coap://localhost:" + testServer.getServerPort() + "/service2"));
        request2.setResponseCallback(clients[1]);
        clients[1].writeCoapRequest(request2);

        clients[2] = new CoapTestClient();
        CoapRequest request3 = new CoapRequest(MsgType.CON, Code.GET,
                new URI("coap://localhost:" + testServer.getServerPort() + "/service3"));
        request3.setResponseCallback(clients[2]);
        clients[2].writeCoapRequest(request3);

        //Add 3 different webservices to server
        NotObservableTestWebService webService1 =
                new NotObservableTestWebService("/service1", "Status of Webservice 1", 3000);
        testServer.registerService(webService1);

        NotObservableTestWebService webService2 =
                new NotObservableTestWebService("/service2", "Status of Webservice 2", 4000);
        testServer.registerService(webService2);

        NotObservableTestWebService webService3 =
                new NotObservableTestWebService("/service3", "Status of Webservice 3", 5000);
        testServer.registerService(webService3);
    }

    @Override
    public void shutdownComponents() throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.core.callback").setLevel(Level.DEBUG);
    }

    @Override
    public void createTestScenario() throws Exception {



        //Create 3 client applications and write seperate requests



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
