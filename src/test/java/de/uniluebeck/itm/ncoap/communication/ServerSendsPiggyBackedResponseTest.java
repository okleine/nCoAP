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
//import de.uniluebeck.itm.ncoap.application.client.TestResponseProcessor;
//import de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint;
//import de.uniluebeck.itm.ncoap.message.*;
//import de.uniluebeck.itm.ncoap.message.MessageCode;
//
//import java.net.InetSocketAddress;
//
////import de.uniluebeck.itm.ncoap.communication.utils.receiver.MessageReceiverResponse;
//import org.junit.Test;
//
//import java.net.URI;
//import java.util.SortedMap;
//
//import static junit.framework.Assert.*;
//
//
///**
//* Tests to verify the client functionality related to piggy-backed responses.
//*
//* @author Oliver Kleine, Stefan Hueske
//*/
//public class ServerSendsPiggyBackedResponseTest extends AbstractCoapCommunicationTest {
//
//    private static final String PAYLOAD = "Some arbitrary content!";
//
//    private static CoapClientApplication client;
//    private static TestResponseProcessor responseProcessor;
//    private static CoapRequest coapRequest;
//
//    private static CoapTestEndpoint endpoint;
//
//    @Override
//    public void setupComponents() throws Exception {
//
//        //Create endpoint
//        endpoint = new CoapTestEndpoint();
//
//        //Create client and response processor
//        client = new CoapClientApplication();
//        responseProcessor = new TestResponseProcessor();
//
//        URI targetUri =  new URI("coap://localhost:" + endpoint.getPort());
//        coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
//    }
//
//    @Override
//    public void shutdownComponents() throws Exception {
//        client.shutdown();
//        endpoint.shutdown();
//    }
//
//    @Override
//    public void setupLogging() throws Exception {
//
//    }
//
//    @Override
//    public void createTestScenario() throws Exception {
//
//        /*
//             testClient                    testEndpoint     DESCRIPTION
//                  |                             |
//              (1) |--------GET----------------->|           client sends GET-Request to testEndpoint
//                  |                             |
//              (2) |<-------ACK-RESPONSE---------|           testEndpoint responds
//                  |                             |
//              (3) |<-------ACK-RESPONSE---------|           testEndpoint sends the response again,
//                  |                             |           nothing should happen here when the client
//                  |                             |           removed the callback as expected
//        */
//
//        //write request
//        client.writeCoapRequest(coapRequest, responseProcessor);
//
//        //Wait some time
//        Thread.sleep(300);
//
//        //Get message ID and token from received message
//        int messageID =
//                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getMessageID();
//        byte[] token =
//                endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getToken();
//
//        //write response #1
//        CoapResponse response = createResponse(messageID, token);
//        endpoint.writeMessage(response, new InetSocketAddress("localhost", client.getClientPort()));
//
//        //Wait some time
//        Thread.sleep(300);
//
//        //write response #2
//        CoapResponse response2 = createResponse(messageID, token);
//        endpoint.writeMessage(response2, new InetSocketAddress("localhost", client.getClientPort()));
//
//        //Wait some time
//        Thread.sleep(300);
//        endpoint.setReceiveEnabled(false);
//    }
//
//
//
//    private static CoapResponse createResponse(int messageID, byte[] token) throws Exception {
//        CoapResponse coapResponse = new CoapResponse(MessageCode.CONTENT_205);
//        coapResponse.getHeader().setMessageType(MessageType.ACK);
//        coapResponse.setMessageID(messageID);
//        coapResponse.setToken(token);
//        coapResponse.setContent(PAYLOAD.getBytes("UTF-8"));
//
//        return coapResponse;
//    }
//
//    @Test
//    public void testReceivedRequestEqualsSentRequest() {
//        SortedMap<Long, CoapMessage> receivedRequests = endpoint.getReceivedMessages();
//        String message = "Written and received request do not equal";
//        assertEquals(message, coapRequest, receivedRequests.get(receivedRequests.firstKey()));
//    }
//
//    @Test
//    public void testReceiverReceivedOnlyOneRequest() {
//        String message = "Receiver received more than one message";
//        assertEquals(message, 1, endpoint.getReceivedMessages().size());
//    }
//
//    @Test
//    public void testClientCallbackInvokedOnce() {
//        String message = "Client callback was invoked less or more than once";
//        assertEquals(message, 1, responseProcessor.getCoapResponses().size());
//    }
//}
