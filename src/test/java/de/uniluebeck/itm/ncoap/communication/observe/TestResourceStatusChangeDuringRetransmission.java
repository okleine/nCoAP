package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.applicationcomponents.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.applicationcomponents.server.webservice.ObservableTestWebservice;
import de.uniluebeck.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Created by olli on 18.03.14.
 */
public class TestResourceStatusChangeDuringRetransmission extends AbstractCoapCommunicationTest{

    private static CoapServerApplication coapServerApplication;
    private static CoapTestEndpoint client;

    private static CoapRequest coapRequest;


    @Override
    public void setupComponents() throws Exception {
        coapServerApplication = new CoapServerApplication();
        coapServerApplication.registerService(new ObservableTestWebservice("/observable", 0, 0, 5000));

        client = new CoapTestEndpoint();

        URI targetUri = new URI("coap", null, "localhost", 5683, "/observable", null, null);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        coapRequest.setObserve();

    }

    @Override
    public void createTestScenario() throws Exception {
        client.writeMessage(coapRequest, new InetSocketAddress("localhost", 5683));

        Thread.sleep(70000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapServerApplication.shutdown();
        client.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.outgoing").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe").setLevel(Level.DEBUG);
    }

    @Test
    public void test(){

    }
}
