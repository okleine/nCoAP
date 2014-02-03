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
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


/**
* An instance of {@link CoapServerApplication} is the component to enable instances of {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} to
* communicate with the outside world, i.e. the Internet. Once a {@link CoapServerApplication} was instanciated
* one can register {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances and by this means make them available at their specified path.
*
* Each instance of {@link CoapServerApplication} is automatically bound to a local port to listen at for
* incoming requests.
*
* @author Oliver Kleine
*/
public class CoapServerApplication{

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private WebserviceManager webserviceManager;
    private Channel channel;

    /**
     * This constructor creates an instance of {@link CoapServerApplication}
     * @param webServiceCreator
     */
    public CoapServerApplication(WebserviceCreator webServiceCreator, InetSocketAddress localSocketAddress){
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP Server I/O Thread#%d").build();

        int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
        log.info("No. of I/O Threads: {}", numberOfThreads);

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(numberOfThreads, threadFactory);


        //Create bootstrap
        ChannelFactory channelFactory =
                new OioDatagramChannelFactory(executorService);

        ServerChannelPipelineFactory pipelineFactory =
                new ServerChannelPipelineFactory(executorService, webServiceCreator);

        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(pipelineFactory);

        this.channel = bootstrap.bind(localSocketAddress);

        this.webserviceManager =
                (WebserviceManager) pipelineFactory.getChannelHandler(ServerChannelPipelineFactory.WEBSERVICE_MANAGER);

        this.webserviceManager.setChannel(channel);

        webServiceCreator.setWebserviceManager(webserviceManager);


    }

    public CoapServerApplication(InetSocketAddress localSocketAddress){
        this(WebserviceCreator.getDefault(), localSocketAddress);
    }


    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on the given port
     * and already provides the default <code>.well-known/core</code> resource
     */
    public CoapServerApplication(int serverPort){
        this(WebserviceCreator.getDefault(), new InetSocketAddress(serverPort));
    }

    public CoapServerApplication(WebserviceCreator webServiceCreator, int serverPort){
        this(webServiceCreator, new InetSocketAddress(serverPort));
    }

    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on port
     * {@link #DEFAULT_COAP_SERVER_PORT} and already provides the default <code>.well-known/core</code> resource.
     */
    public CoapServerApplication(){
        this(DEFAULT_COAP_SERVER_PORT);
    }

    public CoapServerApplication(WebserviceCreator webServiceCreator){
        this(webServiceCreator, DEFAULT_COAP_SERVER_PORT);
    }

    public void registerService(Webservice webservice){
        WebserviceManager manager =
                (WebserviceManager)this.channel.getPipeline().get(ServerChannelPipelineFactory.WEBSERVICE_MANAGER);

        manager.registerService(webservice);
    }


    public void shutdown(){
        this.webserviceManager.shutdownAllServices();

        ChannelFuture future = this.channel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                DatagramChannel closedChannel = (DatagramChannel) future.getChannel();
                log.info("Server channel closed (port {}).", closedChannel.getLocalAddress().getPort());

                channel.getFactory().releaseExternalResources();
                log.info("External resources released, shutdown completed (port {}).",
                        closedChannel.getLocalAddress().getPort());
            }
        });

        future.awaitUninterruptibly();
    }
}