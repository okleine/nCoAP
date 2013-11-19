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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.ncoap.application.server.webservice.ContentFormatNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.ncoap.communication.codec.InternalCodecExceptionMessage;
import de.uniluebeck.itm.ncoap.communication.codec.DecodingException;
import de.uniluebeck.itm.ncoap.communication.codec.EncodingException;
import de.uniluebeck.itm.ncoap.communication.observe.InternalObservableResourceRegistrationMessage;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.OpaqueOption;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


/**
* An instance of {@link CoapServerApplication} is the component to enable instances of {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} to
* communicate with the outside world, i.e. the Internet. Once a {@link CoapServerApplication} was instanciated
* one can register {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances and by this means make them available at their specified path.
*
* Each instance of {@link CoapServerApplication} is automatically bound to a local port to listen at for
* incoming requests.
*
* @author Oliver Kleine
*/
public class CoapServerApplication extends SimpleChannelUpstreamHandler {

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //private DatagramChannel channel;
    private Multimap<InetSocketAddress, CoapRequest> openRequests =
            Multimaps.synchronizedMultimap(HashMultimap.<InetSocketAddress, CoapRequest>create());

    //This map holds all registered webservice (key: URI path, value: Webservice instance)
    private HashMap<String, Webservice> registeredServices = new HashMap<>();

    private ListeningExecutorService listeningExecutorService;
    private ScheduledExecutorService scheduledExecutorService;

    private WebServiceCreator webServiceCreator;

    private Set<Channel> serverChannels;

    public CoapServerApplication(InetSocketAddress... listeningSockets){

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP Server I/O Thread#%d").build();

        int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
        log.info("No. of I/O Threads: {}", numberOfThreads);

        ScheduledExecutorService ioExecutorService =
                Executors.newScheduledThreadPool(numberOfThreads, threadFactory);

        this.serverChannels = new HashSet<>();

        for(InetSocketAddress listeningSocket : listeningSockets){
            CoapServerDatagramChannelFactory factory =
                    new CoapServerDatagramChannelFactory(ioExecutorService, listeningSocket, this);

            serverChannels.add(factory.getChannel());
//            channel = factory.getChannel();
//            channel.getPipeline().addLast("Server Application", this);

            log.info("New server created. Listening at {}.", listeningSocket);
        }

        this.scheduledExecutorService = ioExecutorService;
        this.listeningExecutorService = MoreExecutors.listeningDecorator(scheduledExecutorService);

        this.webServiceCreator = new DefaultWebServiceCreator(this);

        registerService(new WellKnownCoreResource(registeredServices));
    }


    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on the given port
     * and already provides the default <code>.well-known/core</code> resource
     */
    public CoapServerApplication(int serverPort){
        this(new InetSocketAddress(serverPort));
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
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof InternalCodecExceptionMessage){
            InternalCodecExceptionMessage message = (InternalCodecExceptionMessage) me.getMessage();
            handleCodecException(ctx, (InetSocketAddress) me.getRemoteAddress(), message);
            return;
        }

        if(!(me.getMessage() instanceof CoapRequest)){
            log.warn("Server received inadequeate message: {}", me.getMessage());
            ctx.sendUpstream(me);
            return;
        }

        me.getFuture().setSuccess();

        final CoapRequest coapRequest = (CoapRequest) me.getMessage();
        final InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();

        //log.debug("Contains key: {}", openRequests.containsKey(remoteSocketAddress));
        //log.debug("Contains value: {}", openRequests.containsValue(coapRequest));

        //Avoid duplicate request processing!
        if(openRequests.containsEntry(me.getRemoteAddress(), coapRequest)){
            log.warn("Received duplicate request (IGNORE): {}", coapRequest);
            return;
        }

        //Create settable future to wait for response
        final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();

        //Look up web service instance to handle the request
        final Webservice webservice = registeredServices.get(coapRequest.getUriPath());

        responseFuture.addListener(new CoapResponseSender(responseFuture, coapRequest, webservice, ctx,
                remoteSocketAddress), listeningExecutorService);

        //Write error response if no such web service exists
        if(webservice == null){

            if(coapRequest.getMessageCodeName() == MessageCode.Name.PUT){
                //Create new Webservice
                log.debug("Incoming request to create resource");
                this.webServiceCreator.handleWebServiceCreationRequest(responseFuture, coapRequest);
            }

            else{
                //Write error response if there is no such webservice instance registered at this server instance
                try {
                    log.info("Requested service {} not found. Send 404 error response to {}.",
                            coapRequest.getUriPath(), me.getRemoteAddress());
                    responseFuture.set(new CoapResponse(MessageCode.Name.NOT_FOUND_404));
                }
                catch (InvalidHeaderException e) {
                    log.error("This should never happen.", e);
                    responseFuture.setException(e);
                }
            }
        }

        //The IF-NON-MATCH option indicates that the request is only to be processed if the webservice does not (yet)
        //exist. But it does. So send an error response
        if(coapRequest.isIfNonMatchSet()){
            try {
                String message = "IF-NONE-MATCH option was set but service at path " + coapRequest.getUriPath()
                        + " exists.";
                log.debug(message);

                CoapResponse coapResponse = new CoapResponse(MessageCode.Name.PRECONDITION_FAILED_412);
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET));
                responseFuture.set(coapResponse);
            }
            catch (InvalidHeaderException | InvalidMessageException e) {
                log.error("This should never happen.", e);
                responseFuture.setException(e);
            }
        }


        //Check for ETAG related options
        byte[] currentEtag = webservice.getEtag();

        //Check for IF-MATCH option
        for(byte[] etag : coapRequest.getIfMatch()){
            //EMPTY string means existence of the service
            if(etag.length == 0 || Arrays.equals(etag, currentEtag)){
                log.debug("ETAG from IF-MATCH option matches: " + OpaqueOption.toHexString(etag));
                webservice.processCoapRequest(responseFuture, coapRequest, remoteSocketAddress);
                break;
            }
            else{
                log.debug("ETAG from IF-MATCH option does not match: " + OpaqueOption.toHexString(etag));
            }
        }

        //This is only reached if there is no matching ETAG contained as IF-MATCH option
        if(!coapRequest.getIfMatch().isEmpty()){
            try {
                String message = "None of the ETAGs contained as IF-MATCH option matches."
                        + " Current ETAG is " + OpaqueOption.toHexString(webservice.getEtag());
                log.debug(message);
                CoapResponse coapResponse = new CoapResponse(MessageCode.Name.PRECONDITION_FAILED_412);
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET));
                responseFuture.set(coapResponse);
            }
            catch (InvalidHeaderException | InvalidMessageException e) {
                log.error("This should never happen.", e);
                return;
            }

        }

        //Check for ETAG options
        for(byte[] etag : coapRequest.getEtags()){
            if(Arrays.equals(etag, currentEtag)){
                try {
                    log.debug("Valid ETAG option found in request: {}", OpaqueOption.toHexString(etag));
                    CoapResponse coapResponse = new CoapResponse(MessageCode.Name.VALID_203);
                    //coapResponse.setMessageID(coapRequest.getMessageID());
                    //coapResponse.setEtag(etag);
                    responseFuture.set(coapResponse);
                }
                catch (InvalidHeaderException e) {
                    log.error("This should never happen.", e);
                    return;
                }
            }

            log.debug("ETAG is not valid (anymore?): ", OpaqueOption.toHexString(etag));
        }

        boolean added = openRequests.put((InetSocketAddress) me.getRemoteAddress(), coapRequest);
        if(added)
            log.warn("Added request from {} to list of open requests: {}", me.getRemoteAddress(), coapRequest);
        else
            log.error("NOT ADDED!");

        webservice.processCoapRequest(responseFuture, coapRequest, remoteSocketAddress);
    }

    private void handleCodecException(ChannelHandlerContext ctx, InetSocketAddress remoteAddress,
                                      InternalCodecExceptionMessage message) {

        try{
            Throwable ex = message.getCause();
            CoapResponse coapResponse;
            byte[] content;

            //Handle excpetions from incoming message decoding
            if(ex instanceof DecodingException){

                if(ex.getCause() != null && ex.getCause() instanceof InvalidOptionException)
                    coapResponse = new CoapResponse(MessageCode.Name.BAD_OPTION_402);
                else
                    coapResponse = new CoapResponse(MessageCode.Name.BAD_REQUEST_400);


                if(message.getMessageType() == MessageType.Name.CON.getNumber())
                    coapResponse.setMessageType(MessageType.Name.ACK.getNumber());
                else
                    coapResponse.setMessageType(MessageType.Name.NON.getNumber());


                content = ex.getCause().getMessage().getBytes(CoapMessage.CHARSET);

            }

            //Handle excpetions from outgoing  message encoding (actually this should never happen!)
            else if(ex instanceof EncodingException){

                log.error("This should never happen!", ex.getCause());
                coapResponse = new CoapResponse(MessageCode.Name.INTERNAL_SERVER_ERROR_500);
                coapResponse.setMessageType(message.getMessageType());

                content = ex.getCause().getMessage().getBytes(CoapMessage.CHARSET);
            }

            else{

                coapResponse = new CoapResponse(MessageCode.Name.INTERNAL_SERVER_ERROR_500);
                content = ex.getCause().getMessage().getBytes(CoapMessage.CHARSET);

            }

            //Set the message ID and the content of the error message
            coapResponse.setMessageID(message.getMessageID());
            coapResponse.setContent(content);


            sendCoapResponse(ctx, coapResponse, remoteAddress);

        }
        catch (InvalidHeaderException | InvalidMessageException e1) {
            log.error("This should never happen.", e1);
        }
    }

    private void sendCoapResponse(final ChannelHandlerContext ctx, final CoapResponse coapResponse,
                                  final InetSocketAddress remoteAddress){

        //Write response
        log.info("Write response to {}: {}", remoteAddress, coapResponse);
        ChannelFuture future = Channels.write(ctx.getChannel(), coapResponse, remoteAddress);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.debug("Response for token {} successfully sent to recipient {}.",
                        coapResponse.getTokenAsHexString(), remoteAddress);
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
        log.error("Unsupported exception caught! Don't know what to do...", e.getCause());
    }

    /**
     * Shuts the server down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     *
     * Prior to doing so this methods removes all registered {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances from the server, i.e.
     * invokes the {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice#shutdown()} method of all registered services.
     */
    public void shutdown() throws InterruptedException {

        //remove all webservices
        Webservice[] services;
        synchronized (this){
            services = new Webservice[registeredServices.values().size()];
            registeredServices.values().toArray(services);
        }

        for(Webservice service : services){
            removeService(service.getPath());
        }

        //some time to send possible update notifications (404_NOT_FOUND) to observers
        Thread.sleep(1000);

        //Close the datagram datagramChannel (includes unbind)
        for(final Channel channel : serverChannels){
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
    }


    /**
     * Registers a Webservice instance at the server. After registration the service will be available at the path
     * given as <code>service.getPath()</code>.
     *
     * It is not possible to register multiple webServices at a single path. If a new service is registered at the server
     * with a path from another already registered service, then the new service replaces the old one.
     *
     * @param webservice A {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instance to be registered at the server
     */
    public final void registerService(final Webservice webservice) {
        registeredServices.put(webservice.getPath(), webservice);
        log.info("Registered new service at " + webservice.getPath());

        if(webservice instanceof ObservableWebservice){
            InternalObservableResourceRegistrationMessage message =
                    new InternalObservableResourceRegistrationMessage((ObservableWebservice) webservice);

            ChannelFuture future = Channels.write(serverChannels.iterator().next(), message);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Registered {} at observable resource handler.", webservice.getPath());
                }
            });
        }

        webservice.setScheduledExecutorService(scheduledExecutorService);
        //webservice.setListeningExecutorService(listeningExecutorService);
    }

    /**
     * Removes the {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instance registered at the given path from the server
     *
     * @param uriPath the path of the {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instance to be removed
     *
     * @return <code>true</code> if the service was removed succesfully, <code>false</code> otherwise.
     */
    public synchronized boolean removeService(String uriPath) {
        Webservice removedService = registeredServices.remove(uriPath);

//        if(removedService != null && removedService instanceof ObservableWebservice){
//            channel.write(new InternalServiceRemovedFromServerMessage(uriPath));
//            removedService.shutdown();
//        }

        if(removedService != null)
            log.info("Service {} removed from server.", uriPath);
        else
            log.info("Service {} could not be removed. Does not exist.", uriPath);

        return removedService == null;
    }

    /**
     * Removes all registered {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances from the server.
     */
    public void removeAllServices() {

        Webservice[] services;
        synchronized (this){
            services = new Webservice[registeredServices.values().size()];
            registeredServices.values().toArray(services);
        }

        for (String path : registeredServices.keySet()) {
            removeService(path);
        }

        if(!registeredServices.isEmpty()){
            log.error("All Webservices should be removed but there are {} left.", registeredServices.size());
        }
    }

    private class CoapResponseSender implements Runnable{

        private final SettableFuture<CoapResponse> responseFuture;
        private final CoapRequest coapRequest;
        private final Webservice webservice;
        private final ChannelHandlerContext ctx;
        private final InetSocketAddress remoteSocketAddress;

        public CoapResponseSender(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                  Webservice webservice,
                                  ChannelHandlerContext ctx, InetSocketAddress remoteSocketAddress){

            this.responseFuture = responseFuture;
            this.coapRequest = coapRequest;
            this.webservice = webservice;
            this.ctx = ctx;
            this.remoteSocketAddress = remoteSocketAddress;
        }

        @Override
        public void run() {
            CoapResponse coapResponse;
            try {
                coapResponse = responseFuture.get();
                coapResponse.setMessageID(coapRequest.getMessageID());
                coapResponse.setToken(coapRequest.getToken());

                if(openRequests.remove(remoteSocketAddress, coapRequest)){
                    log.info("Removed message {} from {} from list of open requests!",
                            coapRequest, remoteSocketAddress);
                }

                if(MessageCode.isErrorMessage(coapResponse.getMessageCode())){
                    sendCoapResponse(ctx, coapResponse, remoteSocketAddress);
                    return;
                }

                if(webservice != null){

                    //Set Max-Age Option according to the value returned by the Webservice
                    try{
                        coapResponse.setMaxAge(webservice.getMaxAge());
                    }
                    catch(InvalidOptionException e){
                        log.debug("IGNORE invalid option exception:", e);
                    }

                    try{
                        coapResponse.setEtag(webservice.getEtag());
                    }
                    catch(InvalidOptionException e){
                        log.debug("IGNORE invalid option exception:", e);
                    }

                    if(coapRequest.isObserveSet() && webservice instanceof ObservableWebservice)
                        coapResponse.setObservationSequenceNumber(0);

                }

                //Sets the path of the service this response came from
                //coapResponse.setServicePath(webService.getPath());

                //Set message ID and token to match the request
                coapResponse.setMessageID(coapRequest.getMessageID());
                coapResponse.setToken(coapRequest.getToken());


//                        //Set content type if there is payload but no content type
//                        if(coapResponse.getContent().readableBytes() > 0 &&
//                                coapResponse.getContentFormat() == ContentFormat.Name.UNDEFINED)
//                            coapResponse.setContentType(MediaType.TEXT_PLAIN_UTF8);

//                        //Set observe response option if requested
//                        if(webService instanceof ObservableWebservice && !coapRequest.getOption(OBSERVE_REQUEST).isEmpty())
//                            if(!coapResponse.getMessageCode().isErrorMessage())
//                                coapResponse.setObserveOptionValue(0);

            }

            catch (Exception ex) {
                try {
                    if(ex instanceof ExecutionException && ex.getCause() instanceof ContentFormatNotSupportedException){
                        coapResponse = new CoapResponse(MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415);
                        coapResponse.setMessageID(coapRequest.getMessageID());
                        coapResponse.setToken(coapRequest.getToken());
                        coapResponse.setContent(ex.getCause().getMessage().getBytes(CoapMessage.CHARSET));
                    }
                    else{
                        log.error("Unexpected exception!", ex);

                        coapResponse = new CoapResponse(MessageCode.Name.INTERNAL_SERVER_ERROR_500);
                        coapResponse.setMessageID(coapRequest.getMessageID());
                        coapResponse.setToken(coapRequest.getToken());
                        //coapResponse.setServicePath(webService.getPath());
                        StringWriter errors = new StringWriter();
                        ex.printStackTrace(new PrintWriter(errors));
                        coapResponse.setContent(errors.toString().getBytes(CoapMessage.CHARSET));
                    }
                } catch (Exception e) {
                    log.error("This should never happen.", e);
                    return;
                }
            }

            //Send the response
            sendCoapResponse(ctx, coapResponse, remoteSocketAddress);
        }
    }
}