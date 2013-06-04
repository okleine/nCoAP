package de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver;

import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageDecoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageEncoder;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;


/**
* Receives and sends CoAP Messages for testing purposes.
* Receiving and automatic response can be configured.
* To send a message either schedule a automatic response using addMessageToOutgoingQueue (using setWriteEnabled(true))
* or send a message manually using writeMessage (which will send the message immediately regardless of writeEnabled).
*
* @author Oliver Kleine, Stefan Hueske
*/
public class CoapMessageReceiver extends SimpleChannelHandler {

    private DatagramChannel channel;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //Received messages are ignored when set to false
    private boolean receiveEnabled = true;
    private boolean writeEnabled = true;

    //map to save received messages
    private SortedMap<Long, CoapMessage> receivedMessages = new TreeMap<Long, CoapMessage>();

    //contains a list of test specific messages to be sent
    private LinkedList<MessageReceiverResponse> outgoingMessageQueue = new LinkedList<MessageReceiverResponse>();

    public CoapMessageReceiver() {
        //Create datagram datagramChannel to receive messages
        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(Executors.newCachedThreadPool());

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("Encoder", new CoapMessageEncoder());
                pipeline.addLast("Decoder", new CoapMessageDecoder());
                pipeline.addLast("CoAP Message Receiver", CoapMessageReceiver.this );
                return pipeline;
            }
        });

        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(0));
        log.info("New message receiver channel created for port " + channel.getLocalAddress().getPort());
    }

    public int getReceiverPort(){
        return channel.getLocalAddress().getPort();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if ((e.getMessage() instanceof CoapMessage) && receiveEnabled) {
            CoapMessage coapMessage = (CoapMessage) e.getMessage();
            receivedMessages.put(System.currentTimeMillis(), coapMessage);

            log.info("Incoming #{} (from {}): {}.",
                    new Object[]{getReceivedMessages().size(), e.getRemoteAddress(), coapMessage});
        }
    }

    public SortedMap<Long, CoapMessage> getReceivedMessages() {
        return receivedMessages;
    }

//    public LinkedList<MessageReceiverResponse> getOutgoingMessageQueue(){
//        return outgoingMessageQueue;
//    }

    public synchronized void setReceiveEnabled(boolean receiveEnabled) {
        this.receiveEnabled = receiveEnabled;
    }

    public synchronized void setWriteEnabled(boolean writeEnabled) {
        this.writeEnabled = writeEnabled;
    }

//    public synchronized void reset() {
//        receivedMessages.clear();
//        outgoingMessageQueue.clear();
//        setReceiveEnabled(true);
//        setWriteEnabled(true);
//    }

    public void writeMessage(CoapMessage coapMessage, InetSocketAddress remoteAddress) {
        if(!writeEnabled){
            log.error("Write is disabled!");
            return;
        }

        log.info("Write message: " + coapMessage);
        Channels.write(channel, coapMessage, remoteAddress);
    }

//    public void writeNextMessage(InetSocketAddress remoteAddress){
//        if(!writeEnabled)
//            fail("Write is disabled!");
//        CoapMessage coapMessage = outgoingMessageQueue.poll();
//        log.info("Write message: {}", coapMessage);
//        Channels.write(channel, coapMessage, remoteAddress);
//    }
//
//
//    public void addMessageToOutgoingQueue(MessageReceiverResponse response) {
//        outgoingMessageQueue.add(response);
//    }

    /**
     * Shuts the client down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public void shutdown(){
        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture future = channel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                DatagramChannel closedChannel = (DatagramChannel) future.getChannel();
                log.info("Message receiver channel closed (port: " + closedChannel.getLocalAddress().getPort() + ").");

                channel.getFactory().releaseExternalResources();
                log.info("External resources released. Shutdown completed.");
            }
        });

        future.awaitUninterruptibly();
    }
}

