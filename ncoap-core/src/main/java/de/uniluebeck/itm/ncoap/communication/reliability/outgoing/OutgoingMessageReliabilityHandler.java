/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
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

import com.google.common.collect.*;
import de.uniluebeck.itm.ncoap.application.InternalApplicationShutdownMessage;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.communication.codec.InternalEncodingFailedMessage;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This handler deals with outgoing {@link CoapMessage}s with {@link MessageType.Name#CON}. It retransmits the outgoing
 * message in exponentially increasing intervals (up to {@link #MAX_RETRANSMISSIONS} times) until was no corresponding
 * message with {@link MessageType.Name#ACK} or {@link MessageType.Name#RST} received.
 *
 * To relate incoming with outgoing messages this is the handler to set the message ID of outgoing {@link CoapMessage}s
 * if the message ID was not already set previously.
 *
 * Furthermore, the {@link OutgoingMessageReliabilityHandler} changes the {@link OptionValue.Name#MAX_AGE} for
 * retransmitted message (according to the delay compared to the initial transmission attempt).
 *
 * If the outgoing message is an update notification, i.e. a {@link CoapResponse}s with
 * {@link CoapResponse#isUpdateNotification()} returning <code>true</code>, it checks whether there is an ongoing
 * {@link OutgoingReliableUpdateNotificationExchange}, i.e. a previous confirmable update notification was sent
 * but not yet acknowledged. If that is the case, it updates the {@link OutgoingReliableUpdateNotificationExchange}
 * in such a way that the retransmission schedule, i.e. the time of the next retransmission, remains unchanged but
 * the new {@link CoapResponse} (the update notification with the new content) is sent at that time. Furthermore,
 * the message ID of the previous transmissions becomes invalid, i.e. incoming acknowledgements are ignored.
 *
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler implements Observer{

    /**
     * The minimum number of milliseconds (2000) to wait for the first retransmit of an outgoing {@link CoapMessage}
     * with {@link MessageType.Name#CON}
     */
    public static final int ACK_TIMEOUT_MILLIS = 2000;

    /**
     * The factor (1.5) to be multiplied with {@link #ACK_TIMEOUT_MILLIS} to get the maximum number of milliseconds
     * (3000) to wait for the first retransmit of an outgoing {@link CoapMessage} with {@link MessageType.Name#CON}
     */
    public static final double ACK_RANDOM_FACTOR = 1.5;

    /**
     * The maximum number of retransmission of an outgoing {@link CoapMessage} with {@link MessageType.Name#CON}
     */
    public static final int MAX_RETRANSMISSIONS = 4;


    private static final int RETRANSMISSION_TASK_PERIOD_MILLIS = 100;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private final Object monitor = new Object();
    private HashBasedTable<InetSocketAddress, Integer, OutgoingMessageExchange> ongoingMessageExchanges;
    private TreeMultimap<Long, OutgoingReliableMessageExchange> retransmissionSchedule;

    private ChannelHandlerContext ctx;
    private boolean shutdown;

    private MessageIDFactory messageIDFactory;


    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the thread(s) to retransmit outgoing
     *                        {@link CoapMessage}s with {@link MessageType.Name#CON}
     */
    public OutgoingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.shutdown = false;
        this.messageIDFactory = new MessageIDFactory(executorService);
        this.ongoingMessageExchanges = HashBasedTable.create();
        this.retransmissionSchedule =
                TreeMultimap.create(Ordering.<Long>natural(), Ordering.<OutgoingMessageReliabilityHandler>arbitrary());

        //Schedule retransmission task
        executorService.scheduleAtFixedRate(
                new RetransmissionTask(),
                RETRANSMISSION_TASK_PERIOD_MILLIS, RETRANSMISSION_TASK_PERIOD_MILLIS, TimeUnit.MILLISECONDS
        );

        this.messageIDFactory.addObserver(this);
    }


    public void setChannelHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }


    private boolean isShutdown(){
        return this.shutdown;
    }

    /**
     * This method is invoked with a downstream message event. If it is a new message (i.e. to be
     * transmitted the first time) of type CON , it is added to the list of open requests waiting for a response.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception if an unexpected error occurred
     */
    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me) throws Exception{
        if(isShutdown())
            return;

        if(me.getMessage() instanceof CoapResponse && ((CoapResponse) me.getMessage()).isUpdateNotification())
            writeUpdateNotifiction(ctx, me);

        else if((me.getMessage() instanceof CoapMessage))
            writeCoapMessage(ctx, me);


        else if(me.getMessage() instanceof InternalApplicationShutdownMessage)
            handleApplicationShutdown(me.getFuture());

        else
            ctx.sendDownstream(me);
    }


    private void handleApplicationShutdown(ChannelFuture future) {
        this.messageIDFactory.shutdown();

        synchronized (monitor){
            this.shutdown = true;
            this.ongoingMessageExchanges.clear();
            this.retransmissionSchedule.clear();
        }

        future.setSuccess();
    }


    private void writeCoapMessage(ChannelHandlerContext ctx, MessageEvent me){

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        try{
            //Set message ID
            if(coapMessage.getMessageID() == CoapMessage.MESSAGE_ID_UNDEFINED){
                int messageID = messageIDFactory.getNextMessageID(remoteEndpoint);
                coapMessage.setMessageID(messageID);

                log.debug("Message ID set to " + coapMessage.getMessageID());

                if (coapMessage.getMessageTypeName() == MessageType.Name.CON)
                    ensureTransmissionReliability(coapMessage, remoteEndpoint);
            }

            ctx.sendDownstream(me);
        }
        catch (IllegalArgumentException | RetransmissionsAlreadyScheduledException e) {
            log.error("This should never happen.", e);
            me.getFuture().setFailure(e);
        }
        catch(NoMessageIDAvailableException e){
            e.setToken(coapMessage.getToken());
            me.getFuture().setFailure(e);
        }
    }


    private void ensureTransmissionReliability(CoapMessage coapMessage, InetSocketAddress remoteEndpoint)
            throws RetransmissionsAlreadyScheduledException {

        if(ongoingMessageExchanges.contains(remoteEndpoint, coapMessage.getMessageID())){
            log.error("Tried to to schedule retransmissions for already scheduled message: {}", coapMessage);
            throw new RetransmissionsAlreadyScheduledException(remoteEndpoint, coapMessage.getMessageID());
        }

        scheduleFirstRetransmission(coapMessage, remoteEndpoint);
    }


    private OutgoingReliableMessageExchange scheduleFirstRetransmission(CoapMessage coapMessage,
                                                                        InetSocketAddress remoteEndpoint)
            throws RetransmissionsAlreadyScheduledException {

        long firstRetransmissionTime = System.currentTimeMillis() + createNextRetransmissionDelay(0);
        OutgoingReliableMessageExchange messageExchange;

        if(coapMessage instanceof CoapResponse && ((CoapResponse) coapMessage).isUpdateNotification())
            messageExchange =
                    new OutgoingReliableUpdateNotificationExchange(remoteEndpoint, (CoapResponse) coapMessage);

        else
            messageExchange = new OutgoingReliableMessageExchange(remoteEndpoint, coapMessage);

        synchronized (monitor){
            if(ongoingMessageExchanges.contains(remoteEndpoint, coapMessage.getMessageID())){
                log.error("Tried to to schedule retransmissions for already scheduled message: {}", coapMessage);
                throw new RetransmissionsAlreadyScheduledException(remoteEndpoint, coapMessage.getMessageID());
            }

            retransmissionSchedule.put(firstRetransmissionTime, messageExchange);
            log.debug("Number of scheduled retransmissions: {}", retransmissionSchedule.size());
            ongoingMessageExchanges.put(remoteEndpoint, coapMessage.getMessageID(), messageExchange);
        }

        return messageExchange;
    }


    private void writeUpdateNotifiction(ChannelHandlerContext ctx, MessageEvent me){

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapResponse updateNotification = (CoapResponse) me.getMessage();

        log.debug("Write update notification: {}", updateNotification);

        //for CON update notifications update potentially running retransmissions
        if(updateNotification.getMessageTypeName() == MessageType.Name.CON){

            if (ongoingMessageExchanges.contains(remoteEndpoint, updateNotification.getMessageID()))
                    handleConfirmableUpdateNotification(ctx, me);

            else{
                updateNotification.setMessageID(CoapMessage.MESSAGE_ID_UNDEFINED);
                writeCoapMessage(ctx, me);
            }
        }

        //for NON update notifications stop potentially running retransmissions
        else{
            stopRetransmission(remoteEndpoint, updateNotification.getMessageID());

            updateNotification.setMessageID(CoapMessage.MESSAGE_ID_UNDEFINED);
            writeCoapMessage(ctx, me);
        }
    }


    private void handleConfirmableUpdateNotification(ChannelHandlerContext ctx, MessageEvent me){

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapResponse updateNotification = (CoapResponse) me.getMessage();

        int oldMessageID = updateNotification.getMessageID();

        log.debug("Try to update update notification with message ID {}", oldMessageID);

        if(ongoingMessageExchanges.contains(remoteEndpoint, oldMessageID)){
            synchronized (monitor){
                OutgoingMessageExchange messageExchange = ongoingMessageExchanges.get(remoteEndpoint, oldMessageID);

                if(messageExchange != null && messageExchange instanceof OutgoingReliableUpdateNotificationExchange){

                    OutgoingReliableUpdateNotificationExchange updateNotificationExchange =
                            (OutgoingReliableUpdateNotificationExchange) messageExchange;

                    updateNotificationExchange.setCoapMessage(updateNotification);
                    updateNotificationExchange.setChanged(true);

                    log.info("Updated update notification to retransmit: {}", updateNotification);
                }

                else{
                    updateNotification.setMessageID(CoapMessage.MESSAGE_ID_UNDEFINED);
                    writeCoapMessage(ctx, me);
                }
            }
        }

        else {
            updateNotification.setMessageID(CoapMessage.MESSAGE_ID_UNDEFINED);
            writeCoapMessage(ctx, me);
        }
    }


    private static long createNextRetransmissionDelay(int retransmissionNo){
        return (long)(Math.pow(2, retransmissionNo) * ACK_TIMEOUT_MILLIS *
                (1 + RANDOM.nextDouble() * (ACK_RANDOM_FACTOR - 1)) - RETRANSMISSION_TASK_PERIOD_MILLIS);
    }


    /**
     * This method is invoked with an upstream message event. If the message has one of the codes ACK or RESET it is
     * a response for a request waiting for a response. Thus the corresponding request is removed from
     * the list of open requests and the request will not be retransmitted anymore.
     *
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     *
     * @throws Exception if any unexpected error happens
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(isShutdown())
            return;

        log.debug("Upstream (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapMessage){
            handleCoapMessageReceived(ctx, me);
        }

        else if(me.getMessage() instanceof InternalEncodingFailedMessage)
            handleEncodingFailedMessage(ctx, me);

        else{
            ctx.sendUpstream(me);
        }
    }

    private void handleEncodingFailedMessage(ChannelHandlerContext ctx, MessageEvent me) {

        InetSocketAddress remoteEndpoint = ((InternalEncodingFailedMessage) me.getMessage()).getRemoteEndoint();
        int messageID = ((InternalEncodingFailedMessage) me.getMessage()).getMessageID();

        //Try to cancel open retransmissions (if any)
        stopRetransmission(remoteEndpoint, messageID);

        ctx.sendUpstream(me);
    }


    private void handleCoapMessageReceived(ChannelHandlerContext ctx, MessageEvent me){

        if(((CoapMessage) me.getMessage()).getMessageTypeName() == MessageType.Name.ACK)
            handleAcknowledgementReceived(ctx, me);

        else if(((CoapMessage) me.getMessage()).getMessageTypeName() == MessageType.Name.RST)
            handleResetReceived(ctx, me);

        else
            ctx.sendUpstream(me);
    }


    private void handleResetReceived(ChannelHandlerContext ctx, MessageEvent me){

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        //Try to cancel open retransmissions (method returns false if there was no open CON)
        OutgoingMessageExchange messageExchange = stopRetransmission(remoteEndpoint, coapMessage.getMessageID());

        if(messageExchange == null){
            log.error("No open CON found for incoming message: {}", coapMessage);
            return;
        }

        //Send internal message for empty RST reception
        InternalResetReceivedMessage internalMessage =
                new InternalResetReceivedMessage(remoteEndpoint, messageExchange .getMessageID(),
                        messageExchange.getToken());

        Channels.fireMessageReceived(ctx.getChannel(), internalMessage);
    }


    private void handleAcknowledgementReceived(ChannelHandlerContext ctx, MessageEvent me){
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        //Try to cancel open retransmissions (method returns false if there was no open CON)
        OutgoingMessageExchange messageExchange = stopRetransmission(remoteEndpoint, coapMessage.getMessageID());
        if(messageExchange == null){
            log.warn("No open CON found for incoming message: {}", coapMessage);
            return;
        }

        //Send internal message if the received ACK was empty
        if(coapMessage.getMessageCodeName() == MessageCode.Name.EMPTY){
            InternalEmptyAcknowledgementReceivedMessage internalMessage =
                    new InternalEmptyAcknowledgementReceivedMessage(
                            remoteEndpoint,
                            coapMessage.getMessageID(),
                            coapMessage.getToken());

            Channels.fireMessageReceived(ctx.getChannel(), internalMessage, remoteEndpoint);
        }

        //Forward received ACK upstream if it is not empty but a piggy-backed response
        else{
            ctx.sendUpstream(me);
        }

        //This can only happen if the previous retransmission of an update notification was acknowledged but the update
        //notification was changed due to a status update
        if(messageExchange instanceof OutgoingReliableUpdateNotificationExchange &&
                ((OutgoingReliableUpdateNotificationExchange) messageExchange).isChanged()){

                coapMessage.setMessageID(CoapMessage.MESSAGE_ID_UNDEFINED);
                Channels.write(this.ctx.getChannel(), coapMessage, messageExchange.getRemoteEndpoint());

        }

    }


    private OutgoingReliableMessageExchange stopRetransmission(InetSocketAddress remoteEndpoint, int messageID){

        OutgoingMessageExchange messageExchange = null;

        if(ongoingMessageExchanges.contains(remoteEndpoint, messageID)){
            synchronized(monitor){
                messageExchange = ongoingMessageExchanges.remove(remoteEndpoint, messageID);
            }
        }

        if(messageExchange == null || !(messageExchange instanceof OutgoingReliableMessageExchange)){
            return null;
        }
        else{
            ((OutgoingReliableMessageExchange) messageExchange).stopRetransmission();
            return (OutgoingReliableMessageExchange) messageExchange;
        }
    }


    @Override
    public void update(Observable o, Object arg) {
        Object[] args = (Object[]) arg;
        OutgoingReliableMessageExchange messageExchange =
                stopRetransmission((InetSocketAddress) args[0], (Integer) args[1]);

        if(messageExchange != null){
            InternalRetransmissionTimeoutMessage internalMessage = new InternalRetransmissionTimeoutMessage(
                    messageExchange.getRemoteEndpoint(),
                    messageExchange.getMessageID(),
                    messageExchange.getCoapMessage().getToken()
            );

            Channels.fireMessageReceived(this.ctx.getChannel(), internalMessage);
        }
    }


    private void retransmitMessage(OutgoingReliableMessageExchange messageExchange){

        final CoapMessage coapMessage = messageExchange.getCoapMessage();
        final InetSocketAddress remoteEndpoint = messageExchange.getRemoteEndpoint();

        ChannelFuture future = Channels.future(this.ctx.getChannel());

        Channels.write(this.ctx, future, coapMessage, remoteEndpoint);

        //Send an internal message after successful transmission
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    sendInternalRetransmissionMessage(remoteEndpoint, coapMessage.getToken(),
                            coapMessage.getMessageID());

                    log.info("Retransmitted message: {}", coapMessage);
                }
                else{
                    log.error("Error during message retransmission!", future.getCause());
                }
            }
        });

        messageExchange.increaseRetransmissionCount();
    }


    private void sendInternalRetransmissionMessage(InetSocketAddress remoteEndpoint, Token token, int messageID){
        InternalMessageRetransmittedMessage retransmissionMessage =
                new InternalMessageRetransmittedMessage(remoteEndpoint, token, messageID);

        Channels.fireMessageReceived(this.ctx.getChannel(), retransmissionMessage);
    }


    private class RetransmissionTask implements Runnable{
        @Override
        public void run() {
            try{
                long now = System.currentTimeMillis();

                synchronized (monitor){
                    //Iterator over the subset of retransmission that are due
                    Iterator<Map.Entry<Long, Collection<OutgoingReliableMessageExchange>>> dueRetransmissions =
                            retransmissionSchedule.asMap().headMap(now, true).entrySet().iterator();

                    //Temporary map of next retransmissions to be filled with the next retransmissions
                    Multimap<Long, OutgoingReliableMessageExchange> subsequentRetransmissions = HashMultimap.create();

                    while(dueRetransmissions.hasNext()){
                        Map.Entry<Long, Collection<OutgoingReliableMessageExchange>> part = dueRetransmissions.next();

                        for(OutgoingReliableMessageExchange messageExchange : part.getValue()){
                            if(!messageExchange.isRetransmissionStopped()){

                                if(messageExchange instanceof OutgoingReliableUpdateNotificationExchange){

                                    OutgoingReliableUpdateNotificationExchange updateNotificationExchange =
                                            (OutgoingReliableUpdateNotificationExchange) messageExchange;

                                    if(updateNotificationExchange.isChanged()){
                                        InetSocketAddress remoteEndpoint =
                                                updateNotificationExchange.getRemoteEndpoint();

                                        CoapResponse updateNotification = updateNotificationExchange.getCoapMessage();

                                        int oldMessageID = updateNotification.getMessageID();

                                        try{
                                            int newMessageID = messageIDFactory.getNextMessageID(remoteEndpoint);

                                            ongoingMessageExchanges.remove(remoteEndpoint, oldMessageID);

                                            updateNotification.setMessageID(newMessageID);

                                            ongoingMessageExchanges.put(remoteEndpoint, newMessageID,
                                                    updateNotificationExchange);

                                            updateNotificationExchange.setChanged(false);
                                        }

                                        catch(NoMessageIDAvailableException ex){
                                            log.warn("Could not update the update notification for {} (Reason: {})",
                                                    messageExchange.getRemoteEndpoint(), ex.getMessage());
                                        }
                                    }
                                }

                                retransmitMessage(messageExchange);

                                //Schedule next retransmission only if the maximum number was not reached
                                int counter = messageExchange.getRetransmissionCount();
                                if(counter < MAX_RETRANSMISSIONS){
                                    long delay = createNextRetransmissionDelay(counter);

                                    //Update the max age option of CoAP responses for next retransmission
                                    if(messageExchange.getCoapMessage() instanceof CoapResponse){
                                        CoapResponse coapResponse = (CoapResponse) messageExchange.getCoapMessage();
                                        coapResponse.setMaxAge(Math.max(coapResponse.getMaxAge() - delay / 1000, 0));
                                    }

                                    subsequentRetransmissions.put(part.getKey() + delay, messageExchange);
                                }
                            }

                            //if the retransmission was stopped then remove it from owing retransmissions
                            else{
                                ongoingMessageExchanges.remove(
                                        messageExchange.getRemoteEndpoint(),
                                        messageExchange.getCoapMessage().getMessageID()
                                );
                            }
                        }

                        dueRetransmissions.remove();
                    }

                    retransmissionSchedule.putAll(subsequentRetransmissions);
                }
            }
            catch (Exception e){
                log.error("Unexpected exception in retransmission task!", e);
            }
        }
    }

}
