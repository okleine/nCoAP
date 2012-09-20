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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(OutgoingMessageReliabilityHandler.class.getName());

    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(
            1,
            new ThreadFactoryBuilder().setNameFormat("OutgoingMessageReliability-Thread %d").build()
    );

    //Contains remote socket address and message ID of not yet confirmed messages

    private final HashBasedTable<InetSocketAddress, Integer, ScheduledFuture[]> waitingForACK = HashBasedTable.create();

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
        //private constructor to make it singleton
    }

    /**
     * This method is invoked with an upstream message event. If the message has one of the codes ACK or RESET it is
     * a response for a request waiting for a response. Thus the corresponding request is removed from
     * the list of open requests and the request will not be retransmitted anymore.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(!(me.getMessage() instanceof CoapMessage)) {
            ctx.sendUpstream(me);
            return;
        }

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

        log.debug("Incoming " + coapMessage.getMessageType() +
                " (MsgID " + coapMessage.getMessageID() +
                ", MsgHash " + coapMessage.hashCode() +
                ", Block " + coapMessage.getBlockNumber(OptionRegistry.OptionName.BLOCK_2) +
                ", Sender " + remoteAddress + ").");

        if (coapMessage.getMessageType() == MsgType.ACK || coapMessage.getMessageType() == MsgType.RST) {

            //Look up remaining retransmissions
            ScheduledFuture[] futures;
            synchronized(getClass()){
                futures =  waitingForACK.remove(me.getRemoteAddress(), coapMessage.getMessageID());
            }

            //Cancel remaining retransmissions
            if(futures != null){
                log.debug("Open CON found (MsgID " + coapMessage.getMessageID() +
                    ", Rcpt " + remoteAddress + "). CANCEL RETRANSMISSIONS!");

                int canceledRetransmissions = 0;
                for(ScheduledFuture future : futures){
                    if(future.cancel(false)){
                        canceledRetransmissions++;
                    }
                }

                log.debug(canceledRetransmissions + " retransmissions canceled for MsgID " +
                        coapMessage.getMessageID() + ".");
            }
            else{
                log.debug("No open CON found (MsgID " + coapMessage.getMessageID() +
                        ", Rcpt " + remoteAddress + "). IGNORE!");
                me.getFuture().setSuccess();
                return;
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

        log.debug("Outgoing " + coapMessage.getMessageType() + " (MsgID " + coapMessage.getMessageID() +
                ", MsgHash " + Integer.toHexString(coapMessage.hashCode()) + ", Rcpt " + me.getRemoteAddress() +
                ", Block " + coapMessage.getBlockNumber(OptionRegistry.OptionName.BLOCK_2) + ").");

        if(coapMessage.getMessageID() == -1){

            coapMessage.setMessageID(messageIDFactory.nextMessageID());

            log.debug("Set message ID " + coapMessage.getMessageID());
            if(coapMessage.getMessageType() == MsgType.CON){
                if(!waitingForACK.contains(me.getRemoteAddress(), coapMessage.getMessageID())){
                    MessageRetransmissionScheduler scheduler =
                            new MessageRetransmissionScheduler((InetSocketAddress) me.getRemoteAddress(), coapMessage);

                    executorService.schedule(scheduler, 0, TimeUnit.MILLISECONDS);
                }
            }
        }

        ctx.sendDownstream(me);
    }

    private class MessageRetransmissionScheduler implements Runnable{

        private final Random RANDOM = new Random(System.currentTimeMillis());
        private static final int MAX_RETRANSMITS = 4;
        private final int TIMEOUT_MILLIS = 2000;

        private InetSocketAddress rcptAddress;
        private CoapMessage coapMessage;

        public MessageRetransmissionScheduler(InetSocketAddress rcptAddress, CoapMessage coapMessage){
            this.rcptAddress = rcptAddress;
            this.coapMessage = coapMessage;
        }

        @Override
        public void run() {
            synchronized (OutgoingMessageReliabilityHandler.getInstance().getClass()){

                //Schedule retransmissions
                ScheduledFuture[] futures = new ScheduledFuture[MAX_RETRANSMITS];

                int delay = 0;
                for(int i = 0; i < MAX_RETRANSMITS; i++){
                    delay += (int)(Math.pow(2, i) * TIMEOUT_MILLIS * (1 + RANDOM.nextDouble() * 0.3));

                    MessageRetransmitter messageRetransmitter
                            = new MessageRetransmitter(rcptAddress, coapMessage, i+1);

                    futures[i] = executorService.schedule(messageRetransmitter, delay, TimeUnit.MILLISECONDS);

                    log.debug("Scheduled in " + delay + " millis {}", messageRetransmitter);
                }

                waitingForACK.put(rcptAddress, coapMessage.getMessageID(), futures);
            }
        }
    }
    private class MessageRetransmitter implements Runnable {

        private Logger log = LoggerFactory.getLogger(OutgoingMessageReliabilityHandler.class.getName());

        private final DatagramChannel DATAGRAM_CHANNEL = CoapClientDatagramChannelFactory.getInstance().getChannel();

        private InetSocketAddress rcptAddress;
        private CoapMessage coapMessage;
        private int retransmitNo;

        public MessageRetransmitter(InetSocketAddress rcptAddress, CoapMessage coapMessage, int retransmitNo){
            this.rcptAddress = rcptAddress;
            this.retransmitNo = retransmitNo;
            this.coapMessage = coapMessage;
        }

        @Override
        public void run() {
            log.info("BEGIN {}", this);

            synchronized (OutgoingMessageReliabilityHandler.getInstance().getClass()){

                ChannelFuture future = Channels.future(DATAGRAM_CHANNEL);

                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Retransmition completed {}", MessageRetransmitter.this);
                    }
                });

                Channels.write(DATAGRAM_CHANNEL.getPipeline().getContext("OutgoingMessageReliabilityHandler"),
                        future,
                        coapMessage,
                        rcptAddress);
            }
        }

        @Override
        public String toString() {
            try {
                return "MessageRetransmitter {" +
                        "retransmitNo " + retransmitNo +
                        ", MsgID " + coapMessage.getMessageID() +
                        ", MsgHash " + Integer.toHexString(coapMessage.hashCode()) +
                        ", Block " + coapMessage.getBlockNumber(OptionRegistry.OptionName.BLOCK_2) +
                        ", Hashcode " + Integer.toHexString(MessageRetransmitter.this.hashCode()) + "}";
            } catch (InvalidOptionException e) {
                return null;
            }
        }
    }
}
