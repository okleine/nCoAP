package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.client.TestCoapReponseProcessor;
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
import java.util.SortedSet;
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

    private static final int NUMBER_OF_PARALLEL_REQUESTS = 500;

    private static TestCoapReponseProcessor[] responseProcessors =
            new TestCoapReponseProcessor[NUMBER_OF_PARALLEL_REQUESTS];

    private static CoapRequest[] requests = new CoapRequest[NUMBER_OF_PARALLEL_REQUESTS];

    private static CoapTestServer server;

    private static long startTime;

    @Override
    public void setupComponents() throws Exception {

        server = new CoapTestServer(NUMBER_OF_PARALLEL_REQUESTS, 0);

        //Add different webservices to server
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            server.registerService(new NotObservableTestWebService("/service" + (i+1), "Status of Webservice " + (i+1), 3000));
        }

        //Create client, callbacks and requests
        client = new CoapClientApplication();

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            responseProcessors[i] = new TestCoapReponseProcessor();
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
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.application").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.TestParallelRequests").setLevel(Level.DEBUG);
        //Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.INFO);
        //Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.callback").setLevel(Level.DEBUG);
    }

    @Override
    public void createTestScenario() throws Exception {

        startTime = System.currentTimeMillis();

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            client.writeCoapRequest(requests[i], responseProcessors[i]);
        }

        //await responses
        Thread.sleep(5000);
    }



    @Test
    public void testAllResponseProcessorsReceivedACKandCONResponse(){
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            assertEquals("ResponseProcessor " + (i + 1) + " received wrong number of empty ACKs.",
                    1, responseProcessors[i].getEmptyAcknowledgements().size());

            long emptyAckTime = responseProcessors[i].getEmptyAcknowledgements().firstKey();

            assertEquals("ResponseProcessor " + (i + 1) + " received wrong number of CON responses.",
                    1, responseProcessors[i].getCoapResponses().size());

            long conResponseTime = responseProcessors[i].getCoapResponses().firstKey();

            assertTrue("Response Processor " + (i+1) + " received CON response before empty ACK!",
                    conResponseTime > emptyAckTime);
        }

    }

    @Test
    public void testClientsReceivedCorrectResponses(){
        for (int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++){
            CoapResponse coapResponse = responseProcessors[i].getCoapResponses()
                    .get(responseProcessors[i].getCoapResponses().firstKey());

            assertEquals("Response Processor " + (i+1) + " received wrong message type!",
                    MsgType.CON, coapResponse.getMessageType());

            assertEquals("Response Processor " + (i+1) + " received wrong message content",
                    "Status of Webservice " + (i+1), coapResponse.getPayload().toString(Charset.forName("UTF-8")));
        }
    }

    @Test
    public void serverReceivedAllRequests(){

        SortedSet<Integer> usedMessageIDs = new TreeSet<Integer>();
        for(int i = 1; i <= NUMBER_OF_PARALLEL_REQUESTS; i++){
            usedMessageIDs.add(i);
        }

        for(int messageID : server.getRequestReceptionTimes().values()){
            usedMessageIDs.remove(messageID);
        }

        for(int messageID : usedMessageIDs){
            log.info("Missing message ID: " + messageID);
        }

        assertEquals("Server did not receive all requests.",
                NUMBER_OF_PARALLEL_REQUESTS, server.getRequestReceptionTimes().size());
    }
}
