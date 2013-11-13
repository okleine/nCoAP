/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.message.*;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
* This handler deals with outgoing {@link CoapMessage}s with {@link MessageType.Name#CON}. It retransmits the outgoing
* message in exponentially increasing intervals (up to {@link #MAX_RETRANSMIT} times) until was no corresponding
* message with {@link MessageType.Name#ACK} or {@link MessageType.Name#RST} received.
*
* To relate incoming with outgoing messages this is the handler to set the message ID of outgoing {@link CoapMessage}s
* if the message ID was not already set previously.
*
* @author Oliver Kleine
*/
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler {

    public static final int ACK_TIMEOUT_MILLIS = 2000;
    public static final double ACK_RANDOM_FACTOR = 1.5;
    public static final int MAX_RETRANSMIT = 4;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * The maximum number of retransmission attempts for outgoing {@link CoapMessage}s with {@link de.uniluebeck.itm.ncoap.message.MessageType#CON}.
     */
    //public static final int MAX_RETRANSMITS = 4;

    /**
     * The approximate number of milliseconds between the first transmission attempt for outgoing {@link CoapMessage}s
     * with {@link de.uniluebeck.itm.ncoap.message.MessageType#CON} and the first retransmission attempt.
     */
    //public static final int FIRST_RETRANSMISSION_DELAY = 2000;

    /**
     * The approximate number of milliseconds between the last retransmission attempt for outgoing {@link CoapMessage}s
     * with {@link MessageType.Name#CON} and a timeout notification.
     */
    public static final int TIMEOUT_MILLIS_AFTER_LAST_RETRANSMISSION = 5000;

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private ScheduledExecutorService executorService;

    //Contains remote socket address and message ID of not yet confirmed messages
    private final HashBasedTable<InetSocketAddress, Integer, ReliabilitySchedule> reliabilitySchedules
            = HashBasedTable.create();

    private MessageIDFactory messageIDFactory;

    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the thread(s) to retransmit outgoing
     *                        {@link CoapMessage}s with {@link MessageType.Name#CON}
     */
    public OutgoingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
        messageIDFactory = new MessageIDFactory(executorService);
    }

    /**
     * This method is invoked with a downstream message event. If it is a new message (i.e. to be
     * transmitted the first time) of type CON , it is added to the list of open requests waiting for a response.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception if an unexpected error occurred
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        log.debug("Downstream to {}: {}.", me.getRemoteAddress(), me.getMessage());

        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendDownstream(me);
            return;
        }

        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        //Set message ID
        if(coapMessage.getMessageID() == CoapMessage.MESSAGE_ID_UNDEFINED){
            try {
                coapMessage.setMessageID(messageIDFactory.nextMessageID());
            }
            catch (InvalidHeaderException e) {
                log.error("This should never happen.", e);
            }

            log.debug("Message ID set to " + coapMessage.getMessageID());
        }


        if(coapMessage.getMessageTypeName() == MessageType.Name.CON){
            //schedule retransmissionSchedules
            if(!reliabilitySchedules.contains(me.getRemoteAddress(), coapMessage.getMessageID())){
                scheduleRetransmissions(ctx, (InetSocketAddress) me.getRemoteAddress(), coapMessage);
            }
            else{
                log.error("Retransmission already in progress for: {}.", coapMessage);
                return;
            }
        }

        ctx.sendDownstream(me);
    }


    private void scheduleRetransmissions(final ChannelHandlerContext ctx, final InetSocketAddress remoteSocketAddress,
                                         final CoapMessage coapMessage){

        //ReliabilitySchedule retransmissionSchedule = new ReliabilitySchedule(coapMessage);


        //Compute delays for retransmission
        int[] delays = new int[MAX_RETRANSMIT];
        int delay = 0;
        for(int counter = 0; counter < MAX_RETRANSMIT; counter++){
            delay += (int)(Math.pow(2, counter) * ACK_TIMEOUT_MILLIS * (RANDOM.nextDouble() * ACK_RANDOM_FACTOR));
            delays[counter] = delay;
        }


        //Schedule retransmissionSchedules
        Set<ScheduledFuture> retransmissionFutures = new HashSet<>();
        for(int counter = 0; counter < MAX_RETRANSMIT; counter++){

            MessageRetransmission messageRetransmission
                    = new MessageRetransmission(ctx, coapMessage, remoteSocketAddress, counter + 1);

            retransmissionFutures.add(executorService.schedule(messageRetransmission,
                    delays[counter], TimeUnit.MILLISECONDS));

            log.debug("Scheduled in {} millis: {}", delays[counter], messageRetransmission);
        }


        //Schedule timeout notification
        delay = delays[MAX_RETRANSMIT - 1] + TIMEOUT_MILLIS_AFTER_LAST_RETRANSMISSION;

        ScheduledFuture timeoutNotificationFuture =
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    InternalRetransmissionTimeoutMessage timeoutMessage =
                            new InternalRetransmissionTimeoutMessage(coapMessage.getToken(), remoteSocketAddress);

                    MessageEvent timeoutEvent = new UpstreamMessageEvent(ctx.getChannel(), timeoutMessage,
                            new InetSocketAddress(0));

                    log.info("Retransmission timeout for {}.", coapMessage);
                    ctx.sendUpstream(timeoutEvent);
                }
            }, delay, TimeUnit.MILLISECONDS);

        log.debug("Timeout notification scheduled in {} millis.", delay);


        ReliabilitySchedule reliabilitySchedule =
                new ReliabilitySchedule(retransmissionFutures, timeoutNotificationFuture, coapMessage.getToken());

        synchronized (reliabilitySchedules){
            reliabilitySchedules.put(remoteSocketAddress, coapMessage.getMessageID(), reliabilitySchedule);
        }
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
        log.debug("Upstream (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(!(me.getMessage() instanceof CoapMessage)) {
            ctx.sendUpstream(me);
            return;
        }

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();

        if (coapMessage.getMessageTypeName() == MessageType.Name.ACK ||
                coapMessage.getMessageTypeName() == MessageType.Name.RST) {

            //Look up remaining retransmissionSchedules
            ReliabilitySchedule reliabilitySchedule;
            synchronized(reliabilitySchedules){
                reliabilitySchedule = reliabilitySchedules.remove(remoteSocketAddress, coapMessage.getMessageID());
            }

            reliabilitySchedule.cancelRemainingTasks();

            if(!(reliabilitySchedule == null)){
                if(coapMessage.getMessageCodeName() == MessageCode.Name.EMPTY){
                    if(coapMessage.getMessageTypeName() == MessageType.Name.ACK){

                        log.info("Received empty ACK for message ID {} from {}", coapMessage.getMessageID(),
                                remoteSocketAddress);

                        InternalEmptyAcknowledgementReceivedMessage emptyAcknowledgementReceivedMessage =
                                new InternalEmptyAcknowledgementReceivedMessage(reliabilitySchedule.getToken());

                        ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), emptyAcknowledgementReceivedMessage,
                                me.getRemoteAddress()));
                    }
                }
            }
            else {
                log.debug("No open CON found for messageID {} to {}. IGNORE.", coapMessage.getMessageID(),
                        remoteSocketAddress);
            }
        }

        ctx.sendUpstream(me);
    }




//    @Override
//    public void update(Observable o, Object arg) {
//        if(!(o instanceof MessageIDFactory)){
//            return;
//        }
//
//        Integer messageID = (Integer) arg;
//        if(observations.rowKeySet().contains(messageID)){
//            Map<InetSocketAddress, String> observer = observations.row(messageID);
//            for(Map.Entry<InetSocketAddress, String> entry : observer.entrySet()){
//                log.info("Observation of {} by {} cannot be stopped with RST {} anymore.",
//                        new Object[]{entry.getValue(), entry.getKey(), messageID});
//
//            }
//        }
//    }
}
