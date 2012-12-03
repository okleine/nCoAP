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

package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapExecutorService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(OutgoingMessageReliabilityHandler.class.getName());

    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int MAX_RETRANSMITS = 4;
    private static final int INITIAL_TIMEOUT_MILLIS = 2000;
    //private static final int TIMEOUT_MILLIS_AFTER_LAST_RETRANSMISSION = 5000;

    //Contains remote socket address and message ID of not yet confirmed messages
    private final HashBasedTable<InetSocketAddress, Integer, LinkedList<ScheduledFuture>> waitingForACK
            = HashBasedTable.create();

//    private static OutgoingMessageReliabilityHandler instance = new OutgoingMessageReliabilityHandler();
//
//    /**
//     * Returns the one and only instance of class OutgoingMessageReliabilityHandler (Singleton)
//     * @return the one and only instance of class OutgoingMessageReliabilityHandle
//     */
//    public static OutgoingMessageReliabilityHandler getInstance(){
//        return instance;
//    }

//    private OutgoingMessageReliabilityHandler(){
//        //private constructor to make it singleton
//    }

    /**
     * This method is invoked with a downstream message event. If it is a new message (i.e. to be
     * transmitted the first time) of type CON , it is added to the list of open requests waiting for a response.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception if an unexpected error occurred
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendDownstream(me);
            return;
        }

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        log.debug("Outgoing: " + coapMessage);

        if(coapMessage.getMessageID() == Header.MESSAGE_ID_UNDEFINED){
            try {
                coapMessage.setMessageID(MessageIDFactory.nextMessageID());
            } catch (InvalidHeaderException e) {
                log.error("This should never happen.", e);
            }
            log.debug("Message ID set to " + coapMessage.getMessageID());
        }

        if(coapMessage.getMessageType() == MsgType.CON){
            if(!waitingForACK.contains(me.getRemoteAddress(), coapMessage.getMessageID())){
                scheduleRetransmissions(ctx, (InetSocketAddress) me.getRemoteAddress(), coapMessage);
            }
            else{
                log.error("Retransmission already in progress for: {}.", coapMessage);
                return;
            }
        }
        ctx.sendDownstream(me);
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

        log.debug("Incoming: " + coapMessage);

        if (coapMessage.getMessageType() == MsgType.ACK || coapMessage.getMessageType() == MsgType.RST) {

            //Look up remaining retransmissions
            LinkedList<ScheduledFuture> futures;
            synchronized(this){
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


    private void scheduleRetransmissions(final ChannelHandlerContext ctx, final InetSocketAddress rcptAddress,
                                         final CoapMessage coapMessage){

        //Schedule retransmissions
        LinkedList<ScheduledFuture> futures = new LinkedList<ScheduledFuture>();

        int delay = 0;
        for(int counter = 0; counter < MAX_RETRANSMITS; counter++){

            delay += (int)(Math.pow(2, counter) * INITIAL_TIMEOUT_MILLIS * (1 + RANDOM.nextDouble() * 0.3));

            MessageRetransmitter messageRetransmitter
                    = new MessageRetransmitter(ctx, rcptAddress, coapMessage, counter + 1);

            futures.add(CoapExecutorService.schedule(messageRetransmitter, delay, TimeUnit.MILLISECONDS));

            log.info("Scheduled in {} millis {}", delay, messageRetransmitter);
        }

        //Schedule timeout notification
        if(coapMessage.getToken().length > 0){
            delay += (int)(Math.pow(2, MAX_RETRANSMITS) * INITIAL_TIMEOUT_MILLIS * (1 + RANDOM.nextDouble() * 0.3));
            futures.add(CoapExecutorService.schedule(new Runnable() {
                @Override
                public void run(){
                    String message = "Could not deliver message dispite " + MAX_RETRANSMITS + " retransmitions.";
                    Throwable cause = new RetransmissionTimeoutException(coapMessage.getToken(), rcptAddress, message);
                    ExceptionEvent event = new DefaultExceptionEvent(ctx.getChannel(), cause);

                    log.info("Retransmission timed out. Send timeout message to client.");
                    ctx.sendUpstream(event);
                }
            }, delay, TimeUnit.MILLISECONDS));

            log.info("Timeout notification scheduled in {} millis.", delay);
        }
        synchronized (this){
            waitingForACK.put(rcptAddress, coapMessage.getMessageID(), futures);
        }
    }
}
