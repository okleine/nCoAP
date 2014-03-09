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
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.communication;
//
//import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
//import de.uniluebeck.itm.ncoap.plugtest.client.TestResponseProcessor;
//import de.uniluebeck.itm.ncoap.plugtest.endpoint.CoapTestEndpoint;
//import de.uniluebeck.itm.ncoap.message.*;
//import de.uniluebeck.itm.ncoap.message.MessageCode;
//import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.nio.charset.Charset;
//import java.util.Iterator;
//import java.util.List;
//
//import static junit.framework.Assert.assertEquals;
//import static junit.framework.Assert.assertFalse;
//import static junit.framework.Assert.assertTrue;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 22.08.13
// * Time: 15:12
// * To change this template use File | Settings | File Templates.
// */
//public class ObservationTimesOutWithoutRestart extends AbstractCoapCommunicationTest{
//
//    private static CoapClientApplication coapClient;
//    private static CoapRequest coapRequest;
//    private static TestResponseProcessor responseProcessor;
//
//    private static CoapTestEndpoint coapEndpoint;
//    private static CoapResponse updateNotification1;
//    private static CoapResponse updateNotification2;
//    private static CoapResponse updateNotification3;
//
//    private static long MAX_AGE = 10;
//    @Override
//    public void setupComponents() throws Exception {
//
//        coapEndpoint = new CoapTestEndpoint();
//
//        coapClient = new CoapClientApplication();
//        URI serviceUri = new URI("coap", null, "localhost", coapEndpoint.getPort(), "/observable", null, null);
//
//        coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, serviceUri);
//        coapRequest.setObserveOptionRequest();
//
//        responseProcessor = new TestResponseProcessor();
//
//        updateNotification1 = new CoapResponse(MessageCode.CONTENT_205);
//        updateNotification1.getHeader().setMessageType(MessageType.ACK);
//        updateNotification1.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
//        updateNotification1.setContent("Status 1".getBytes(Charset.forName("UTF-8")));
//        updateNotification1.setMaxAge(MAX_AGE);
//        updateNotification1.setObserveOptionValue(1);
//
//        updateNotification2 = new CoapResponse(MessageCode.CONTENT_205);
//        updateNotification2.getHeader().setMessageType(MessageType.CON);
//        updateNotification2.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
//        updateNotification2.setContent("Status 2".getBytes(Charset.forName("UTF-8")));
//        updateNotification2.setMaxAge(MAX_AGE);
//        updateNotification2.setObserveOptionValue(2);
//
//        updateNotification3 = new CoapResponse(MessageCode.CONTENT_205);
//        updateNotification3.getHeader().setMessageType(MessageType.CON);
//        updateNotification3.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
//        updateNotification3.setContent("Status 3".getBytes(Charset.forName("UTF-8")));
//        updateNotification3.setMaxAge(MAX_AGE);
//        updateNotification3.setObserveOptionValue(3);
//    }
//
//    @Override
//    public void createTestScenario() throws Exception {
//
//         /*
//             testClient                    testEndpoint     DESCRIPTION
//                  |                             |
//              (1) |--------GET (observe)------->|           client sends GET CON (observe) to testEndpoint
//                  |                             |
//              (2) |<-------ACK-RESPONSE---------|           testEndpoint responds with ACK and observe #1
//                  |                             |
//                  |                             |           time passes (9 sec)
//                  |                             |
//              (3) |<-------CON-RESPONSE---------|           testEndpoint responds with CON and observe #2
//                  |                             |
//                  |------- empty ACK ---------->|           testClient sends empty ACK
//                  |                             |
//                  |                             |           time passes (15 sec)
//                  |                             |           -> observation times out on client,
//                  |                             |
//              (4) |<-------CON-RESPONSE---------|           testEndpoint responds with CON and observe #3
//                  |                             |
//                  |------- empty ACK ---------->|           testClient sends empty ACK but does not process the
//                  |                             |           update notification!
//        */
//
//
//        //write CON request with observe option
//        coapClient.sendCoapRequest(coapRequest, responseProcessor);
//        Thread.sleep(500);
//
//        //write first update notification (ACK)
//        CoapRequest receivedRequest = (CoapRequest) coapEndpoint.getReceivedCoapMessages().values().toArray()[0];
//        updateNotification1.setMessageID(receivedRequest.getMessageID());
//        updateNotification1.setToken(receivedRequest.getToken());
//
//        coapEndpoint.writeMessage(updateNotification1, new InetSocketAddress("localhost", coapClient.getClientPort()));
//
//        //Send update notification within max-age
//        Thread.sleep(9000);
//        updateNotification2.setMessageID(receivedRequest.getMessageID());
//        updateNotification2.setToken(receivedRequest.getToken());
//
//        coapEndpoint.writeMessage(updateNotification2, new InetSocketAddress("localhost", coapClient.getClientPort()));
//
//        //Send update notification after observation is supposed to be timed out on client side
//        Thread.sleep(17000);
//        updateNotification3.setMessageID(receivedRequest.getMessageID());
//        updateNotification3.setToken(receivedRequest.getToken());
//
//        coapEndpoint.writeMessage(updateNotification3, new InetSocketAddress("localhost", coapClient.getClientPort()));
//
//        Thread.sleep(12000);
//    }
//
//    @Override
//    public void shutdownComponents() throws Exception {
//        coapEndpoint.shutdown();
//        coapClient.shutdown();
//    }
//
//    @Override
//    public void setupLogging() throws Exception {
//        Logger.getLogger("de.uniluebeck.itm.ncoap.plugtest.endpoint.CoapTestEndpoint")
//                .setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.plugtest.client").setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.ObservationTimesOutWithoutRestart").setLevel(Level.DEBUG);
//    }
//
//    @Test
//    public void testEndpointReceivedCorrectMessages(){
//        Iterator<CoapMessage> receivedMessages =  coapEndpoint.getReceivedCoapMessages().values().iterator();
//
//        //First message is the request
//        assertTrue(receivedMessages.next() instanceof CoapRequest);
//
//        //2nd message is the ACK for the first update notification
//        assertEquals(MessageType.ACK, receivedMessages.next().getMessageType());
//
//        //3rd message is the ACK for the second update notification, even though the observation timed out.
//        //This is to avoid retransmissions of unexpected CON messages by the remote host.
//        assertEquals(MessageType.RST, receivedMessages.next().getMessageType());
//
//        //There should be no more messages!
//        assertFalse("There should be no more messages!", receivedMessages.hasNext());
//    }
//
//    @Test
//    public void testClientReceived2UpdateNotifications(){
//        List<CoapResponse> updateNotifications = responseProcessor.getCoapResponses();
//
//        //There should be 2 update notifications received
//        assertEquals(2, updateNotifications.size());
//
//        //First update notification is ACK
//         assertTrue("First update notification should be an ACK",
//                 updateNotifications.get(0).getMessageType() == MessageType.ACK);
//
//        //Second update notification is CON
//        assertTrue("Second update notification should be CON",
//                updateNotifications.get(1).getMessageType() == MessageType.CON);
//    }
//
//    @Test
//    public void testObservationTimedOut(){
//        assertTrue("Observation did not time out!", responseProcessor.isObservationTimedOut());
//    }
//
//}
