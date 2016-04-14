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
import de.uzl.itm.ncoap.communication.reliability.inbound.ServerInboundReliabilityHandler;
import de.uzl.itm.ncoap.endpoints.DummyEndpoint;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

import static org.junit.Assert.*;


/**
* Tests of CoAP coapRequest and response messages including reliability,
* piggy-backed and separate response.
*
* @author Oliver Kleine, Stefan Hüske
*/
public class ClientSendsCONRequestThatTimesOut extends AbstractCoapCommunicationTest {

    private static Logger log = Logger.getLogger(ClientSendsCONRequestThatTimesOut.class.getName());

    private static long timeRequestSent;
    private static CoapRequest coapRequest;

    private static CoapClient client;
    private static TestCallback callback;

    private static DummyEndpoint testEndpoint;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(DummyEndpoint.class.getName())
                .setLevel(Level.DEBUG);

        Logger.getLogger(TestCallback.class.getName())
                .setLevel(Level.DEBUG);

        Logger.getLogger(ServerInboundReliabilityHandler.class.getName())
                .setLevel(Level.DEBUG);

        Logger.getLogger(ClientSendsCONRequestThatTimesOut.class.getName())
              .setLevel(Level.DEBUG);

        Logger.getRootLogger().setLevel(Level.ERROR);
    }


    @Override
    public void setupComponents() throws Exception {
        testEndpoint = new DummyEndpoint();

        client = new CoapClient();
        callback = new TestCallback();

        URI targetUri = new URI("coap://localhost:" + testEndpoint.getPort() + "/testpath");
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
        testEndpoint.shutdown();
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

//             client                        testEndpoint     DESCRIPTION
//                  |                             |
//              (1) |----CON-GET----------------->|           Client sends confirmable request
//                  |                             |
//              (2) |----1st RETRANSMISSION------>|           (Client should send four retransmissions)
//                  |                             |
//              (3) |----2nd RETRANSMISSION------>|
//                  |                             |
//              (4) |----3rd RETRANSMISSION------>|
//                  |                             |
//              (5) |----4th RETRANSMISSION------>|
//                  |                             |
//                  |                             |           internal timeout notification to response processor


        //Send coapRequest
        InetSocketAddress remoteSocket = new InetSocketAddress("localhost", testEndpoint.getPort());
        timeRequestSent = System.currentTimeMillis();
        client.sendCoapRequest(coapRequest, remoteSocket, callback);

        //Wait for the message ID to retire (takes 247 seconds).
        Thread.sleep(48000);
        log.warn("Now we have to wait for the message ID to time out (~200 seconds)... Time to get a coffee!");
        Thread.sleep(201000);
    }


    @Test
    public void testNumberOfReceivedRequests() {
        int expected = 5;
        int actual = testEndpoint.getReceivedCoapMessages().size();
        assertEquals("Endpoint received wrong number of requests!", expected, actual);
    }


    @Test
    public void testAllRequestsAreEqual() {
        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedCoapMessages();
        CoapMessage firstMessage = receivedMessages.get(receivedMessages.firstKey());

        for(CoapMessage message : receivedMessages.values()) {
            assertEquals("Received requests did not equal.", firstMessage, message);
        }
    }


    /**
     * Resulting absolute intervals
     * 1st retransmission should be received after  2 - 3  sec
     * 2nd retransmission should be received after  6 - 9  sec
     * 3rd retransmission should be received after 14 - 21 sec
     * 4th retransmission should be received after 30 - 45 sec
     */
    @Test
    public void testRetransmissionsWereSentInTime() {

        int expectedMessages = 4;

        Set<Long> transmissions = callback.getTransmissions();
        assertEquals("Wrong number of sent messages!", expectedMessages, transmissions.size());

        Iterator<Long> transmissionIterator = transmissions.iterator();

        long[][] delay = new long[][]{
                new long[]{2000, 3000}, new long[]{6000, 9000}, new long[]{14000, 21000}, new long[]{30000, 45000}
        };

        int i = -1;
        while(transmissionIterator.hasNext()) {
            i += 1;
            long actualDelay = transmissionIterator.next() - timeRequestSent;

            String format = "Retransmission #%d (expected delay: %d - %d millis, actual delay: %d millis)";
            log.info(String.format(format, i + 1, delay[i][0], delay[i][1], actualDelay));

            assertTrue("Message was sent too early!", delay[i][0] <= actualDelay);
            assertTrue("Message was sent too late!", delay[i][1] >= actualDelay);
        }
    }


    /**
     * Test if the notification was to early. Minimum delay is the sum of minimal timeouts (30000) plus the delay
     * for timeout notification (5000). The maximum delay is the sum of maximum timeouts (45000) plus the delay for timeout
     * notification (5000) plus a tolerance of 2000.
     */
    @Test
    public void testClientReceivesTimeoutNotification() {
        assertFalse("Client did not receive a timeout notification at all.",
                callback.getTransmissionTimeouts().isEmpty());

        long minDelay = 247000;

        long transmissionTimeout = callback.getTransmissionTimeouts().iterator().next();
        long actualDelay = transmissionTimeout - timeRequestSent;

        String format = "Internal transmission timeout notification (expected minimum delay: %d millis, actual: %d" +
                "millis)";
        log.info(String.format(format, minDelay, actualDelay));

        assertTrue("Internal transmission timeout notification was too early!", minDelay <= actualDelay);
    }
}

