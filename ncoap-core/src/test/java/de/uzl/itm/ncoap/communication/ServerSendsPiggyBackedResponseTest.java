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
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.reliability.inbound.ClientInboundReliabilityHandler;
import de.uzl.itm.ncoap.endpoints.DummyEndpoint;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.URI;
import java.util.SortedMap;

import static junit.framework.Assert.*;


/**
* Tests to verify the client functionality related to piggy-backed responses.
*
* @author Oliver Kleine, Stefan Hueske
*/
public class ServerSendsPiggyBackedResponseTest extends AbstractCoapCommunicationTest {

    private static final String PAYLOAD = "Some arbitrary content!";

    private static CoapClient client;
    private static TestCallback callback;
    private static CoapRequest coapRequest;

    private static DummyEndpoint endpoint;
    private static InetSocketAddress endpointSocket;

    @Override
    public void setupComponents() throws Exception {

        //Create endpoint
        endpoint = new DummyEndpoint(5683);
        endpointSocket = new InetSocketAddress("localhost", endpoint.getPort());

        //Create client and callback
        client = new CoapClient();
        callback = new TestCallback();

        URI targetUri =  new URI("coap", "localhost" + endpoint.getPort(), "/", null);
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        endpoint.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(TestCallback.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(DummyEndpoint.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ClientInboundReliabilityHandler.class.getName()).setLevel(Level.DEBUG);
    }

    @Override
    public void createTestScenario() throws Exception {

        /*
             testClient                    testEndpoint     DESCRIPTION
                  |                             |
              (1) |--------GET----------------->|           client sends GET-Request to testEndpoint
                  |                             |
              (2) |<-------ACK-RESPONSE---------|           testEndpoint responds
                  |                             |
              (3) |<-------ACK-RESPONSE---------|           testEndpoint sends the response again,
                  |                             |           nothing should happen here if the client
                  |                             |           removed the callback as expected
        */

        //write request
        client.sendCoapRequest(coapRequest, endpointSocket, callback);

        //Wait some time
        Thread.sleep(300);

        //Get message ID and token from received message
        int messageID = endpoint.getReceivedCoapMessages().values().iterator().next().getMessageID();
        Token token = endpoint.getReceivedCoapMessages().values().iterator().next().getToken();

        //write response #1
        CoapResponse response = new CoapResponse(MessageType.ACK, MessageCode.CONTENT_205);
        response.setMessageID(messageID);
        response.setToken(token);
        response.setContent(PAYLOAD.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        endpoint.writeMessage(response, new InetSocketAddress("localhost", client.getPort()));

        //Wait some time
        Thread.sleep(1000);

        //write response #2 (should be ignored by the client!)
        endpoint.writeMessage(response, new InetSocketAddress("localhost", client.getPort()));

        //Wait some time
        Thread.sleep(500);
    }


    @Test
    public void testReceivedRequestEqualsSentRequest() {
        SortedMap<Long, CoapMessage> receivedRequests = endpoint.getReceivedCoapMessages();
        String message = "Written and received request do not equal";
        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
    }

    @Test
    public void testReceiverReceivedOnlyOneRequest() {
        String message = "Receiver received more than one message";
        assertEquals(message, 1, endpoint.getReceivedCoapMessages().size());
    }

    @Test
    public void testClientCallbackInvokedOnce() {
        String message = "Client callback was invoked less or more than once";
        assertEquals(message, 1, callback.getCoapResponses().size());
    }
}
