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
package de.uzl.itm.ncoap.communication.observe;

import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.endpoints.DummyEndpoint;
import de.uzl.itm.ncoap.endpoints.server.ObservableTestWebresource;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;

import static junit.framework.Assert.assertEquals;

/**
* Tests for the removal of observers.
*
* @author Oliver Kleine, Stefan Hüske
*/
public class ObservationTerminationTest extends AbstractCoapCommunicationTest {

    private static String PATH_TO_SERVICE = "/observable";

    private static CoapServer server;
    private static InetSocketAddress serverSocket;
    private static ObservableTestWebresource service;

    private static DummyEndpoint clientEndpoint;


    //requests
    private static CoapRequest request1;
    private static CoapRequest request2;
    private static CoapRequest request3;

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uzl.itm.ncoap.endpoints.DummyEndpoint")
                .setLevel(Level.DEBUG);

        Logger.getLogger("de.uzl.itm.ncoap.endpoints.server.ObservableTestWebresource")
                .setLevel(Level.DEBUG);

        Logger.getLogger("de.uzl.itm.ncoap.communication.observing.ServerObservationHandler")
                .setLevel(Level.DEBUG);

        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServer();
        serverSocket = new InetSocketAddress("localhost", server.getPort());

        service = new ObservableTestWebresource(PATH_TO_SERVICE, 1, 0, server.getExecutor());
        server.registerWebresource(service);

        clientEndpoint = new DummyEndpoint();

        URI targetURI = new URI("coap://localhost:" + server.getPort() + PATH_TO_SERVICE);

        request1 = new CoapRequest(MessageType.CON, MessageCode.GET, targetURI);
        request1.setMessageID(1);
        request1.setToken(new Token(new byte[]{1,2,3,4}));
        request1.setObserve(0);

        request2 = new CoapRequest(MessageType.CON, MessageCode.GET, targetURI);
        request2.setMessageID(2);
        request2.setToken(new Token(new byte[]{1,2,3,4}));
        request2.setObserve(1);

        request3 = new CoapRequest(MessageType.CON, MessageCode.GET, targetURI);
        request3.setMessageID(3);
        request3.setToken(new Token(new byte[]{2,3,4,5,6}));
        request3.setObserve(0);
    }


    @Override
    public void createTestScenario() throws Exception {

//             testEndpoint                    Server        DESCRIPTION
//                  |                             |
//                  |---- GET OBS: 0 ------------>|          Register observer
//                  |                             |
//                  |<------------ ACK OBS: 1 ----|          Receive first notification (status: 1)
//                  |                             |
//                  |                             |  <-----  Status update (new status: 2) (after 1000 ms)
//                  |                             |
//                  |<-------- CON 205 OBS: 2 ----|
//                  |                             |
//                  |---- ACK ------------------->|
//                  |                             |
//                  |---- GET OBS: 1------------->|          Cancel observation (with GET and OBS set to 1)
//                  |                             |
//                  |<------------------- ACK ----|          Receive ACK response (without observing option)
//                  |                             |
//                  |                             |  <-----  Status update (new status: 3) (after 2000 ms)
//                  |                             |
//                  |                             |          some time passes... nothing should happen!
//                  |                             |
//                  |                             |  <-----  Status update (new status: 4) (after 2000 ms)
//                  |                             |
//                  |---- GET OBS: 0 ------------>|          Register observer
//                  |                             |
//                  |<------------ ACK OBS: 1 ----|          Receive first notification (ACK)
//                  |                             |
//                  |                             |  <-----  Status update (new status: 5) (after 4000 ms)
//                  |                             |
//                  |<-------- CON 205 OBS: 2 ----|
//                  |                             |
//                  |--------RST----------------->|          Respond with reset to the 2nd notification
//                  |                             |
//                  |                             |  <-----  Status update (new status: 6) (after 4000 ms)
//                  |                             |
//                  |                             |          some time passes... nothing should happen!
//                  |                             |

        //schedule first observation request
        clientEndpoint.writeMessage(request1, serverSocket);

        //Wait for ACK
        Thread.sleep(3000);

        service.setResourceStatus(2, 0);
        Thread.sleep(50);

        //There should be a CON update notification to be acknowledged
        CoapResponse coapResponse = (CoapResponse) clientEndpoint.getReceivedCoapMessages().get(
                clientEndpoint.getReceivedCoapMessages().lastKey()
        );
        CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(coapResponse.getMessageID());
        clientEndpoint.writeMessage(emptyACK, serverSocket);


        //Wait for three seconds and then actively cancel the observation
        Thread.sleep(3000);
        clientEndpoint.writeMessage(request2, serverSocket);

        //Wait another second to let the server process the observation cancelation
        Thread.sleep(1000);
        service.setResourceStatus(3, 0);

        //Wait 5 seconds (no message should be sent)
        Thread.sleep(5000);
        service.setResourceStatus(4, 0);
        Thread.sleep(5000);

        //Re-register as observer and wait 1 second
        clientEndpoint.writeMessage(request3, serverSocket);

        //Wait 3 seconds and update status
        Thread.sleep(3000);
        service.setResourceStatus(5, 0);
        Thread.sleep(50);

        //There should be a CON update notification to be acknowledged
        coapResponse = (CoapResponse) clientEndpoint.getReceivedCoapMessages().get(
                clientEndpoint.getReceivedCoapMessages().lastKey()
        );
        CoapMessage emptyRST = CoapMessage.createEmptyReset(coapResponse.getMessageID());
        clientEndpoint.writeMessage(emptyRST, serverSocket);

        //Wait 2 seconds then update status (no more message should be sent...)
        Thread.sleep(2000);
        service.setResourceStatus(6, 0);
        Thread.sleep(5000);

        server.shutdown();
        Thread.sleep(2000);
    }


    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown().get();
        clientEndpoint.shutdown();
    }


    @Test
    public void testReceiverReceived5Messages() {
        String message = "Receiver did not receive 5 messages";
        assertEquals(message, 5, clientEndpoint.getReceivedCoapMessages().values().size());
    }

    @Test
    public void testFirstReceivedMessage() {
        CoapMessage receivedMessage = clientEndpoint.getReceivedMessage(0);

        String message = "Wrong message type!";
        assertEquals(message, MessageType.ACK, receivedMessage.getMessageType());

        message = "Wrong message code!";
        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());

        message = "Wrong payload!";
        assertEquals(message, "Status #1",receivedMessage.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testSecondReceivedMessage() {
        CoapMessage receivedMessage = clientEndpoint.getReceivedMessage(1);

        String message = "Wrong message type!";
        assertEquals(message, MessageType.CON , receivedMessage.getMessageType());

        message = "Wrong message code!";
        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());

        message = "Wrong payload!";
        assertEquals(message, "Status #2", receivedMessage.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testThirdReceivedMessage() {
        CoapMessage receivedMessage = clientEndpoint.getReceivedMessage(2);

        String message = "Wrong message type!";
        assertEquals(message, MessageType.ACK , receivedMessage.getMessageType());

        message = "Wrong message code!";
        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());

        message = "Wrong payload!";
        assertEquals(message, "Status #2", receivedMessage.getContent().toString(Charset.forName("UTF-8")));

        message = "There should be no observation sequence number!";
        Assert.assertEquals(message, new Long(UintOptionValue.UNDEFINED), new Long(receivedMessage.getObserve()));
    }

    @Test
    public void testFourthReceivedMessage() {
        CoapMessage receivedMessage = clientEndpoint.getReceivedMessage(3);

        String message = "Wrong message type!";
        assertEquals(message, MessageType.ACK , receivedMessage.getMessageType());

        message = "Wrong message code!";
        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());

        message = "Wrong payload!";
        assertEquals(message, "Status #4", receivedMessage.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testFifthReceivedMessage() {
        CoapMessage receivedMessage = clientEndpoint.getReceivedMessage(4);

        String message = "Wrong message type!";
        assertEquals(message, MessageType.CON, receivedMessage.getMessageType());

        message = "Wrong message code!";
        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());

        message = "Wrong payload!";
        assertEquals(message, "Status #5", receivedMessage.getContent().toString(Charset.forName("UTF-8")));
    }
}
