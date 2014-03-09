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
//import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
//import de.uniluebeck.itm.ncoap.plugtest.server.webservice.ObservableTestWebService;
//import de.uniluebeck.itm.ncoap.message.CoapRequest;
//import de.uniluebeck.itm.ncoap.message.CoapResponse;
//import de.uniluebeck.itm.ncoap.message.MessageCode;
//import de.uniluebeck.itm.ncoap.message.MessageType;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//
//import java.net.URI;
//import java.nio.charset.Charset;
//
//import static junit.framework.Assert.assertEquals;
//
///**
//* Tests if a client receives notifications.
//* @author Stefan Hueske, Oliver Kleine
//*/
//public class ClientReceivesObserveNotification extends AbstractCoapCommunicationTest{
//
//    private static final String PATH_TO_SERVICE = "/observable";
//
//    private static CoapClientApplication client;
//    private static TestResponseProcessor responseProcessor;
//
//    private static CoapServerApplication server;
//    private static ObservableTestWebService service;
//
//    //observable request
//    private static CoapRequest request;
//
//
//    @Override
//    public void setupLogging() throws Exception {
//        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe")
//                .setLevel(Level.INFO);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.plugtest.client.TestResponseProcessor")
//                .setLevel(Level.INFO);
//    }
//
//    @Override
//    public void setupComponents() throws Exception {
//        server = new CoapServerApplication(0);
//        service = new ObservableTestWebService(PATH_TO_SERVICE, 1, 0);
//        server.registerService(service);
//
//        client = new CoapClientApplication();
//        responseProcessor = new TestResponseProcessor();
//
//        URI targetUri = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);
//        request = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
//        request.setObserveOptionRequest();
//    }
//
//    @Override
//    public void shutdownComponents() throws Exception {
//        client.shutdown();
//    }
//
//
//    @Override
//    public void createTestScenario() throws Exception {
//
////               Client                        Server
////              (1) |------GET-OBSERVE----------->|           send observable request to server
////                  |                             |
////              (2) |<-----ACK-NOTIFICATION-------|           server responds with initial, piggy-backed notification
////                  |                             |
////                  |                             |  <------  status update (new status: 2)
////                  |                             |
////              (3) |<-----CON-NOTIFICATION-------|           server sends 2nd notification,
////                  |                             |
////              (4) |------EMPTY-ACK------------->|
////                  |                             |
////                  |                             |  <------- shutdown server
////                  |                             |
////              (5) |<-----CON-NOTIFICATION-------|           server sends 3rd notification (404 not found)
//
//
//
//        //write request
//        client.sendCoapRequest(request, responseProcessor);
//
//        Thread.sleep(3000);
//        service.setResourceStatus(2);
//
//        Thread.sleep(1000);
//
//        server.shutdown();
//        Thread.sleep(1000);
//    }
//
//    @Test
//    public void testClientReceived3Messages() {
//        String message = "Receiver did not receive 3 messages";
//        assertEquals(message, 3, responseProcessor.getCoapResponses().size());
//    }
//
//    @Test
//    public void testFirstMessage(){
//        CoapResponse response = responseProcessor.getCoapResponse(0);
//
//        assertEquals("Messagt type is not ACK", MessageType.ACK, response.getMessageType());
//
//        assertEquals("Content does not match.", "Status #1",
//                response.getContent().toString(Charset.forName("UTF-8")));
//    }
//
//    @Test
//    public void testSecondMessage(){
//        CoapResponse response = responseProcessor.getCoapResponse(1);
//
//        assertEquals("Messagt type is not CON", MessageType.CON, response.getMessageType());
//
//        assertEquals("Content does not match.", "Status #2",
//                response.getContent().toString(Charset.forName("UTF-8")));
//    }
//
//    @Test
//    public void testThirdMessage(){
//        CoapResponse response = responseProcessor.getCoapResponse(2);
//
//        assertEquals("Messagt type is not CON", MessageType.CON, response.getMessageType());
//
//        assertEquals("MessageCode is not 404", MessageCode.NOT_FOUND_404, response.getMessageCode());
//    }
//
////    @Test
////    public void testReceiverReceivedNotification1() {
////        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
////        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
////        String message = "1st notification: MessageType is not ACK";
////        assertEquals(message, MessageType.ACK, receivedMessage.getMessageType());
////        message = "1st notification: MessageCode is not 2.05 (Content)";
////        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());
////        message = "1st notification: Payload does not match";
////        assertEquals(message, expectedNotification1.getContent(), receivedMessage.getContent());
////    }
////
////    @Test
////    public void testReceiverReceivedNotification2() {
////        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
////        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
////        timeKeys.next();
////        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
////        String message = "2nd notification: MessageType is not ACK";
////        assertEquals(message, MessageType.CON, receivedMessage.getMessageType());
////        message = "2nd notification: MessageCode is not 2.05 (Content)";
////        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());
////        message = "2nd notification: Payload does not match";
////        assertEquals(message, expectedNotification2.getContent(), receivedMessage.getContent());
////    }
//
//
//}
