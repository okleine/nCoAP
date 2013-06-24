package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.client.TestCoapResponseProcessor;
import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
* Tests for the removal of observers.
*
* @author Stefan Hueske, Oliver Kleine
*/
public class ObserveOptionCancellationTest extends AbstractCoapCommunicationTest {

    private static String PATH_TO_SERVICE = "/observable";

    private static CoapServerApplication server;
    private static ObservableTestWebService service;

    private static CoapTestEndpoint endpoint;
    private static TestCoapResponseProcessor responseProcessor;

    //requests
    private static CoapRequest observationRequest1;
    private static CoapRequest normalRequest;
    private static CoapRequest observationRequest2;

    ScheduledExecutorService executorService;

    @Override
    public void setupLogging() throws Exception {
        Logger
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.encoding").setLevel(Level.DEBUG);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication(0);
        service = new ObservableTestWebService(PATH_TO_SERVICE, 0, 0, 1000);
        service.setUpdateNotificationConfirmable(false);
        server.registerService(service);

        endpoint = new CoapTestEndpoint();
        responseProcessor = new TestCoapResponseProcessor();

        URI targetURI = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);

        observationRequest1 = new CoapRequest(MsgType.CON, Code.GET, targetURI);
        observationRequest1.setObserveOptionRequest();
        observationRequest1.getHeader().setMsgID(123);
        observationRequest1.setToken(new byte[]{1,2,3,4});

        normalRequest = new CoapRequest(MsgType.CON, Code.GET, targetURI);
        observationRequest1.getHeader().setMsgID(456);

        observationRequest2 = new CoapRequest(MsgType.CON, Code.GET, targetURI);
        observationRequest2.setObserveOptionRequest();
        observationRequest1.getHeader().setMsgID(789);
        observationRequest1.setToken(new byte[]{5,6,7,8});

        executorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        endpoint.shutdown();
    }


    @Override
    public void createTestScenario() throws Exception {

//             testEndpoint                    Server        DESCRIPTION
//                  |                             |
//                  |--------GET_OBSERVE--------->|          Register observer
//                  |                             |
//                  |<-------1st Notification-----|          Receive first notification
//                  |                             |
//                  |                             |  <-----  Status update (new status: 2) (after 1000 ms)
//                  |                             |
//                  |<-------2nd Notification-----|
//                  |                             |
//                  |--------GET----------------->|          Remove observer using a GET request without observe option
//                  |                             |
//                  |<-------Simple ACK response--|          Receive ACK response (without observe option)
//                  |                             |
//                  |                             |
//                  |                             |  <-----  Status update (new status: 3) (after 2000 ms)
//                  |                             |
//                  |                             |          some time passes... nothing should happen!
//                  |                             |
//                  |                             |  <-----  Status update (new status: 4) (after 3000 ms)
//                  |                             |
//                  |--------GET_OBSERVE--------->|          Register observer
//                  |                             |
//                  |<-------1st Notification-----|          Receive first notification (ACK)
//                  |                             |
//                  |                             |  <-----  Status update (new status: 0) (after 4000 ms)
//                  |                             |
//                  |<-------2nd Notification-----|
//                  |                             |
//                  |--------RST----------------->|          Respond with reset to the 2nd notification
//                  |                             |

        //schedule first observation request
        executorService.schedule(new Runnable(){
            @Override
            public void run() {
                endpoint.writeMessage(observationRequest1, new InetSocketAddress("localhost", server.getServerPort()));
            }
        }, 0, TimeUnit.MILLISECONDS);

        //send GET for same resource without Observe Option
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                endpoint.writeMessage(normalRequest, new InetSocketAddress("localhost", server.getServerPort()));
            }
        }, 1100, TimeUnit.MILLISECONDS);

        //send GET for same resource without Observe Option
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                endpoint.writeMessage(observationRequest2, new InetSocketAddress("localhost", server.getServerPort()));
            }
        }, 3100, TimeUnit.MILLISECONDS);

        //send GET for same resource without Observe Option
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                int messageID =
                        endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getMessageID();

                endpoint.writeMessage(CoapMessage.createEmptyReset(messageID),
                        new InetSocketAddress("localhost", server.getServerPort()));
            }
        }, 4100, TimeUnit.MILLISECONDS);

        Thread.sleep(5000);
    }



    @Test
    public void testReceiverReceived5Messages() {
        String message = "Receiver did not receive 5 messages";
        assertEquals(message, 5, endpoint.getReceivedMessages().values().size());
    }

//    @Test
//    public void testReceiverReceivedRegistration1Notification1() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "1st notification: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration1ACKresponse() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "Simple ACK response: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "Simple ACK response: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "Simple ACK response: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration2Notification1() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd registration, 1st notification: MsgType is not ACK";
//        assertEquals(message, MsgType.ACK, receivedMessage.getMessageType());
//        message = "2nd registration, 1st notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "2nd registration, 1st notification: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReceiverReceivedRegistration2Notification2() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//        String message = "2nd registration, 2nd notification: MsgType is not ACK";
//        assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//        message = "2nd registration, 2nd notification: Code is not 2.05 (Content)";
//        assertEquals(message, Code.CONTENT_205, receivedMessage.getCode());
//        message = "2nd registration, 2nd notification: Payload does not match";
//        assertEquals(message, notification.getPayload(), receivedMessage.getPayload());
//    }
//
//    @Test
//    public void testReg2ObserveOptionSequenceIsSetProperly() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//        timeKeys.next();
//        CoapMessage reg2notification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage reg2notification2 = receivedMessages.get(timeKeys.next());
//
//        Long observe1 = ((UintOption)reg2notification1.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//        Long observe2 = ((UintOption)reg2notification2.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
//
//        String message = String.format("ObserveOption sequence in second "
//                + "registration is not set properly (1st: %d, 2nd: %d)",
//                observe1, observe2);
//        assertTrue(message, observe1 < observe2);
//    }
//
//    @Test
//    public void testObserveOptions() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        //reg1 notification1
//        CoapMessage receivedMessage1 = receivedMessages.get(timeKeys.next());
//        //simple ack
//        CoapMessage receivedMessage2 = receivedMessages.get(timeKeys.next());
//        //reg2 notification1
//        CoapMessage receivedMessage3 = receivedMessages.get(timeKeys.next());
//        //reg2 notification2
//        CoapMessage receivedMessage4 = receivedMessages.get(timeKeys.next());
//
//
//        String message = "Observe Option should be set for reg1 notification1";
//        assertFalse(message, receivedMessage1.getOption(OBSERVE_RESPONSE).isEmpty());
//        message = "Observe Option should NOT be set for simple ack response";
//        assertTrue(message, receivedMessage2.getOption(OBSERVE_RESPONSE).isEmpty());
//        message = "Observe Option should be set for reg2 notification1";
//        assertFalse(message, receivedMessage3.getOption(OBSERVE_RESPONSE).isEmpty());
//        message = "Observe Option should be set for reg2 notification2";
//        assertFalse(message, receivedMessage4.getOption(OBSERVE_RESPONSE).isEmpty());
//    }
//
//    @Test
//    public void testFirstNotificationHasSameMsgID() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        CoapMessage receivedMessage = receivedMessages.get(receivedMessages.firstKey());
//        String message = "First notification Msg ID does not match with request Msg ID";
////        assertEquals(message, coapRequest.getMessageID(), receivedMessage.getMessageID());
//    }
//
//    @Test
//    public void testReg2NotificationsHaveTheSameToken() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        CoapMessage reg1notification1 = receivedMessages.get(timeKeys.next());
//        timeKeys.next();
//        CoapMessage reg2notification1 = receivedMessages.get(timeKeys.next());
//        CoapMessage reg2notification2 = receivedMessages.get(timeKeys.next());
//
//        String message = "At least one notification has a wrong token";
//        assertTrue(message, Arrays.equals(reg1Request.getToken(), reg1notification1.getToken()));
//        assertTrue(message, Arrays.equals(reg2Request.getToken(), reg2notification1.getToken()));
//        assertTrue(message, Arrays.equals(reg2Request.getToken(), reg2notification2.getToken()));
//    }

}
