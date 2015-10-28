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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.AbstractCoapApplication;
import de.uzl.itm.ncoap.application.server.webresource.Webresource;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.RequestDispatcher;
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

    private static Logger LOG = LoggerFactory.getLogger(CoapServer.class.getName());

    private RequestDispatcher requestDispatcher;

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
        this.requestDispatcher = getChannel().getPipeline().get(RequestDispatcher.class);
        this.requestDispatcher.registerWellKnownCoreResource();
//        this.requestDispatcher.setChannel(this.getChannel());
//
//        notFoundHandler.setRequestDispatcher(requestDispatcher);
//
//        ServerObservationHandler handler = getChannel().getPipeline().get(ServerObservationHandler.class);
//        ChannelHandlerContext context = getChannel().getPipeline().getContext(handler);
//        handler.setChannelHandlerContext(context);
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
        this.getRequestDispatcher().registerWebresource(webresource);
    }

    private RequestDispatcher getRequestDispatcher(){
        return getChannel().getPipeline().get(RequestDispatcher.class);
    }


    public void shutdownWebresource(Webresource webresource){
        this.getRequestDispatcher().shutdownWebresource(webresource.getUriPath());
    }

    /**
     * Gracefully shuts down the server by sequentially shutting down all its components, i.e. the registered
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} to write and receive messages.
     */
    public ListenableFuture<Void> shutdown(){
        LOG.warn("Shutdown server...");
        final SettableFuture<Void> shutdownFuture = SettableFuture.create();
        Futures.addCallback(this.requestDispatcher.shutdown(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                ChannelFuture channelClosedFuture = getChannel().close();

                //Await the closure and let the factory release its external resource to finalize the shutdown
                channelClosedFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        LOG.warn("Server channel closed. Release external resources...");

                        getChannel().getFactory().releaseExternalResources();
                    }
                });

                channelClosedFuture.awaitUninterruptibly().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        LOG.warn("Server shutdown completed!");
                        shutdownFuture.set(null);
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                onSuccess(null);
            }
        });
        return shutdownFuture;
    }
}