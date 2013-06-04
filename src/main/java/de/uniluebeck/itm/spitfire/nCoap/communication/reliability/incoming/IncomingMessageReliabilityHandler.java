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

package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.incoming;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.internal.InternalNullResponseFromWebserviceMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_RESPONSE;


/**
 * This class is the first {@link ChannelUpstreamHandler} to deal with incoming decoded {@link CoapMessage}s. If the
 * incoming message is a confirmable {@link CoapRequest} it schedules the sending of an empty acknowledgement to the
 * sender if there wasn't a piggy-backed response within a period of 2 seconds. If the incoming message is
 * confirmable {@link CoapResponse} it immediately sends a proper acknowledgement.
 *
 * @author Oliver Kleine
 */
public class IncomingMessageReliabilityHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(IncomingMessageReliabilityHandler.class.getName());

    //Remote socket address, message ID, acknowledgement status, incoming is confirmable request
    private final HashBasedTable<InetSocketAddress, Integer, Boolean> acknowledgementStates
            = HashBasedTable.create();

    private ScheduledExecutorService executorService;

    public IncomingMessageReliabilityHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
    }

    /**
     * If the incoming message is a confirmable {@link CoapRequest} it schedules the sending of an empty
     * acknowledgement to the sender if there wasn't a piggy-backed response within a period of 2 seconds.
     * If the incoming message is a confirmable {@link CoapResponse} it immediately sends a proper acknowledgement.
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     * @throws Exception if an error occured
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendUpstream(me);
            return;
        }

        final CoapMessage coapMessage = (CoapMessage) me.getMessage();
        log.info("Incoming (from " + me.getRemoteAddress() + "): " + coapMessage);

        final InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();
        final int messageID = coapMessage.getMessageID();

        if(coapMessage.getMessageType() == MsgType.CON){
            if(coapMessage instanceof CoapRequest){
                if(!addAcknowledgementStatus(remoteAddress, coapMessage)){
                    me.getFuture().setSuccess();
                    return;
                };

                //Schedule empty ACK fo incoming request for in 1900ms
                executorService.schedule(new Runnable(){

                    @Override
                    public void run() {
                        if(setAcknowledgementSent(remoteAddress, messageID)){
                            writeEmptyAcknowledgement(ctx, remoteAddress, messageID);
                        }
                    }
                }, 1900, TimeUnit.MILLISECONDS);
            }

            if(coapMessage instanceof CoapResponse)
                writeEmptyAcknowledgement(ctx, remoteAddress, coapMessage.getMessageID());

        }

        ctx.sendUpstream(me);
    }

    /**
     * If the message to be written is a {@link CoapResponse} this method decides whether the message type is
     * {@link MsgType#ACK} (if there wasn't an empty acknowledgement sent yet) or {@link MsgType#CON} (if there
     * already was an empty acknowledgement sent). In the latter case it additionally cancels the sending of
     * an empty acknowledgement (which was scheduled by the <code>messageReceived</code> method when the request
     * was received).
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     * @throws Exception if an error occured
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        log.debug("Downstream to {}: {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof InternalNullResponseFromWebserviceMessage){
            InternalNullResponseFromWebserviceMessage nullResponse =
                    (InternalNullResponseFromWebserviceMessage) me.getMessage();

            if(removeAcknowledgementStatus(nullResponse.getRemoteAddress(), nullResponse.getMessageID()) == null)
                log.debug("No ACK status for message with ID {} from {} available.",
                        nullResponse.getMessageID(), nullResponse.getRemoteAddress());
            else
                log.debug("Removed ACK status for message with ID {} from {}.",
                        nullResponse.getMessageID(), nullResponse.getRemoteAddress());

            return;
        }


        if(!(me.getMessage() instanceof CoapResponse)){
            ctx.sendDownstream(me);
            return;
        }

        CoapResponse coapResponse = (CoapResponse) me.getMessage();

        log.debug("Handle downstream event for message with ID " +
                coapResponse.getMessageID() + " for " + me.getRemoteAddress() );

        //Check if this is a response on a confirmable request and set the message type properly (CON or ACK)
        updateResponse(coapResponse,
                 removeAcknowledgementStatus((InetSocketAddress) me.getRemoteAddress(), coapResponse.getMessageID()));

        ctx.sendDownstream(me);
    }

    private void updateResponse(CoapResponse coapResponse, Boolean acknowledgementSent) throws Exception{

        //the response is either on a NON request or is an update notification for observers
        if(acknowledgementSent == null){
            return;
        }

        //the response is on a CON request
        if (acknowledgementSent){
            if(coapResponse.getOption(OBSERVE_RESPONSE).isEmpty()){
                //remove message ID to make the OutgoingMessageReliabilityHandler set a new one
                coapResponse.getHeader().setMsgID(Header.MESSAGE_ID_UNDEFINED);
            }
            coapResponse.getHeader().setMsgType(MsgType.CON);
        }
        else{
            coapResponse.getHeader().setMsgType(MsgType.ACK);
        }
    }

    /**
     * Checks whether there is an empty acknowledgement to be sent for the given combination of remote address
     * and message ID and sets its status according to "sent".
     *
     * @param remoteAddress the {@link InetSocketAddress} of the recipient
     * @param messageID the message ID of the empty acknowledgement
     * @return <code>true</code> if the previous status was <code>false</code>, <code>false</code> if the
     * previous state was <code>true</code> or if there is or was no such empty acknowledgement to be sent
     */
    private synchronized boolean setAcknowledgementSent(InetSocketAddress remoteAddress, int messageID){
        if(acknowledgementStates.contains(remoteAddress, messageID)){
            boolean acknowledgementSent = acknowledgementStates.put(remoteAddress, messageID, true);
            if(acknowledgementSent){
                log.debug("Empty ACK already sent for message ID " + messageID + " to " + remoteAddress + "!");
                return false;
            }
            else{
                log.debug("ACK status for message ID " + messageID + " to " + remoteAddress + " changed to true.");
                return true;
            }
        }
        else{
            log.debug("No response status found for message ID " + messageID + " to " + remoteAddress + "!");
            return false;
        }
    }


    private synchronized boolean addAcknowledgementStatus(InetSocketAddress remoteAddress, CoapMessage coapMessage){
        //Ignore duplicates
        if(acknowledgementStates.contains(remoteAddress, coapMessage.getMessageID())){
            log.info("Received duplicate (" + coapMessage + "). IGNORE!");
            return false;
        }

        acknowledgementStates.put(remoteAddress, coapMessage.getMessageID(), false);
        log.debug("Added response status for " + coapMessage);
        return true;
    }


    private synchronized Boolean removeAcknowledgementStatus(InetSocketAddress remoteAddress, int messageID){
        return acknowledgementStates.remove(remoteAddress, messageID);
    }


    private void writeEmptyAcknowledgement(ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
                                           final int messageID){
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

//    private class EmptyAcknowledgementSender implements Runnable{
//
//        private Logger log = LoggerFactory.getLogger(EmptyAcknowledgementSender.class.getName());
//
//        private ChannelHandlerContext ctx;
//        private InetSocketAddress remoteAddress;
//        private int messageID;
//
//
//        public EmptyAcknowledgementSender(ChannelHandlerContext ctx, InetSocketAddress remoteAddress,
//                                          int messageID){
//            this.ctx = ctx;
//            this.remoteAddress = remoteAddress;
//            this.messageID = messageID;
//        }
//
//        @Override
//        public void run(){
//            log.debug("Start to send empty ACK for message ID {} to {}.", messageID, remoteAddress);
//            if(setAcknowledgementSent(remoteAddress, messageID)){
//
//            }
//            else{
//                log.debug("ACK for message ID {} to {} was already sent as piggy-backed response.", messageID, remoteAddress);
//            }
//        }
//    }

}
