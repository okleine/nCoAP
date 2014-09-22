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
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.endpoints.client.CoapClientTestCallback;
import de.uniluebeck.itm.ncoap.endpoints.server.ObservableTestWebservice;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
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

    private static CoapClientApplication client;
    private static CoapClientTestCallback clientCallback;

    private static CoapServerApplication server;
    private static ObservableTestWebservice service;

    //observable request
    private static CoapRequest request;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.ClientReceivesUpdateNotifications$SpecificClientCallback")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.server.WebserviceManager")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe.server.WebserviceObservationHandler")
                .setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice")
                .setLevel(Level.DEBUG);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication(0);
        service = new ObservableTestWebservice(PATH_TO_SERVICE, 1, 0);
        server.registerService(service);

        client = new CoapClientApplication();
        clientCallback = new SpecificClientCallback();

        URI targetUri = new URI("coap://localhost:" + server.getPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        request.setObserve(true);
    }

    @Override
    public void shutdownComponents() throws Exception {
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
        client.sendCoapRequest(request, clientCallback, new InetSocketAddress("localhost", server.getPort()));

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
    public void testFirstMessage(){
        Iterator<Long> receptionTimes = clientCallback.getCoapResponses().keySet().iterator();

        CoapResponse response = clientCallback.getCoapResponses().get(receptionTimes.next());

        assertEquals("Message type is not ACK", MessageType.Name.ACK, response.getMessageTypeName());

        assertEquals("Content does not match.", "Status #1",
                response.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testSecondMessage(){
        Iterator<Long> receptionTimes = clientCallback.getCoapResponses().keySet().iterator();
        receptionTimes.next();

        CoapResponse response = clientCallback.getCoapResponses().get(receptionTimes.next());

        assertEquals("Wrong Message Code!", MessageCode.Name.CONTENT_205, response.getMessageCodeName());

        assertEquals("Content does not match.", "Status #2",
                response.getContent().toString(Charset.forName("UTF-8")));
    }

    @Test
    public void testThirdMessage(){
        Iterator<Long> receptionTimes = clientCallback.getCoapResponses().keySet().iterator();
        receptionTimes.next();
        receptionTimes.next();

        CoapResponse response = clientCallback.getCoapResponses().get(receptionTimes.next());

        assertEquals("MessageCode is not 404", MessageCode.Name.NOT_FOUND_404, response.getMessageCodeName());
    }

//    @Test
//    public void testReceiverReceivedNotification1() {
//        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "1st notification: MessageType is not ACK";
//        assertEquals(message, MessageType.ACK, receivedMessage.getMessageType());
//        message = "1st notification: MessageCode is not 2.05 (Content)";
//        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, expectedNotification1.getContent(), receivedMessage.getContent());
//    }
//
//    @Test
//    public void testReceiverReceivedNotification2() {
//        SortedMap<Long, CoapResponse> receivedMessages = testClient.getReceivedResponses();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd notification: MessageType is not ACK";
//        assertEquals(message, MessageType.CON, receivedMessage.getMessageType());
//        message = "2nd notification: MessageCode is not 2.05 (Content)";
//        assertEquals(message, MessageCode.CONTENT_205, receivedMessage.getMessageCode());
//        message = "2nd notification: Payload does not match";
//        assertEquals(message, expectedNotification2.getContent(), receivedMessage.getContent());
//    }


    private class SpecificClientCallback extends CoapClientTestCallback{

        private Logger log = Logger.getLogger(SpecificClientCallback.class.getName());

        @Override
        public boolean continueObservation(InetSocketAddress remoteEndpoint, Token token){
            log.debug("TEST!!!");
            return true;
        }
    }

}
