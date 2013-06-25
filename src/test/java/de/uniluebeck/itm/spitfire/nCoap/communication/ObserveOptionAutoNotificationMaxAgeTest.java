package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_RESPONSE;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.assertEquals;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


/**
* Tests if the server sends a new notification when Max-Age ends.
*
* @author Stefan Hueske, Oliver Kleine
*/
public class ObserveOptionAutoNotificationMaxAgeTest extends AbstractCoapCommunicationTest{

    private static String PATH_TO_SERVICE = "/observable";

    //registration requests
    private static CoapRequest request;

    private static CoapTestEndpoint endpoint;
    private static CoapServerApplication server;
    private static ObservableTestWebService service;

    private ScheduledExecutorService executorService;

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.observe")
              .setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint")
              .setLevel(Level.DEBUG);
    }

    @Override
    public void setupComponents() throws Exception {
        server = new CoapServerApplication();
        service = new ObservableTestWebService(PATH_TO_SERVICE, 1, 30);
        service.setMaxAge(5);

        server.registerService(service);

        endpoint = new CoapTestEndpoint();

        URI targetUri = new URI("coap://localhost:" + server.getServerPort() + PATH_TO_SERVICE);
        request = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        request.getHeader().setMsgID(1111);
        request.setToken(new byte[]{0x13, 0x53, 0x34});
        request.setObserveOptionRequest();

        executorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void shutdownComponents() throws Exception {
        server.shutdown();
        Thread.sleep(1000);
        endpoint.shutdown();
    }


    @Override
    public void createTestScenario() throws Exception {

//             testEndpoint                    Server      DESCRIPTION
//                  |                             |
//              (1) |--------GET_OBSERVE--------->|        Register observer
//                  |                             |
//              (2) |<-------1st Notification-----|        Receive first notification
//                  |                             | |
//                  |                             | | 5 seconds until max-age ends
//                  |                             | |
//              (3) |<-------2nd Notification-----|        Auto notification should be send by the server
//                  |                             |        before max-age ends
//              (4) |--------RST----------------->|
//                  |                             |
//                  |                             |

        executorService.schedule(new Runnable(){
            @Override
            public void run() {
                endpoint.writeMessage(request, new InetSocketAddress("localhost", server.getServerPort()));
            }
        }, 0, TimeUnit.MILLISECONDS);


        //wait for Max-Age to end and resulting notification
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                int messageID = endpoint.getReceivedMessages().get(endpoint.getReceivedMessages().lastKey()).getMessageID();
                endpoint.writeMessage(CoapMessage.createEmptyReset(messageID),
                        new InetSocketAddress("localhost", server.getServerPort()));
            }
        }, 5500, TimeUnit.MILLISECONDS);

        Thread.sleep(6000);
    }


    @Test
    public void testReceiverReceived2Messages() {
        String message = "Receiver did not receive 2 messages";
        assertEquals(message, 2, endpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testReceivedMessageArrivedIn2secDelay() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        Long msg1time = timeKeys.next();
        Long msg2time = timeKeys.next();
        long delay = msg2time - msg1time;

        String message = "Scheduled Max-Age notification did not arrive after 5 seconds but after " + delay;
        assertTrue(message, Math.abs(5000 - delay) < 200); //200ms tolerance
    }

    @Test
    public void testObserveOptionIsSetProperly() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());

        Long notifiationReceivedTime1 =
                ((UintOption)recNotification1.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
        Long notifiationReceivedTime2 =
                ((UintOption)recNotification2.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();

        String message = String.format("ObserveOption sequence is not set properly (1st: %d, 2nd: %d)",
                notifiationReceivedTime1, notifiationReceivedTime2);
        assertTrue(message, notifiationReceivedTime1 < notifiationReceivedTime2);
    }

    @Test
    public void testMessageType() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());

        String message = "1st notification should be ACK";
        assertEquals(message, MsgType.ACK, recNotification1.getMessageType());
        message = "2nd notification should be CON";
        assertEquals(message, MsgType.CON, recNotification2.getMessageType());
    }

    @Test
    public void testMessagePayload() {
        SortedMap<Long, CoapMessage> receivedMessages = endpoint.getReceivedMessages();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        CoapMessage recNotification1 = receivedMessages.get(timeKeys.next());
        CoapMessage recNotification2 = receivedMessages.get(timeKeys.next());

        String message = "1st notifications payload does not match";
        assertEquals(message, "Status #1", new String(recNotification1.getPayload().toString(Charset.forName("UTF-8"))));
        message = "2nd notifications payload does not match";
        assertEquals(message, "Status #1", new String(recNotification2.getPayload().toString(Charset.forName("UTF-8"))));
    }
}
