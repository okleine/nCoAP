package de.uzl.itm.ncoap.linkformat;

import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.application.linkformat.LinkValueList;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.application.server.resource.Webresource;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.endpoints.server.NotObservableTestWebresource;
import de.uzl.itm.ncoap.endpoints.server.ObservableTestWebresource;
import de.uzl.itm.ncoap.message.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Created by olli on 09.05.16.
 */
public class WellKnownCoreResourceTests extends AbstractCoapCommunicationTest {

    private static Logger LOG = Logger.getLogger(WellKnownCoreResourceTests.class.getName());

    private static CoapServer server;
    private static Webresource[] webresources;
    private static CoapClient client;
    private static WkcCallback[] wkcCallbacks;
    private static URI wkcUri;

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServer();
        webresources = new Webresource[3];
        webresources[0] = new NotObservableTestWebresource("/res1", "1", 60, 0, server.getExecutor());
        webresources[1] = new NotObservableTestWebresource("/res2", "2", 60, 0, server.getExecutor());
        webresources[2] = new ObservableTestWebresource("/res3", 3, 60, 0, server.getExecutor());

        client = new CoapClient();
        wkcCallbacks = new WkcCallback[7];
        wkcCallbacks[0] = new WkcCallback();
        wkcCallbacks[1] = new WkcCallback();
        wkcCallbacks[2] = new WkcCallback();
        wkcCallbacks[3] = new WkcCallback();
        wkcCallbacks[4] = new WkcCallback();
        wkcCallbacks[5] = new WkcCallback();
        wkcCallbacks[6] = new WkcCallback();
        wkcUri = new URI("coap", "localhost", "/.well-known/core", null);
    }

    @Override
    public void createTestScenario() throws Exception {

        InetSocketAddress serverSocket = new InetSocketAddress("localhost", server.getPort());
        for (int i = 0; i < 3; i++) {
            server.registerWebresource(webresources[i]);
            Thread.sleep(500);

            CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, wkcUri);
            client.sendCoapRequest(coapRequest, serverSocket, wkcCallbacks[i]);
            Thread.sleep(500);
        }

        {
            webresources[2].setLinkParam(LinkParam.createLinkParam(LinkParam.Key.CT, "30"));
            Thread.sleep(500);

            CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, wkcUri);
            client.sendCoapRequest(coapRequest, serverSocket, wkcCallbacks[3]);
            Thread.sleep(500);
        }


        for (int i = 0; i < 3; i++) {
            server.shutdownWebresource(webresources[i].getUriPath());
            Thread.sleep(500);

            CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, wkcUri);
            client.sendCoapRequest(coapRequest, serverSocket, wkcCallbacks[i+2]);
            Thread.sleep(500);
        }

        Thread.sleep(1000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        client.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(WellKnownCoreResourceTests.class.getName()).setLevel(Level.DEBUG);
//        Logger.getLogger("de.uzl.itm.ncoap.communication").setLevel(Level.DEBUG);
//        Logger.getLogger("de.uzl.itm.ncoap.communication.codec").setLevel(Level.ERROR);
//        //Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    @Test
    public void testFirstResponseContainsTwoResources() {
        assertEquals("Wrong number of resources", 2, wkcCallbacks[0].getNumberOfResources());
    }

    @Test
    public void testSecondResponseContainsThreeResources() {
        assertEquals("Wrong number of resources", 3, wkcCallbacks[1].getNumberOfResources());
    }

    @Test
    public void testThirdResponseContainsFourResources() {
        assertEquals("Wrong number of resources", 4, wkcCallbacks[2].getNumberOfResources());
    }

    @Test
    public void testFourthResponseContainsThreeResources() {
        assertEquals("Wrong number of resources", 3, wkcCallbacks[3].getNumberOfResources());
    }

    @Test
    public void testFifthResponseContainsTwoResources() {
        assertEquals("Wrong number of resources", 2, wkcCallbacks[0].getNumberOfResources());
    }

    private class WkcCallback extends ClientCallback {

        private int numberOfResources = 0;

        @Override
        public void processCoapResponse(CoapResponse coapResponse) {
            LinkValueList linkValueList = LinkValueList.decode(
                    new String(coapResponse.getContentAsByteArray(), CoapMessage.CHARSET)
            );
            LOG.debug("Received: " + linkValueList);
            this.numberOfResources = linkValueList.getUriReferences().size();
        }

        public int getNumberOfResources() {
            return this.numberOfResources;
        }
    }
}
