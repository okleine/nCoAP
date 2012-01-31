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

import com.google.common.collect.HashMultimap;
import de.uniluebeck.itm.spitfire.nCoap.core.CoapChannel;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;
import de.uniluebeck.itm.spitfire.nCoap.message.Request;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Oliver Kleine
 */
public class OutgoingMessageReliabilityHandler extends SimpleChannelHandler {

    public static int MAX_RETRANSMIT = 4;
    public static int RESPONSE_TIMEOUT = 2000;

    private static OutgoingMessageReliabilityHandler instance = new OutgoingMessageReliabilityHandler();
    private static Logger log = Logger.getLogger("nCoap");

    private final ConcurrentHashMap<Integer, OpenOutgoingRequest> openRequests = new ConcurrentHashMap<>();

    public static HashMultimap<Integer, Date> messagesSent = HashMultimap.create();

    /**
     * Returns the one and only instance of class OutgoingMessageReliabilityHandler (Singleton)
     * @return the one and only instance
     */
    public static OutgoingMessageReliabilityHandler getInstance(){
        return instance;
    }

    private OutgoingMessageReliabilityHandler(){
        new Thread(){
            @Override
            public void run(){
                while(true){
                    Enumeration<OpenOutgoingRequest> requests;
                    synchronized(openRequests){
                       requests = openRequests.elements();
                    }

                    while(requests.hasMoreElements()){
                        OpenOutgoingRequest openRequest = requests.nextElement();

                        //Retransmit message if its time to do so
                        if(openRequest.getRetransmissionCount() ==  MAX_RETRANSMIT){
                            openRequests.remove(openRequest.getRequest().getHeader().getMsgID());

                            log.info("{OutgoingMessageReliabilityHandler] Message with ID " +
                                openRequest.getRequest().getHeader().getMsgID() + " has reached maximum number of " +
                                "retransmits. Deleted from list of open requests.");

                            break;
                        }

                        if(System.currentTimeMillis() >= openRequest.getNextTransmitTime()){

                            Channels.write(CoapChannel.getInstance().channel,
                                    openRequest.getRequest(), openRequest.getRcptSocketAddress());

                            openRequest.increaseRetransmissionCount();
                            openRequest.setNextTransmitTime();

                            if(log.isDebugEnabled()){
                                log.debug("[ConfirmableMessageRetransmitter] Retransmission no. " +
                                    openRequest.getRetransmissionCount() + " for message with ID " +
                                    openRequest.getRequest().getHeader().getMsgID() + " to " +
                                    openRequest.getRcptSocketAddress());
                            }
                        }
                    }
                    //Sleeping for a millisecond causes a CPU load of 1% instead of 25% without sleep
                    try{
                        sleep(1);
                    } catch (InterruptedException e) {
                       log.fatal("[OutgoingMessageReliabilityHandler] This should never happen:\n" +
                               e.getStackTrace());
                    }
                }
            }
        }.start();
    }

    /**
     * This method is invoked with an upstream message event. If the message has one of the codes ACK or RESET it is
     * most likely a response for a request waiting for a response. Thus the corresponding request is removed from
     * the list of open requests and the request will not be retransmitted anymore.
     * @param ctx The {@link ChannelHandlerContext}
     * @param me The {@link MessageEvent}
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        Message message = (Message) me.getMessage();
        if(message.getHeader().getMsgType() == MsgType.ACK || message.getHeader().getMsgType() == MsgType.RST){
            synchronized (openRequests){
                if(openRequests.remove(message.getHeader().getMsgID()) != null) {
                    if(log.isDebugEnabled()){
                        log.debug("[OutgoingMessageReliabilityHandler] Received " + message.getHeader().getMsgType() +
                                " message for open request with message ID " + message.getHeader().getMsgID());
                    }
                }
            }
        }

        //Send only non-empty messages further upstream
        if(message.getHeader().getCode() != Code.EMPTY){
            ctx.sendUpstream(me);
        }
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
        if(!(me.getMessage() instanceof Request)){
            super.messageReceived(ctx, me);
        }

        Request request = (Request) me.getMessage();

        if(request.getHeader().getMsgType() == MsgType.CON){

            synchronized (openRequests){
                if(!openRequests.containsKey(request.getHeader().getMsgID())){

                    OpenOutgoingRequest newOpenRequest =
                        new OpenOutgoingRequest((InetSocketAddress)me.getRemoteAddress(), request);

                    openRequests.put(request.getHeader().getMsgID(), newOpenRequest);

                    if(log.isDebugEnabled()){
                        log.debug("[OutgoingMessageReliabilityHandler] New open request with request ID " +
                            request.getHeader().getMsgID());
                    }
                }
            }
        }


        ctx.sendDownstream(me);
        if(log.isDebugEnabled()){
            log.debug("[OutgoingMessageReliabilityHandler] Message with ID " + request.getHeader().getMsgID() + " sent.");
        }

        //For Debugging only!
        //messagesSent.put(request.getHeader().getMsgID(), new Date(System.currentTimeMillis()));
    }
}
