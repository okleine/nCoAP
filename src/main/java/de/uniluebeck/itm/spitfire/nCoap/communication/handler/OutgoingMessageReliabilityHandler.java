package de.uniluebeck.itm.spitfire.nCoap.communication.handler;

import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.MessageIDFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.OpenOutgoingRequest;
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
    public static int RESPONSE_TIMEOUT = 2000;

    private static OutgoingMessageReliabilityHandler instance = new OutgoingMessageReliabilityHandler();
    private static Logger log = Logger.getLogger("nCoap");

    public final ConcurrentHashMap<Integer, OpenOutgoingRequest> openRequests = new ConcurrentHashMap<>();

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
                while(true){
                    Enumeration<OpenOutgoingRequest> requests;
                    synchronized(openRequests){
                       requests = openRequests.elements();
                    }

                    while(requests.hasMoreElements()){
                        OpenOutgoingRequest openRequest = requests.nextElement();

                        //Retransmit message if its time to do so
                        if(openRequest.getRetransmissionCount() ==  MAX_RETRANSMIT){
                            openRequests.remove(openRequest.getMessage().getHeader().getMsgID());

                            log.info("{OutgoingMessageReliabilityHandler] Message with ID " +
                                openRequest.getMessage().getHeader().getMsgID() + " has reached maximum number of " +
                                "retransmits. Deleted from list of open requests.");

                            break;
                        }

                        if(System.currentTimeMillis() >= openRequest.getNextTransmitTime()){

                            Channels.write(Main.channel, openRequest.getMessage(), openRequest.getRcptSocketAddress());
                            openRequest.increaseRetransmissionCount();
                            openRequest.setNextTransmitTime();

                            log.debug("[ConfirmableMessageRetransmitter] Retransmission no. " +
                                openRequest.getRetransmissionCount() + " for message with ID " +
                                openRequest.getMessage().getHeader().getMsgID() + " to " +
                                openRequest.getRcptSocketAddress());
                        }
                    }
                    //Sleeping for a millisecond causes a CPU load of 1% instead of 25% without sleep
                    try{
                        sleep(1);
                    } catch (InterruptedException e) {
                       log.fatal("[OutgoingMessageReliabilityHandler] This should never happen:\n" +
                               e.getStackTrace());
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
                if(openRequests.remove(message.getHeader().getMsgID()) != null) {
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

        synchronized (openRequests){
            if(message.getHeader().getMsgID() == 0 || (!openRequests.containsKey(message.getHeader().getMsgID()))){
                //Set the message ID
                final int messageId = MessageIDFactory.getInstance().nextMessageID();
                message.setMessageId(messageId);

                if(message.getHeader().getMsgType() == MsgType.CON){
                    OpenOutgoingRequest newOpenRequest =
                            new OpenOutgoingRequest((InetSocketAddress)me.getRemoteAddress(), message);

                    openRequests.put(messageId, newOpenRequest);

                    log.debug("[OutgoingMessageReliabilityHandler] New open request with message ID " + messageId);
                }
            }
        }

        ctx.sendDownstream(me);
        log.debug("[OutgoingMessageReliabilityHandler] Message with ID " + message.getHeader().getMsgID() + " sent.");
    }
}
