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

package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.endpoints.DummyEndpoint;
import de.uniluebeck.itm.ncoap.endpoints.server.NotObservableTestWebservice;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
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
public class ClientSendsTheSameRequestTwice extends AbstractCoapCommunicationTest{

    private static final String PATH = "/path";

    private static CoapServerApplication server;
    private InetSocketAddress serverSocket;
    private static DummyEndpoint endpoint;

    private static CoapRequest coapRequest1;
    private static CoapRequest coapRequest2;
    private static int messageID;
    private static Token token;

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication();
        server.registerService(new NotObservableTestWebservice(PATH, "Status 1", 0, 6000, server.getExecutor()));

        endpoint = new DummyEndpoint();

        serverSocket = new InetSocketAddress("localhost", server.getPort());

        URI serviceURI = new URI("coap", null, "localhost", server.getPort(), PATH, null, null);
        token = new Token(new byte[]{1, 2, 3, 4});
        messageID = 1;

        coapRequest1 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, serviceURI);
        coapRequest1.setMessageID(messageID);
        coapRequest1.setToken(token);

        coapRequest2 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, serviceURI);
        coapRequest2.setMessageID(messageID);
        coapRequest2.setToken(token);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        endpoint.shutdown();
    }

    @Override
    public void createTestScenario() throws Exception {
        endpoint.writeMessage(coapRequest1, serverSocket);
        Thread.sleep(3000);

        endpoint.writeMessage(coapRequest2, serverSocket);
        Thread.sleep(3500);

        CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(endpoint.getReceivedMessage(2).getMessageID());
        endpoint.writeMessage(emptyACK, serverSocket);

        Thread.sleep(2000);
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.endpoints")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability")
                .setLevel(Level.INFO);
    }

    @Test
    public void testMessagesForEquality(){
        assertEquals(coapRequest1, coapRequest2);
        assertEquals(coapRequest1.hashCode(), coapRequest2.hashCode());
    }

    @Test
    public void testEndpointReceivedThreeMessages(){
        assertEquals(3, endpoint.getReceivedCoapMessages().size());
    }

    @Test
    public void testFirstMessageisEmptyAck(){
        CoapMessage message1 = endpoint.getReceivedMessage(0);

        assertEquals("First message is not empty!", MessageCode.Name.EMPTY, message1.getMessageCodeName());
        assertEquals("First message has wrong ID!", messageID, message1.getMessageID());
        assertEquals("First message is no ACK!", MessageType.Name.ACK, message1.getMessageTypeName());
    }

    @Test
    public void testSecondMessageisEmptyAck(){
        CoapMessage message2 = endpoint.getReceivedMessage(1);

        assertEquals("Second message is not empty!", MessageCode.Name.EMPTY, message2.getMessageCodeName());
        assertEquals("Second message has wrong ID!", messageID, message2.getMessageID());
        assertEquals("Second message is no ACK!", MessageType.Name.ACK, message2.getMessageTypeName());
    }

    @Test
    public void testThirdMessage(){
        CoapMessage message3 = endpoint.getReceivedMessage(2);

        assertEquals("Third message has wrong message code!", MessageCode.Name.CONTENT_205,
                message3.getMessageCodeName());

        assertEquals("3rd message has wrong message type!", MessageType.Name.CON, message3.getMessageTypeName());
    }


}
