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
package de.uzl.itm.ncoap.communication;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.endpoints.server.NotObservableTestWebresource;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 18.06.13
* Time: 14:58
* To change this template use File | Settings | File Templates.
*/
public class TestParallelRequests extends AbstractCoapCommunicationTest {

    private static CoapClient client;

    private static final int NUMBER_OF_PARALLEL_REQUESTS = 200;

    private static TestCallback[] clientCallbacks =
            new TestCallback[NUMBER_OF_PARALLEL_REQUESTS];

    private static CoapRequest[] requests = new CoapRequest[NUMBER_OF_PARALLEL_REQUESTS];

    private static CoapServer server;
    private static InetSocketAddress serverSocket;

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(TestCallback.class.getName()).setLevel(Level.DEBUG);

        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    @Override
    public void setupComponents() throws Exception {

        server = new CoapServer();
        serverSocket = new InetSocketAddress("localhost", server.getPort());

        //Add different webservices to server
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            server.registerWebresource(new NotObservableTestWebresource("/service" + (i + 1),
                    "This is the status of service " + (i + 1), 0, 0, server.getExecutor()));
        }

        //Create client, callbacks and requests
        client = new CoapClient();

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            clientCallbacks[i] = new TestCallback();
            requests[i] =  new CoapRequest(MessageType.CON, MessageCode.GET,
                    new URI("coap://localhost:" + server.getPort() + "/service" + (i+1)));
        }
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown().get();
        client.shutdown();
    }

   @Override
    public void createTestScenario() throws Exception {

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            client.sendCoapRequest(requests[i], serverSocket, clientCallbacks[i]);
        }

        //await responses (10 seconds should more than enough for 100 requests!)
        Thread.sleep(10000);
    }

    @Test
    public void testClientsReceivedCorrectResponses() {
        for (int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            CoapResponse coapResponse = clientCallbacks[i].getCoapResponses().values().iterator().next();

            assertEquals("Response Processor " + (i+1) + " received wrong message content",
                    "This is the status of service " + (i+1),
                    coapResponse.getContent().toString(Charset.forName("UTF-8"))
            );
        }
    }
}
