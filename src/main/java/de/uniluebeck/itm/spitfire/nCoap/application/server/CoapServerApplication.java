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

package de.uniluebeck.itm.spitfire.nCoap.application.server;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.WebService;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.observe.InternalObservableResourceRegistrationMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.*;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_REQUEST;

/**
 * Even though the communication is based on the Netty
 * framework, a developer of such a server doesn't have to go into details regarding the architecture. The whole
 * architecture is hidden from the users perspective. Technically speaking, the extending class will be the
 * topmost {@link ChannelUpstreamHandler} of the automatically generated netty handler stack.
 *
 * @author Oliver Kleine
 */
public class CoapServerApplication extends SimpleChannelUpstreamHandler {

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;
    //public static final int NUMBER_OF_THREADS_TO_HANDLE_REQUESTS = 1000;

    private static Logger log = LoggerFactory.getLogger(CoapServerApplication.class.getName());

    private DatagramChannel channel;


    //This map holds all registered webServices (key: URI path, value: WebService instance)
    private HashMap<String, WebService> registeredServices = new HashMap<String, WebService>();

    private ListeningExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on the given port
     * and already provides the default <code>.well-known/core</code> resource
     */
    public CoapServerApplication(int serverPort){

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP Server I/O Thread#%d").build();

        ScheduledExecutorService ioExecutorService =
                Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory);

        CoapServerDatagramChannelFactory factory = new CoapServerDatagramChannelFactory(ioExecutorService, serverPort);
        channel = factory.getChannel();

        channel.getPipeline().addLast("Server Application", this);

        registerService(new WellKnownCoreResource(registeredServices));

        this.scheduledExecutorService = ioExecutorService;
        this.executorService = MoreExecutors.listeningDecorator(scheduledExecutorService);

        log.info("New server created. Listening on port {}.", getServerPort());

    }

    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on port
     * {@link #DEFAULT_COAP_SERVER_PORT} and already provides the default <code>.well-known/core</code> resource.
     */
    public CoapServerApplication(){
        this(DEFAULT_COAP_SERVER_PORT);
    }

      /**
     * Set the {@link ScheduledExecutorService} instance to handle incoming requests in seperate threads. The
     * nCoAP framework sets an executor service automatically so usually there is no need to set another one.
     *
     * @param executorService a {@link ScheduledExecutorService}
     */
    public void setExecutorService(ScheduledExecutorService executorService){
        this.executorService = MoreExecutors.listeningDecorator(executorService);
        this.scheduledExecutorService = executorService;
    }

    /**
     * This method is called by the Netty framework whenever a new message is received to be processed by the server.
     * For each incoming request a new Thread is created to handle the request (by invoking the method
     * <code>receiveCoapRequest</code>).
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent me){
        log.info("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(!(me.getMessage() instanceof CoapRequest)){
            ctx.sendUpstream(me);
            return;
        }

        me.getFuture().setSuccess();

        final CoapRequest coapRequest = (CoapRequest) me.getMessage();
        final InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

        //Look up web service instance to handle the request
        final WebService webService = registeredServices.get(coapRequest.getTargetUri().getPath());

        //Write error response if no such web service exists
        if(webService == null){
            log.info("Requested service {} not found. Send 404 error response to {}.",
                    coapRequest.getTargetUri().getPath(), me.getRemoteAddress());

            //Write error response if there is no such webservice instance registered at this server instance
            CoapResponse coapResponse = new CoapResponse(Code.NOT_FOUND_404);
            coapResponse.setServicePath(coapRequest.getTargetUri().getPath());
            try {
                coapResponse.setToken(coapRequest.getToken());
            } catch (Exception e) {
                log.error("This should never happen.", e);
            }

            sendCoapResponse(coapResponse, (InetSocketAddress) me.getRemoteAddress());
            return;
        }

        //process the request in a new thread
        final ListenableFuture <CoapResponse> executionFuture =
                executorService.submit(new Callable<CoapResponse>() {
            @Override
            public CoapResponse call() throws Exception {
                return webService.processMessage(coapRequest, remoteAddress);
            }
        });

        executionFuture.addListener(new Runnable(){
            @Override
            public void run() {
                CoapResponse coapResponse;
                try {
                    coapResponse = executionFuture.get();
                    coapResponse.setMaxAge(webService.getMaxAge());
                    coapResponse.setServicePath(webService.getPath());

                    //Set message ID and token to match the request
                    coapResponse.setMessageID(coapRequest.getMessageID());
                    if(coapRequest.getToken().length > 0){
                        coapResponse.setToken(coapRequest.getToken());
                    }

                    //Set content type if there is payload but no content type
                    if(coapResponse.getPayload().readableBytes() > 0 && coapResponse.getContentType() == null)
                        coapResponse.setContentType(MediaType.TEXT_PLAIN_UTF8);

                    //Set observe response option if requested
                    if(webService instanceof ObservableWebService && !coapRequest.getOption(OBSERVE_REQUEST).isEmpty())
                        if(!coapResponse.getCode().isErrorMessage())
                            coapResponse.setObserveOptionValue(0);
                }
                catch (Exception ex) {
                    coapResponse = new CoapResponse(Code.INTERNAL_SERVER_ERROR_500);
                    try {
                        coapResponse.setMessageID(coapRequest.getMessageID());
                        coapResponse.setToken(coapRequest.getToken());
                        coapResponse.setServicePath(webService.getPath());
                        StringWriter errors = new StringWriter();
                        ex.printStackTrace(new PrintWriter(errors));
                        coapResponse.setPayload(errors.toString().getBytes(Charset.forName("UTF-8")));
                    } catch (Exception e) {
                        log.error("This should never happen.", e);
                    }
                }

                //Send the response
                sendCoapResponse(coapResponse, remoteAddress);
            }
        }, executorService);

    }

    private void sendCoapResponse(final CoapResponse coapResponse, final InetSocketAddress remoteAddress){
        //Write response
        ChannelFuture future = channel.write(coapResponse, remoteAddress);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Response for token {} successfully sent to recipient {}.",
                        new ByteArrayWrapper(coapResponse.getToken()), remoteAddress);
            }
        });
    }

    /**
     * This method is invoked by the framework if an exception occured during the reception or sending of a
     * {@link CoapMessage}.
     *
     * Classes extending {@link CoapServerApplication} may override this method to, e.g. deal with different
     * types of exceptions differently. This implementation only logs an the error message (if logging is
     * enabled).
     *
     * @param ctx the {@link ChannelHandlerContext} relating the {@link CoapServerApplication} and the
     * {@link DatagramChannel} on which the exception occured
     *
     * @param e the {@link ExceptionEvent} containing, e.g. the exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
        log.error("Exception while processing I/O task.", e.getCause());
    }

    /**
     * Shuts the server down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public void shutdown(){

        //remove all webServices
        WebService[] services;
        synchronized (this){
            services = new WebService[registeredServices.values().size()];
            registeredServices.values().toArray(services);
        }

        for(WebService service : services){
            removeService(service.getPath());
        }

        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture future = channel.close();

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

        executorService.shutdownNow();
    }
    

    /**
     * Registers a WebService instance at the server. After registration the service will be available at the path
     * given as <code>service.getPath()</code>.
     *
     * It is not possible to register multiple webServices at a single path. If a new service is registered at the server
     * with a path from another already registered service, then the new service replaces the old one.
     *
     * @param webService A {@link WebService} instance to be registered at the server
     */
    public final void registerService(final WebService webService) {
        registeredServices.put(webService.getPath(), webService);
        log.info("Registered new service at " + webService.getPath());

        if(webService instanceof ObservableWebService){
            InternalObservableResourceRegistrationMessage message =
                    new InternalObservableResourceRegistrationMessage((ObservableWebService) webService);

            ChannelFuture future = Channels.write(channel, message);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Registered {} at observable resource handler.", webService.getPath());
                }
            });
        }

        webService.setExecutorService(scheduledExecutorService);
    }
    
    /**
     * Removes a service from the server
     * 
     * @param uriPath service path
     * @return true if a registered service was removed
     */
    public synchronized boolean removeService(String uriPath) {
        WebService removedService = registeredServices.remove(uriPath);

        if(removedService != null && removedService instanceof ObservableWebService){
            channel.write(new InternalServiceRemovedFromServerMessage(uriPath));
            removedService.shutdown();
        }

        if(removedService != null)
            log.info("Service {} removed from server with port {}.", uriPath, getServerPort());
        else
            log.info("Service {} could not be removed. Does not exist on port {}.", uriPath, getServerPort());

        return removedService == null;
    }
    
    /**
     * Removes all webServices from server.
     */
    public void removeAllServices() {

        WebService[] services;
        synchronized (this){
            services = new WebService[registeredServices.values().size()];
            registeredServices.values().toArray(services);
        }

        for (String path : registeredServices.keySet()) {
            removeService(path);
        }

        if(!registeredServices.isEmpty()){
            log.error("All Webservices should be removed but there are {} left.", registeredServices.size());
        }
    }

    public int getServerPort(){
        return channel.getLocalAddress().getPort();
    }
}