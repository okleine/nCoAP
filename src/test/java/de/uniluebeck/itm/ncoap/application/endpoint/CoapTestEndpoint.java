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
///**
//* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
//* All rights reserved
//*
//* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//* following conditions are met:
//*
//*  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
//*    disclaimer.
//*
//*  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//*    following disclaimer in the documentation and/or other materials provided with the distribution.
//*
//*  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
//*    products derived from this software without specific prior written permission.
//*
//* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
//* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
//* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
//* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//*/
//package de.uniluebeck.itm.ncoap.application.endpoint;
//
//import de.uniluebeck.itm.ncoap.communication.encoding.CoapMessageDecoder;
//import de.uniluebeck.itm.ncoap.communication.encoding.CoapMessageEncoder;
//import de.uniluebeck.itm.ncoap.message.CoapMessage;
//import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
//import org.jboss.netty.channel.*;
//import org.jboss.netty.channel.socket.DatagramChannel;
//import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.util.SortedMap;
//import java.util.TreeMap;
//import java.util.concurrent.Executors;
//
//
///**
//* Receives and sends CoAP Messages for testing purposes. A {@link CoapTestEndpoint} has no automatic functionality
//* besides encoding of incoming and decoding of outgoing messages.
//*
//* @author Oliver Kleine, Stefan Hueske
//*/
//public class CoapTestEndpoint extends SimpleChannelHandler {
//
//    private DatagramChannel channel;
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//    //Received messages are ignored when set to false
//    private boolean receiveEnabled = true;
//    //private boolean writeEnabled = true;
//
//    //map to save received messages
//    private SortedMap<Long, CoapMessage> receivedMessages = new TreeMap<Long, CoapMessage>();
//
//    public CoapTestEndpoint() {
//        //Create datagram datagramChannel to receive and send messages
//        ChannelFactory channelFactory =
//                new NioDatagramChannelFactory(Executors.newCachedThreadPool());
//
//        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
//        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
//
//            @Override
//            public ChannelPipeline getPipeline() throws Exception {
//                ChannelPipeline pipeline = Channels.pipeline();
//                pipeline.addLast("Encoder", new CoapMessageEncoder());
//                pipeline.addLast("Decoder", new CoapMessageDecoder());
//                pipeline.addLast("CoAP Endpoint", CoapTestEndpoint.this );
//                return pipeline;
//            }
//        });
//
//        channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(0));
//        log.info("New message receiver channel created for port " + channel.getLocalAddress().getPort());
//    }
//
//    public int getPort(){
//        return channel.getLocalAddress().getPort();
//    }
//
//    @Override
//    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//        if ((e.getMessage() instanceof CoapMessage) && receiveEnabled) {
//
//            receivedMessages.put(System.currentTimeMillis(), (CoapMessage) e.getMessage());
//
//            log.info("Incoming #{} (from {}): {}.",
//                    new Object[]{getReceivedMessages().size(), e.getRemoteAddress(), e.getMessage()});
//        }
//    }
//
//    public SortedMap<Long, CoapMessage> getReceivedMessages() {
//        return receivedMessages;
//    }
//
//    public synchronized void setReceiveEnabled(boolean receiveEnabled) {
//        this.receiveEnabled = receiveEnabled;
//    }
//
//    public void writeMessage(CoapMessage coapMessage, InetSocketAddress remoteAddress) {
//        log.info("Write " + coapMessage);
//        Channels.write(channel, coapMessage, remoteAddress);
//    }
//
//    /**
//     * Shuts the client down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
//     * by this means free the port. All blocked or bound external resources are released.
//     */
//    public void shutdown(){
//        //Close the datagram datagramChannel (includes unbind)
//        ChannelFuture future = channel.close();
//
//        //Await the closure and let the factory release its external resource to finalize the shutdown
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                DatagramChannel closedChannel = (DatagramChannel) future.getChannel();
//                log.info("Message receiver channel closed (port {}).", closedChannel.getLocalAddress().getPort());
//
//                channel.getFactory().releaseExternalResources();
//                log.info("External resources released, shutdown completed (port {}).",
//                        closedChannel.getLocalAddress().getPort());
//            }
//        });
//
//        future.awaitUninterruptibly();
//    }
//}
//
