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

package de.uniluebeck.itm.ncoap.application.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.ncoap.application.AbstractCoapChannelPipelineFactory;
import de.uniluebeck.itm.ncoap.application.InternalApplicationShutdownMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingMessageReliabilityHandler;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.ResetProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.TransmissionInformationProcessor;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * An instance of {@link CoapClientApplication} is the entry point to send {@link CoapMessage}s to a (remote)
 * server or proxy.
 * 
 * With {@link #sendCoapRequest(CoapRequest, CoapResponseProcessor, InetSocketAddress)} it e.g. provides an
 * easy-to-use method to write CoAP requests to a server.
 * 
 * Furthermore, with {@link #sendCoapPing(ResetProcessor, InetSocketAddress)} it provides a method to test if a remote
 * CoAP endpoint (i.e. the CoAP application and not only the host(!)) is alive.
 * 
 * @author Oliver Kleine
*/
public class CoapClientApplication {

    public static final int RECEIVE_BUFFER_SIZE = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ScheduledThreadPoolExecutor scheduledExecutorService;
    private DatagramChannel channel;

    private String name;

    /**
     * Creates a new instance of {@link CoapClientApplication}.
     * 
     * @param name the name of the application (used for logging purposes)
     * @param port the port, this {@link CoapClientApplication} should be bound to (use <code>0</code> for
     *             arbitrary port)
     * @param numberOfThreads the number of threads to be used for I/O operations. The minimum number is 4, i.e. even
     *                        if the given number is smaller then 4, the application will use 4 threads.
     *                        
     * @param maxTokenLength the maximum length of {@link Token}s to be created by the {@link TokenFactory}. The minimum
     *                       length is <code>0</code>, the maximum length (and default value) is <code>8</code>. This
     *                       can be used to limit the amount of parallel requests (see {@link TokenFactory} for
     *                       details).
     */
    public CoapClientApplication(String name, int port, int numberOfThreads, int maxTokenLength){

        this.name = name;

        if(maxTokenLength < 0 || maxTokenLength > 8)
            throw new IllegalArgumentException("Token length must be between 0 and 8 (both inclusive)");

        int threads = Math.max(numberOfThreads, 4);

        ThreadFactory threadFactory =
                new ThreadFactoryBuilder().setNameFormat(name + " I/O worker #%d").build();

        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(threads, threadFactory);
        this.scheduledExecutorService.setRemoveOnCancelPolicy(true);

        TokenFactory tokenFactory = new TokenFactory(maxTokenLength);

        //Create factories for channel and pipeline
        ChannelFactory channelFactory = new NioDatagramChannelFactory(scheduledExecutorService, threads/2);
        ClientChannelPipelineFactory clientChannelPipelineFactory =
                new ClientChannelPipelineFactory(scheduledExecutorService, tokenFactory);

        //Create and configure bootstrap
        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(clientChannelPipelineFactory);
        bootstrap.setOption("receiveBufferSizePredictor",
                new FixedReceiveBufferSizePredictor(RECEIVE_BUFFER_SIZE));

        //Create datagram channel
        this.channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(port));

        //Set the ChannelHandlerContext for the outgoing reliability handler
        OutgoingMessageReliabilityHandler outgoingMessageReliabilityHandler =
                (OutgoingMessageReliabilityHandler) this.channel.getPipeline()
                        .get(AbstractCoapChannelPipelineFactory.OUTGOING_MESSAGE_RELIABILITY_HANDLER);

        outgoingMessageReliabilityHandler.setChannelHandlerContext(
            this.channel.getPipeline()
                    .getContext(AbstractCoapChannelPipelineFactory.OUTGOING_MESSAGE_RELIABILITY_HANDLER)
        );

        //Set the ChannelHandlerContext for the incoming reliability handler
        IncomingMessageReliabilityHandler incomingMessageReliabilityHandler =
                (IncomingMessageReliabilityHandler) this.channel.getPipeline()
                        .get(AbstractCoapChannelPipelineFactory.INCOMING_MESSAGE_RELIABILITY_HANDLER);

        incomingMessageReliabilityHandler.setChannelHandlerContext(
                this.channel.getPipeline()
                        .getContext(AbstractCoapChannelPipelineFactory.INCOMING_MESSAGE_RELIABILITY_HANDLER)
        );

        log.info("New client channel created for address {}", this.channel.getLocalAddress());
    }

    /**
     * Creates a new instance.
     * 
     * Invocation of this constructor has the same effect as {@link #CoapClientApplication(String, int, int, int)} with
     * <code>"CoAP Client"</code> as parameter name.
     *
     * @param port the port, this {@link CoapClientApplication} should be bound to (use <code>0</code> for
     *             arbitrary port)
     * @param numberOfThreads the number of threads to be used for I/O operations. The minimum number is 4, i.e. even
     *                        if the given number is smaller then 4, the application will use 4 threads.
     *
     * @param maxTokenLength the maximum length of {@link Token}s to be created by the {@link TokenFactory}. The minimum
     *                       length is <code>0</code>, the maximum length (and default value) is <code>8</code>. This
     *                       can be used to limit the amount of parallel requests (see {@link TokenFactory} for
     *                       details).
     */
    public CoapClientApplication(int port, int numberOfThreads, int maxTokenLength){
        this("CoAP Client", port, numberOfThreads, maxTokenLength);
    }


    /**
     * Creates a new instance of {@link CoapClientApplication} with default parameters.
     * 
     * Invocation of this constructor has the same effect as {@link #CoapClientApplication(String, int, int)} with
     * parameters <code>name = "CoAP Client"</code>, <code>port = 0</code>, <code>maxTokenLength = 8</code>.
     */
    public CoapClientApplication(){
        this("CoAP Client", 0, 8);
    }

    /**
     * Creates a new instance of {@link CoapClientApplication}.
     *
     * Invocation of this constructor has the same effect as {@link #CoapClientApplication(String, int, int)} with
     * parameters <code>name = name</code>, <code>port = 0</code>, <code>maxTokenLength = 8</code>.
     */
    public CoapClientApplication(String name){
        this(name, 0, 8);
    }

    /**
     * Creates a new instance of {@link CoapClientApplication}.
     *
     * Invocation of this constructor has the same effect as {@link #CoapClientApplication(String, int, int, int)} with
     * parameters <code>name = name</code>, <code>port = 0</code>, <code>maxTokenLength = 8</code>, and
     * <code>numberOfThreads = Runtime.getRuntime().availableProcessors() * 2)</code>
     */
    public CoapClientApplication(String name, int port, int maxTokenLength){
        this(name, port, Runtime.getRuntime().availableProcessors() * 2, maxTokenLength);
    }


    /**
     * Sends a {@link CoapRequest} to the given remote endpoint, i.e. CoAP server or proxy, and registers the
     * given {@link CoapResponseProcessor} to be called upon reception of a {@link CoapResponse}.
     *
     * @param coapRequest the {@link CoapRequest} to be sent
     * @param coapResponseProcessor the {@link CoapResponseProcessor} to process the corresponding response
     * @param remoteEndpoint the desired recipient of the given {@link CoapRequest}
     */
    public void sendCoapRequest(final CoapRequest coapRequest, final CoapResponseProcessor coapResponseProcessor,
                                final InetSocketAddress remoteEndpoint){

        scheduledExecutorService.schedule(new Runnable(){

            @Override
            public void run() {
                InternalWrappedOutgoingCoapMessage message =
                        new InternalWrappedOutgoingCoapMessage(coapRequest, coapResponseProcessor);

                ChannelFuture future = Channels.write(channel, message, remoteEndpoint);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(future.isSuccess()){
                            log.debug("Sent to {}:{}: {}",
                                    new Object[]{remoteEndpoint.getAddress().getHostAddress(),
                                            remoteEndpoint.getPort(), coapRequest});

                            if (coapResponseProcessor instanceof TransmissionInformationProcessor)
                                ((TransmissionInformationProcessor) coapResponseProcessor).messageTransmitted(
                                        coapRequest.getToken(), coapRequest.getMessageID(), false);
                        }
                    }
                });
            }

        }, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Sends a CoAP PING, i.e. a {@link CoapMessage} with {@link MessageType.Name#CON} and
     * {@link MessageCode.Name#EMPTY} to the given CoAP endpoint and registers the given {@link ResetProcessor}
     * to be called upon reception of the corresponding {@link MessageType.Name#RST} message (CoAP PONG).
     *
     * @param resetProcessor the {@link ResetProcessor} to be called upon reception of the corresponding
     *                       {@link MessageType.Name#RST} message.
     * @param remoteEndpoint the desired recipient of the CoAP PING message
     */
    public void sendCoapPing(final ResetProcessor resetProcessor, final InetSocketAddress remoteEndpoint){

        scheduledExecutorService.schedule(new Runnable(){

            @Override
            public void run() {

                final CoapMessage resetMessage =
                        CoapMessage.createEmptyConfirmableMessage(CoapMessage.MESSAGE_ID_UNDEFINED);

                InternalWrappedOutgoingCoapMessage message =
                        new InternalWrappedOutgoingCoapMessage(resetMessage, resetProcessor);

                ChannelFuture future = Channels.write(channel, message, remoteEndpoint);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(future.isSuccess()){
                            log.debug("Sent to {}:{}: {}",
                                    new Object[]{remoteEndpoint.getAddress().getHostAddress(),
                                            remoteEndpoint.getPort(), resetMessage});

                            if (resetProcessor instanceof TransmissionInformationProcessor)
                                ((TransmissionInformationProcessor) resetProcessor).messageTransmitted(
                                        resetMessage.getToken(), resetMessage.getMessageID(), false);
                        }
                        else{
                            log.error("Message could not be sent!", future.getCause());
                        }
                    }
                });
            }

        }, 0, TimeUnit.MILLISECONDS);

    }


    /**
     * Returns the local port the {@link DatagramChannel} of this {@link CoapClientApplication} is bound to.
     *
     * @return the local port the {@link DatagramChannel} of this {@link CoapClientApplication} is bound to.
     */
    public int getPort() {
        return this.channel.getLocalAddress().getPort();
    }

    /**
     * Shuts this {@link CoapClientApplication} down by closing its {@link DatagramChannel} which includes to unbind
     * this {@link DatagramChannel} from the listening port and by this means free the port.
     */
    public final ChannelFuture shutdown(){
        log.warn("Start to shutdown " + this.name + " (Port : " + this.getPort() + ")");

        InternalApplicationShutdownMessage shutdownMessage = new InternalApplicationShutdownMessage();
        ChannelFuture shutdownFuture = Channels.write(this.channel, shutdownMessage);

        final ChannelFuture channelClosedFuture = this.channel.getCloseFuture();

        shutdownFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.warn("Internal component shutdown completed... Close channel now.");
                if(future.isSuccess()){
                    //Close the datagram datagramChannel (includes unbind)
                    CoapClientApplication.this.channel.close();

                    //Await the closure and let the factory release its external resource to finalize the shutdown
                    channelClosedFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            log.warn("Channel closed.");
                            channel.getFactory().releaseExternalResources();
                            //scheduledExecutorService.shutdownNow();
                            log.warn("Shutdown of " + CoapClientApplication.this.name + " completed.");
                        }
                    });
                }

                else{
                    log.error("Excpetion while shutting application down!", future.getCause());
                }

            }
        });

        return channelClosedFuture.awaitUninterruptibly();

    }

    /**
     * Returns the name of this {@link CoapClientApplication} instance
     *
     * @return the name of this {@link CoapClientApplication} instance
     */
    public String getName() {
        return name;
    }
}
