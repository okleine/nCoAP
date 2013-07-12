/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.util.concurrent.*;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.ncoap.application.server.webservice.WebService;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.ncoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.ncoap.communication.observe.InternalObservableResourceRegistrationMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
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

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.OBSERVE_REQUEST;

/**
 * An instance of {@link CoapServerApplication} is the component to enable instances of {@link WebService} to
 * communicate with the outside world, i.e. the Internet. Once a {@link CoapServerApplication} was instanciated
 * one can register {@link WebService} instances and by this means make them available at their specified path.
 *
 * Each instance of {@link CoapServerApplication} is automatically bound to a local port to listen at for
 * incoming requests.
 *
 * @author Oliver Kleine
 */
public class CoapServerApplication extends SimpleChannelUpstreamHandler {

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private DatagramChannel channel;

    //This map holds all registered webServices (key: URI path, value: WebService instance)
    private HashMap<String, WebService> registeredServices = new HashMap<String, WebService>();

    private ListeningExecutorService listeningExecutorService;
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

        this.scheduledExecutorService = ioExecutorService;
        this.listeningExecutorService = MoreExecutors.listeningDecorator(scheduledExecutorService);

        registerService(new WellKnownCoreResource(registeredServices));

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
        this.scheduledExecutorService = executorService;
        this.listeningExecutorService = MoreExecutors.listeningDecorator(executorService);
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
        log.debug("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

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


        final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();
        webService.processCoapRequest(responseFuture, coapRequest, remoteAddress);

        responseFuture.addListener(new Runnable(){
            @Override
            public void run() {
                CoapResponse coapResponse;
                try {
                    coapResponse = responseFuture.get();

                    if(coapResponse.getCode().isErrorMessage()){
                        sendCoapResponse(coapResponse, remoteAddress);
                        return;
                    }

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
        }, listeningExecutorService);

    }

    private void sendCoapResponse(final CoapResponse coapResponse, final InetSocketAddress remoteAddress){
        //Write response
        ChannelFuture future = channel.write(coapResponse, remoteAddress);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.debug("Response for token {} successfully sent to recipient {}.",
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
     *
     * Prior to doing so this methods removes all registered {@link WebService} instances from the server, i.e.
     * invokes the {@link WebService#shutdown()} method of all registered services.
     */
    public void shutdown() throws InterruptedException {

        //remove all webServices
        WebService[] services;
        synchronized (this){
            services = new WebService[registeredServices.values().size()];
            registeredServices.values().toArray(services);
        }

        for(WebService service : services){
            removeService(service.getPath());
        }

        //some time to send possible update notifications (404_NOT_FOUND) to observers
        Thread.sleep(1000);

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

        webService.setScheduledExecutorService(scheduledExecutorService);
        webService.setListeningExecutorService(listeningExecutorService);
    }
    
    /**
     * Removes the {@link WebService} instance registered at the given path from the server
     * 
     * @param uriPath the path of the {@link WebService} instance to be removed
     *
     * @return <code>true</code> if the service was removed succesfully, <code>false</code> otherwise.
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
     * Removes all registered {@link WebService} instances from the server.
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