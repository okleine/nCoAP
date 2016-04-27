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
package de.uzl.itm.ncoap.application;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * The abstract base class for all kinds of CoAP applications, i.e. clients, servers, and endpoints (combining client
 * and server functionality)
 *
 * @author Oliver Kleine
 */
public abstract class AbstractCoapApplication {

    /**
     * {@value #RECEIVE_BUFFER_SIZE}
     */
    public static final int RECEIVE_BUFFER_SIZE = 65536;

    /**
     * {@value #NOT_BOUND}
     */
    public static final int NOT_BOUND = -1;

    private ScheduledThreadPoolExecutor executor;
    private DatagramChannel channel;
    private String applicationName;


    /**
     * Creates a new instance of {@link AbstractCoapApplication}.
     *
     * @param applicationName the given name of this application (for logging only)
     */
    protected AbstractCoapApplication(String applicationName) {

        this.applicationName = applicationName;

        ThreadFactory threadFactory =
                new ThreadFactoryBuilder().setNameFormat(applicationName + " I/O Worker #%d").build();

        ThreadRenamingRunnable.setThreadNameDeterminer(new ThreadNameDeterminer() {
            @Override
            public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
                return null;
            }
        });

        // determine number of I/O threads and create thread pool executor of that size
        int ioThreads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 4);
        this.executor = new ScheduledThreadPoolExecutor(ioThreads, threadFactory);
//        this.executor = new SynchronizedExecutor(ioThreads, threadFactory);
    }

    /**
     * Starts this application
     *
     * @param pipelineFactory the {@link CoapChannelPipelineFactory} that creates the instances of
     * {@link AbstractCoapChannelHandler}s that deal with inbound and outbound messages
     * @param localSocket the socket address to be used for inbound and outbound messages
     */
    protected void startApplication(CoapChannelPipelineFactory pipelineFactory, InetSocketAddress localSocket) {
        //ChannelFactory channelFactory = new NioDatagramChannelFactory(executor, executor.getCorePoolSize() / 2 );
        ChannelFactory channelFactory = new NioDatagramChannelFactory(executor, 1 );

        //System.out.println("Threads: " + (executor.getCorePoolSize() - 1));
        //Create and configure bootstrap
        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.setOption("receiveBufferSizePredictor",
                new FixedReceiveBufferSizePredictor(RECEIVE_BUFFER_SIZE));

        //Create datagram channel
        this.channel = (DatagramChannel) bootstrap.bind(localSocket);

        // set the channel handler contexts
        for (ChannelHandler handler : pipelineFactory.getChannelHandlers()) {
            if (handler instanceof AbstractCoapChannelHandler) {
                ChannelHandlerContext context = this.channel.getPipeline().getContext(handler.getClass());
                ((AbstractCoapChannelHandler) handler).setContext(context);
            }
        }
    }

    /**
     * Returns the local port number the {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uzl.itm.ncoap.application.client.CoapClient} is bound to or
     * {@link #NOT_BOUND} if the application has not yet been started.
     *
     * @return the local port number the {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uzl.itm.ncoap.application.client.CoapClient} is bound to or
     * {@link #NOT_BOUND} if the application has not yet been started.
     */
    public int getPort() {
        try {
            return this.channel.getLocalAddress().getPort();
        } catch(Exception ex) {
            return NOT_BOUND;
        }
    }

    /**
     * Returns the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
     * {@link de.uzl.itm.ncoap.application.AbstractCoapApplication} to handle tasks, e.g. write and
     * receive messages. The returned {@link java.util.concurrent.ScheduledExecutorService} may also be used by
     * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s to handle inbound
     * {@link de.uzl.itm.ncoap.message.CoapRequest}s
     *
     * @return the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
     * {@link de.uzl.itm.ncoap.application.AbstractCoapApplication} to handle tasks, e.g. write and
     * receive messages.
     */
    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }


    /**
     * Returns the {@link DatagramChannel} instance this application uses to communicate with other endpoints
     *
     * @return the {@link DatagramChannel} instance this application uses to communicate with other endpoints
     */
    public DatagramChannel getChannel() {
        return this.channel;
    }

    /**
     * Returns the name this application was given
     *
     * @return the name this application was given
     */
    public String getApplicationName() {
        return applicationName;
    }

//    private class SynchronizedExecutor extends ScheduledThreadPoolExecutor {
//
//        public SynchronizedExecutor(int corePoolSize, ThreadFactory threadFactory) {
//            super(corePoolSize, threadFactory);
//        }
//
//        @Override
//        public synchronized ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit timeUnit) {
//            return super.schedule(task, delay, timeUnit);
//        }
//    }
}
