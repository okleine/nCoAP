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
package de.uzl.itm.ncoap.application.endpoint;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.AbstractCoapApplication;
import de.uzl.itm.ncoap.application.server.webresource.Webresource;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.communication.dispatching.client.ResponseDispatcher;
import de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.RequestDispatcher;
import de.uzl.itm.ncoap.message.CoapRequest;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * A {@link CoapEndpoint} combines both, client and server functionality, i.e.
 * it enables to send {@link de.uzl.itm.ncoap.message.CoapRequest}s and receive
 * {@link de.uzl.itm.ncoap.message.CoapResponse}s on the one hand and provides
 * {@link Webresource}s to be queried by other clients on the other hand.
 *
 * This is particularly useful, if one needs to specify a single UDP port for client and server due to some
 * network restrictions (such as firewall rules).
 *
 * @author Oliver Kleine
 */
public class CoapEndpoint extends AbstractCoapApplication {
    
    private static Logger LOG = LoggerFactory.getLogger(CoapEndpoint.class.getName());

    private ResponseDispatcher responseDispatcher;
    private RequestDispatcher requestDispatcher;

    /**
     * Creates a new instance of {@link de.uzl.itm.ncoap.application.endpoint.CoapEndpoint}
     *
     * @param applicationName the name of the application
     * @param notFoundHandler the {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler}
     * to deal with requests that are addressed to resources that do not (yet) exist.
     * @param localSocket the local {@link java.net.InetSocketAddress} to listen at for incoming messages
     */
    public CoapEndpoint(String applicationName, NotFoundHandler notFoundHandler, InetSocketAddress localSocket){

        super(applicationName, Math.max(Runtime.getRuntime().availableProcessors() * 2, 8));

        CoapEndpointChannelPipelineFactory pipelineFactory = new CoapEndpointChannelPipelineFactory(
                this.getExecutor(), new TokenFactory(8), notFoundHandler
        );

        startApplication(pipelineFactory, localSocket);

        // retrieve the request dispatcher (server component)
        this.requestDispatcher = getChannel().getPipeline().get(RequestDispatcher.class);
//        this.requestDispatcher.setChannel(this.getChannel());
//        notFoundHandler.setRequestDispatcher(requestDispatcher);
//
//        // set the context for the servers observation handler
//        ServerObservationHandler handler = getChannel().getPipeline().get(ServerObservationHandler.class);
//        ChannelHandlerContext context = getChannel().getPipeline().getContext(handler);
//        handler.setChannelHandlerContext(context);

        // retrieve the response dispatcher (client component)
        this.responseDispatcher = getChannel().getPipeline().get(ResponseDispatcher.class);
    }

    /**
     * <p>Creates a new instance of {@link de.uzl.itm.ncoap.application.endpoint.CoapEndpoint}</p>
     * <p>Using this constructor has the same effect as invoking the other constructor with name
     * <code>COAP ENDPOINT</code>.
     *
     * @param notFoundHandler the {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler}
     * to deal with requests that are addressed to resources that do not (yet) exist.
     * @param localSocket the local {@link java.net.InetSocketAddress} to listen at for incoming messages
     */
    public CoapEndpoint(NotFoundHandler notFoundHandler, InetSocketAddress localSocket) {
        this("COAP ENDPOINT", notFoundHandler, localSocket);
    }


    /**
     * Sends a {@link de.uzl.itm.ncoap.message.CoapRequest} to the given remote endpoints, i.e. CoAP server or
     * proxy, and registers the given {@link de.uzl.itm.ncoap.application.client.ClientCallback}
     * to be called upon reception of a {@link de.uzl.itm.ncoap.message.CoapResponse}.
     *
     * <b>Note:</b> Override {@link de.uzl.itm.ncoap.application.client.ClientCallback
     * #continueObservation(InetSocketAddress, Token)} on the given callback for observations!
     *
     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be sent
     *
     * @param callback the {@link de.uzl.itm.ncoap.application.client.ClientCallback} to process the corresponding response, resp.
     *                              update notification (which are also instances of {@link de.uzl.itm.ncoap.message.CoapResponse}.
     *
     * @param remoteSocket the desired recipient of the given {@link de.uzl.itm.ncoap.message.CoapRequest}
     */
    public void sendCoapRequest(CoapRequest coapRequest, ClientCallback callback, InetSocketAddress remoteSocket){
        this.responseDispatcher.sendCoapRequest(coapRequest, remoteSocket, callback);
    }


    /**
     * Sends a CoAP PING, i.e. a {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType.Name#CON} and
     * {@link de.uzl.itm.ncoap.message.MessageCode.Name#EMPTY} to the given CoAP endpoints and registers the
     * given {@link de.uzl.itm.ncoap.application.client.ClientCallback}
     * to be called upon reception of the corresponding {@link de.uzl.itm.ncoap.message.MessageType.Name#RST}
     * message (CoAP PONG).
     *
     * Make sure to override {@link de.uzl.itm.ncoap.application.client.ClientCallback
     * #processReset()} to handle the CoAP PONG!
     *
     * @param callback the {@link de.uzl.itm.ncoap.application.client.ClientCallback} to be
     *                       called upon reception of the corresponding
     *                       {@link de.uzl.itm.ncoap.message.MessageType.Name#RST} message.
     *                       <br><br>
     *                       <b>Note:</b> To handle the CoAP PONG, i.e. the empty RST, the method
     *                       {@link de.uzl.itm.ncoap.application.client.ClientCallback
     *                       #processReset()} MUST be overridden
     * @param remoteSocket the desired recipient of the CoAP PING message
     */
    public void sendCoapPing(final ClientCallback callback, final InetSocketAddress remoteSocket){
        this.responseDispatcher.sendCoapPing(remoteSocket, callback);

    }

    /**
     * Registers a new {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} at this
     * {@link de.uzl.itm.ncoap.application.server.CoapServer}.
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

    /**
     *
     * @param webresource
     */
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
