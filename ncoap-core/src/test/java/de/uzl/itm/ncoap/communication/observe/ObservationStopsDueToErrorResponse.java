/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
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

package de.uzl.itm.ncoap.communication.observe;

import de.uzl.itm.ncoap.application.client.CoapClientApplication;
import de.uzl.itm.ncoap.application.server.CoapServerApplication;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.endpoints.client.ClientTestCallback;
import de.uzl.itm.ncoap.endpoints.server.ObservableTestWebresource;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;


/**
* Created by olli on 23.09.14.
*/
public class ObservationStopsDueToErrorResponse extends AbstractCoapCommunicationTest {

    private static CoapClientApplication client;
    private static ClientTestCallback clientCallback;

    private static CoapServerApplication server;
    private static InetSocketAddress serverSocket;

    private static ObservableTestWebresource service;

    private static URI serviceUri;

    @Override
    public void setupComponents() throws Exception {
        client = new CoapClientApplication();
        clientCallback = new ClientTestCallback();
        server = new CoapServerApplication();
        service = new ObservableTestWebresource("/test", 1, 0, server.getExecutor());
        server.registerService(service);

        serverSocket = new InetSocketAddress("localhost", server.getPort());
        serviceUri = new URI("coap", null, "localhost", -1, "/test", null, null);
    }

    @Override
    public void createTestScenario() throws Exception {
        CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, serviceUri);
        coapRequest.setObserve(0);
        coapRequest.setAccept(111);
        client.sendCoapRequest(coapRequest, clientCallback, serverSocket);
        Thread.sleep(2000);
        service.setResourceStatus(2, 0);
        Thread.sleep(2000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        server.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.endpoints.client")
                .setLevel(Level.INFO);
        Logger.getLogger("ClientObservationHandler")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.server.webresource.ObservableWebservice")
                .setLevel(Level.INFO);
        Logger.getLogger("WebserviceManager")
                .setLevel(Level.INFO);
    }

    @Test
    public void test(){

    }
}
