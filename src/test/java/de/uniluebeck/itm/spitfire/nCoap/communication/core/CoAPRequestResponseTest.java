package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageDecoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageEncoder;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


/**
 * Tests of CoAP request and response messages including reliability,
 * piggy-backed and separate response.
 * 
 * @author Stefan Hueske
 */
public class CoAPRequestResponseTest {
    private static final CoAPTestClient testClient = new CoAPTestClient();
    private static final CoAPTestReceiver testReceiver = new CoAPTestReceiver();
    
    //All tests are synchronized because JUnit tests could be
    //executed parallel. This is currently not supportet by the
    //nCoAP implementation.
    
    /**
     * Tests the timing of all retransmissions for a CON message.
     * Note: This test will take about 46 seconds.
     * Because of this I suggest to comment this test out
     * until modifying a reliability relevant class.
     */
    @Test 
    public synchronized void reliabilityRetransmissionTest() throws Exception {
        testClient.reset();
        testReceiver.reset();
        
        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, 
                new URI("coap://localhost:" + CoAPTestReceiver.PORT + "/testpath"));
        
        System.out.println("Sending request...");
        long firstRequestSendTime = System.currentTimeMillis();
        long tolerance = 700; //700ms margin of error for processing time
        testClient.writeCoapRequest(coapRequest);
        
        /* 
         * Retransmission intervals (RC = Retransmission Counter)
         * send CON message, set RC = 0
         * wait  2 - 3  sec then send retransmission, set RC = 1
         * wait  4 - 6  sec then send retransmission, set RC = 2
         * wait  8 - 12 sec then send retransmission, set RC = 3
         * wait 16 - 24 sec then send retransmission, set RC = 4
         * wait 32 - 48 sec then fail transmission
         * 
         * Resulting absolute intervals
         * 1st retransmission should be received after  2 - 3  sec
         * 2nd retransmission should be received after  6 - 9  sec
         * 3rd retransmission should be received after 14 - 21 sec
         * 4th retransmission should be received after 30 - 45 sec
         * 
         * (see http://tools.ietf.org/html/draft-ietf-core-coap-08#section-4.1)
         */
        
        Thread.sleep(45000 + tolerance); //see above

        testClient.disableReceiving();
        testReceiver.disableReceiving();
        System.out.println("Messages received:");
        int i = 0;
        for (ReceivedMessage<CoapMessage> msg: testReceiver.receivedMessages) {
            System.out.printf("Msg %d: Received after %6.3f seconds.%n", 
                    ++i, (msg.receivingTime - firstRequestSendTime) / 1000.0);
        }
        
        assertEquals("Unexpected response received by testClient",
                0, testClient.receivedResponses.size());
        //Expect 5 received messages, 1 org. request + 4 retransmissions
        assertEquals("testReceiver did not receive 5 messages",
                5, testReceiver.receivedMessages.size());
        
        
        //Check if receiving times match with the intervals above
        
        long timeDiff = testReceiver.receivedMessages.get(1).receivingTime - firstRequestSendTime;
        assertTrue("1st retransmission did not arrived in time.", 
                timeDiff > 2000 - tolerance &&
                timeDiff < 3000 + tolerance);
        
        timeDiff = testReceiver.receivedMessages.get(2).receivingTime - firstRequestSendTime;
        assertTrue("2nd retransmission did not arrived in time.", 
                timeDiff > 6000 - tolerance &&
                timeDiff < 9000 + tolerance);
        
        timeDiff = testReceiver.receivedMessages.get(3).receivingTime - firstRequestSendTime;
        assertTrue("3rd retransmission did not arrived in time.", 
                timeDiff > 14000 - tolerance &&
                timeDiff < 21000 + tolerance);
        
        timeDiff = testReceiver.receivedMessages.get(4).receivingTime - firstRequestSendTime;
        assertTrue("4th retransmission did not arrived in time.", 
                timeDiff > 30000 - tolerance &&
                timeDiff < 45000 + tolerance);
        
    }
}
class CoAPTestClient extends CoapClientApplication {
    public static final int PORT = CoapClientDatagramChannelFactory.COAP_CLIENT_PORT;
            
    //if false receivedResponses will not be modified
    boolean receivingEnabled = true;
    public List<ReceivedMessage<CoapResponse>> receivedResponses = 
            new LinkedList<ReceivedMessage<CoapResponse>>();
    
    @Override
    public void receiveCoapResponse(CoapResponse coapResponse) {
        if (receivingEnabled) {
            receivedResponses.add(new ReceivedMessage<CoapResponse>(coapResponse));
        }
    }
    
    public void enableReceiving() {
        receivingEnabled = true;
    }
    
    public void disableReceiving() {
        receivingEnabled = false;
    }
    
    public void reset() {
        receivedResponses.clear();
        enableReceiving();
    }
}

class CoAPTestReceiver extends SimpleChannelHandler {
    public static final int PORT = 30431;
    DatagramChannel channel;

    //if false receivedResponses will not be modified
    boolean receivingEnabled = true;
    public List<ReceivedMessage<CoapMessage>> receivedMessages = 
            new LinkedList<ReceivedMessage<CoapMessage>>();
    
    public CoAPTestReceiver() {
        final CoAPTestReceiver testReceiverInstance = this;
        //Create Datagram Channel
        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(Executors.newCachedThreadPool());

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("CoAP Message Encoder", new CoapMessageEncoder());
                pipeline.addLast("CoAP Message Decoder", new CoapMessageDecoder());
                pipeline.addLast("CoAP Test Receiver", testReceiverInstance);
                return pipeline;
            }
        });

        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(PORT));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof CoapMessage) {
            CoapMessage coapMessage = (CoapMessage) e.getMessage();
            if (receivingEnabled) {
                receivedMessages.add(new ReceivedMessage<CoapMessage>(coapMessage));
            }
        }
    }
    
    public void enableReceiving() {
        receivingEnabled = true;
    }
    
    public void disableReceiving() {
        receivingEnabled = false;
    }
    
    public void reset() {
        receivedMessages.clear();
        enableReceiving();
    }
}

class ReceivedMessage<T> {
    T message;
    long receivingTime;

    public ReceivedMessage(T message) {
        this.message = message;
        receivingTime = System.currentTimeMillis();
    }
}