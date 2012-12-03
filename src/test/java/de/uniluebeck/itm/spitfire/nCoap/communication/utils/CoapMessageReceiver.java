package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageDecoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageEncoder;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 30.11.12
 * Time: 17:18
 * To change this template use File | Settings | File Templates.
 */
public class CoapMessageReceiver extends SimpleChannelHandler {
    public static final int RECEIVER_PORT = 18954;
    private DatagramChannel channel;

    private static CoapMessageReceiver instance = new CoapMessageReceiver();

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //Received messages are ignored when set to false
    private boolean receiveEnabled = true;
    private boolean writeEnabled = true;
    private SortedMap<Long, CoapMessage> receivedMessages = new TreeMap<Long, CoapMessage>();
    
    //list of responses
    private LinkedList<MsgReceiverResponse> responsesToSend = new LinkedList<MsgReceiverResponse>();
    
    public static CoapMessageReceiver getInstance(){
        return instance;
    }
    
    private CoapMessageReceiver() {
        //Create datagram channel to receive messages
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

        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(RECEIVER_PORT));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if ((e.getMessage() instanceof CoapMessage) && receiveEnabled) {
            CoapMessage coapMessage = (CoapMessage) e.getMessage();
            log.info("CoAP message received.");
            receivedMessages.put(System.currentTimeMillis(), coapMessage);

            if(writeEnabled && (coapMessage instanceof CoapRequest)){
                MsgReceiverResponse responseToSend = responsesToSend.size() == 0 ? 
                        null : responsesToSend.removeFirst();
                
                if (responseToSend == null) {
                    throw new InternalError("Unexpected request received. No response for: " + coapMessage);
                }
                CoapResponse coapResponse = responseToSend.getCoapResponse();
                if (responseToSend.isLetReceiverSetMessageID()) {
                    coapResponse.setMessageID(coapMessage.getMessageID());
                }
                if (responseToSend.isLetReceiverSetToken()) {
                    coapResponse.setToken(coapMessage.getToken());
                }
                Channels.write(channel, coapResponse, e.getRemoteAddress());
            }
        }
    }

    public SortedMap<Long, CoapMessage> getReceivedMessages() {
        return receivedMessages;
    }

    public synchronized void setReceiveEnabled(boolean receiveEnabled) {
        this.receiveEnabled = receiveEnabled;
    }

    public synchronized void setWriteEnabled(boolean writeEnabled) {
        this.writeEnabled = writeEnabled;
    }

    public synchronized void reset() {
        receivedMessages.clear();
        setReceiveEnabled(true);
        setWriteEnabled(true);
    }

    public void writeMessage(CoapMessage coapMessage, InetSocketAddress remoteAddress){
        Channels.write(channel, coapMessage, remoteAddress);
    }

    public void addResponse(MsgReceiverResponse response) {
        responsesToSend.add(response);
    }
    
    /**
     * Shuts the client down by closing the channel which includes to unbind the channel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public void shutdown(){
        //Close the datagram channel (includes unbind)
        ChannelFuture future = channel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Channel closed.");

                channel.getFactory().releaseExternalResources();
                log.info("External resources released. Shutdown completed.");
            }
        });

        future.awaitUninterruptibly();
    }


//    public void blockUntilMessagesReceivedOrTimeout(long timeout, int messagesCount)
//            throws InterruptedException {
//        long startTime = System.currentTimeMillis();
//        while(System.currentTimeMillis() - startTime < timeout) {
//            synchronized(this) {
//                if (receivedMessages.size() >= messagesCount) {
//                    return;
//                }
//            }
//            Thread.sleep(50);
//        }
//    }
    
    public static class MsgReceiverResponse {
        private CoapResponse coapResponse;
        private boolean letReceiverSetMessageID;
        private boolean letReceiverSetToken;

        public MsgReceiverResponse(CoapResponse coapResponse, boolean letReceiverSetMessageID, boolean letReceiverSetToken) {
            this.coapResponse = coapResponse;
            this.letReceiverSetMessageID = letReceiverSetMessageID;
            this.letReceiverSetToken = letReceiverSetToken;
        }

        public CoapResponse getCoapResponse() {
            return coapResponse;
        }

        public boolean isLetReceiverSetMessageID() {
            return letReceiverSetMessageID;
        }

        public boolean isLetReceiverSetToken() {
            return letReceiverSetToken;
        }
    }   
}

