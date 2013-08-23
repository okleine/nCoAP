package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.TestResponseProcessor;
import de.uniluebeck.itm.ncoap.application.client.TestResponseProcessorToRestartObservation;
import de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 22.08.13
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */
public class ObservationTimesOutWithRestart extends AbstractCoapCommunicationTest {

    private static CoapClientApplication coapClient;
    private static CoapRequest coapRequest;
    private static TestResponseProcessorToRestartObservation responseProcessor;

    private static CoapTestEndpoint coapEndpoint;
    private static CoapResponse updateNotification1;
    private static CoapResponse updateNotification2;
    private static CoapResponse updateNotification3;

    private static long MAX_AGE = 10;

    @Override
    public void setupComponents() throws Exception {

        coapEndpoint = new CoapTestEndpoint();

        coapClient = new CoapClientApplication();
        URI serviceUri = new URI("coap", null, "localhost", coapEndpoint.getPort(), "/observable", null, null);

        coapRequest = new CoapRequest(MsgType.CON, Code.GET, serviceUri);
        coapRequest.setObserveOptionRequest();

        responseProcessor = new TestResponseProcessorToRestartObservation(coapRequest);

        updateNotification1 = new CoapResponse(Code.CONTENT_205);
        updateNotification1.getHeader().setMsgType(MsgType.ACK);
        updateNotification1.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
        updateNotification1.setPayload("Status 1".getBytes(Charset.forName("UTF-8")));
        updateNotification1.setMaxAge(MAX_AGE);
        updateNotification1.setObserveOptionValue(1);

        updateNotification2 = new CoapResponse(Code.CONTENT_205);
        updateNotification2.getHeader().setMsgType(MsgType.CON);
        updateNotification2.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
        updateNotification2.setPayload("Status 2".getBytes(Charset.forName("UTF-8")));
        updateNotification2.setMaxAge(MAX_AGE);
        updateNotification2.setObserveOptionValue(2);

        updateNotification3 = new CoapResponse(Code.CONTENT_205);
        updateNotification3.getHeader().setMsgType(MsgType.ACK);
        updateNotification3.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
        updateNotification3.setPayload("Status 3".getBytes(Charset.forName("UTF-8")));
        updateNotification3.setMaxAge(MAX_AGE);
        updateNotification3.setObserveOptionValue(1);
    }

    @Override
    public void createTestScenario() throws Exception {

     /*
         testClient                    testEndpoint     DESCRIPTION
              |                             |
          (1) |--------GET (observe)------->|           client sends GET CON (observe) to testEndpoint
              |                             |
          (2) |<-------ACK-RESPONSE---------|           testEndpoint responds with ACK and observe #1
              |                             |
              |                             |           time passes (9 sec)
              |                             |
          (3) |<-------CON-RESPONSE---------|           testEndpoint responds with CON and observe #2
              |                             |
              |------- empty ACK ---------->|           testClient sends empty ACK
              |                             |
              |                             |           time passes (15 sec)
              |                             |           -> observation times out on client,
              |                             |
          (4) |<-------CON-RESPONSE---------|           testEndpoint responds with CON and observe #3
              |                             |
              |------- RST ---------------->|           testClient sends RST and does not process the
              |                             |           update notification!
    */


        //write CON request with observe option
        coapClient.writeCoapRequest(coapRequest, responseProcessor);
        Thread.sleep(500);

        //write first update notification (ACK)
        CoapRequest receivedRequest = (CoapRequest) coapEndpoint.getReceivedMessages().values().toArray()[0];
        updateNotification1.setMessageID(receivedRequest.getMessageID());
        updateNotification1.setToken(receivedRequest.getToken());

        coapEndpoint.writeMessage(updateNotification1, new InetSocketAddress("localhost", coapClient.getClientPort()));

        //Send update notification within max-age
        Thread.sleep(9000);
        updateNotification2.setMessageID(receivedRequest.getMessageID());
        updateNotification2.setToken(receivedRequest.getToken());

        coapEndpoint.writeMessage(updateNotification2, new InetSocketAddress("localhost", coapClient.getClientPort()));

        //Await observation timeout and restart of observation
        Thread.sleep(16000);

        CoapRequest receivedRequest2 = (CoapRequest) coapEndpoint.getReceivedMessages().values().toArray()[2];
        updateNotification3.setMessageID(receivedRequest2.getMessageID());
        updateNotification3.setToken(receivedRequest2.getToken());

        coapEndpoint.writeMessage(updateNotification3, new InetSocketAddress("localhost", coapClient.getClientPort()));

        Thread.sleep(1000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapEndpoint.shutdown();
        coapClient.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint")
                .setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.application.client").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.ObservationTimesOutWithoutRestart").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.incoming").setLevel(Level.DEBUG);
    }

    @Test
    public void testEndpointReceivedCorrectMessages(){
        Iterator<CoapMessage> receivedMessages =  coapEndpoint.getReceivedMessages().values().iterator();

        //First message is the request
        assertTrue(receivedMessages.next() instanceof CoapRequest);

        //2nd message is the ACK for the first update notification
        assertEquals(MsgType.ACK, receivedMessages.next().getMessageType());

        //3rd message is the CON for the second update notification, because the observation timed out.
        assertEquals(MsgType.CON, receivedMessages.next().getMessageType());

        //There should be no more messages!
        assertFalse("There should be no more messages!", receivedMessages.hasNext());
    }

    @Test
    public void testClientReceived3UpdateNotifications(){
        List<CoapResponse> updateNotifications = responseProcessor.getCoapResponses();

        //There should be 2 update notifications received
        assertEquals(3, updateNotifications.size());

        //First update notification is ACK
        assertTrue("First update notification should be an ACK",
                updateNotifications.get(0).getMessageType() == MsgType.ACK);

        //Second update notification is CON
        assertTrue("Second update notification should be CON",
                updateNotifications.get(1).getMessageType() == MsgType.CON);

        //Third update notification is ACK
        assertTrue("Second update notification should be ACK",
                updateNotifications.get(2).getMessageType() == MsgType.ACK);
    }

    @Test
    public void testObservationTimedOut(){
        assertTrue("Observation did not time out!", responseProcessor.isObservationTimedOut());
    }

}

