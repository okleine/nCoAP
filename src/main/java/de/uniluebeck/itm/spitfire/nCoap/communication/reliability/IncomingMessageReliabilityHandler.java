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
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is the first {@link ChannelUpstreamHandler} to deal with incoming decoded {@link CoapMessage}s. If the
 * incoming message is a confirmable {@link CoapRequest} it schedules the sending of an empty acknowledgement to the
 * sender if there wasn't a piggy-backed response within a period of 2 seconds. If the incoming message is
 * confirmable {@link CoapResponse} it immediately sends a proper acknowledgement.
 *
 * @author Oliver Kleine
 */
public class IncomingMessageReliabilityHandler extends SimpleChannelHandler {

    private static Logger log = Logger.getLogger(IncomingMessageReliabilityHandler.class.getName());

    //Remote socket address, message ID, already confirmed
    private final HashBasedTable<InetSocketAddress, Integer, Boolean> incomingMessagesToBeConfirmed
            = HashBasedTable.create();

    private Object monitor = new Object();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    private static IncomingMessageReliabilityHandler instance = new IncomingMessageReliabilityHandler();

    public static IncomingMessageReliabilityHandler getInstance(){
        return instance;
    }

    //Empty private constructor for singleton
    private IncomingMessageReliabilityHandler(){
    }

    /**
     * If the incoming message is a confirmable {@link CoapRequest} it schedules the sending of an empty
     * acknowledgement to the sender if there wasn't a piggy-backed response within a period of 2 seconds.
     * If the incoming message is a confirmable {@link CoapResponse} it immediately sends a proper acknowledgement.
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the channel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     * @throws Exception if an error occured
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendUpstream(me);
            return;
        }

        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        if(coapMessage.getMessageType() != MsgType.CON){
            ctx.sendUpstream(me);
            return;
        }


        if(coapMessage instanceof CoapRequest){

            boolean inserted;
            synchronized (monitor){
                if(!incomingMessagesToBeConfirmed.contains(me.getRemoteAddress(), coapMessage.getMessageID())){

                    incomingMessagesToBeConfirmed.put((InetSocketAddress) me.getRemoteAddress(),
                                                   coapMessage.getMessageID(), false);
                    monitor.notifyAll();
                }
                inserted = true;
            }

            if(log.isDebugEnabled()){
                log.debug("[IncomingMessageReliabilityHandler] New confirmable request with message ID " +
                        coapMessage.getMessageID() + " from " + me.getRemoteAddress() + " received (duplicate = " +
                        !inserted + ")");
            }
            //The value of "inserted" is true if the incoming message was no duplicate
            if(inserted){
                //Schedule empty ACK if there was no piggy backed ACK within 2 seconds
                EmptyACKSender emptyACKSender = new EmptyACKSender((InetSocketAddress) me.getRemoteAddress(),
                                                                    coapMessage.getMessageID());

                executorService.schedule(emptyACKSender, 2000, TimeUnit.MILLISECONDS);

                ctx.sendUpstream(me);
            }
        }
        else{
            EmptyACKSender emptyACKSender = new EmptyACKSender((InetSocketAddress) me.getRemoteAddress(),
                                                                    coapMessage.getMessageID());

            //Schedule to send an empty ACK asap
            executorService.schedule(emptyACKSender, 0, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * If the message to be written is a {@link CoapResponse} this method decides whether the message type is
     * {@link MsgType#ACK} (if there wasn't an empty acknowledgement sent yet) or {@link MsgType#CON} (if there
     * already was an empty acknowledgement sent). In the latter case it additionally cancels the sending of
     * an empty acknowledgement (which was scheduled by the <code>messageReceived</code> method when the request
     * was received).
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the channel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     * @throws Exception if an error occured
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(log.isDebugEnabled()){
            log.debug("[IncomingMessageReliablityHandler] Handle Downstream Message Event.");
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse coapResponse = (CoapResponse) me.getMessage();

            if(log.isDebugEnabled()){
                log.debug("[IncomingMessageReliabilityHandler] Handle downstream event for message with ID " +
                    coapResponse.getMessageID() + " for " + me.getRemoteAddress() );
            }

            Boolean alreadyConfirmed;

            synchronized(monitor){
                alreadyConfirmed = incomingMessagesToBeConfirmed.remove(me.getRemoteAddress(),
                                                                 coapResponse.getMessageID());
            }

            if (alreadyConfirmed == null){
                System.out.println("Object o ist NULL!!!");

                coapResponse.getHeader().setMsgType(MsgType.NON);
            }
            else{
                System.out.println("Object o ist NICHT NULL!!!");

                if(alreadyConfirmed){
                    coapResponse.getHeader().setMsgType(MsgType.CON);
                }
                else{
                    coapResponse.getHeader().setMsgType(MsgType.ACK);
                }
            }
        }

        ctx.sendDownstream(me);
    }

    private class EmptyACKSender implements Runnable{

        private InetSocketAddress rcptAddress;
        private int messageID;

        private DatagramChannel datagramChannel = CoapClientDatagramChannelFactory.getInstance().getChannel();

        public EmptyACKSender(InetSocketAddress rcptAddress, int messageID){
            this.rcptAddress = rcptAddress;
            this.messageID = messageID;
        }

        @Override
        public void run(){
            boolean confirmed = false;
             synchronized (monitor){
                 if(incomingMessagesToBeConfirmed.contains(rcptAddress, messageID)){
                    confirmed = true;
                    incomingMessagesToBeConfirmed.put(rcptAddress, messageID, true);
                }
             }

             if(confirmed){
                CoapMessage coapMessage = null;

                try {
                    coapMessage = new CoapResponse(MsgType.ACK, Code.EMPTY, messageID);
                }
                catch (ToManyOptionsException e) {
                    log.fatal("[IncomingMessageReliabilityHandler] Exception while creating empty ACK. This should " +
                            " never happen!", e);
                }
                catch (InvalidHeaderException e) {
                    log.fatal("[IncomingMessageReliabilityHandler] Exception while creating empty ACK. This should " +
                            " never happen!", e);
                }

                try {
                    coapMessage.setMessageID(messageID);
                }
                catch (InvalidHeaderException e) {
                    log.fatal("[EmptyACKSender] This should never happen! Exception while setting message ID for " +
                            "empty ACK. This should never happen!", e);
                }

                ChannelFuture future = Channels.future(datagramChannel);
                Channels.write(datagramChannel.getPipeline().getContext("OutgoingMessageReliabilityHandler"),
                               future,
                               coapMessage,
                               rcptAddress);

                if(log.isDebugEnabled()){
                    future.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            log.debug("[EmptyACKSender] Sent empty ACK for message with ID " + messageID +
                                    " to recipient " + rcptAddress);
                        }
                    });
                }
            }
        }
    }
}
