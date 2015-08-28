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
package de.uzl.itm.ncoap.application;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uzl.itm.ncoap.application.server.ServerChannelPipelineFactory;
import de.uzl.itm.ncoap.communication.reliability.OutboundReliabilityHandler;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Created by olli on 27.08.15.
 */
public abstract class CoapApplication {

    public static final int RECEIVE_BUFFER_SIZE = 65536;
    public static final int NOT_BOUND = -1;

    private ScheduledThreadPoolExecutor executor;
    private DatagramChannel channel;
    private String applicationName;



    protected CoapApplication(String applicationName, int ioThreads){

        this.applicationName = applicationName;


        ThreadFactory threadFactory =
                new ThreadFactoryBuilder().setNameFormat(applicationName + " I/O worker #%d").build();

        ThreadRenamingRunnable.setThreadNameDeterminer(new ThreadNameDeterminer() {
            @Override
            public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
                return null;
            }
        });

        this.executor = new ScheduledThreadPoolExecutor(Math.max(ioThreads, 4), threadFactory);
    }

    protected void startApplication(CoapChannelPipelineFactory pipelineFactory, InetSocketAddress socketAddress){
        ChannelFactory channelFactory = new NioDatagramChannelFactory(executor, executor.getCorePoolSize()/2);

        //Create and configure bootstrap
        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.setOption("receiveBufferSizePredictor",
                new FixedReceiveBufferSizePredictor(RECEIVE_BUFFER_SIZE));

        //Create datagram channel
        this.channel = (DatagramChannel) bootstrap.bind(socketAddress);

        //Set the ChannelHandlerContext for the outbound reliability handler
        OutboundReliabilityHandler outboundReliabilityHandler =
                (OutboundReliabilityHandler) this.channel.getPipeline()
                        .get(ServerChannelPipelineFactory.OUTBOUND_RELIABILITY_HANDLER);

        outboundReliabilityHandler.setChannelHandlerContext(
                this.channel.getPipeline()
                        .getContext(ServerChannelPipelineFactory.OUTBOUND_RELIABILITY_HANDLER)
        );
    }


    protected void startApplication(CoapChannelPipelineFactory pipelineFactory, int port){
        this.startApplication(pipelineFactory, new InetSocketAddress(port));
    }

    /**
     * Returns the local port number the {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uzl.itm.ncoap.application.client.CoapClientApplication} is bound to or
     * {@value #NOT_BOUND} if the application has not yet been started.
     *
     * @return the local port number the {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uzl.itm.ncoap.application.client.CoapClientApplication} is bound to or
     * {@value #NOT_BOUND} if the application has not yet been started.
     */
    public int getPort() {
        try {
            return this.channel.getLocalAddress().getPort();
        }
        catch(Exception ex){
            return NOT_BOUND;
        }
    }

    /**
     * Returns the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
     * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication} to handle tasks, e.g. write and
     * receive messages. The returned {@link java.util.concurrent.ScheduledExecutorService} may also be used by
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s to handle inbound
     * {@link de.uzl.itm.ncoap.message.CoapRequest}s
     *
     * @return the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
     * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication} to handle tasks, e.g. write and
     * receive messages.
     */
    public ScheduledExecutorService getExecutor(){
        return this.executor;
    }


    public DatagramChannel getChannel() {
        return channel;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
