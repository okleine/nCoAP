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
import de.uniluebeck.itm.spitfire.nCoap.communication.observe.InternalUpdateNotificationRejectedMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler implements Observer {

    private static Logger log = LoggerFactory.getLogger(OutgoingMessageReliabilityHandler.class.getName());

    /**
     * The maximum number of retransmission attempts for outgoing {@link CoapMessage}s with {@link MsgType#CON}.
     */
    public static final int MAX_RETRANSMITS = 4;

    /**
     * The approximate number of milliseconds between the first transmission attempt for outgoing {@link CoapMessage}s
     * with {@link MsgType#CON} and the first retransmission attempt.
     */
    public static final int FIRST_RETRANSMISSION_DELAY = 2000;

    /**
     * The approximate number of milliseconds between the last retransmission attempt for outgoing {@link CoapMessage}s
     * with {@link MsgType#CON} and a timeout notification, i.e. invokation of
     * {@link RetransmissionTimeoutProcessor#processRetransmissionTimeout(RetransmissionTimeoutMessage)}.
     */
    public static final int TIMEOUT_MILLIS_AFTER_LAST_RETRANSMISSION = 5000;

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private ScheduledExecutorService executorService;

    //Contains remote socket address and message ID of not yet confirmed messages
    private final HashBasedTable<InetSocketAddress, Integer, RetransmissionSchedule> retransmissionSchedules
            = HashBasedTable.create();

    //Contains running observations (message ID, observer address, observed service path)
    private HashBasedTable<Integer, InetSocketAddress, String> observations = HashBasedTable.create();

    private MessageIDFactory messageIDFactory;

    public OutgoingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
        messageIDFactory = new MessageIDFactory(executorService);
        messageIDFactory.registerObserver(this);
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
        if(coapMessage.getMessageID() == Header.MESSAGE_ID_UNDEFINED){
            try {
                coapMessage.setMessageID(messageIDFactory.nextMessageID());
            } catch (InvalidHeaderException e) {
                log.error("This should never happen.", e);
            }
            log.info("Message ID set to " + coapMessage.getMessageID());
        }

        if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()){
            observations.put(coapMessage.getMessageID(), (InetSocketAddress) me.getRemoteAddress(),
                    ((CoapResponse) coapMessage).getServicePath());
        }

        if(coapMessage.getMessageType() == MsgType.CON){
            if (coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()) {
                //check all open CON messages to me.getObserverAddress() for retransmission with same token
                if (updateRetransmissions(coapMessage, (InetSocketAddress) me.getRemoteAddress())) {
                    log.info("Existing retransmission updated: {}.", coapMessage);
                    return;
                }
            }

            //schedule retransmissionSchedules
            if(!retransmissionSchedules.contains(me.getRemoteAddress(), coapMessage.getMessageID())){
                scheduleRetransmissions(ctx, (InetSocketAddress) me.getRemoteAddress(), coapMessage);
            }
            else{
                log.error("Retransmission already in progress for: {}.", coapMessage);
                return;
            }
        }

        ctx.sendDownstream(me);
    }


    private void scheduleRetransmissions(final ChannelHandlerContext ctx, final InetSocketAddress rcptAddress,
                                         final CoapMessage coapMessage){

        //Schedule retransmissionSchedules
        RetransmissionSchedule retransmissionSchedule = new RetransmissionSchedule(coapMessage);

        //Compute delays
        int[] delays = new int[MAX_RETRANSMITS];
        if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification()
                && ((CoapResponse) coapMessage).getMaxAge() > 30){
            long maxAge = ((CoapResponse) coapMessage).getMaxAge() * 1000;
            double maxExponent = Math.log(maxAge) / Math.log(2);
            for(int counter = MAX_RETRANSMITS; counter > 0; counter--){
                delays[counter - 1] = (int) Math.pow(2, maxExponent - MAX_RETRANSMITS + counter);
            }
        }
        else{
            int delay = 0;
            for(int counter = 0; counter < MAX_RETRANSMITS; counter++){
                delay += (int)(Math.pow(2, counter) * FIRST_RETRANSMISSION_DELAY * (1 + RANDOM.nextDouble() * 0.3));
                delays[counter] = delay;
            }
        }

        //Schedule retransmissionSchedules
        for(int counter = 0; counter < MAX_RETRANSMITS; counter++){
            MessageRetransmitter messageRetransmitter
                    = new MessageRetransmitter(ctx, rcptAddress, retransmissionSchedule, counter + 1);

            ScheduledFuture retransmissionFuture =
                    executorService.schedule(messageRetransmitter, delays[counter], TimeUnit.MILLISECONDS);

            if(retransmissionSchedule.addRetransmissionFuture(retransmissionFuture))
                log.debug("Scheduled in {} millis: {}", delays[counter], messageRetransmitter);
            else
                log.error("This should never happen! Could not add retransmission: {}", messageRetransmitter);
        }

        //Schedule timeout notification
        int delay = delays[MAX_RETRANSMITS - 1] + TIMEOUT_MILLIS_AFTER_LAST_RETRANSMISSION;

        ScheduledFuture timeoutNotificationFuture =
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    RetransmissionTimeoutMessage timeoutMessage =
                            new RetransmissionTimeoutMessage(coapMessage.getToken(), rcptAddress);

                    MessageEvent timeoutEvent = new UpstreamMessageEvent(ctx.getChannel(), timeoutMessage,
                            new InetSocketAddress(0));

                    log.info("Retransmission timeout for {}.", coapMessage);
                    ctx.sendUpstream(timeoutEvent);
                }
            }, delay, TimeUnit.MILLISECONDS);

        retransmissionSchedule.setTimeoutNotificationFuture(timeoutNotificationFuture);
        log.debug("Timeout notification scheduled in {} millis.", delay);

        //add new retransmission schedule
        synchronized (retransmissionSchedules){
            retransmissionSchedules.put(rcptAddress, coapMessage.getMessageID(), retransmissionSchedule);
        }
    }

    private synchronized boolean updateRetransmissions(CoapMessage coapMessage, InetSocketAddress remoteAddress) {
        for(int messageID : retransmissionSchedules.row(remoteAddress).keySet()){
            RetransmissionSchedule retransmissionSchedule = retransmissionSchedules.get(remoteAddress, messageID);
            if(Arrays.equals(retransmissionSchedule.getToken(), coapMessage.getToken())){
                retransmissionSchedule.setCoapMessage(coapMessage);

                retransmissionSchedules.remove(remoteAddress, messageID);
                retransmissionSchedules.put(remoteAddress, coapMessage.getMessageID(), retransmissionSchedule);

                return true;
            }
        }

        return false;
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
        InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

        if(coapMessage.getMessageType() == MsgType.RST){
            if(observations.contains(coapMessage.getMessageID(), me.getRemoteAddress())){
                String servicePath =
                        observations.get(coapMessage.getMessageID(), me.getRemoteAddress());

                InternalUpdateNotificationRejectedMessage message =
                        new InternalUpdateNotificationRejectedMessage((InetSocketAddress) me.getRemoteAddress(), servicePath);

                ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), message, new InetSocketAddress(0)));
            }
        }

       if (coapMessage.getMessageType() == MsgType.ACK || coapMessage.getMessageType() == MsgType.RST) {

            //Look up remaining retransmissionSchedules
            RetransmissionSchedule retransmissionSchedule;
            synchronized(retransmissionSchedules){
                retransmissionSchedule =
                        retransmissionSchedules.remove(me.getRemoteAddress(), coapMessage.getMessageID());
            }

            if(retransmissionSchedule != null){
                retransmissionSchedule.stopScheduledTasks();
                if(coapMessage.getCode() == Code.EMPTY){
                    if(coapMessage.getMessageType() == MsgType.ACK){

                        log.info("Empty ACK received for message ID " + coapMessage.getMessageID());

                        EmptyAcknowledgementReceivedMessage emptyAcknowledgementReceivedMessage =
                                new EmptyAcknowledgementReceivedMessage(retransmissionSchedule.getToken());

                        ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), emptyAcknowledgementReceivedMessage,
                                me.getRemoteAddress()));
                    }
                }
            }
            else {
                log.debug("No open CON found for messageID {} to {}. IGNORE.",
                        coapMessage.getMessageID(), remoteAddress);
                me.getFuture().setSuccess();
                return;
            }
        }

        ctx.sendUpstream(me);
    }




    @Override
    public void update(Observable o, Object arg) {
        if(!(o instanceof MessageIDFactory)){
            return;
        }

        Integer messageID = (Integer) arg;
        if(observations.rowKeySet().contains(messageID)){
            Map<InetSocketAddress, String> observer = observations.row(messageID);
            for(Map.Entry<InetSocketAddress, String> entry : observer.entrySet()){
                log.info("Observation of {} by {} cannot be stopped with RST {} anymore.",
                        new Object[]{entry.getValue(), entry.getKey(), messageID});

            }
        }
    }
}
