package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapMessageReceiver;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.*;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
import static de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory.*;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;

/**
 * During a retransmission of a notification its observe option sequence number is incremented.
 * This incrementation takes place in the OutgoingMessageReliabilityHandler.
 * Future new notifications will be send from the ObservableHandler,
 * thus their usage of sequence numbers must be synchronized.
 * 
 * @author Stefan Hueske
 */
public class ObserveOptionSequenceNumberSyncTest {
    private static CoapTestServer testServer = CoapTestServer.getInstance();
    private static CoapMessageReceiver testReceiver = CoapMessageReceiver.getInstance();

    //registration requests
    private static CoapRequest regRequest;    
    
    //notifications
    private static CoapResponse notification1;
    private static CoapResponse notification2;
    private static CoapResponse notification3;
    
    @BeforeClass
    public static void init() throws Exception {
        //init
        testReceiver.reset();
        testServer.reset();
        testReceiver.setReceiveEnabled(true);
        
        /*
            testReceiver               Server               Service
                 |                        |                    |
    (regRequest) |-----GET OBSERVE------->|                    |
                 |                        |                    |
 (notification1) |<----ACK OBSERVE--------|                    |
                 |                        |<-----UPDATE()------|  
 (notification2) |<----CON OBSERVE--------|                    |  
                 |                        |                    | | max 3 seconds
                 |<----CON RETR.1---------|                    | 
                 |                        |                    |
      (emptyACK) |-----EMPTY ACK--------->|                    |
                 |                        |<-----UPDATE()------|
 (notification3) |<----CON OBSERVE--------|                    | 
                 |                        |                    |
                 |-----RST message------->|                    |
                 |                        |                    |
        */
        
        //create registration request
        String requestPath = "/testpath";
        URI targetUri = new URI("coap://localhost:" + 
                CoapServerDatagramChannelFactory.COAP_SERVER_PORT + requestPath);
        regRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
        regRequest.getHeader().setMsgID(12315);
        regRequest.setToken(new byte[]{0x12, 0x23, 0x34});
        regRequest.setObserveOptionRequest();
        
        //create notifications
        (notification1 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload1".getBytes("UTF-8"));
        (notification2 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload2".getBytes("UTF-8"));
        (notification3 = new CoapResponse(Code.CONTENT_205)).setPayload("testpayload3".getBytes("UTF-8"));
        
        //setup testServer
        testServer.registerDummyService(requestPath);
        //if both cancellations fail, the notification will be send six times
        testServer.addResponse(notification1, notification2, notification3);
        
        
        //run test sequence
        
        //registration
        testReceiver.writeMessage(regRequest, new InetSocketAddress("localhost", COAP_SERVER_PORT));
        //wait for response        
        Thread.sleep(150);
        
        //first resource update
        testServer.notifyCoapObservers();
        Thread.sleep(3000);
        
        //send empty ACK after first CON retransmission
        CoapResponse emptyACK = new CoapResponse(Code.EMPTY);
        emptyACK.setMessageID(notification2.getMessageID());
        emptyACK.getHeader().setMsgType(MsgType.ACK);
        testReceiver.writeMessage(emptyACK, new InetSocketAddress("localhost", 
                CoapServerDatagramChannelFactory.COAP_SERVER_PORT));
        //wait for empty ACK to arrive
        Thread.sleep(300);
        
        //second resource update
        testServer.notifyCoapObservers();
        Thread.sleep(150);
        
        //send RST message
        CoapResponse cancelRSTmsg = new CoapResponse(MsgType.RST, Code.EMPTY);
        cancelRSTmsg.setMessageID(notification3.getMessageID());
        testReceiver.writeMessage(cancelRSTmsg, new InetSocketAddress("localhost", COAP_SERVER_PORT));
        
        testReceiver.setReceiveEnabled(false);
    }
    
    @Test
    public void testReceiverReceived4Messages() {
        String message = "Receiver did not receive 4 messages";
        assertEquals(message, 4, testReceiver.getReceivedMessages().values().size());
    }
    
    @Test
    public void squenceNumbersAreSynchronized() {
        SortedMap<Long, CoapMessage> receivedMessages = testReceiver.getReceivedMessages();
        Iterator<Long> timeKeys = receivedMessages.keySet().iterator();
        timeKeys.next();
        timeKeys.next();
        CoapMessage receivedMessage3 = receivedMessages.get(timeKeys.next());
        CoapMessage receivedMessage4 = receivedMessages.get(timeKeys.next());
        
        Long observe3 = ((UintOption)receivedMessage3.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();       
        Long observe4 = ((UintOption)receivedMessage4.getOption(OBSERVE_RESPONSE).get(0)).getDecodedValue();
        
        String message = String.format("Observe Option sequence number of the last notification must be "
                + "greater than the number of the ealier CON retransmission. observe4: %d, observe3: %d",
                observe4, observe3);
        assertTrue(message, observe4 > observe3);
    }
    
}
