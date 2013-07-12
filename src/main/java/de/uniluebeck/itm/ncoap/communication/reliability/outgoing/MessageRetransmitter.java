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

import de.uniluebeck.itm.ncoap.communication.observe.InternalUpdateNotificationRetransmissionMessage;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import org.jboss.netty.channel.UpstreamMessageEvent;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;

/**
 * Implementation of {@link Runnable} to retransmit a confirmable message in exponentially increasing
 * intervals.
 */
class MessageRetransmitter implements Runnable {

    private Logger log = LoggerFactory.getLogger(MessageRetransmitter.class.getName());

    private InetSocketAddress rcptAddress;
    private RetransmissionSchedule retransmissionSchedule;
    private int retransmitNo;
    private ChannelHandlerContext ctx;

    /**
     * @param ctx the {@link ChannelHandlerContext} to get the {@link DatagramChannel} to be used to send the
     *            outgoing {@link CoapMessage}s
     * @param rcptAddress the address of the recipient
     * @param retransmissionSchedule the {@link RetransmissionSchedule} providing the times of retransmission
     * @param retransmitNo the number of previously sent retransmitions plus 1
     */
    public MessageRetransmitter(ChannelHandlerContext ctx, InetSocketAddress rcptAddress,
            RetransmissionSchedule retransmissionSchedule, int retransmitNo){
        this.rcptAddress = rcptAddress;
        this.retransmitNo = retransmitNo;
        this.retransmissionSchedule = retransmissionSchedule;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        final CoapMessage coapMessage = retransmissionSchedule.getCoapMessage();
        if (!coapMessage.getOption(OBSERVE_RESPONSE).isEmpty()) {

            CoapResponse coapResponse = (CoapResponse) coapMessage;

            //increment OBSERVE notification count before retransmission
            long notificationCount = (Long) coapResponse.getOption(OBSERVE_RESPONSE).get(0).getDecodedValue() + 1;

            try {
                coapResponse.setObserveOptionValue(notificationCount);
                UpstreamMessageEvent upstreamEvent = new UpstreamMessageEvent(ctx.getChannel(),
                        new InternalUpdateNotificationRetransmissionMessage(rcptAddress,
                                coapResponse.getServicePath()), null);

                ctx.sendUpstream(upstreamEvent);
            }
            catch (ToManyOptionsException ex) {
                log.error("This should never happen.", ex);
            }
        }
        
        ChannelFuture future = Channels.future(ctx.getChannel());

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Retransmition completed {}", MessageRetransmitter.this);
                UpstreamMessageEvent upstreamEvent = new UpstreamMessageEvent(ctx.getChannel(),
                        new InternalMessageRetransmissionMessage(rcptAddress, coapMessage.getToken()), null);

                ctx.sendUpstream(upstreamEvent);
            }
        });

       Channels.write(ctx, future, coapMessage, rcptAddress);
    }

    @Override
    public String toString() {
        CoapMessage coapMessage = retransmissionSchedule.getCoapMessage();
        return  "RetransmitNo " + retransmitNo +
                (coapMessage == null ? "" : (", MsgID " + coapMessage.getMessageID())) +
                ", RcptAddress " + rcptAddress + "}";
    }

}
