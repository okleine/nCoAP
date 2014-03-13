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

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.applicationcomponents.client.TestResponseProcessor;
import de.uniluebeck.itm.ncoap.applicationcomponents.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.Assert.*;


/**
* Tests of CoAP coapRequest and response messages including reliability,
* piggy-backed and separate response.
*
* @author Oliver Kleine, Stefan Hüske
*/
public class ClientGetsNoAckForConRequestTest extends AbstractCoapCommunicationTest {

    private static Logger log = Logger.getLogger(ClientGetsNoAckForConRequestTest.class.getName());

    private static long timeRequestSent;
    private static CoapRequest coapRequest;

    private static CoapClientApplication client;
    private static TestResponseProcessor responseProcessor;

    private static CoapTestEndpoint testEndpoint;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.outgoing").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.applicationcomponents").setLevel(Level.DEBUG);
    }


    @Override
    public void setupComponents() throws Exception {
        testEndpoint = new CoapTestEndpoint();

        client = new CoapClientApplication();
        responseProcessor = new TestResponseProcessor();
        URI targetUri = new URI("coap://localhost:" + testEndpoint.getPort() + "/testpath");
        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
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
        InetSocketAddress remoteEndpoint = new InetSocketAddress("127.0.0.1", testEndpoint.getPort());
        client.sendCoapRequest(coapRequest, responseProcessor, remoteEndpoint);

        timeRequestSent = System.currentTimeMillis();

        //Maximum time to pass before last retransmission is 48 sec.
        //Wait another 6 sec. to let the last retransmission time out before test methods start
        Thread.sleep(250000);
    }


    @Test
    public void testNumberOfRequests(){
        int expected = 5;
        int actual = testEndpoint.getReceivedCoapMessages().size();
        assertEquals("Endpoint received wrong number of requests!", expected, actual);
    }


    @Test
    public void testAllRequestsAreEqual(){
        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedCoapMessages();
        CoapMessage firstMessage = receivedMessages.get(receivedMessages.firstKey());

        for(CoapMessage message : receivedMessages.values()){
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
    public void testRetransmissionsWereReceivedInTime(){

        assertEquals("Wrong number of received messages!", 5, testEndpoint.getReceivedCoapMessages().size());

        Iterator<Map.Entry<Long, CoapMessage>> receivedMessages =
                testEndpoint.getReceivedCoapMessages().entrySet().iterator();

        //ignore first message...
        receivedMessages.next();

        long[][] delay = new long[][]{
                new long[]{2000, 3000}, new long[]{6000, 9000}, new long[]{14000, 21000}, new long[]{30000, 45000}
        };


        for(int i = 0; i < 2; i++){
            long actualDelay = receivedMessages.next().getKey() - timeRequestSent;

            String message = "Retransmission " + (i+1)
                           + " (expected delay between " + delay[i][0] + " and " + delay[i][1] + "ms,"
                           + " actual delay " + actualDelay + "ms).";

            log.info(message);
            assertTrue(message, delay[i][0] <= actualDelay);
            assertTrue(message, delay[i][1] >= actualDelay);
        }
    }


    /**
     * Test if the notification was to early. Minimum delay is the sum of minimal timeouts (30000) plus the delay
     * for timeout notification (5000). The maximum delay is the sum of maximum timeouts (45000) plus the delay for timeout
     * notification (5000) plus a tolerance of 2000.
     */
    @Test
    public void testClientReceivesTimeoutNotification(){
        //Test if the client received a timeout notifiaction
        assertFalse("Client did not receive a timeout notification at all.",
                responseProcessor.getRequestTransmissionTimeoutTimes().isEmpty());

        long minDelay = 247000;

        long firstTransmissionTime = responseProcessor.getRequestTransmissionTimes().firstEntry().getElement();
        long transmissionTimeoutTime = responseProcessor.getRequestTransmissionTimeoutTimes().firstEntry().getElement();

        long actualDelay = transmissionTimeoutTime - firstTransmissionTime;

        String message = "Retransmission timeout notification"
                + " (expected delay is at least " + minDelay + "ms,"
                + " actual delay " + actualDelay + "ms).";

        log.info(message);
        assertTrue(message, minDelay <= actualDelay);
    }


    @Test
    public void testClientWasNotifiedOf5AttemptsToDeliverRequest(){
        assertEquals("Client was not notfied the proper number of times about transmission attempts.",
                5, responseProcessor.getRequestTransmissionTimes().size());
    }
}

