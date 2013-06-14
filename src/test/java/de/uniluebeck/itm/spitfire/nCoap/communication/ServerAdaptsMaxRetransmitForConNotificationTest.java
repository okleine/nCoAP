package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
* Tests if the server adapts MAX_RETRANSMIT to avoid CON timeout before Max-Age ends.
* (Only for observe notifications)
*
* @author Stefan Hueske
*/
public class ServerAdaptsMaxRetransmitForConNotificationTest extends AbstractCoapCommunicationTest {

    //registration requests
    private static CoapRequest observationRequest;
    private static long observationRequestSent;


    @BeforeClass
    public static void setLogLevels(){
        Logger.getRootLogger().setLevel(Level.ERROR);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.reliability").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.observe").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver").setLevel(Level.DEBUG);
    }

    @Override
    public void createTestScenario() throws Exception {

        /*
               testEndpoint               Server               Service
                     |                        |                    |
(observationRequest) |-----GET OBSERVE------->|                    |
                     |                        |                    |
     (notification1) |<----ACK OBSERVE--------|                    |
                     |                        |<-----UPDATE()------|
     (notification2) |<-CON Not.,MAX-AGE:100--|                    |
                     |                        |                    | |
                     |<----CON RETR.1---------|                    | |
                     |                        |                    | |
                     |<----CON RETR.2---------|                    | |
                     |                        |                    | |
                     |<----CON RETR.3---------|                    | | Time for 4th retransmission
                     |                        |                    | | >= MAX-AGE
                     |<----CON RETR.4---------|                    | |

          (If the 5th retransmission exists, MAX_RETRANSMIT was adapted)
        */

        //Create observable service
        ObservableTestWebService webService = new ObservableTestWebService(OBSERVABLE_SERVICE_PATH, 0, 0);
        webService.setMaxAge(90);
        testServer.registerService(webService);
        webService.scheduleAutomaticStatusChange();

        //create observation request
        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + OBSERVABLE_SERVICE_PATH);
        observationRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        observationRequest.getHeader().setMsgID(12345);
        observationRequest.setToken(new byte[]{0x12, 0x23, 0x34});
        observationRequest.setObserveOptionRequest();

        //send observation request to server
        observationRequestSent = System.currentTimeMillis();
        testEndpoint.writeMessage(observationRequest, new InetSocketAddress("localhost", testServer.getServerPort()));

        //wait 2 seconds and update observed resource
        Thread.sleep(2000);
        webService.setResourceStatus(1);

        //wait for 2 retransmissions and change status then
        Thread.sleep(35000);
        webService.setResourceStatus(2);

        //wait another 65 seconds to finish the other 2 retransmissions
        Thread.sleep(65000);
        testEndpoint.setReceiveEnabled(false);
    }

    @Test
    public void testEndpointReceived6Messages() {
        String message = "Receiver did not receive 6 messages";
        assertEquals(message, 6, testEndpoint.getReceivedMessages().values().size());
    }

    @Test
    public void testFirstMessageIsAck(){
        CoapMessage firstMessage =
                testEndpoint.getReceivedMessages().get(testEndpoint.getReceivedMessages().firstKey());

        assertEquals("First received message was no ACK.", firstMessage.getMessageType(), MsgType.ACK);
        assertTrue("First message is no update notification.", ((CoapResponse) firstMessage).isUpdateNotification());
        assertTrue("Token does not match.", Arrays.equals(firstMessage.getToken(), observationRequest.getToken()));
    }

    @Test
    public void testLastMessageWasNotSentBeforeMaxAgeEnded(){
        long delay = testEndpoint.getReceivedMessages().lastKey() - observationRequestSent;

        assertTrue("Last retransmission was to early (after (" + delay +") millis. But max-age is 90 seconds!",
                delay >= 90000);
    }

//    @Test
//    public void testLast4ConRetransmissions() {
//        SortedMap<Long, CoapMessage> receivedMessages = testEndpoint.getReceivedMessages();
//        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
//        timeKeys.next();
//
//        for (int i = 1; i < 6; i++) {
//            CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
//            String message = "Notification Nr. " + i + "was not of type CON";
//            assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
//            message = "Notification Nr. " + i + "has invalid message ID";
//            assertEquals(message, notification2.getMessageID(), receivedMessage.getMessageID());
//            message = "Notification Nr. " + i + "has invalid payload";
//            assertEquals(message, notification2.getPayload(), receivedMessage.getPayload());
//        }
//    }
}
