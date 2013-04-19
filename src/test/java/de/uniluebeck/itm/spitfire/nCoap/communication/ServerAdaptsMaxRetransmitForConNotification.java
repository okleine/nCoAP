package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableDummyWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapMessageReceiver;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.SortedMap;

import static de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication.DEFAULT_COAP_SERVER_PORT;
import static de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest.testServer;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.assertEquals;
import static junit.framework.Assert.assertEquals;

/**
* Tests if the server adapts MAX_RETRANSMIT to avoid CON timeout before Max-Age ends.
* (Only for observe notifications)
*
* @author Stefan Hueske
*/
public class ServerAdaptsMaxRetransmitForConNotification extends AbstractCoapCommunicationTest {
    
    //registration requests
    private static CoapRequest regRequest;

    //notifications
    private static CoapResponse notification1;
    private static CoapResponse notification2;

    @Override
    public void createTestScenario() throws Exception {
        
        /*
            testReceiver               Server               Service
                 |                        |                    |
    (regRequest) |-----GET OBSERVE------->|                    |
                 |                        |                    |
 (notification1) |<----ACK OBSERVE--------|                    |
                 |                        |<-----UPDATE()------|
 (notification2) |<-CON Not.,MAX-AGE:100--|                    |
                 |                        |                    | |
                 |<----CON RETR.1---------|                    | |
                 |                        |                    | |
                 |<----CON RETR.2---------|                    | |
                 |                        |                    | |
                 |<----CON RETR.3---------|                    | | Time for 5th retransmission
                 |                        |                    | | < 130 seconds
                 |<----CON RETR.4---------|                    | |
                 |                        |                    | |
                 |<----CON RETR.5---------|                    | |
                 |                        |                    | |
                 |-----RST message------->|                    |
                 |                        |                    |
          (If the 5th retransmission exists, MAX_RETRANSMIT was adapted)
        */

        //create registration request
        String requestPath = "/testpath";
        URI targetUri = new URI("coap://localhost:" + testServer.getServerPort() + requestPath);
        regRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        regRequest.getHeader().setMsgID(12314);
        regRequest.setToken(new byte[]{0x12, 0x23, 0x34});
        regRequest.setObserveOptionRequest();
        
        //create notifications
        (notification1 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload1".getBytes("UTF-8"));
        (notification2 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload2".getBytes("UTF-8"));
        notification2.setMaxAge(100);

        //setup testServer
        ObservableDummyWebService observableDummyWebService = new ObservableDummyWebService(requestPath, true, 0, 0);
        observableDummyWebService.addPreparedResponses(notification1, notification2);
        registerObservableDummyService(observableDummyWebService);

        //run test sequence

        //registration
        testReceiver.writeMessage(regRequest, new InetSocketAddress("localhost", testServer.getServerPort()));
        //wait for response
        Thread.sleep(150);

        //first resource update
        observableDummyWebService.setResourceStatus(true);
        Thread.sleep(130*1000);

        //send RST message
        CoapResponse cancelRSTmsg = new CoapResponse(MsgType.RST, Code.EMPTY);
        cancelRSTmsg.setMessageID(notification2.getMessageID());
        testReceiver.writeMessage(cancelRSTmsg, new InetSocketAddress("localhost", testServer.getServerPort()));

        testReceiver.setReceiveEnabled(false);
    }

    @Test
    public void testReceiverReceived7Messages() {
        String message = "Receiver did not receive 7 messages";
        assertEquals(message, 7, testReceiver.getReceivedMessages().values().size());
    }

    @Test
    public void testLast6ConRetransmissions() {
         SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        timeKeys.next();

        for (int i = 1; i < 6; i++) {
            CoapMessage receivedMessage = receivedMessages.get(timeKeys.next());
            String message = "Notification Nr. " + i + "was not of type CON";
            assertEquals(message, MsgType.CON, receivedMessage.getMessageType());
            message = "Notification Nr. " + i + "has invalid message ID";
            assertEquals(message, notification2.getMessageID(), receivedMessage.getMessageID());
            message = "Notification Nr. " + i + "has invalid payload";
            assertEquals(message, notification2.getPayload(), receivedMessage.getPayload());
        }
    }
}
