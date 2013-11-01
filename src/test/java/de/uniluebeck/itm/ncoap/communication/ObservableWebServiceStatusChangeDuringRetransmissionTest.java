/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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

import de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableTestWebService;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import de.uniluebeck.itm.ncoap.toolbox.Token;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;

import static junit.framework.Assert.*;

/**
* Tests if the server adapts MAX_RETRANSMIT to avoid CON timeout before Max-Age ends.
* (Only for observe notifications)
*
* @author Oliver Kleine, Stefan Hueske
*/
public class ObservableWebServiceStatusChangeDuringRetransmissionTest extends AbstractCoapCommunicationTest {

    private static String PATH_TO_SERVICE = "/observable";
    private static CoapTestEndpoint endpoint;

    private static CoapServerApplication server;
    private static ObservableTestWebService service;

    //registration requests
    private static CoapRequest observationRequest;
    private static long observationRequestSent;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler")
              .setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe")
              .setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint")
              .setLevel(Level.DEBUG);
    }


    @Override
    public void setupComponents() throws Exception {
        //start server
        server = new CoapServerApplication(0);

        //create observable service
        service = new ObservableTestWebService(PATH_TO_SERVICE, 0, 0);
        service.setMaxAge(90);
        server.registerService(service);


        //create endpoint (i.e. client)
        endpoint = new CoapTestEndpoint();

        //create observation request
        URI targetUri = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);
        observationRequest = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
        observationRequest.getHeader().setMsgID(12345);
        observationRequest.setToken(new byte[]{0x12, 0x23, 0x34});
        observationRequest.setObserveOptionRequest();
    }

    @Override
    public void createTestScenario() throws Exception {

//                       testEndpoint               Server               Service
//                             |                        |                    |
//        (observationRequest) |-----GET OBSERVE------->|                    |
//                             |                        |                    |
//             (notification1) |<----ACK OBSERVE--------|                    |
//                             |                        |<-----UPDATE()------|
//             (notification2) |<-CON Not.,MAX-AGE:100--|                    |      time: 0
//                             |                        |                    | |
//                             |<----CON RETR.1---------|                    | |    time: 11.25 sec
//                             |                        |                    | |
//                             |<----CON RETR.2---------|                    | |    time: 22.5 sec
//                             |                        |                    | |
//                             |                        | <---UPDATE()-------| |    time: ~35 sec.
//                             |                        |                    | |
//                             |<----CON Update---------|                    | |    time: ~35.001 sec
//                             |                        |                    | |
//                             |<----CON RETR.3---------|                    | |    time: 45 sec
//                             |                        |                    | |
//                             |<----CON RETR.4---------|                    | |    time: 90 sec. (>= MAX AGE)



        //send observation request to server
        observationRequestSent = System.currentTimeMillis();
        endpoint.writeMessage(observationRequest, new InetSocketAddress("localhost", server.getServerPort()));

        //wait 2.5 seconds (until first retransmission was sent and update observed resource
        Thread.sleep(2000);
        service.setResourceStatus(1);

        //wait for update notification and 2 retransmissions and change status then
        Thread.sleep(35000);
        service.setResourceStatus(2);

        //wait another 65 seconds to finish the other 2 retransmissions
        Thread.sleep(65000);
        endpoint.setReceiveEnabled(false);
    }



    @Override
    public void shutdownComponents() throws Exception {
        endpoint.shutdown();
        server.shutdown();
    }


    @Test
    public void testEndpointReceived7Messages() {
        String message = "Receiver did not receive 7 messages";
        assertEquals(message, 7, endpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testFirstMessageIsAck(){
        CoapMessage firstMessage =
                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().firstKey());

        assertEquals("First received message was no ACK.", firstMessage.getMessageType(), MessageType.ACK);
        assertTrue("First message is no update notification.", ((CoapResponse) firstMessage).isUpdateNotification());
        assertTrue("Token does not match.", Arrays.equals(firstMessage.getToken(), observationRequest.getToken()));
    }

    @Test
    public void testLastMessageWasNotSentBeforeMaxAgeEnded(){
        long delay = endpoint.getReceivedMessages().lastKey() - observationRequestSent;

        assertTrue("Last retransmission was to early (after (" + delay +") millis. But max-age is 90 seconds!",
                delay >= 90000);
    }

    @Test
    public void testUpdateOfStatusDuringRetransmission(){
        CoapMessage[] receivedMessages = new CoapMessage[6];
        receivedMessages = endpoint.getReceivedMessages().values().toArray(receivedMessages);

        Token[] payloads = new Token[5];

        for(int i = 1; i < 6; i++){
            payloads[i-1] = new Token(receivedMessages[i].getPayload().copy().array());
        }

        assertEquals("Original message and 1st retransmission do not match", payloads[0], payloads[1]);
        assertEquals("1st and 2nd retransmission do not match", payloads[1], payloads[2]);
        assertFalse("2nd and 3rd retransmission do match!", payloads[2].equals(payloads[3]));
        assertEquals("3rd and 4th retransmission do not match", payloads[3], payloads[4]);
    }

    @Test
    public void testNotificationCount(){
        CoapResponse[] receivedMessages = new CoapResponse[6];
        receivedMessages = endpoint.getReceivedMessages().values().toArray(receivedMessages);

        int exectedNotifications = 7;

        long[] notificationCounts = new long[exectedNotifications];

        for(int i = 0; i < exectedNotifications; i++){
            notificationCounts[i] =
                    (Long) receivedMessages[i]
                           .getOption(OptionRegistry.OptionName.OBSERVE_RESPONSE)
                           .get(0)
                           .getDecodedValue();
        }

        for(int i = 1; i < exectedNotifications; i++){
            long actual = notificationCounts[i];
            long previous = notificationCounts[i-1];

            assertTrue("Notification count (" + actual + ") was not larger than previous (" + previous + ")!",
                    notificationCounts[i] > notificationCounts[i-1]);
        }
    }
}
