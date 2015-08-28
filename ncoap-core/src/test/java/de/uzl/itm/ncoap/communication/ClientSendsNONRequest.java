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

package de.uzl.itm.ncoap.communication;

import de.uzl.itm.ncoap.application.server.CoapServerApplication;
import de.uzl.itm.ncoap.endpoints.DummyEndpoint;
import de.uzl.itm.ncoap.endpoints.server.NotObservableTestWebresource;
import de.uzl.itm.ncoap.message.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.SortedMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
* Tests to verify the server functionality related to piggy-backed responses.
*
* @author Stefan Hueske, Oliver Kleine
*/
public class ClientSendsNONRequest extends AbstractCoapCommunicationTest {

    private static CoapServerApplication server;
    private static NotObservableTestWebresource service;
    private static String PATH_TO_SERVICE = "/could/be/any/path";
    private static String PAYLOAD = "some arbitrary payload";

    private static DummyEndpoint endpoint;
    private static CoapRequest request;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("DummyEndpoint")
                .setLevel(Level.INFO);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication(0);
        service = new NotObservableTestWebresource(PATH_TO_SERVICE, PAYLOAD, 0, 0, server.getExecutor());
        server.registerService(service);

        endpoint = new DummyEndpoint();
        URI targetUri = new URI("coap://localhost:" + server.getPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MessageType.Name.NON, MessageCode.Name.GET, targetUri);
        request.setMessageID(54321);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        endpoint.shutdown();
    }

    @Override
    public void createTestScenario() throws Exception {

//             testEndpoint                    Server      DESCRIPTION
//                  |                             |
//              (1) |--------GET (NON)----------->|        send GET-Request to server
//                  |                             |
//              (2) |<-------NON-RESPONSE---------|        server responds with NON response
//                  |                             |
//                  |                             |

        //send request to testServer
        endpoint.writeMessage(request, new InetSocketAddress("localhost", server.getPort()));

        //wait some time for response from server
        Thread.sleep(150);
    }

    @Test
    public void testReceiverReceivedOnlyOneMessage() {
        String message = "Receiver received unexpected number of messages.";
        assertEquals(message, 1, endpoint.getReceivedCoapMessages().values().size());
    }

    @Test
    public void testReceiverReceivedResponse() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Received message is not a CoapResponse";
        assertTrue(message, receivedMessage instanceof CoapResponse);
    }

    @Test
    public void testReceivedMessageHasSameToken() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response token does not match with request token";
        assertEquals(message, request.getToken(), receivedMessage.getToken());
    }

    @Test
    public void testReceivedMessageHasCodeContent() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response code is not CONTENT 205";
        assertEquals(message, MessageCode.Name.CONTENT_205, receivedMessage.getMessageCodeName());
    }

    @Test
    public void testReceivedMessageHasUnmodifiedPayload() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response payload was modified by testServer";
        assertEquals(message, PAYLOAD, receivedMessage.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testReceivedMessageHasMsgTypeNON() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedCoapMessages();
        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
        String message = "Response Msg EventType is not NON";
        assertEquals(message, MessageType.Name.NON, receivedMessage.getMessageTypeName());
    }
}
