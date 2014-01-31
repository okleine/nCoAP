package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
     * @param listeningSockets
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