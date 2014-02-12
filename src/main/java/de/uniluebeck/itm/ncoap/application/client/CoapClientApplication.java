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
import de.uniluebeck.itm.ncoap.application.TokenFactory;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.TransmissionInformationProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
* An instance of {@link CoapClientApplication} is the entry point to send {@link CoapRequest}s. By
* {@link #writeCoapRequest(CoapRequest, CoapResponseProcessor, InetSocketAddress)} it provides an
* easy-to-use method to write CoAP requests to a server.
*
* Each instance of {@link CoapClientApplication} is automatically bound to a (random) available local port.
*
* @author Oliver Kleine
*/
public class CoapClientApplication extends SimpleChannelUpstreamHandler {

    public static final int RECEIVE_BUFFER_SIZE = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ScheduledThreadPoolExecutor scheduledExecutorService;
    private DatagramChannel channel;


    public CoapClientApplication(int port, int numberOfThreads, int maxTokenLength){

        if(maxTokenLength < 0 || maxTokenLength > 8)
            throw new IllegalArgumentException("Token length must be between 0 and 8 (both inclusive)");

        int threads = Math.max(numberOfThreads, 4);

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("New I/O worker #%d (out)").build();
        //this.scheduledExecutorService = Executors.newScheduledThreadPool(threads, threadFactory);
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

        log.info("New client channel created for address {}", this.channel.getLocalAddress());
    }


    /**
     * Creates a new instance of {@link CoapClientApplication} which is bound to a local socket and provides all
     * functionality to send {@link CoapRequest}s and receive {@link CoapResponse}s.
     */
    public CoapClientApplication(){
        this(0, 8);
    }


    public CoapClientApplication(int port, int maxTokenLength){
        this(port, Runtime.getRuntime().availableProcessors() * 2, maxTokenLength);
    }


    /**
     *
     * @param coapRequest
     * @param coapResponseProcessor
     * @param remoteSocketAddress
     */
    public void writeCoapRequest(final CoapRequest coapRequest, final CoapResponseProcessor coapResponseProcessor,
                                 final InetSocketAddress remoteSocketAddress){

        scheduledExecutorService.schedule(new Runnable(){

            @Override
            public void run() {
                InternalCoapRequestToBeSentMessage message =
                        new InternalCoapRequestToBeSentMessage(coapRequest, coapResponseProcessor);

                ChannelFuture future = Channels.write(channel, message, remoteSocketAddress);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.debug("Sent to {}:{}: {}",
                                new Object[]{remoteSocketAddress.getAddress().getHostAddress(),
                                        remoteSocketAddress.getPort(), coapRequest});

                        if (coapResponseProcessor instanceof TransmissionInformationProcessor)
                            ((TransmissionInformationProcessor) coapResponseProcessor).messageTransmitted(false);
                    }
                });
            }

        }, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the local port the {@link DatagramChannel} of this {@link CoapClientApplication} is bound to.
     * @return the local port the {@link DatagramChannel} of this {@link CoapClientApplication} is bound to.
     */
    public int getClientPort() {
        return this.channel.getLocalAddress().getPort();
    }

    /**
     * Shuts the client down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public final void shutdown(){
        log.warn("Start to shutdown client...");

        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture channelClosedFuture = this.channel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        channelClosedFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channel.getFactory().releaseExternalResources();
                log.info("External resources released. Shutdown completed.");
            }
        });

        channelClosedFuture.awaitUninterruptibly().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Shutdown completed!");
            }
        });
    }
}
