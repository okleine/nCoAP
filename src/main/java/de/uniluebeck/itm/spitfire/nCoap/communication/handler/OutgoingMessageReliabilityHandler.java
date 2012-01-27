package de.uniluebeck.itm.spitfire.nCoap.communication.handler;

import de.uniluebeck.itm.spitfire.nCoap.core.Main;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler {

    public static int MAX_RETRANSMIT = 4;
    private static OutgoingMessageReliabilityHandler instance = new OutgoingMessageReliabilityHandler();
    private static Logger log = Logger.getLogger("nCoap");


    private final ConcurrentHashMap<Integer, OpenRequest> openRequests = new ConcurrentHashMap<>();
    private int nextMessageId = 0;

    /**
     * Returns the one and only instance of class OutgoingMessageReliabilityHandler (Singleton)
     * @return the one and only instance
     */
    public static OutgoingMessageReliabilityHandler getInstance(){
        return instance;
    }

    private OutgoingMessageReliabilityHandler(){
        new Thread(){
            @Override
            public void run(){
                log.debug("Retransmission Thread (" + this.getId() + ") started!");
                while(true){
                    Enumeration<OpenRequest> requests;
                    synchronized (openRequests){
                        requests = openRequests.elements();
                    }

                    //Check if there are any more messages to retransmit
                    while(requests.hasMoreElements()){
                        OpenRequest openRequest = requests.nextElement();

                        if(System.currentTimeMillis() - openRequest.getLastTransmitTime() >=
                                Math.pow(2, openRequest.getTransmitCount()) * 1000){

                            Channels.write(Main.channel, openRequest.getMessage(), openRequest.getRcptAddress());

                            openRequest.setTransmitCount(openRequest.getTransmitCount() + 1);
                            openRequest.setLastTransmitTime(System.currentTimeMillis());

                            log.debug("[OutgoingMessageReliabilityHandler] " +
                                        "Attempt no. " + openRequest.getTransmitCount() + " to send message with ID " +
                                        openRequest.getMessage().getHeader().getMsgID() + " to " +
                                        openRequest.getRcptAddress());

                            if(openRequest.getTransmitCount() > MAX_RETRANSMIT){
                                log.debug("[OutgoingMessageReliabilityHandler] Message with id " +
                                        openRequest.getMessage().getHeader().getMsgID() + " removed after " +
                                        openRequest.getTransmitCount() + " transmits.");

                                synchronized (openRequests){
                                    openRequests.remove(openRequest.getMessage().getHeader().getMsgID());
                                }
                            }
                        }
                    }
                    try {
                        //Sleeping for a millisecond causes a CPU load of 1% instead of 25% without sleep
                        sleep(1);
                    } catch (InterruptedException e) {
                        log.fatal("[OutgoingMessageReliabilityHandler] This should never happen:\n" + e.getStackTrace());
                    }
                }
            }
        }.start();
    }

    /**
     * This method is invoked with an upstream message event. If the message has one of the codes ACK or RESET it is
     * most likely a response for a request waiting for a response. Thus the corresponding request is removed from
     * the list of open requests and the request will not be retransmitted anymore.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        Message message = (Message) me.getMessage();
        if(message.getHeader().getMsgType() == MsgType.ACK || message.getHeader().getMsgType() == MsgType.RST){
            synchronized (openRequests){
                if(openRequests.remove(message.getHeader().getMsgID()) != null){
                    log.debug("[OutgoingMessageReliabilityHandler] Received " + message.getHeader().getMsgType() +
                            " message for open request with message ID " + message.getHeader().getMsgID());
                }
            }
        }

        ctx.sendUpstream(me);

    }

    /**
     * This method is invoked with a downstream message event. If it is a new message (i.e. to be
     * transmitted the first time) of type CON , it is added to the list of open requests waiting for a response.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        Message message = (Message) me.getMessage();
        OpenRequest newOpenRequest = null;

        synchronized (openRequests){
            log.debug("Contained: " + openRequests.containsKey(message.getHeader().getMsgID()));

            if(message.getHeader().getMsgID() == 0 || (!openRequests.containsKey(message.getHeader().getMsgID()))){
                final int messageId = getNextMessageId();
                message.setMessageId(messageId);

                newOpenRequest =
                        new OpenRequest((InetSocketAddress)me.getRemoteAddress(), message, System.currentTimeMillis());

                openRequests.put(messageId, newOpenRequest);

                log.debug("[OutgoingMessageReliabilityHandler] New open request with message ID " + messageId);
            }
        }

        ctx.sendDownstream(me);
        log.debug("[OutgoingMessageReliabilityHandler] Message with ID " + message.getHeader().getMsgID() + " sent.");
    }

    //Returns the next message Id with range 1 to (2^16)-1
    private int getNextMessageId(){
        nextMessageId = (nextMessageId + 1) & 0x0000FFFF;
        synchronized (openRequests){
            while(openRequests.containsKey(nextMessageId) || nextMessageId == 0){
                nextMessageId = (nextMessageId + 1) & 0x0000FFFF;
            }
        }
        return nextMessageId;
    }

    private class OpenRequest{

        private InetSocketAddress rcptAddress;
        private Message message;
        private int transmitCount;

        private long lastTransmitTime;

        public OpenRequest(InetSocketAddress rcptAddress, Message message, long lastTransmitTime){
            this.rcptAddress = rcptAddress;
            this.message = message;
            this.transmitCount = 1;
            this.lastTransmitTime = lastTransmitTime;
        }

        public long getLastTransmitTime() {
            return lastTransmitTime;
        }

        public void setLastTransmitTime(long lastTransmitTime) {
            this.lastTransmitTime = lastTransmitTime;
        }

        public InetSocketAddress getRcptAddress() {
            return rcptAddress;
        }

        public void setRcptAddress(InetSocketAddress rcptAddress) {
            this.rcptAddress = rcptAddress;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public int getTransmitCount() {
            return transmitCount;
        }

        public void setTransmitCount(int transmitCount) {
            this.transmitCount = transmitCount;
        }



    }
}
