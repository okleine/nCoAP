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

package de.uniluebeck.itm.ncoap.communication.core;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Oliver Kleine
 */
public class CoapServerDatagramChannelFactory {

    private static Logger log = LoggerFactory.getLogger(CoapServerDatagramChannelFactory.class.getName());

    private DatagramChannel datagramChannel;

    /**
     *
     * @param executorService
     * @param serverPort
     */
    public CoapServerDatagramChannelFactory(ScheduledExecutorService executorService, int serverPort){
        ChannelFactory channelFactory =
                new NioDatagramChannelFactory(executorService);

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        CoapServerPipelineFactory pipelineFactory = new CoapServerPipelineFactory(executorService);
        bootstrap.setPipelineFactory(pipelineFactory);

        datagramChannel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(serverPort));

        pipelineFactory.getObservableResourceHandler().setChannel(datagramChannel);

        log.info("New server datagramChannel created for port {}.", datagramChannel.getLocalAddress().getPort());
    }

    /**
     *
     */
    public DatagramChannel getChannel(){
        return this.datagramChannel;
    }
}
