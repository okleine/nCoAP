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
package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.endpoints.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.endpoints.server.ObservableTestWebservice;
import de.uniluebeck.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.ncoap.message.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by olli on 18.03.14.
 */
public class ResourceStatusChangeDuringRetransmission extends AbstractCoapCommunicationTest{

    private static CoapServerApplication server;
    private static ObservableTestWebservice service;
    private static CoapTestEndpoint clientEndpoint;

    private static CoapRequest coapRequest;
    private static SortedMap<Long, CoapMessage> receivedMessages;

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication();
        service = new ObservableTestWebservice("/observable", 1, 0);
        server.registerService(service);

        clientEndpoint = new CoapTestEndpoint();

        URI targetUri = new URI("coap", null, "localhost", -1, "/observable", null, null);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        coapRequest.setObserve(true);
    }

    /**
     * Retransmission intervals (RC = Retransmission Counter):
     * immediately send CON message, set RC = 0
     * wait  2 - 3  sec then send retransmission, set RC = 1
     * wait  4 - 6  sec then send retransmission, set RC = 2
     * wait  8 - 12 sec then send retransmission, set RC = 3
     * wait 16 - 24 sec then send retransmission, set RC = 4
     * wait 32 - 48 sec then fail transmission
     *
     * @throws Exception
     */
    @Override
    public void createTestScenario() throws Exception {

//         testEndpoint                  Server
//              |                           |
//              |---- GET OBS(0) ---------->|
//              |                           |
//              |<----------- ACK OBS:1 ----|
//              |                           |
//              |                      new status (2) after ~ 5 sec.
//              |                           |
//              |<----------- CON OBS:2 ----|
//              |                           |
//              |<----------- CON OBS:2 ----| (retransmission 1) (after ~ 7-8 sec.)
//              |                           |
//              |<----------- CON OBS:2 ----| (retransmission 2) (after ~ 11-14 sec.)
//              |                           |
//              |                      new status (3) after ~ 16 sec.
//              |                           |
//              |<----------- CON OBS:3 ----| (retransmission 3) (after ~ 19-26 sec.)
//              |                           |
//              |<----------- CON OBS:3 ----| (retransmission 4) (after ~ 35-50 sec.)
//              |                           |
//              |                      server shutdown
//              |                           |
//              |<------- NON NOT_FOUND ----| (shutdown notification) (after ~51 sec.)

        clientEndpoint.writeMessage(coapRequest, new InetSocketAddress("localhost", 5683));

        //Wait 5 sec. then update status to "2"
        Thread.sleep(5000);
        service.setResourceStatus(2, 120);

        //Wait 11 sec. (i.e. 16 sec. in total) then update status to "3"
        Thread.sleep(11000);
        service.setResourceStatus(3, 120);

        //Wait 35 sec. (i.e. 51 sec. in total) then shut server down
        Thread.sleep(35000);

        receivedMessages = clientEndpoint.getReceivedCoapMessages();
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        clientEndpoint.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.outgoing")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe.server.WebserviceObservationHandler")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice")
                .setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.endpoints.CoapTestEndpoint")
                .setLevel(Level.INFO);
    }

    @Test
    public void testEndpointReceivedSevenMessages(){
        assertEquals("Client Endpoint received wrong number of messages!",
                7, clientEndpoint.getReceivedCoapMessages().size());
    }

    @Test
    public void testFirstMessage(){
        CoapMessage coapMessage = clientEndpoint.getReceivedMessage(0);
        assertEquals("Wrong message type!", MessageType.Name.ACK, coapMessage.getMessageTypeName());
    }

    @Test
    public void testAscendingSequenceNumbers(){
        for(int i = 1; i < 6; i++){
            CoapResponse previous = (CoapResponse) clientEndpoint.getReceivedMessage(i-1);
            CoapResponse actual = (CoapResponse) clientEndpoint.getReceivedMessage(i);

            String message = String.format(
                    "OBS value (%d) of notification #%d is not larger than OBS value (%d) of  notification #%d!",
                    actual.getObservationSequenceNumber(), (i+1), previous.getObservationSequenceNumber(), i
            );

            assertTrue(message, actual.getObservationSequenceNumber() > previous.getObservationSequenceNumber());

        }
    }

    @Test
    public void testResponseContent(){
        for(int i = 0; i < 6; i++){
            CoapResponse response = (CoapResponse) clientEndpoint.getReceivedMessage(i);
            String content = response.getContent().toString(CoapMessage.CHARSET);

            String expected;
            if(i == 0)
                expected = "Status #1";
            else if(i > 0 && i < 4)
                expected = "Status #2";
            else
                expected = "Status #3";

            assertEquals("Wrong message content in response #" + (i+1), expected, content);
        }
    }
}
