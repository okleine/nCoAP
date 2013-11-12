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

package de.uniluebeck.itm.ncoap.communication.reliability.incoming;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.uniluebeck.itm.ncoap.communication.reliability.ReliabilityHandler;
import de.uniluebeck.itm.ncoap.message.*;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
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
public class IncomingMessageReliabilityHandler extends ReliabilityHandler {

    public static final int EMPTY_ACK_DELAY_MILLIS = 1900;

    private static Logger log = LoggerFactory.getLogger(IncomingMessageReliabilityHandler.class.getName());

    //Remote socket address, message ID, acknowledgement status for incoming confirmable requests
    private final HashBasedTable<InetSocketAddress, Integer, MessageType.Name> owingResponses
            = HashBasedTable.create();

//    private final Multimap<InetSocketAddress, Long> waitingForResponse
//            = Multimaps.synchronizedMultimap(HashMultimap.<InetSocketAddress, Long>create());

    private ScheduledExecutorService executorService;

    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the threads that execute the
     *                        operations for reliability.
     */
    public IncomingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
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

        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendUpstream(me);
            return;
        }

        final CoapMessage coapMessage = (CoapMessage) me.getMessage();

        final InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();
        final int messageID = coapMessage.getMessageID();


        //Incoming requests
        if(coapMessage instanceof CoapRequest){

            //Incoming confirmable requests
            if(coapMessage.getMessageTypeName() == MessageType.Name.CON){
                owingResponses.put(remoteSocketAddress, messageID, MessageType.Name.ACK);

                //Schedule empty ACK fo incoming request for in 1900ms
                executorService.schedule(new Runnable(){

                    @Override
                    public void run() {
                        if(owingResponses.contains(remoteSocketAddress, messageID)){
                            owingResponses.put(remoteSocketAddress, messageID, MessageType.Name.CON);
                            writeEmptyAcknowledgement(ctx, remoteSocketAddress, messageID);
                        }
                        else{
                            log.debug("ACK for {} from {} was already sent as piggy-backed response.",
                                    messageID, remoteSocketAddress);
                        }
                    }
                }, EMPTY_ACK_DELAY_MILLIS, TimeUnit.MILLISECONDS);

                log.debug("Scheduled empty ACK for {}.", coapMessage);

            }

            //Incoming non-confirmable requests
            else{
                owingResponses.put(remoteSocketAddress, messageID, MessageType.Name.NON);
            }
        }

        //Incoming responses
        else if(coapMessage instanceof CoapResponse){
            //Incoming confirmable responses
            if(coapMessage.getMessageTypeName() == MessageType.Name.CON){
                writeEmptyAcknowledgement(ctx, remoteSocketAddress, messageID);
            }
        }

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
     * @throws Exception if an error occured
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Downstream to {}: {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapResponse){

            CoapResponse coapResponse = (CoapResponse) me.getMessage();
            InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();
            int messageID = coapResponse.getMessageID();


            MessageType.Name messageTypeName = owingResponses.remove(remoteSocketAddress, messageID);
            if(messageTypeName == MessageType.Name.CON)
                coapResponse.setMessageID(CoapMessage.MESSAGE_ID_UNDEFINED);

            log.debug("Set messageType to {}", messageTypeName);
            coapResponse.setMessageType(messageTypeName.getNumber());
        }

        ctx.sendDownstream(me);
    }

//    private synchronized void setMessageType(CoapResponse coapResponse, InetSocketAddress remoteSocketAddress, int messageID)
//            throws Exception{
//
//
//        }
//
//        //the response is either on a NON request or is an update notification for observers
////        if(acknowledgementSent == null && !coapResponse.isUpdateNotification()){
////            coapResponse.getHeader().setMsgID(Header.MESSAGE_ID_UNDEFINED);
////            coapResponse.getHeader().setMessageType(MessageType.NON);
////            return;
////        }
//
//        //the response is on a CON request
//        if (acknowledgementSent){
//            if(coapResponse.getOption(OBSERVE_RESPONSE).isEmpty()){
//                //remove message ID to make the OutgoingMessageReliabilityHandler set a new one
//                coapResponse.getHeader().setMsgID(Header.MESSAGE_ID_UNDEFINED);
//            }
//            coapResponse.getHeader().setMessageType(MessageType.CON);
//        }
//        else{
//            coapResponse.getHeader().setMessageType(MessageType.ACK);
//        }
//    }




//    /**
//     * Checks whether there is an empty acknowledgement to be sent for the given combination of remote address
//     * and message ID and sets its status according to "sent".
//     *
//     * @param remoteAddress the {@link InetSocketAddress} of the recipient
//     * @param messageID the message ID of the empty acknowledgement
//     * @return <code>true</code> if the previous status was <code>false</code>, <code>false</code> if the
//     * previous state was <code>true</code> or if there is or was no such empty acknowledgement to be sent
//     */
//    private synchronized boolean setAcknowledgementSent(InetSocketAddress remoteAddress, int messageID){
//        if(owingResponses.contains(remoteAddress, messageID)){
//            boolean acknowledgementSent = incomingConfirmableMessages.put(remoteAddress, messageID, true);
//            if(acknowledgementSent){
//                log.debug("Empty ACK already sent for message ID {} to {}.", messageID, remoteAddress);
//                return false;
//            }
//            else{
//                log.debug("ACK status for message ID {} to {} changed to true.", messageID, remoteAddress);
//                return true;
//            }
//        }
//        else{
//            log.debug("No ACK status found for message ID {} to {}.", messageID, remoteAddress);
//            return false;
//        }
//    }


//    private synchronized boolean addAcknowledgementStatus(InetSocketAddress remoteAddress, CoapMessage coapMessage){
//        //Ignore duplicates
//        if(acknowledgementStates.contains(remoteAddress, coapMessage.getMessageID())){
//            log.info("Received duplicate (IGNORE): {}.", coapMessage);
//            return false;
//        }
//
//        acknowledgementStates.put(remoteAddress, coapMessage.getMessageID(), false);
//        log.debug("Added ACK status for {}.", coapMessage);
//        return true;
//    }
//
//
//    private synchronized Boolean removeAcknowledgementStatus(InetSocketAddress remoteAddress, int messageID){
//        return acknowledgementStates.remove(remoteAddress, messageID);
//    }


    private void writeEmptyAcknowledgement(ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
                                           final int messageID){
        try{
            CoapMessage emptyACK = CoapMessage.createEmptyAcknowledgement(messageID);

            ChannelFuture future = Channels.future(ctx.getChannel());
            Channels.write(ctx, future, emptyACK, remoteAddress);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Empty ACK for message ID {} succesfully sent to {}.", messageID, remoteAddress);
                }
            });
        }
        catch (InvalidHeaderException e) {
            log.error("This should never happen.", e);
        }
    }

//    private void writeReset(ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
//                                           final int messageID, final byte[] token){
//        CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
//
//        ChannelFuture future = Channels.future(ctx.getChannel());
//        Channels.write(ctx, future, resetMessage, remoteAddress);
//
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                log.info("RST for message ID {} and token {} succesfully sent to {}.", new Object[]{messageID,
//                        new Token(token), remoteAddress});
//            }
//        });
//    }
}
