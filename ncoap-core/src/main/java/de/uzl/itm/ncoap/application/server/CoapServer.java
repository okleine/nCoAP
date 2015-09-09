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

package de.uzl.itm.ncoap.application.server;

import com.sun.corba.se.spi.activation.Server;
import de.uzl.itm.ncoap.application.AbstractCoapApplication;
import de.uzl.itm.ncoap.application.server.webresource.Webresource;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager;
import de.uzl.itm.ncoap.communication.observing.ServerObservationHandler;
import de.uzl.itm.ncoap.communication.reliability.InboundReliabilityHandler;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


/**
* An instance of {@link CoapServer} is the component to enable instances of {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} to
* communicate with the outside world, i.e. the Internet. Once a {@link CoapServer} was instanciated
* one can register {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instances and by this means make them available at their specified path.
*
* Each instance of {@link CoapServer} is automatically bound to a local port to listen at for
* inbound requests.
*
* @author Oliver Kleine
*/
public class CoapServer extends AbstractCoapApplication {

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private WebresourceManager webresourceManager;

    /**
     * <p>Creates a new instance of {@link CoapServer}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor uses
     * {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler#getDefault()} to handle
     * {@link de.uzl.itm.ncoap.message.CoapRequest}s for unknown resources and listens on port
     * {@link #DEFAULT_COAP_SERVER_PORT} (all IP addresses)
     */
    public CoapServer(){
        this(DEFAULT_COAP_SERVER_PORT);
    }


    /**
     * <p>Creates a new instance of {@link CoapServer}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor uses
     * {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler#getDefault()} to handle
     * {@link de.uzl.itm.ncoap.message.CoapRequest}s targeting unknown resources.</p>
     *
     * @param serverPort the port number for the server to listen at (holds for all IP addresses of the server)
     */
    public CoapServer(int serverPort){
        this(new InetSocketAddress(serverPort));
    }
    
    
    /**
     * <p>Creates a new instance of {@link CoapServer}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor uses
     * {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler#getDefault()} to handle
     * {@link de.uzl.itm.ncoap.message.CoapRequest}s targeting unknown resources.</p>
     *
     * @param localSocket the socket address for the server to listen at
     */
    public CoapServer(InetSocketAddress localSocket){
        this(NotFoundHandler.getDefault(), localSocket);
    }


    /**
     * <p>Creates a new instance of {@link CoapServer}</p>
     *
     * <p><b>Note:</b> An instance created with this constructor listens on port
     * {@link #DEFAULT_COAP_SERVER_PORT} (all IP addresses)
     * 
     * @param notFoundHandler to handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s for unknown resources
     */
    public CoapServer(NotFoundHandler notFoundHandler){
        this(notFoundHandler, DEFAULT_COAP_SERVER_PORT);
    }

    /**
     * Creates a new instance of {@link CoapServer}
     *
     * @param notFoundHandler to handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s for unknown resources
     * @param serverPort the port number for the server to listen at (holds for all IP addresses of the server)
     */
    public CoapServer(NotFoundHandler notFoundHandler, int serverPort){
        this(notFoundHandler, new InetSocketAddress(serverPort));
    }
    

    /**
     * Creates a new instance of {@link CoapServer}
     *
     * @param notFoundHandler to handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s for unknown resources
     * @param localSocket the socket address for the server to listen at
     */
    public CoapServer(NotFoundHandler notFoundHandler, InetSocketAddress localSocket){

        super("CoAP Server", Math.max(Runtime.getRuntime().availableProcessors() * 2, 8));

        CoapServerChannelPipelineFactory pipelineFactory =
                new CoapServerChannelPipelineFactory(this.getExecutor(), notFoundHandler);

        startApplication(pipelineFactory, localSocket);
        
        // set the webresource manager
        this.webresourceManager = getChannel().getPipeline().get(WebresourceManager.class);
        this.webresourceManager.setChannel(this.getChannel());
        
        notFoundHandler.setWebresourceManager(webresourceManager);

        ServerObservationHandler handler = getChannel().getPipeline().get(ServerObservationHandler.class);
        ChannelHandlerContext context = getChannel().getPipeline().getContext(handler);
        handler.setChannelHandlerContext(context);

//        //Set the ChannelHandlerContext for the inbound reliability handler
//        InboundReliabilityHandler inboundReliabilityHandler =
//                (InboundReliabilityHandler) this.getChannel().getPipeline()
//                        .get(CoapServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER);
//
//        inboundReliabilityHandler.setChannelHandlerContext(
//                this.getChannel().getPipeline().getContext(CoapServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER)
//        );

        //Set the ChannelHandlerContext for the server observation handler
//        ServerObservationHandler serverObservationHandler =
//                (ServerObservationHandler) this.getChannel().getPipeline()
//                        .get(CoapServerChannelPipelineFactory.SERVER_OBSERVATION_HANDLER);
//
//        serverObservationHandler.setChannelHandlerContext(
//                this.getChannel().getPipeline().getContext(CoapServerChannelPipelineFactory.SERVER_OBSERVATION_HANDLER)
//        );
    }
    


    /**
     * Registers a new {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} at this
     * {@link CoapServer}.
     *
     * @param webresource the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance to
     *                   be registered
     *
     * @throws java.lang.IllegalArgumentException if there was already a
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} registered with the same path
     */
    public void registerWebresource(Webresource webresource) throws IllegalArgumentException{
        this.getWebresourceManager().registerWebresource(webresource);
    }

    private WebresourceManager getWebresourceManager(){
        return getChannel().getPipeline().get(WebresourceManager.class);
    }

    public void shutdownWebresource(Webresource webresource){
        this.getWebresourceManager().shutdownWebresource(webresource.getUriPath());
    }


//    /**
//     * Returns the port number this {@link CoapServerApplication} listens at
//     * @return the port number this {@link CoapServerApplication} listens at
//     */
//    public int getPort(){
//        return this.channel.getLocalAddress().getPort();
//    }


//    /**
//     * Returns the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
//     * {@link CoapServerApplication} to handle tasks, e.g. write and
//     * receive messages. The returned {@link java.util.concurrent.ScheduledExecutorService} may also be used by
//     * {@link de.uniluebeck.itm.ncoap.application.server.webresource.Webservice}s to handle inbound
//     * {@link CoapRequest}s
//     *
//     * @return the {@link java.util.concurrent.ScheduledExecutorService} which is used by this
//     * {@link CoapServerApplication} to handle tasks, e.g. write and
//     * receive messages.
//     */
//    public ScheduledExecutorService getExecutor(){
//        return this.executor;
//    }


    /**
     * Gracefully shuts down the server by sequentially shutting down all its components, i.e. the registered
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} to write and receive messages.
     */
    public void shutdown(){
        log.warn("Shutdown server...");

        this.webresourceManager.shutdown();

        ChannelFuture channelClosedFuture = this.getChannel().close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        channelClosedFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.warn("Server channel closed. Release external resources...");

                getChannel().getFactory().releaseExternalResources();
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