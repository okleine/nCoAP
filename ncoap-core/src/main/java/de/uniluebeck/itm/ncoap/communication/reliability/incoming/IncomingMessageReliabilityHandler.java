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
package de.uniluebeck.itm.ncoap.communication.reliability.incoming;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import de.uniluebeck.itm.ncoap.application.InternalApplicationShutdownMessage;
import de.uniluebeck.itm.ncoap.message.*;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
* This class is the first {@link ChannelUpstreamHandler} to deal with incoming decoded {@link CoapMessage}s. If the
* incoming message is a confirmable {@link CoapRequest} it schedules the sending of an empty acknowledgement to the
* sender if there wasn't a piggy-backed response within a period of 2 seconds.
*
* If the incoming message is a confirmable {@link CoapResponse} it immediately sends a proper acknowledgement if there
* was an open request waiting for a seperate response or update-notification. It immediately sends an RST
* if there was no such response expected.
*
* @author Oliver Kleine
*/
public class IncomingMessageReliabilityHandler extends SimpleChannelHandler {

    public static final int MIN_EMPTY_ACK_DELAY_MILLIS = 1700;
    public static final int RELIABILITY_TASK_PERIOD_MILLIS = 100;

    private static Logger log = LoggerFactory.getLogger(IncomingMessageReliabilityHandler.class.getName());

    private final Object monitor = new Object();
    private TreeMultimap<Long, IncomingReliableMessageExchange> emptyAcknowledgementSchedule;
    private HashBasedTable<InetSocketAddress, Integer, IncomingMessageExchange> ongoingMessageExchanges;

    private ChannelHandlerContext ctx;

    private boolean shutdown;

    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the threads that execute the
     *                        operations for reliability.
     */
    public IncomingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.shutdown = false;
        this.emptyAcknowledgementSchedule =
                TreeMultimap.create(Ordering.<Long>natural(), Ordering.<IncomingReliableMessageExchange>arbitrary());

        this.ongoingMessageExchanges = HashBasedTable.create();

        executorService.scheduleAtFixedRate(
                new ReliabilityTask(),
                RELIABILITY_TASK_PERIOD_MILLIS, RELIABILITY_TASK_PERIOD_MILLIS, TimeUnit.MILLISECONDS
        );
    }


    public void setChannelHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }


    public ChannelHandlerContext getChannelHandlerContext(){
        return this.ctx;
    }


    private boolean isShutdown(){
        return this.shutdown;
    }

    /**
     * If the incoming message is a confirmable {@link CoapRequest} it schedules the sending of an empty
     * acknowledgement to the sender if there wasn't a piggy-backed response within a period of 2 seconds.
     *
     * If the incoming message is a confirmable {@link CoapResponse} it immediately sends a proper acknowledgement if there
     * was an open request waiting for a seperate response or update-notification. It immediately sends an RST
     * if there was no such response expected.
     *
     * @param ctx The {@link ChannelHandlerContext} relating this handler (which implements the
     * {@link ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     *
     * @throws Exception if an error occured
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Upstream from {}: {}.", me.getRemoteAddress(), me.getMessage());

        if(isShutdown())
            return;

        if(me.getMessage() instanceof CoapMessage){

            CoapMessage coapMessage = (CoapMessage) me.getMessage();

            if (coapMessage.getMessageTypeName() == MessageType.Name.CON)
                handleIncomingConfirmableCoapMessage(ctx, me);

            else if (coapMessage.getMessageTypeName() == MessageType.Name.NON)
                handleIncomingNonConfirmableMessage(ctx, me);

            else
                ctx.sendUpstream(me);
        }

        else
            ctx.sendUpstream(me);

    }


    private void handleIncomingNonConfirmableMessage(ChannelHandlerContext ctx, MessageEvent me) {
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        boolean isDuplicate = true;

        if(!ongoingMessageExchanges.contains(remoteEndpoint, coapMessage.getMessageID())){
            IncomingMessageExchange messageExchange =
                    new IncomingMessageExchange(remoteEndpoint, coapMessage.getMessageID());

            synchronized (monitor){
                if(!ongoingMessageExchanges.contains(remoteEndpoint, coapMessage.getMessageID())){
                    ongoingMessageExchanges.put(remoteEndpoint, coapMessage.getMessageID(), messageExchange);

                    isDuplicate = false;
                }
            }

            ctx.sendUpstream(me);
        }

        if(isDuplicate)
            log.info("Received duplicate (non-confirmable). IGNORE! (Message: {})", coapMessage);
    }


    private void handleIncomingConfirmableCoapMessage(ChannelHandlerContext ctx, MessageEvent me){

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        //Empty CON messages can be used as application layer PING (is CoAP endpoint alive?)
        if(coapMessage.getMessageCodeName() == MessageCode.Name.EMPTY)
            writeReset(remoteEndpoint, coapMessage.getMessageID());

        else if(MessageCode.isResponse(coapMessage.getMessageCode()))
            handleIncomingConfirmableCoapResponse(ctx, me);

        else if(MessageCode.isRequest(coapMessage.getMessageCode()))
            handleIncomingConfirmableCoapRequest(ctx, me);

        else
            log.error("Incoming CoAP message is neither empty nor request nor response: ", coapMessage);
    }


    private void handleIncomingConfirmableCoapResponse(ChannelHandlerContext ctx, MessageEvent me){
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapResponse coapResponse = (CoapResponse) me.getMessage();

        writeEmptyAcknowledgement(remoteEndpoint, coapResponse.getMessageID());

        ctx.sendUpstream(me);
    }


    private void handleIncomingConfirmableCoapRequest(ChannelHandlerContext ctx, MessageEvent me) {
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        IncomingReliableMessageExchange newMessageExchange =
                new IncomingReliableMessageExchange(remoteEndpoint, coapMessage.getMessageID());

        IncomingMessageExchange oldMessageExchange =
                ongoingMessageExchanges.get(remoteEndpoint, coapMessage.getMessageID());

        //Check if there is an ongoing
        if(oldMessageExchange != null){

            if (oldMessageExchange instanceof IncomingReliableMessageExchange){

                //if the old message exchange is reliable and the empty ACK was already sent send another empty ACK
                if(((IncomingReliableMessageExchange) oldMessageExchange).isAcknowledgementSent())
                    writeEmptyAcknowledgement(remoteEndpoint, coapMessage.getMessageID());

            }

            //if the old message was unreliable and the duplicate message is confirmable send empty ACK
            else
                writeEmptyAcknowledgement(remoteEndpoint, coapMessage.getMessageID());

            //As the message is already being processed there is nothing more to do
            return;
        }

        //try to add new reliable message exchange
        boolean added = false;
        synchronized (monitor){
            Long time = System.currentTimeMillis() + MIN_EMPTY_ACK_DELAY_MILLIS;

            //Add message exchange to set of ongoing exchanges to detect duplicates
            if(!ongoingMessageExchanges.contains(remoteEndpoint, coapMessage.getMessageID())){
                ongoingMessageExchanges.put(remoteEndpoint, coapMessage.getMessageID(), newMessageExchange);
                added = true;
            }

            //If the scheduling of the empty ACK does not work then it was already scheduled
            if(!emptyAcknowledgementSchedule.put(time, newMessageExchange)){
                log.error("Could not schedule empty ACK for message: {}", coapMessage);
                ongoingMessageExchanges.remove(remoteEndpoint, coapMessage.getMessageID());
                added = false;
            }

        }

        //everything is fine, so further process message
        if(added)
            ctx.sendUpstream(me);
    }


    /**
     * If the message to be written is a {@link CoapResponse} this method decides whether the message type is
     * {@link MessageType.Name#ACK} (if there wasn't an empty acknowledgement sent yet) or {@link MessageType.Name#CON}
     * (if there was already an empty acknowledgement sent). In the latter case it additionally cancels the sending of
     * an empty acknowledgement (which was scheduled by the <code>messageReceived</code> method when the request
     * was received).
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     *
     * @throws Exception if an error occurred
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(isShutdown())
            return;

        if(me.getMessage() instanceof CoapResponse)
            handleOutgoingCoapResponse(ctx, me);

        else if(me.getMessage() instanceof InternalApplicationShutdownMessage)
            handleApplicationShutdown(ctx, me);

        else
            ctx.sendDownstream(me);
    }


    private void handleApplicationShutdown(ChannelHandlerContext ctx, MessageEvent me) {
        synchronized (monitor){
            this.shutdown = true;
            emptyAcknowledgementSchedule.clear();
            ongoingMessageExchanges.clear();
        }
        ctx.sendDownstream(me);
    }


    private void handleOutgoingCoapResponse(ChannelHandlerContext ctx, MessageEvent me){

        CoapResponse coapResponse = (CoapResponse) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        IncomingMessageExchange messageExchange;
        synchronized (monitor){
            messageExchange = ongoingMessageExchanges.remove(remoteEndpoint, coapResponse.getMessageID());
        }

        if(messageExchange instanceof IncomingReliableMessageExchange){

            //if the ongoing message exchange is reliable and the empty ACK was not yet sent make response piggy-
            //backed and suppress scheduled empty ACK
            if(!((IncomingReliableMessageExchange) messageExchange).isAcknowledgementSent()){
                coapResponse.setMessageType(MessageType.Name.ACK.getNumber());
                ((IncomingReliableMessageExchange) messageExchange).setAcknowledgementSent();
            }

        }

        ctx.sendDownstream(me);

    }


    private void writeEmptyAcknowledgement(final InetSocketAddress remoteAddress, final int messageID){
        try{
            CoapMessage emptyAcknowledgement = CoapMessage.createEmptyAcknowledgement(messageID);

            ChannelFuture future = Channels.future(ctx.getChannel());
            Channels.write(ctx, future, emptyAcknowledgement, remoteAddress);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Empty ACK for message ID {} succesfully sent to {}.", messageID, remoteAddress);
                }
            });
        }
        catch (IllegalArgumentException e) {
            log.error("This should never happen.", e);
        }
    }


    private void writeReset(final InetSocketAddress remoteEndpoint, final int messageID){
        try{
            CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
            ChannelFuture future = Channels.future(ctx.getChannel());

            Channels.write(this.getChannelHandlerContext(), future, resetMessage, remoteEndpoint);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("RST for message ID {} succesfully sent to {}.", messageID, remoteEndpoint);
                }
            });
        }
        catch (IllegalArgumentException e) {
            log.error("This should never happen.", e);
        }
    }


    private class ReliabilityTask implements Runnable{

        @Override
        public void run() {
            try{
                long now = System.currentTimeMillis();

                synchronized (monitor){

                    //Send due empty acknowledgements
                    Iterator<Map.Entry<Long, Collection<IncomingReliableMessageExchange>>> dueAcknowledgements =
                            emptyAcknowledgementSchedule.asMap().headMap(now, true).entrySet().iterator();

                    while(dueAcknowledgements.hasNext()){
                        Map.Entry<Long, Collection<IncomingReliableMessageExchange>> part = dueAcknowledgements.next();

                        for(IncomingReliableMessageExchange messageExchange : part.getValue()){
                            if(!messageExchange.isAcknowledgementSent()){
                                InetSocketAddress remoteEndpoint = messageExchange.getRemoteEndpoint();
                                int messageID = messageExchange.getMessageID();

                                writeEmptyAcknowledgement(remoteEndpoint, messageID);

                                messageExchange.setAcknowledgementSent();
                            }
                        }

                        dueAcknowledgements.remove();
                    }

                    //Retire open NON messages


                }
            }
            catch(Exception e){
                log.error("Error in reliability task for incoming message exchanges!", e);
            }
        }
    }
}
