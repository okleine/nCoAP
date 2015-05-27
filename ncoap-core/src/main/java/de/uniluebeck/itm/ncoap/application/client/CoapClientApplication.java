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
import de.uniluebeck.itm.ncoap.communication.dispatching.client.TokenFactory;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.OutboundMessageWrapper;
import de.uniluebeck.itm.ncoap.communication.reliability.OutboundReliabilityHandler;
import de.uniluebeck.itm.ncoap.message.*;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * An instance of {@link CoapClientApplication} is the entry point to send {@link CoapMessage}s to a (remote)
 * server or proxy.
 * 
 * With {@link #sendCoapRequest(CoapRequest, de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback, InetSocketAddress)} it e.g. provides an
 * easy-to-use method to write CoAP requests to a server.
 * 
 * Furthermore, with {@link #sendCoapPing(de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback, java.net.InetSocketAddress)} it provides a method to test
 * if a remote CoAP endpoint (i.e. the CoAP application and not only the host(!)) is alive.
 * 
 * @author Oliver Kleine
*/
public class CoapClientApplication {

    public static final int RECEIVE_BUFFER_SIZE = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ScheduledThreadPoolExecutor executor;
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
     * @param maxTokenLength the maximum length of
     *                       {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.Token}s to be created by
     *                       the {@link TokenFactory}. The minimum length is <code>0</code>, the maximum length
     *                       (and default value) is <code>8</code>. This can be used to limit the amount of parallel
     *                       message exchanges with one server (see {@link TokenFactory} for details).
     */
    public CoapClientApplication(String name, int port, int numberOfThreads, int maxTokenLength){

        this.name = name;

        if(maxTokenLength < 0 || maxTokenLength > 8)
            throw new IllegalArgumentException("Token length must be between 0 and 8 (both inclusive)");

        int threads = Math.max(numberOfThreads, 4);

        ThreadFactory threadFactory =
                new ThreadFactoryBuilder().setNameFormat(name + " I/O worker #%d").build();

        ThreadRenamingRunnable.setThreadNameDeterminer(new ThreadNameDeterminer() {
            @Override
            public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
                return null;
            }
        });

        this.executor = new ScheduledThreadPoolExecutor(threads, threadFactory);
//        try {
//            this.executor.setRemoveOnCancelPolicy(true);
//        }
//        catch(NoSuchMethodError e){
//            log.error("Method \"setRemoveOnCancelPolicy\" not supported!");
//        }

        TokenFactory tokenFactory = new TokenFactory(maxTokenLength);

        //Create factories for channel and pipeline
        ChannelFactory channelFactory = new NioDatagramChannelFactory(executor, threads/2);
        ClientChannelPipelineFactory clientChannelPipelineFactory =
                new ClientChannelPipelineFactory(executor, tokenFactory);

        //Create and configure bootstrap
        ConnectionlessBootstrap bootstrap = new ConnectionlessBootstrap(channelFactory);
        bootstrap.setPipelineFactory(clientChannelPipelineFactory);
        bootstrap.setOption("receiveBufferSizePredictor",
                new FixedReceiveBufferSizePredictor(RECEIVE_BUFFER_SIZE));

        //Create datagram channel
        this.channel = (DatagramChannel) bootstrap.bind(new InetSocketAddress(port));

        //set the outbound reliability handler with its channel handler context
        ChannelPipeline pipeline = this.channel.getPipeline();
        String handlerName = ClientChannelPipelineFactory.OUTBOUND_RELIABILITY_HANDLER;
        ChannelHandlerContext ctx = pipeline.getContext(handlerName);
        ((OutboundReliabilityHandler) pipeline.get(handlerName)).setChannelHandlerContext(ctx);


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
     * @param maxTokenLength the maximum length of
     *                       {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.Token}s to be created by
     *                       the {@link TokenFactory}. The minimum length is <code>0</code>, the maximum length
     *                       (and default value) is <code>8</code>. This can be used to limit the amount of parallel
     *                       message exchanges with one server (see {@link TokenFactory} for details).
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
     * <code>numberOfThreads = Runtime.getRuntime().availableProcessors() * 2)</code>.
     *
     * @param name the name of the application (used for logging purposes)
     * @param port the port, this {@link CoapClientApplication} should be bound to (use <code>0</code> for
     *             arbitrary port)
     * @param maxTokenLength the maximum length of
     *                       {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.Token}s to be created by
     *                       the {@link TokenFactory}. The minimum length is <code>0</code>, the maximum length
     *                       (and default value) is <code>8</code>. This can be used to limit the amount of parallel
     *                       message exchanges with one server (see {@link TokenFactory} for details).
     */
    public CoapClientApplication(String name, int port, int maxTokenLength){
        this(name, port, Runtime.getRuntime().availableProcessors() * 2, maxTokenLength);
    }


    /**
     * Sends a {@link de.uniluebeck.itm.ncoap.message.CoapRequest} to the given remote endpoints, i.e. CoAP server or
     * proxy, and registers the given {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback}
     * to be called upon reception of a {@link de.uniluebeck.itm.ncoap.message.CoapResponse}.
     *
     * <b>Note:</b> Override {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback
     * #continueObservation(InetSocketAddress, Token)} on the given callback for observations!
     *
     * @param coapRequest the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} to be sent
     *
     * @param clientCallback the {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback} to process the corresponding response, resp.
     *                              update notification (which are also instances of {@link CoapResponse}.
     *
     * @param remoteEndpoint the desired recipient of the given {@link de.uniluebeck.itm.ncoap.message.CoapRequest}
     */
    public void sendCoapRequest(final CoapRequest coapRequest, final ClientCallback clientCallback,
                                final InetSocketAddress remoteEndpoint){

        executor.submit(new Runnable() {

            @Override
            public void run() {
                OutboundMessageWrapper message = new OutboundMessageWrapper(coapRequest, clientCallback);

                ChannelFuture future = Channels.write(channel, message, remoteEndpoint);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            log.debug("Sent to {}:{}: {}",
                                    new Object[]{remoteEndpoint.getAddress().getHostAddress(),
                                            remoteEndpoint.getPort(), coapRequest});
                        }
                    }
                });
            }

        });
    }


    /**
     * Sends a CoAP PING, i.e. a {@link de.uniluebeck.itm.ncoap.message.CoapMessage} with
     * {@link de.uniluebeck.itm.ncoap.message.MessageType.Name#CON} and
     * {@link de.uniluebeck.itm.ncoap.message.MessageCode.Name#EMPTY} to the given CoAP endpoints and registers the
     * given {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback}
     * to be called upon reception of the corresponding {@link de.uniluebeck.itm.ncoap.message.MessageType.Name#RST}
     * message (CoAP PONG).
     *
     * Make sure to override {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback
     * #processReset()} to handle the CoAP PONG!
     *
     * @param clientCallback the {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback} to be
     *                       called upon reception of the corresponding
     *                       {@link de.uniluebeck.itm.ncoap.message.MessageType.Name#RST} message.
     *                       <br><br>
     *                       <b>Note:</b> To handle the CoAP PONG, i.e. the empty RST, the method
     *                       {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback
     *                       #processReset()} MUST be overridden
     * @param remoteEndpoint the desired recipient of the CoAP PING message
     */
    public void sendCoapPing(final ClientCallback clientCallback, final InetSocketAddress remoteEndpoint){

        executor.submit(new Runnable() {

            @Override
            public void run() {

                final CoapMessage coapPing = CoapMessage.createPing(CoapMessage.UNDEFINED_MESSAGE_ID);
                OutboundMessageWrapper wrapper = new OutboundMessageWrapper(coapPing, clientCallback);

                ChannelFuture future = Channels.write(channel, wrapper, remoteEndpoint);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            Throwable cause = future.getCause();
                            log.error("Error with CoAP ping!", cause);
                            String description = cause == null ? "UNEXPECTED ERROR!" : cause.getMessage();
                            clientCallback.processMiscellaneousError(description);
                        }
                    }
                });
            }
        });

    }


    /**
     * Returns the local port number the {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} is bound to.
     *
     * @return the local port number the {@link org.jboss.netty.channel.socket.DatagramChannel} of this
     * {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} is bound to.
     */
    public int getPort() {
        return this.channel.getLocalAddress().getPort();
    }

    /**
     * Shuts this {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} down by closing its
     * {@link org.jboss.netty.channel.socket.DatagramChannel} which includes to unbind
     * this {@link org.jboss.netty.channel.socket.DatagramChannel} from the listening port and by this means free the
     * port.
     */
    public final void shutdown(){
        log.warn("Start to shutdown " + this.getName() + " (Port : " + this.getPort() + ")");

        this.channel.close().awaitUninterruptibly().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.warn("Channel closed ({}).", CoapClientApplication.this.getName());
                channel.getFactory().releaseExternalResources();
                log.warn("External resources released ({}).", CoapClientApplication.this.getName());
                log.warn("Shutdown of " + CoapClientApplication.this.name + " completed.");
            }
        });
    }

    /**
     * Returns the name of this {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} instance
     *
     * @return the name of this {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} instance
     */
    public String getName() {
        return name;
    }
}
