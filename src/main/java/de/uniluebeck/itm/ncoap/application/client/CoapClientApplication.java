/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
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

import com.google.common.collect.HashBasedTable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.ncoap.communication.blockwise.InternalNextBlockReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.blockwise.InternalNextBlockReceivedMessageProcessor;
import de.uniluebeck.itm.ncoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.*;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * An instance of {@link CoapClientApplication} is the entry point to send {@link CoapRequest}s. By
 * {@link #writeCoapRequest(CoapRequest, CoapResponseProcessor)} it provides an
 * easy-to-use method to write CoAP requests to a server.
 *
 * Each instance of {@link CoapClientApplication} is automatically bound to a (random) available local port.
 *
 * @author Oliver Kleine
 */
public class CoapClientApplication extends SimpleChannelUpstreamHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private TokenFactory tokenFactory = new TokenFactory();
    private HashBasedTable<ByteArrayWrapper, InetSocketAddress, CoapResponseProcessor> responseProcessors =
            HashBasedTable.create();

    private DatagramChannel datagramChannel;

    private ScheduledExecutorService executorService;

    /**
     * Creates a new instance of {@link CoapClientApplication} which is bound to a local socket and provides all
     * functionality to send {@link CoapRequest}s and receive {@link CoapResponse}s.
     */
    public CoapClientApplication(){
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP Client I/O Thread#%d").build();
        this.executorService =
                Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory);

        CoapClientDatagramChannelFactory factory = new CoapClientDatagramChannelFactory(executorService);

        datagramChannel = factory.getChannel();
        datagramChannel.getPipeline().addLast("Client Application", this);


        log.info("New CoAP client on port {}.", datagramChannel.getLocalAddress().getPort());
    }

    /**
     * This method is to send a CoAP request to a remote
     * recipient. All necessary information to send the message (like the recipient IP address or port) is
     * automatically extracted from the given {@link CoapRequest} instance.
     *
     * @param coapRequest The {@link CoapRequest} object to be sent
     * @param coapResponseProcessor The {@link CoapResponseProcessor} instance to handle responses and
     *                              status information
     */
    public void writeCoapRequest(final CoapRequest coapRequest, final CoapResponseProcessor coapResponseProcessor)
            throws ToManyOptionsException, InvalidOptionException {

        executorService.submit(new Runnable(){

            @Override
            public void run() {
                try {
                    coapRequest.setToken(tokenFactory.getNextToken());

                    int targetPort = coapRequest.getTargetUri().getPort();
                    if(targetPort == -1)
                        targetPort = OptionRegistry.COAP_PORT_DEFAULT;


                    final InetSocketAddress rcptSocketAddress =
                            new InetSocketAddress(coapRequest.getTargetUri().getHost(), targetPort);

                    addResponseCallback(coapRequest.getToken(), rcptSocketAddress, coapResponseProcessor);

                    ChannelFuture future = Channels.write(datagramChannel, coapRequest, rcptSocketAddress);

                    future.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            log.info("Sent to to {}:{}: {}",
                                    new Object[]{rcptSocketAddress.getAddress().getHostAddress(),
                                            rcptSocketAddress.getPort(), coapRequest});

                            if(coapResponseProcessor instanceof RetransmissionProcessor)
                                ((RetransmissionProcessor) coapResponseProcessor).requestSent();
                        }
                    });

                } catch (Exception e) {
                    log.error("Exception while trying to send message.", e);
                }
            }
        });

    }

    /**
     * Returns the local port the {@link DatagramChannel} of this {@link CoapClientApplication} is bound to.
     * @return the local port the {@link DatagramChannel} of this {@link CoapClientApplication} is bound to.
     */
    public int getClientPort() {
        return datagramChannel.getLocalAddress().getPort();
    }

    /**
     * Shuts the client down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public final void shutdown(){
        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture future = datagramChannel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                DatagramChannel closedChannel = (DatagramChannel) future.getChannel();
                log.info("Client channel closed (port: " + closedChannel.getLocalAddress().getPort() + ").");

                executorService.shutdownNow();

                datagramChannel.getFactory().releaseExternalResources();
                log.info("External resources released. Shutdown completed.");
            }
        });

        future.awaitUninterruptibly();
    }

    private synchronized void addResponseCallback(byte[] token, InetSocketAddress remoteAddress,
                                                  CoapResponseProcessor coapResponseProcessor){
        responseProcessors.put(new ByteArrayWrapper(token), remoteAddress, coapResponseProcessor);
        log.debug("Number of clients waiting for response: {}. ", responseProcessors.size());
    }


    private synchronized CoapResponseProcessor removeResponseCallback(byte[] token, InetSocketAddress remoteAddress){
        CoapResponseProcessor result = responseProcessors.remove(new ByteArrayWrapper(token), remoteAddress);
        log.debug("Number of clients waiting for response: {}. ", responseProcessors.size());
        return result;
    }

    /**
     * This method is automaically invoked by the framework and relates incoming responses to open requests and invokes
     * the appropriate method of the {@link CoapResponseProcessor} instance given with the {@link CoapRequest}.
     *
     * The invoked method depends on the message contained in the {@link MessageEvent}. See
     * {@link CoapResponseProcessor} for more details on possible status updates.
     *
     * @param ctx The {@link ChannelHandlerContext} to relate this handler to the {@link Channel}
     * @param me The {@link MessageEvent} containing the {@link CoapMessage}
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        log.debug("Received: {}.", me.getMessage());

        if(me.getMessage() instanceof InternalEmptyAcknowledgementReceivedMessage){
            InternalEmptyAcknowledgementReceivedMessage message =
                    (InternalEmptyAcknowledgementReceivedMessage) me.getMessage();

            //find proper callback
            CoapResponseProcessor callback =
                    responseProcessors.get(message.getToken(), me.getRemoteAddress());

            if(callback != null && callback instanceof EmptyAcknowledgementProcessor)
                ((EmptyAcknowledgementProcessor) callback).processEmptyAcknowledgement(message);

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalRetransmissionTimeoutMessage){
            InternalRetransmissionTimeoutMessage timeoutMessage = (InternalRetransmissionTimeoutMessage) me.getMessage();

            //Find proper callback
            CoapResponseProcessor callback =
                    removeResponseCallback(timeoutMessage.getToken(), timeoutMessage.getRemoteAddress());

            //Invoke method of callback instance
            if(callback != null && callback instanceof RetransmissionTimeoutProcessor)
                ((RetransmissionTimeoutProcessor) callback).processRetransmissionTimeout(timeoutMessage);

            //pass the token back
            tokenFactory.passBackToken(timeoutMessage.getToken());

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalMessageRetransmissionMessage){
            InternalMessageRetransmissionMessage retransmissionMessage =
                    (InternalMessageRetransmissionMessage) me.getMessage();

            CoapResponseProcessor callback =
                    responseProcessors.get(retransmissionMessage.getToken(), retransmissionMessage.getRemoteAddress());

            if(callback != null && callback instanceof RetransmissionProcessor)
                ((RetransmissionProcessor) callback).requestSent();

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalNextBlockReceivedMessage){
            InternalNextBlockReceivedMessage message = (InternalNextBlockReceivedMessage) me.getMessage();

            CoapResponseProcessor callback =
                    responseProcessors.get(message.getToken(), me.getRemoteAddress());

            if(callback != null && callback instanceof InternalNextBlockReceivedMessageProcessor)
                ((InternalNextBlockReceivedMessageProcessor) callback).receivedNextBlock();

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse coapResponse = (CoapResponse) me.getMessage();

            log.debug("Response received: {}.", coapResponse);
            log.debug("Remote address is IPv6: {}",
                    ((InetSocketAddress) me.getRemoteAddress()).getAddress() instanceof Inet6Address);
            log.debug("InetAddress: {}.", ((InetSocketAddress) me.getRemoteAddress()).getAddress().toString());

            CoapResponseProcessor callback;

            if(coapResponse.isUpdateNotification())
                callback = responseProcessors.get(new ByteArrayWrapper(coapResponse.getToken()), me.getRemoteAddress());
            else
                callback = removeResponseCallback(coapResponse.getToken(), (InetSocketAddress) me.getRemoteAddress());

            if(callback != null){
                log.debug("Callback found for token {}.", new ByteArrayWrapper(coapResponse.getToken()));
                callback.processCoapResponse(coapResponse);
            }
            else{
                log.debug("No callback found for token {}.", new ByteArrayWrapper(coapResponse.getToken()));
            }

            //pass the token back
            tokenFactory.passBackToken(coapResponse.getToken());

            me.getFuture().setSuccess();
        }

        else{
            me.getFuture().setFailure(new RuntimeException("Could not deal with message "
                    + me.getMessage().getClass().getName()));
        }
    }


}
