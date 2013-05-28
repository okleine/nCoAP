package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;

import java.util.*;

import static org.junit.Assert.fail;

/**
 * A CoapServerApplication for testing purposes.
 * To add a prepared response use the addResponse() method.
 * In addition a path (or multiple) must be registered on which the resource (response) will be available.
 * (Use the registerDummyService() method.)
 * Received requests can be retrieved using getReceivedRequests().
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public class CoapTestServer extends CoapServerApplication {

    //if "false" incoming messages are ignored and thrown away
    private  SortedMap<Long, CoapRequest> receivedRequests = new TreeMap<Long, CoapRequest>();

    //if "false" responses are not actually sent but stored in an outgoing queue
    private List<CoapResponse> responsesToSend = new LinkedList<CoapResponse>();


    public CoapTestServer(int serverPort){
        super(serverPort);
    }


    @Override
    public void handleRetransmissionTimout() {}

    public SortedMap<Long, CoapRequest> getReceivedRequests() {
        return receivedRequests;
    }

    public static void main(String[] args){
        AbstractCoapCommunicationTest.initializeLogging();
        CoapTestServer server = new CoapTestServer(5683);

        server.registerService(new ObservableDummyWebService("/obs", true, 0, 2000));

    }
}
