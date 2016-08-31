/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.endpoints;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uzl.itm.ncoap.communication.codec.CoapMessageDecoder;
import de.uzl.itm.ncoap.communication.codec.CoapMessageEncoder;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapMessageEnvelope;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;


/**
* Receives and sends CoAP Messages for testing purposes. A {@link DummyEndpoint} has no automatic functionality
* besides encoding of inbound and decoding of outgoing messages.
*
* @author Oliver Kleine, Stefan Hüske
*/
public class DummyEndpoint extends ChannelDuplexHandler {

    private DatagramChannel channel;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //Received messages are ignored when set to false
    private boolean receptionEnabled = true;

    //map to save received messages
    private SortedMap<Long, CoapMessage> receivedCoapMessages =
            Collections.synchronizedSortedMap(new TreeMap<Long, CoapMessage>());

    private NioEventLoopGroup executor;

    public DummyEndpoint() {
        this(0);
    }

    public DummyEndpoint(int port) {
        //Create the thread-pool using the previously defined factory
        int threads = Runtime.getRuntime().availableProcessors() * 2 + 1;
        this.executor = new NioEventLoopGroup(threads, new DefaultThreadFactory("Dummy Endpoint"));

        //Create datagram datagramChannel to receive and send messages
        Bootstrap bootstrap = new Bootstrap()
                .channel(NioDatagramChannel.class)
                .group(executor)
                .handler(new ChannelInitializer<Channel>()
                {
                    @Override
                    protected void initChannel(Channel channel) throws Exception
                    {
                        channel.pipeline().addLast("Encoder", new CoapMessageEncoder());
                        channel.pipeline().addLast("Decoder", new CoapMessageDecoder());
                        channel.pipeline().addLast("CoAP Endpoint", DummyEndpoint.this );
                    }
                });

        this.channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(port)).awaitUninterruptibly().channel();
        log.info("New message receiver channel created for port " + channel.localAddress().getPort());
    }


    public int getPort() {
        return channel.localAddress().getPort();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
        if (!receptionEnabled) {
            return;
        }

        if ((object instanceof CoapMessageEnvelope)) {
            CoapMessageEnvelope envelope = (CoapMessageEnvelope) object;
            CoapMessage message = envelope.content();
            InetSocketAddress remoteAddress = envelope.sender();
            receivedCoapMessages.put(System.currentTimeMillis(), message);
            log.info("Received #{} (from {}): {}.",
                    new Object[]{getReceivedCoapMessages().size(), remoteAddress, message});
        }
    }


    public SortedMap<Long, CoapMessage> getReceivedCoapMessages() {
        return receivedCoapMessages;
    }


    public CoapMessage getReceivedMessage(int index) throws IndexOutOfBoundsException {
        if (this.getReceivedCoapMessages().size() < index - 1) {
            return null;
        }

        Iterator<Long> receptionTimes = this.receivedCoapMessages.keySet().iterator();

        for(int i = 0; i < index; i++) {
            receptionTimes.next();
        }

        return receivedCoapMessages.get(receptionTimes.next());
    }

    public synchronized void setReceptionEnabled(boolean receptionEnabled) {
        this.receptionEnabled = receptionEnabled;
    }


    public void writeMessage(final CoapMessage coapMessage, final InetSocketAddress remoteAddress) {
        try{
            this.executor.schedule(new TransmissionTask(coapMessage, remoteAddress), 0, TimeUnit.MILLISECONDS);
        }
        catch(Exception ex) {
            log.error("EXCEPTION!", ex);
        }
    }

    private class TransmissionTask implements Runnable{

        private CoapMessage coapMessage;
        private InetSocketAddress remoteSocket;


        private TransmissionTask(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
            this.coapMessage = coapMessage;
            this.remoteSocket = remoteSocket;
        }


        @Override
        public void run() {
            log.info("Send CoAP message: {}", coapMessage);
            channel.writeAndFlush(new CoapMessageEnvelope(coapMessage, remoteSocket));
        }
    }

    /**
     * Shuts the client down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public void shutdown() {
        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture future = channel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("External resources released, shutdown completed");
            }
        });

        future.awaitUninterruptibly();
        executor.shutdownGracefully();
    }
}

