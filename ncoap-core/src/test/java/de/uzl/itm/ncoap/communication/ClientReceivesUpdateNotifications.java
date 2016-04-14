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
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.communication.observing.ServerObservationHandler;
import de.uzl.itm.ncoap.communication.reliability.inbound.ClientInboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.outbound.ServerOutboundReliabilityHandler;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.endpoints.server.ObservableTestWebresource;
import de.uzl.itm.ncoap.application.server.CoapServer;
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
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;

/**
* Tests if a client receives notifications.
* @author Stefan Hueske, Oliver Kleine
*/
public class ClientReceivesUpdateNotifications extends AbstractCoapCommunicationTest{

    private static final String PATH_TO_SERVICE = "/observable";

    private static CoapClient client;
    private static TestCallback clientCallback;

    private static CoapServer server;
    private static ObservableTestWebresource service;

    //observable request
    private static CoapRequest request;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(ServerObservationHandler.class.getName())
              .setLevel(Level.DEBUG);

        Logger.getLogger(ObservableWebresource.class.getName())
              .setLevel(Level.DEBUG);

        Logger.getLogger(ServerObservationHandler.class.getName())
              .setLevel(Level.DEBUG);

        Logger.getLogger(ClientInboundReliabilityHandler.class.getName())
                .setLevel(Level.DEBUG);

        Logger.getLogger(ServerOutboundReliabilityHandler.class.getName())
                .setLevel(Level.DEBUG);

        Logger.getLogger(SpecificClientCallback.class.getName())
                .setLevel(Level.DEBUG);

        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServer();

        service = new ObservableTestWebresource(PATH_TO_SERVICE, 1, 0, server.getExecutor());
        server.registerWebresource(service);

        client = new CoapClient();
        clientCallback = new SpecificClientCallback();

        URI targetUri = new URI("coap://localhost:" + server.getPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
        request.setObserve(0);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown().get();
        client.shutdown();
    }


    @Override
    public void createTestScenario() throws Exception {

//               Client                        Server
//              (1) |------GET-OBSERVE----------->|           send observable request to server
//                  |                             |
//              (2) |<-----ACK-NOTIFICATION-------|           server responds with initial, piggy-backed notification
//                  |                             |
//                  |                             |  <------  status update (new status: 2)
//                  |                             |
//              (3) |<-----CON-NOTIFICATION-------|           server sends 2nd notification,
//                  |                             |
//              (4) |------EMPTY-ACK------------->|
//                  |                             |
//                  |                             |  <------- shutdown server
//                  |                             |
//              (5) |<-----CON-NOTIFICATION-------|           server sends 3rd notification (404 not found)



        //write request
        client.sendCoapRequest(request, new InetSocketAddress("localhost", server.getPort()), clientCallback);

        Thread.sleep(5000);
        service.setResourceStatus(2, 10);

        Thread.sleep(5000);


        server.shutdown();
        Thread.sleep(5000);
    }

    @Test
    public void testClientReceived3Messages() {
        String message = "Receiver did not receive 3 messages";
        assertEquals(message, 3, clientCallback.getCoapResponses().size());
    }

    @Test
    public void testFirstMessage() {
        Iterator<Long> receptionTimes = clientCallback.getCoapResponses().keySet().iterator();

        CoapResponse response = clientCallback.getCoapResponses().get(receptionTimes.next());

        assertEquals("Message type is not ACK", MessageType.ACK, response.getMessageType());

        assertEquals("Content does not match.", "Status #1",
                response.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testSecondMessage() {
        Iterator<Long> receptionTimes = clientCallback.getCoapResponses().keySet().iterator();
        receptionTimes.next();

        CoapResponse response = clientCallback.getCoapResponses().get(receptionTimes.next());

        assertEquals("Wrong Message Code!", MessageCode.CONTENT_205, response.getMessageCode());

        assertEquals("Content does not match.", "Status #2",
                response.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testThirdMessage() {
        Iterator<Long> receptionTimes = clientCallback.getCoapResponses().keySet().iterator();
        receptionTimes.next();
        receptionTimes.next();

        CoapResponse response = clientCallback.getCoapResponses().get(receptionTimes.next());

        assertEquals("MessageCode is not 404", MessageCode.NOT_FOUND_404, response.getMessageCode());
    }


    private class SpecificClientCallback extends TestCallback {

        @Override
        public boolean continueObservation() {
            return true;
        }
    }

}
