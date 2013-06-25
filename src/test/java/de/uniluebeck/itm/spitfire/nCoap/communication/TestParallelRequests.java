package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.client.TestCoapResponseProcessor;
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
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

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

    private static CoapClientApplication client;

    private static final int NUMBER_OF_PARALLEL_REQUESTS = 1000;

    private static TestCoapResponseProcessor[] responseProcessors =
            new TestCoapResponseProcessor[NUMBER_OF_PARALLEL_REQUESTS];

    private static CoapRequest[] requests = new CoapRequest[NUMBER_OF_PARALLEL_REQUESTS];

    private static CoapTestServer server;

    @Override
    public void setupComponents() throws Exception {

        server = new CoapTestServer(0);

        //Add different webservices to server
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            server.registerService(new NotObservableTestWebService("/service" + (i+1),
                    "Status of Webservice " + (i+1), 0));
        }

        //Create client, callbacks and requests
        client = new CoapClientApplication();

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            responseProcessors[i] = new TestCoapResponseProcessor();
            requests[i] =  new CoapRequest(MsgType.CON, Code.GET,
                    new URI("coap://localhost:" + server.getServerPort() + "/service" + (i+1)));
        }
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        server.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.TestParallelRequests").setLevel(Level.DEBUG);
        //Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.application.client.TestCoapResponseProcessor").setLevel(Level.DEBUG);
    }

    @Override
    public void createTestScenario() throws Exception {

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            client.writeCoapRequest(requests[i], responseProcessors[i]);
        }

        //await responses
        Thread.sleep(10000);
    }

    @Test
    public void testClientsReceivedCorrectResponses(){
        for (int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            CoapResponse coapResponse = responseProcessors[i].getCoapResponses().get(0);

            assertEquals("Response Processor " + (i+1) + " received wrong message content",
                    "Status of Webservice " + (i+1), coapResponse.getPayload().toString(Charset.forName("UTF-8")));
        }
    }

    @Test
    public void serverReceivedAllRequests(){

        Set<Integer> usedMessageIDs = Collections.synchronizedSet(new TreeSet<Integer>());
        for(int i = 1; i <= NUMBER_OF_PARALLEL_REQUESTS; i++){
            usedMessageIDs.add(i);
        }

        for(int messageID : server.getRequestReceptionTimes().keySet()){
            usedMessageIDs.remove(messageID);
        }

        for(int messageID : usedMessageIDs){
            log.info("Missing message ID: " + messageID);
        }

        assertEquals("Server did not receive all requests.",
                NUMBER_OF_PARALLEL_REQUESTS, server.getRequestReceptionTimes().size());
    }
}
