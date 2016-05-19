/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;

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

        // register resources /res1, /res2, and /res3 and send a request to /.well-known/core after each registration
        for (int i = 0; i < 3; i++) {
            server.registerWebresource(webresources[i]);
            Thread.sleep(500);

            CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, wkcUri);
            client.sendCoapRequest(coapRequest, serverSocket, wkcCallbacks[i]);
            Thread.sleep(500);
        }

        // add link param to /res3 and send a request to to /.well-known/core
        {
            webresources[2].setLinkParam(LinkParam.createLinkParam(LinkParam.Key.CT, "30"));
            Thread.sleep(500);

            CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, wkcUri);
            client.sendCoapRequest(coapRequest, serverSocket, wkcCallbacks[3]);
            Thread.sleep(500);
        }

        // shutdown resources /res1, /res2, and /res3 and send a request to /.well-known/core after each shutdown
        for (int i = 0; i < 3; i++) {
            server.shutdownWebresource(webresources[i].getUriPath());
            Thread.sleep(500);

            CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, wkcUri);
            client.sendCoapRequest(coapRequest, serverSocket, wkcCallbacks[i+4]);
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
    }

    @Test
    public void testFirstResponseContainsTwoUriReferences() {
        assertEquals("Wrong number of resources", 2, wkcCallbacks[0].getNumberOfResources());
    }

    @Test
    public void testSecondResponseContainsThreeUriReferences() {
        assertEquals("Wrong number of resources", 3, wkcCallbacks[1].getNumberOfResources());
    }

    @Test
    public void testThirdResponseContainsFourUriReferences() {
        assertEquals("Wrong number of resources", 4, wkcCallbacks[2].getNumberOfResources());
    }

    @Test
    public void testThirdResponseContainsObsParamForRes3() {
        Collection<LinkParam> linkParams = wkcCallbacks[2].getLinkValueList().getLinkParams("/res3");
        boolean contained = false;
        for (LinkParam linkParam : linkParams) {
            if (linkParam.getKey() == LinkParam.Key.OBS) {
                contained = true;
                break;
            }
        }
        assertTrue("No link param 'obs' for /res3", contained);
    }

    @Test
    public void testThirdResponseDoesNotContainCtParamForRes3() {
        Collection<LinkParam> linkParams = wkcCallbacks[2].getLinkValueList().getLinkParams("/res3");
        boolean contained = false;
        for (LinkParam linkParam : linkParams) {
            if (linkParam.getKey() == LinkParam.Key.CT) {
                contained = true;
                break;
            }
        }
        assertFalse("Unexpected link param 'ct' for /res3", contained);
    }


    @Test
    public void testFourthResponseContainsFourUriReferences() {
        assertEquals("Wrong number of resources", 4, wkcCallbacks[3].getNumberOfResources());
    }


    @Test
    public void testFourthResponseContainsCt30ParamForRes3() {
        Collection<LinkParam> linkParams = wkcCallbacks[3].getLinkValueList().getLinkParams("/res3");
        boolean contained = false;
        for (LinkParam linkParam : linkParams) {
            if (linkParam.getKey() == LinkParam.Key.CT && "30".equals(linkParam.getValue())) {
                contained = true;
                break;
            }
        }
        assertTrue("No link param 'ct=\"30\"' for /res3", contained);
    }


    @Test
    public void testFifthResponseContainsThreeUriReferences() {
        assertEquals("Wrong number of resources", 3, wkcCallbacks[4].getNumberOfResources());
    }

    @Test
    public void testSixthResponseContainsTwoUriReferences() {
        assertEquals("Wrong number of resources", 2, wkcCallbacks[5].getNumberOfResources());
    }

    @Test
    public void testSeventhResponseContainsOneUriReferences() {
        assertEquals("Wrong number of resources", 1, wkcCallbacks[6].getNumberOfResources());
    }

    private class WkcCallback extends ClientCallback {

        private LinkValueList linkValueList2;
        //private int numberOfResources = 0;

        @Override
        public void processCoapResponse(CoapResponse coapResponse) {
            LinkValueList linkValueList = LinkValueList.decode(
                    new String(coapResponse.getContentAsByteArray(), CoapMessage.CHARSET)
            );
            LOG.debug("Received: " + linkValueList);
            this.linkValueList2 = linkValueList;
            //this.numberOfResources = linkValueList.getUriReferences().size();
        }

        public int getNumberOfResources() {
            if (this.linkValueList2 == null) {
                return 0;
            } else {
                return this.linkValueList2.getUriReferences().size();
            }
        }

        public LinkValueList getLinkValueList() {
            return this.linkValueList2;
        }
    }
}
