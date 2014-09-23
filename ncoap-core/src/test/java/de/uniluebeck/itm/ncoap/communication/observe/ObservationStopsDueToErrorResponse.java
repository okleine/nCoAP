package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.ncoap.endpoints.client.CoapClientTestCallback;
import de.uniluebeck.itm.ncoap.endpoints.server.ObservableTestWebservice;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by olli on 23.09.14.
 */
public class ObservationStopsDueToErrorResponse extends AbstractCoapCommunicationTest{

    private static CoapClientApplication client;
    private static CoapClientTestCallback clientCallback;

    private static CoapServerApplication server;
    private static InetSocketAddress serverSocket;

    private static ObservableTestWebservice service;

    private static URI serviceUri;

    @Override
    public void setupComponents() throws Exception {
        client = new CoapClientApplication();
        clientCallback = new CoapClientTestCallback();
        server = new CoapServerApplication();
        serverSocket = new InetSocketAddress("localhost", server.getPort());

//        service = new ObservableTestWebservice("/obs", 0, 0);
//        server.registerService(service);
        serviceUri = new URI("coap", null, "localhost", -1, "/does/not/exist", null, null);
    }

    @Override
    public void createTestScenario() throws Exception {
        CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, serviceUri);
        coapRequest.setObserve(true);

        client.sendCoapRequest(coapRequest, clientCallback, serverSocket);
        Thread.sleep(10000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        server.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.endpoints.client").setLevel(Level.INFO);
    }

    @Test
    public void test(){

    }
}
