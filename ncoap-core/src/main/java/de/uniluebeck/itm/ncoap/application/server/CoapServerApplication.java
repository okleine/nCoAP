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

package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uniluebeck.itm.ncoap.communication.dispatching.server.WebserviceManager;
import de.uniluebeck.itm.ncoap.communication.reliability.InboundReliabilityHandler;
import de.uniluebeck.itm.ncoap.communication.reliability.OutboundReliabilityHandler;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;


/**
* An instance of {@link CoapServerApplication} is the component to enable instances of {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} to
* communicate with the outside world, i.e. the Internet. Once a {@link CoapServerApplication} was instanciated
* one can register {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances and by this means make them available at their specified path.
*
* Each instance of {@link CoapServerApplication} is automatically bound to a local port to listen at for
* inbound requests.
*
* @author Oliver Kleine
*/
public class CoapServerApplication{

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private WebserviceManager webserviceManager;
    private DatagramChannel channel;
    private ScheduledExecutorService executor;

    /**
     * Creates a new instance of {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication}
     *
     * @param webServiceNotFoundHandler to handle inbound {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s
     *                                  targeting unknown services
     * @param localSocket the IP address and port number for the server to listen at
     */
    public CoapServerApplication(NotFoundHandler webServiceNotFoundHandler, InetSocketAddress localSocket){

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP Server I/O Thread#%d").build();

        ThreadRenamingRunnable.setThreadNameDeterminer(new ThreadNameDeterminer() {
            @Override
            public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
                return null;
            }
        });

        int numberOfThreads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
        log.info("No. of I/O Threads: {}", numberOfThreads);

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(numberOfThreads, threadFactory);
        //executor.setRemoveOnCancelPolicy(true);
        this.executor = executor;

        //Create bootstrap
        ChannelFactory channelFactory = new NioDatagramChannelFactory(this.executor, numberOfThreads/2);
        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);

        ServerChannelPipelineFactory pipelineFactory =
                new ServerChannelPipelineFactory(this.executor, webServiceNotFoundHandler);


        bootstrap.setPipelineFactory(pipelineFactory);

        this.channel = (DatagramChannel) bootstrap.bind(localSocket);
        log.debug("Bound to local address: {}", this.channel.getLocalAddress());

        this.webserviceManager =
                (WebserviceManager) pipelineFactory.getChannelHandler(ServerChannelPipelineFactory.WEBSERVICE_MANAGER);

        this.webserviceManager.setChannel(channel);

        webServiceNotFoundHandler.setWebserviceManager(webserviceManager);

        //Set the ChannelHandlerContext for the outbound reliability handler
        OutboundReliabilityHandler outboundReliabilityHandler =
                (OutboundReliabilityHandler) this.channel.getPipeline()
                                 .get(ServerChannelPipelineFactory.OUTBOUND_RELIABILITY_HANDLER);

        outboundReliabilityHandler.setChannelHandlerContext(
                this.channel.getPipeline()
                        .getContext(ServerChannelPipelineFactory.OUTBOUND_RELIABILITY_HANDLER)
        );

        //Set the ChannelHandlerContext for the inbound reliability handler
        InboundReliabilityHandler inboundReliabilityHandler =
                         (InboundReliabilityHandler) this.channel.getPipeline()
                                 .get(ServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER);

        inboundReliabilityHandler.setChannelHandlerContext(
                this.channel.getPipeline()
                        .getContext(ServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER)
        );
    }

    public CoapServerApplication(InetSocketAddress localSocketAddress){
        this(NotFoundHandler.getDefault(), localSocketAddress);
    }


    /**
     * <p>Creates a new instance of {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor uses
     * {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler#getDefault()} to handle
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s targeting unknown services.
     *
     * @param serverPort the port number for the server to listen at (holds for all IP addresses of the server)
     */
    public CoapServerApplication(int serverPort){
        this(NotFoundHandler.getDefault(), new InetSocketAddress(serverPort));
    }

    /**
     * Creates a new instance of {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication}
     *
     * @param webServiceNotFoundHandler to handle inbound {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s
     *                                  targeting unknown services
     * @param serverPort the port number for the server to listen at (holds for all IP addresses of the server)
     */
    public CoapServerApplication(NotFoundHandler webServiceNotFoundHandler, int serverPort){
        this(webServiceNotFoundHandler, new InetSocketAddress(serverPort));
    }

    /**
     * <p>Creates a new instance of {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor uses
     * {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler#getDefault()} to handle
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s targeting unknown services and listens on port
     * {@link #DEFAULT_COAP_SERVER_PORT} (all IP addresses)
     */
    public CoapServerApplication(){
        this(DEFAULT_COAP_SERVER_PORT);
    }

    /**
     * <p>Creates a new instance of {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor listens on port
     * {@link #DEFAULT_COAP_SERVER_PORT} (all IP addresses)
     */
    public CoapServerApplication(NotFoundHandler webServiceNotFoundHandler){
        this(webServiceNotFoundHandler, DEFAULT_COAP_SERVER_PORT);
    }


    /**
     * Registeres a new {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} at this
     * {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication}.
     *
     * @param webservice the {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instance to
     *                   be registered
     */
    public void registerService(Webservice webservice){
        WebserviceManager manager =
                (WebserviceManager)this.channel.getPipeline().get(ServerChannelPipelineFactory.WEBSERVICE_MANAGER);

        manager.registerService(webservice);
    }


    /**
     * Returns the port number this {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication} listens at
     * @return the port number this {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication} listens at
     */
    public int getPort(){
        return this.channel.getLocalAddress().getPort();
    }


    /**
     * Returns the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
     * {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication} to handle tasks, e.g. write and
     * receive messages. The returned {@link java.util.concurrent.ScheduledExecutorService} may also be used by
     * {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice}s to handle inbound
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s
     *
     * @return the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
     * {@link de.uniluebeck.itm.ncoap.application.server.CoapServerApplication} to handle tasks, e.g. write and
     * receive messages.
     */
    public ScheduledExecutorService getExecutor(){
        return this.executor;
    }


    /**
     * Gracefully shuts down the server by sequentially shutting down all its components, i.e. the registered
     * {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice}s and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} to write and receive messages.
     */
    public void shutdown(){
        log.warn("Shutdown server...");

        this.webserviceManager.shutdownAllServices();

        ChannelFuture channelClosedFuture = this.channel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        channelClosedFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.warn("Server channel closed. Release external resources...");

                channel.getFactory().releaseExternalResources();
            }
        });

        channelClosedFuture.awaitUninterruptibly().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.warn("Server shutdown completed!");
            }
        });
    }
}