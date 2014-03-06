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
import de.uniluebeck.itm.ncoap.communication.observe.InternalStopUpdateNotificationRetransmissionMessage;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
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
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler implements Observer{

    public static final int ACK_TIMEOUT_MILLIS = 2000;
    public static final int MAX_RETRANSMIT = 4;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private ScheduledExecutorService executorService;

    //Contains remote socket address and message ID of not yet confirmed messages
    private HashBasedTable<InetSocketAddress, Integer, MessageRetransmission> owingRetransmissions;
    private TreeMultimap<Long, MessageRetransmission> retransmissionSchedule;

    private final Object monitor = new Object();

    private MessageIDFactory messageIDFactory;


    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the thread(s) to retransmit outgoing
     *                        {@link CoapMessage}s with {@link MessageType.Name#CON}
     */
    public OutgoingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
        this.messageIDFactory = new MessageIDFactory(executorService);
        this.owingRetransmissions = HashBasedTable.create();
        this.retransmissionSchedule =
                TreeMultimap.create(Ordering.<Long>natural(), Ordering.<MessageRetransmission>arbitrary());

        //Schedule retransmission task
        executorService.scheduleAtFixedRate(new Runnable(){

            @Override
            public void run() {
                long now = System.currentTimeMillis();

                synchronized (monitor){
                    Iterator<Map.Entry<Long, Collection<MessageRetransmission>>> actualRetransmissions =
                    retransmissionSchedule.asMap().headMap(now, true).entrySet().iterator();

                    Multimap<Long, MessageRetransmission> subsequentRetransmissions = HashMultimap.create();

                    while(actualRetransmissions.hasNext()){
                        Map.Entry<Long, Collection<MessageRetransmission>> part = actualRetransmissions.next();

                        for(MessageRetransmission retransmission : part.getValue()){
                            if(!retransmission.isStopped()){

                                final ChannelHandlerContext ctx = retransmission.getChannelHandlerContext();
                                final CoapMessage coapMessage = retransmission.getCoapMessage();
                                final InetSocketAddress remoteSocketAddress = retransmission.getRemoteSocketAddress();

                                ChannelFuture future =
                                        Channels.future(retransmission.getChannelHandlerContext().getChannel());

                                Channels.write(ctx, future, coapMessage, remoteSocketAddress);

                                //Create time for next retransmission
                                int counter = retransmission.increaseRetransmissionCount();
                                long delay = (long)(Math.pow(2, counter) * ACK_TIMEOUT_MILLIS *
                                        (1 + RANDOM.nextDouble() / 2));

                                subsequentRetransmissions.put(now + delay, retransmission);

                                future.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        if(future.isSuccess()){
                                            InternalMessageRetransmittedMessage internalMessage =
                                                    new InternalMessageRetransmittedMessage(remoteSocketAddress,
                                                            coapMessage.getToken());

                                            Channels.fireMessageReceived(ctx, internalMessage);
                                        }
                                        else{
                                            log.error("This should never happen!", future.getCause());
                                        }
                                    }
                                });
                            }

                            else{
                                owingRetransmissions.remove(
                                        retransmission.getRemoteSocketAddress(),
                                        retransmission.coapMessage.getMessageID()
                                );
                            }
                        }

                        actualRetransmissions.remove();
                    }

                    retransmissionSchedule.putAll(subsequentRetransmissions);
                }
            }

        }, 500, 500, TimeUnit.MILLISECONDS);
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

        if((me.getMessage() instanceof CoapMessage))
            writeCoapRequest(ctx, me);
        else
            ctx.sendDownstream(me);
    }


    private void writeCoapRequest(ChannelHandlerContext ctx, MessageEvent me){

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();

        try{
            //Set message ID
            if(coapMessage.getMessageID() == CoapMessage.MESSAGE_ID_UNDEFINED){
                int messageID = messageIDFactory.getNextMessageID(remoteSocketAddress);
                coapMessage.setMessageID(messageID);

                log.debug("Message ID set to " + coapMessage.getMessageID());

                if (coapMessage.getMessageTypeName() == MessageType.Name.CON)
                    ensureTransmissionReliability(ctx, coapMessage, remoteSocketAddress);
            }

            ctx.sendDownstream(me);
        }
        catch (InvalidHeaderException | RetransmissionsAlreadyScheduledException e) {
            log.error("This should never happen.", e);
            me.getFuture().setFailure(e);
        }
        catch(NoMessageIDAvailableException e){
            e.setToken(coapMessage.getToken());
            me.getFuture().setFailure(e);
        }
    }

    private long createNextRetransmissionDelay(int retransmissionNo){
        return (long)(Math.pow(2, retransmissionNo) * ACK_TIMEOUT_MILLIS * (1 + RANDOM.nextDouble() / 2));
    }


    private void ensureTransmissionReliability(ChannelHandlerContext ctx, CoapMessage coapMessage,
                                               InetSocketAddress remoteSocketAddress)
            throws RetransmissionsAlreadyScheduledException {

        if(owingRetransmissions.contains(remoteSocketAddress, coapMessage.getMessageID())){
            log.error("Tried to to schedule retransmissions for already scheduled message: {}", coapMessage);
            throw new RetransmissionsAlreadyScheduledException(remoteSocketAddress, coapMessage.getMessageID());
        }

        MessageRetransmission retransmission = scheduleFirstRetransmission(ctx, coapMessage, remoteSocketAddress);

        synchronized (monitor){
            owingRetransmissions.put(remoteSocketAddress, coapMessage.getMessageID(), retransmission);
        }
    }



    private MessageRetransmission scheduleFirstRetransmission(ChannelHandlerContext ctx, CoapMessage coapMessage,
                                             InetSocketAddress remoteSocketAddress)
            throws RetransmissionsAlreadyScheduledException {

        long firstRetransmissionTime = System.currentTimeMillis() + createNextRetransmissionDelay(1);
        MessageRetransmission retransmission = new MessageRetransmission(ctx, coapMessage, remoteSocketAddress);

        synchronized (monitor){
            if(owingRetransmissions.contains(remoteSocketAddress, coapMessage.getMessageID())){
                log.error("Tried to to schedule retransmissions for already scheduled message: {}", coapMessage);
                throw new RetransmissionsAlreadyScheduledException(remoteSocketAddress, coapMessage.getMessageID());
            }

            retransmissionSchedule.put(firstRetransmissionTime, retransmission);
        }

        return retransmission;
    }

    /**
     * This method is invoked with an upstream message event. If the message has one of the codes ACK or RESET it is
     * a response for a request waiting for a response. Thus the corresponding request is removed from
     * the list of open requests and the request will not be retransmitted anymore.
     *
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     *
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Upstream (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapMessage){
            handleCoapMessageReceived(ctx, me);
        }

        else if(me.getMessage() instanceof InternalStopUpdateNotificationRetransmissionMessage){
            stopRetransmission(
                    ((InternalStopUpdateNotificationRetransmissionMessage) me.getMessage()).getRemoteSocketAddress(),
                    ((InternalStopUpdateNotificationRetransmissionMessage) me.getMessage()).getMessageID()
            );
        }

        else{
            ctx.sendUpstream(me);
        }
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

        InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        //Try to cancel open retransmissions (method returns false if there was no open CON)
        if(stopRetransmission(remoteSocketAddress, coapMessage.getMessageID()) == null){
            log.error("No open CON found for incoming message: {}", coapMessage);
            return;
        }

        //Send internal message for empty RST reception
        InternalResetReceivedMessage internalMessage =
                new InternalResetReceivedMessage(remoteSocketAddress, coapMessage.getToken());

        Channels.fireMessageReceived(ctx, internalMessage);
    }


    private void handleAcknowledgementReceived(ChannelHandlerContext ctx, MessageEvent me){
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        //Try to cancel open retransmissions (method returns false if there was no open CON)
        if(stopRetransmission(remoteSocketAddress, coapMessage.getMessageID()) == null){
            log.error("No open CON found for incoming message: {}", coapMessage);
            return;
        }

        //Send internal message if the received ACK was empty
        if(coapMessage.getMessageCodeName() == MessageCode.Name.EMPTY){
            InternalEmptyAcknowledgementReceivedMessage internalMessage =
                    new InternalEmptyAcknowledgementReceivedMessage(remoteSocketAddress, coapMessage.getToken());

            Channels.fireMessageReceived(ctx, internalMessage, remoteSocketAddress);
        }

        //Forward received ACK upstream if it is not empty but a piggy-backed response
        else{
            ctx.sendUpstream(me);
        }
    }


    private MessageRetransmission stopRetransmission(InetSocketAddress remoteSocketAddress, int messageID){

        MessageRetransmission retransmission = null;

        if(owingRetransmissions.contains(remoteSocketAddress, messageID)){
            synchronized(monitor){
                retransmission = owingRetransmissions.get(remoteSocketAddress, messageID);
            }
        }

        if (retransmission != null)
            retransmission.stop();

        return retransmission;
    }


    @Override
    public void update(Observable o, Object arg) {
        Object[] args = (Object[]) arg;
        MessageRetransmission retransmission = stopRetransmission((InetSocketAddress) args[0], (Integer) args[1]);

        if(retransmission != null){
            InternalRetransmissionTimeoutMessage internalMessage = new InternalRetransmissionTimeoutMessage(
                    retransmission.getRemoteSocketAddress(),
                    retransmission.getCoapMessage().getToken()
            );

            Channels.fireMessageReceived(retransmission.getChannelHandlerContext(), internalMessage);
        }
    }


    private class MessageRetransmission{

        private ChannelHandlerContext ctx;
        private CoapMessage coapMessage;
        private InetSocketAddress remoteSocketAddress;
        private int retransmissionCount;
        private boolean stopped;


        public MessageRetransmission(ChannelHandlerContext ctx, CoapMessage coapMessage,
                                     InetSocketAddress remoteSocketAddress){
            this.ctx = ctx;
            this.coapMessage = coapMessage;
            this.remoteSocketAddress = remoteSocketAddress;
            this.stopped = false;
        }

        public ChannelHandlerContext getChannelHandlerContext(){
            return this.ctx;
        }

        public CoapMessage getCoapMessage(){
            return this.coapMessage;
        }

        public InetSocketAddress getRemoteSocketAddress() {
            return remoteSocketAddress;
        }

        public boolean isStopped() {
            return stopped;
        }

        public void stop(){
            this.stopped = true;
        }

        public int increaseRetransmissionCount(){
            return ++retransmissionCount;
        }

        @Override
        public String toString() {
            return "Retransmission no. " + retransmissionCount + " for " + coapMessage;
        }
    }
}
