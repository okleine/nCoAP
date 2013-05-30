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

package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author Oliver Kleine
 */
public abstract class CoapServerDatagramChannelFactory {

    private static Logger log = LoggerFactory.getLogger(CoapServerDatagramChannelFactory.class.getName());
    private static final int NO_OF_THREADS = 10;

    //private static HashMap<Integer, DatagramChannel> channels = new HashMap<Integer, DatagramChannel>();

    /**
     * Creates a new {@link DatagramChannel} instance associated with the given local server port. Upon creation the
     * channel is bound to the given local server port and listens for incoming messages.
     *
     * @param serverApp the instance of {@link CoapServerApplication} to handle incoming {@link CoapRequest}s.
     * @param coapServerPort the local port the server is supposed to listen at
     * @return the newly created {@link DatagramChannel} instance
     * @throws ChannelException if the channel could not be created for any reason.
     */
    public static DatagramChannel createChannel(CoapServerApplication serverApp, int coapServerPort)
            throws ChannelException {

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP server #%d").build();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(NO_OF_THREADS, (threadFactory));

        serverApp.setExecutorService(executorService);

        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(executorService);

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        CoapServerPipelineFactory pipelineFactory =
                new CoapServerPipelineFactory(serverApp, coapServerPort, executorService);
        bootstrap.setPipelineFactory(pipelineFactory);

        DatagramChannel channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(coapServerPort));

        pipelineFactory.getObservableResourceHandler().setChannel(channel);

        //channels.put(channel.getLocalAddress().getPort(), channel);

        log.info("New server channel created for port " + channel.getLocalAddress().getPort());
        return channel;
    }

//    /**
//     * Returns the {@link DatagramChannel} instance associated with the given local server port. If there is no such
//     * instance, it returns null.
//     *
//     * @param coapServerPort the local server port
//     * @return the {@link DatagramChannel} instance associated with the given local server port. If there is no such
//     * instance, it returns {@code null}.
//     */
//    public static DatagramChannel getChannel(int coapServerPort){
//        return channels.get(coapServerPort);
//    }
}
