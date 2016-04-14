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

import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.reliability.inbound.ServerInboundReliabilityHandler;
import de.uzl.itm.ncoap.endpoints.DummyEndpoint;
import de.uzl.itm.ncoap.endpoints.server.NotObservableTestWebresource;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static junit.framework.Assert.assertEquals;


/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 19.08.13
 * Time: 12:37
 * To change this template use File | Settings | File Templates.
 */
public class ClientSendsTheSameNONRequestTwice extends AbstractCoapCommunicationTest{

    private static final String PATH = "/path";

    private static CoapServer server;
    private InetSocketAddress serverSocket;
    private static DummyEndpoint endpoint;

    private static CoapRequest coapRequest1;
    private static CoapRequest coapRequest2;
    private static int messageID;
    private static Token token;

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServer();
        server.registerWebresource(new NotObservableTestWebresource(PATH, "Status 1", 0, 4000, server.getExecutor()));

        endpoint = new DummyEndpoint();

        serverSocket = new InetSocketAddress("localhost", server.getPort());

        URI serviceURI = new URI("coap", null, "localhost", server.getPort(), PATH, null, null);
        token = new Token(new byte[]{1, 2, 3, 4});
        messageID = 1;

        coapRequest1 = new CoapRequest(MessageType.NON, MessageCode.GET, serviceURI);
        coapRequest1.setMessageID(messageID);
        coapRequest1.setToken(token);

        coapRequest2 = new CoapRequest(MessageType.NON, MessageCode.GET, serviceURI);
        coapRequest2.setMessageID(messageID);
        coapRequest2.setToken(token);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown().get();
        endpoint.shutdown();
    }

    @Override
    public void createTestScenario() throws Exception {
        endpoint.writeMessage(coapRequest1, serverSocket);
        Thread.sleep(3000);

        endpoint.writeMessage(coapRequest2, serverSocket);
        Thread.sleep(2000);
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(DummyEndpoint.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(NotObservableTestWebresource.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ServerInboundReliabilityHandler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(AbstractCoapChannelHandler.class.getName()).setLevel(Level.DEBUG);

        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    @Test
    public void testMessagesForEquality() {
        assertEquals(coapRequest1, coapRequest2);
        assertEquals(coapRequest1.hashCode(), coapRequest2.hashCode());
    }

    @Test
    public void testEndpointReceivedOnlyOneMessages() {
        assertEquals(1, endpoint.getReceivedCoapMessages().size());
    }


    @Test
    public void testFirstMessage() {
        CoapMessage message2 = endpoint.getReceivedMessage(0);

        assertEquals("Third message has wrong message code!", MessageCode.CONTENT_205, message2.getMessageCode());
    }
}
