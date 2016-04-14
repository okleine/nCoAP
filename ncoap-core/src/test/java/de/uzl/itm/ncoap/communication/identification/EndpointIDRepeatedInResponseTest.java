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
package de.uzl.itm.ncoap.communication.identification;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by olli on 01.09.15.
 */
public class EndpointIDRepeatedInResponseTest extends AbstractCoapCommunicationTest{

    private static Logger log = Logger.getLogger(EndpointIDRepeatedInResponseTest.class.getName());

    private static CoapClient client;
    private static CoapServer server;
    private static CoapRequest coapRequest;
    private static CoapResponse coapResponse;

    @Override
    public void setupComponents() throws Exception {
        client = new CoapClient();
        server = new CoapServer();

        URI targetUri = new URI("coap://localhost:" + server.getPort() + "/.well-known/core");
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
        coapRequest.setEndpointID1();
    }

    @Override
    public void createTestScenario() throws Exception {
        client.sendCoapRequest(coapRequest, new InetSocketAddress("localhost", server.getPort()), new ClientCallback() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                log.info("Request: " + coapRequest);
                log.info("Response: " + coapResponse);
                EndpointIDRepeatedInResponseTest.coapResponse = coapResponse;
            }
        });

        Thread.sleep(1000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown().get();
        client.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(EndpointIDRepeatedInResponseTest.class.getName())
                .setLevel(Level.INFO);

        Logger.getLogger("de.uzl.itm.ncoap.communication.identification")
                .setLevel(Level.DEBUG);

    }

    @Test
    public void testEndpointIDsMatch() {
        assertArrayEquals("IDs do not match", coapRequest.getEndpointID1(), coapResponse.getEndpointID2());
    }
}
