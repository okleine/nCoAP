/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.spitfire.nCoap.communication.reliability;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler {

    private static Logger log = Logger.getLogger(OutgoingMessageReliabilityHandler.class.getName());

    private static int TIMEOUT_MILLIS = 2000;
    private static int MAX_RETRANSMITS = 4;
    private static Random random = new Random(System.currentTimeMillis());


    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    //Contains remote socket address and message ID of not yet confirmed messages
    private final HashBasedTable<InetSocketAddress, Integer, byte[]> openOutgoingConMsg = HashBasedTable.create();
    private MessageIDFactory messageIDFactory = MessageIDFactory.getInstance();

    private static OutgoingMessageReliabilityHandler instance = new OutgoingMessageReliabilityHandler();

    /**
     * Returns the one and only instance of class OutgoingMessageReliabilityHandler (Singleton)
     * @return the one and only instance of class OutgoingMessageReliabilityHandle
     */
    public static OutgoingMessageReliabilityHandler getInstance(){
        return instance;
    }

    private OutgoingMessageReliabilityHandler(){
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

        if(me.getMessage() instanceof CoapMessage) {

            CoapMessage coapMessage = (CoapMessage) me.getMessage();
            InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

            if (coapMessage.getMessageType() == MsgType.ACK || coapMessage.getMessageType() == MsgType.RST) {

                if (log.isDebugEnabled()) {
                    log.debug("[OutgoingMessageReliabilityHandler] Incoming " + coapMessage.getMessageType() +
                            " message with ID " + coapMessage.getMessageID() + " from remote address " + remoteAddress);
                }

                byte[] removedToken;
                synchronized(openOutgoingConMsg){
                    removedToken = openOutgoingConMsg.remove(me.getRemoteAddress(), coapMessage.getMessageID());
                }

                if(removedToken != null){
                    if(log.isDebugEnabled()){
                        log.debug("[OutgoingMessageReliabilityHandler] Matching not yet confirmed message found (" +
                                " remote address: " + remoteAddress + ", message ID " + coapMessage.getMessageID() +
                                " ).");
                    }

                    //Do not send empty ACKs further upstream
                    if(coapMessage.getMessageType() == MsgType.ACK && coapMessage.getCode() == Code.EMPTY){
                        return;
                    }
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
        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendDownstream(me);
            return;
        }

        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        if(log.isDebugEnabled()){
            log.debug("[OutgoingMessageReliabilityHandler] Handle downstream event for message with ID " +
                coapMessage.getMessageID() + " for " + me.getRemoteAddress() );
        }

        if(coapMessage.getMessageID() == -1){

            coapMessage.setMessageID(messageIDFactory.nextMessageID());

            if(coapMessage.getMessageType() == MsgType.CON){
                if(!openOutgoingConMsg.contains(me.getRemoteAddress(), coapMessage.getMessageID())){

                    synchronized (openOutgoingConMsg){
                        openOutgoingConMsg.put((InetSocketAddress) me.getRemoteAddress(),
                                                           coapMessage.getMessageID(),
                                                           coapMessage.getToken());
                    }

                    //Schedule first retransmission
                    Retransmitter retransmitter
                            = new Retransmitter((InetSocketAddress) me.getRemoteAddress(), coapMessage);
                    int delay = (int) (TIMEOUT_MILLIS * retransmitter.randomFactor);
                    executorService.schedule(retransmitter, delay, TimeUnit.MILLISECONDS);

                    if(log.isDebugEnabled()){
                        log.debug("[OutgoingMessageReliabilityHandler] First retransmit for " +
                                coapMessage.getMessageType() + " message with ID " + coapMessage.getMessageID() +
                                " to be confirmed by " +  me.getRemoteAddress() + " scheduled with a delay of " +
                                delay + " millis.");
                    }
                }
            }
        }

        ctx.sendDownstream(me);
    }

    //Private class to handle the retransmission of confirmable message using a thread scheduler
    private class Retransmitter implements Runnable {

        private DatagramChannel datagramChannel = CoapClientDatagramChannelFactory.getInstance().getChannel();
        private InetSocketAddress rcptAddress;
        private CoapMessage coapMessage;
        private double randomFactor;
        private int retransmits;



        public Retransmitter(InetSocketAddress rcptAddress, CoapMessage coapMessage){
            this.rcptAddress = rcptAddress;
            this.coapMessage = coapMessage;
            this.randomFactor = 1 + random.nextDouble() * 0.5;
            this.retransmits = 0;
        }

        @Override
        public void run() {
            //Retransmit message if it's not yet confirmed
            if(openOutgoingConMsg.contains(rcptAddress, coapMessage.getMessageID())){

                //Remove message from the list of messages to be confirmed
                if(retransmits == MAX_RETRANSMITS){

                    byte[] removedToken;
                    synchronized (openOutgoingConMsg){
                        removedToken = openOutgoingConMsg.remove(rcptAddress, coapMessage.getMessageID());
                    }

                    if(log.isDebugEnabled()){
                        if(removedToken != null){
                            log.debug("[RetransmitionRunnable] Message with ID " + coapMessage.getMessageID() +
                                    " for recipient " + rcptAddress + " reached the maximum number of retransmits.");
                        }
                    }

                    //TODO let the application know about the timeout

                }
                else{
                    ChannelFuture future = Channels.future(datagramChannel);

                    Channels.write(datagramChannel.getPipeline().getContext("OutgoingMessageReliabilityHandler"),
                                   future,
                                   coapMessage,
                                   rcptAddress);

                    retransmits += 1;

                    if(log.isDebugEnabled()){
                        future.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                log.debug("[RetransmitionRunnable] Retransmit no " + retransmits + " for message " +
                                        "with ID " + coapMessage.getMessageID() + " for recipient " + rcptAddress +
                                        " finished.");
                            }
                        });
                    }

                    //Schedule the next retransmission, resp. removal from list of messages to be confirmed
                    int delay = (int)(Math.pow(2, retransmits) * TIMEOUT_MILLIS * randomFactor);
                    executorService.schedule(this, delay, TimeUnit.MILLISECONDS);

                    if(log.isDebugEnabled()){
                        if(retransmits + 1 <= MAX_RETRANSMITS){
                            log.debug("[RetransmitionRunnable] Retransmit no " + (retransmits + 1) + " for " +
                                    coapMessage.getMessageType() + " message with ID " + coapMessage.getMessageID() +
                                    " to be confirmed by " +  rcptAddress + " scheduled with a delay of " +
                                    delay + " millis.");
                        }
                        else{
                            log.debug("[RetransmitionRunnable] Removal of " + coapMessage.getMessageType() +
                                    " message with ID " + coapMessage.getMessageID() + " to be confirmed by " +
                                    rcptAddress + " from the list of messages to be confirmed scheduled with a delay " +
                                    "of " + delay + " millis.");
                        }
                    }
                }
            }
        }
    }
}
