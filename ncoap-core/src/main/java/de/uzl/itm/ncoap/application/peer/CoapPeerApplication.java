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
package de.uzl.itm.ncoap.application.peer;


import de.uzl.itm.ncoap.application.CoapApplication;
import de.uzl.itm.ncoap.application.server.ServerChannelPipelineFactory;
import de.uzl.itm.ncoap.application.server.webresource.Webresource;
import de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uzl.itm.ncoap.communication.dispatching.client.OutboundMessageWrapper;
import de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager;
import de.uzl.itm.ncoap.communication.reliability.InboundReliabilityHandler;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * A {@link de.uzl.itm.ncoap.application.peer.CoapPeerApplication} combines both, client and server functionality, i.e.
 * it enables to send {@link de.uzl.itm.ncoap.message.CoapRequest}s and receive
 * {@link de.uzl.itm.ncoap.message.CoapResponse}s on the one hand and provides
 * {@link Webresource}s to be queried by other clients on the other hand.
 *
 * This is particularly useful, if one needs to specify a single UDP port for client and server due to some
 * network restrictions (such as firewall rules).
 *
 * @author Oliver Kleine
 */
public class CoapPeerApplication extends CoapApplication {
    
    private static Logger LOG = LoggerFactory.getLogger(CoapPeerApplication.class.getName());

    public static final String VERSION = "1.8.3-SNAPSHOT-2";

    private WebresourceManager webresourceManager;

    public CoapPeerApplication(String applicationName, NotFoundHandler notFoundHandler, InetSocketAddress localSocket){

        super(applicationName, Math.max(Runtime.getRuntime().availableProcessors() * 2, 8));

        PeerChannelPipelineFactory pipelineFactory = new PeerChannelPipelineFactory(
                this.getExecutor(), new TokenFactory(8), notFoundHandler
        );

        startApplication(pipelineFactory, localSocket);

        this.webresourceManager =
                (WebresourceManager) pipelineFactory.getChannelHandler(ServerChannelPipelineFactory.WEBRESOURCE_MANAGER);

        this.webresourceManager.setChannel(this.getChannel());

        notFoundHandler.setWebresourceManager(webresourceManager);

        //Set the ChannelHandlerContext for the inbound reliability handler
        InboundReliabilityHandler inboundReliabilityHandler =
                (InboundReliabilityHandler) this.getChannel().getPipeline()
                        .get(ServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER);

        inboundReliabilityHandler.setChannelHandlerContext(
                this.getChannel().getPipeline()
                        .getContext(ServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER)
        );
    }

    /**
     * Creates a new instance of {@link de.uzl.itm.ncoap.application.peer.CoapPeerApplication}.
     *
     * @param notFoundHandler the {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler}
     * to deal with incoming requests for unknown {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s.
     *
     * @param localSocket the socket for both, listening for incoming requests and send requests
     */
    public CoapPeerApplication(NotFoundHandler notFoundHandler, InetSocketAddress localSocket) {
        this("CoAP Peer", notFoundHandler, localSocket);
    }


    /**
     * Sends a {@link de.uzl.itm.ncoap.message.CoapRequest} to the given remote endpoints, i.e. CoAP server or
     * proxy, and registers the given {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback}
     * to be called upon reception of a {@link de.uzl.itm.ncoap.message.CoapResponse}.
     *
     * <b>Note:</b> Override {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback
     * #continueObservation(InetSocketAddress, Token)} on the given callback for observations!
     *
     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be sent
     *
     * @param clientCallback the {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback} to process the corresponding response, resp.
     *                              update notification (which are also instances of {@link de.uzl.itm.ncoap.message.CoapResponse}.
     *
     * @param remoteEndpoint the desired recipient of the given {@link de.uzl.itm.ncoap.message.CoapRequest}
     */
    public void sendCoapRequest(final CoapRequest coapRequest, final ClientCallback clientCallback,
                                final InetSocketAddress remoteEndpoint){

        this.getExecutor().submit(new Runnable() {

            @Override
            public void run() {
                OutboundMessageWrapper message = new OutboundMessageWrapper(coapRequest, clientCallback);

                ChannelFuture future = Channels.write(getChannel(), message, remoteEndpoint);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            LOG.info("Sent to {}:{}: {}",
                                    new Object[]{remoteEndpoint.getAddress().getHostAddress(),
                                            remoteEndpoint.getPort(), coapRequest});
                        }
                    }
                });
            }
        });
    }


    /**
     * Sends a CoAP PING, i.e. a {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType.Name#CON} and
     * {@link de.uzl.itm.ncoap.message.MessageCode.Name#EMPTY} to the given CoAP endpoints and registers the
     * given {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback}
     * to be called upon reception of the corresponding {@link de.uzl.itm.ncoap.message.MessageType.Name#RST}
     * message (CoAP PONG).
     *
     * Make sure to override {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback
     * #processReset()} to handle the CoAP PONG!
     *
     * @param clientCallback the {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback} to be
     *                       called upon reception of the corresponding
     *                       {@link de.uzl.itm.ncoap.message.MessageType.Name#RST} message.
     *                       <br><br>
     *                       <b>Note:</b> To handle the CoAP PONG, i.e. the empty RST, the method
     *                       {@link de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback
     *                       #processReset()} MUST be overridden
     * @param remoteEndpoint the desired recipient of the CoAP PING message
     */
    public void sendCoapPing(final ClientCallback clientCallback, final InetSocketAddress remoteEndpoint){

        this.getExecutor().submit(new Runnable() {

            @Override
            public void run() {

                final CoapMessage coapPing = CoapMessage.createPing(CoapMessage.UNDEFINED_MESSAGE_ID);
                OutboundMessageWrapper wrapper = new OutboundMessageWrapper(coapPing, clientCallback);

                ChannelFuture future = Channels.write(getChannel(), wrapper, remoteEndpoint);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            Throwable cause = future.getCause();
                            LOG.error("Error with CoAP ping!", cause);
                            String description = cause == null ? "UNEXPECTED ERROR!" : cause.getMessage();
                            clientCallback.processMiscellaneousError(description);
                        }
                    }
                });
            }
        });

    }

    /**
     * Registers a new {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} at this
     * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication}.
     *
     * @param webresource the {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} instance to
     *                   be registered
     *
     * @throws java.lang.IllegalArgumentException if there was already a
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} registered with the same path
     */
    public void registerResource(Webresource webresource) throws IllegalArgumentException{
        WebresourceManager manager =
                (WebresourceManager) this.getChannel().getPipeline().get(ServerChannelPipelineFactory.WEBRESOURCE_MANAGER);

        manager.registerWebresource(webresource);
    }

    /**
     * Gracefully shuts down the server by sequentially shutting down all its components, i.e. the registered
     * {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} to write and receive messages.
     */
    public void shutdown(){
        LOG.warn("Shutdown server...");

        this.webresourceManager.shutdownAllServices();

        ChannelFuture channelClosedFuture = this.getChannel().close();

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
            }
        });
    }

}
