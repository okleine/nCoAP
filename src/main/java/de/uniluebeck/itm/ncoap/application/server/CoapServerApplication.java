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
import de.uniluebeck.itm.ncoap.application.server.webservice.WebService;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.ncoap.communication.codec.CodecException;
import de.uniluebeck.itm.ncoap.communication.codec.DecodingException;
import de.uniluebeck.itm.ncoap.communication.codec.EncodingException;
import de.uniluebeck.itm.ncoap.communication.codec.InternalExceptionMessage;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;


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
    private Multimap<InetSocketAddress, CoapRequest> openRequests =
            Multimaps.synchronizedMultimap(HashMultimap.<InetSocketAddress, CoapRequest>create());

    //This map holds all registered webservice (key: URI path, value: WebService instance)
    private HashMap<String, WebService> registeredServices = new HashMap<String, WebService>();

    private ListeningExecutorService listeningExecutorService;
    private ScheduledExecutorService scheduledExecutorService;

    public CoapServerApplication(InetSocketAddress... listeningSockets){

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoAP Server I/O Thread#%d").build();

        int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
        log.info("No. of I/O Threads: {}", numberOfThreads);

        ScheduledExecutorService ioExecutorService =
                Executors.newScheduledThreadPool(numberOfThreads, threadFactory);

        for(InetSocketAddress listeningSocket : listeningSockets){
            CoapServerDatagramChannelFactory factory =
                    new CoapServerDatagramChannelFactory(ioExecutorService, listeningSocket);

            channel = factory.getChannel();
            channel.getPipeline().addLast("Server Application", this);

            this.scheduledExecutorService = ioExecutorService;
            this.listeningExecutorService = MoreExecutors.listeningDecorator(scheduledExecutorService);

            registerService(new WellKnownCoreResource(registeredServices));

            log.info("New server created. Listening on port {}.", getServerPort());
        }
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
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof InternalExceptionMessage){
            InternalExceptionMessage message = (InternalExceptionMessage) me.getMessage();
            handleCodecException(ctx, (InetSocketAddress) me.getRemoteAddress(), message);
            return;
        }

        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendUpstream(me);
            return;
        }

        me.getFuture().setSuccess();

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        if(!(MessageCode.isRequest(coapMessage.getMessageCode()))){
            log.error("Server received CoAP message which is not a request. IGNORE");
            return;
        }


        //Create settable future to wait for response
        final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();

        final CoapRequest coapRequest = (CoapRequest) me.getMessage();
        final InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

        //Look up web service instance to handle the request
        final WebService webService = registeredServices.get(coapRequest.getUriPath());

        //Write error response if no such web service exists
        if(webService == null){
            log.info("Requested service {} not found. Send 404 error response to {}.",
                    coapRequest.getUriPath(), me.getRemoteAddress());

            //Write error response if there is no such webservice instance registered at this server instance
            CoapResponse coapResponse = null;
            try {
                coapResponse = new CoapResponse(MessageCode.Name.NOT_FOUND_404);
            } catch (InvalidHeaderException e) {
                log.error("This should never happen.", e);
            }
            coapResponse.setServicePath(coapRequest.getUriPath());
            coapResponse.setToken(coapRequest.getToken());


            sendCoapResponse(coapResponse, (InetSocketAddress) me.getRemoteAddress());
            return;
        }

        //Avoid duplicate request processing!
        log.debug("Contains key: {}", openRequests.containsKey(me.getRemoteAddress()));
        log.debug("Contains value: {}", openRequests.containsValue(coapRequest));

        if(!openRequests.containsEntry(me.getRemoteAddress(), coapRequest)){

            boolean added = openRequests.put((InetSocketAddress) me.getRemoteAddress(), coapRequest);
            if(added)
                log.warn("Added request from {} to list of open requests: {}", me.getRemoteAddress(), coapRequest);
            else
                log.error("NOT ADDED!");

            webService.processCoapRequest(responseFuture, coapRequest, remoteAddress);

            responseFuture.addListener(new Runnable(){
                @Override
                public void run() {
                    CoapResponse coapResponse;
                    try {
                        coapResponse = responseFuture.get();

                        if(openRequests.remove(me.getRemoteAddress(), coapRequest)){
                            log.warn("Remove message {} from {} from list of open requests!",
                                coapRequest, me.getRemoteAddress());
                        }

                        if(MessageCode.isErrorMessage(coapResponse.getMessageCode())){
                            coapResponse.setMessageID(coapRequest.getMessageID());
                            coapResponse.setToken(coapRequest.getToken());
                            sendCoapResponse(coapResponse, remoteAddress);
                            return;
                        }

                        //Set Max-Age Option according to the value returned by the Webservice
                        if(webService.getMaxAge() != Option.MAX_AGE_DEFAULT)
                            coapResponse.setMaxAge(webService.getMaxAge());

                        coapResponse.setEtag(webService.getEtag());

                        //Sets the path of the service this response came from
                        coapResponse.setServicePath(webService.getPath());

                        //Set message ID and token to match the request
                        coapResponse.setMessageID(coapRequest.getMessageID());
                        coapResponse.setToken(coapRequest.getToken());


//                        //Set content type if there is payload but no content type
//                        if(coapResponse.getContent().readableBytes() > 0 &&
//                                coapResponse.getContentFormat() == ContentFormat.Name.UNDEFINED)
//                            coapResponse.setContentType(MediaType.TEXT_PLAIN_UTF8);

//                        //Set observe response option if requested
//                        if(webService instanceof ObservableWebService && !coapRequest.getOption(OBSERVE_REQUEST).isEmpty())
//                            if(!coapResponse.getMessageCode().isErrorMessage())
//                                coapResponse.setObserveOptionValue(0);

                    }
                    catch (Exception ex) {
                        log.error("Unexpected exception!", ex);
                        try {
                            coapResponse = new CoapResponse(MessageCode.Name.INTERNAL_SERVER_ERROR_500);
                            coapResponse.setMessageID(coapRequest.getMessageID());
                            coapResponse.setToken(coapRequest.getToken());
                            coapResponse.setServicePath(webService.getPath());
                            StringWriter errors = new StringWriter();
                            ex.printStackTrace(new PrintWriter(errors));
                            coapResponse.setContent(errors.toString().getBytes(Charset.forName("UTF-8")));
                        } catch (Exception e) {
                            log.error("This should never happen.", e);
                            return;
                        }
                    }

                    //Send the response
                    sendCoapResponse(coapResponse, remoteAddress);
                }
            }, listeningExecutorService);
        }
        else{
            log.warn("A request from  {} with message ID {} is already in progess! IGNORE!",
                    me.getRemoteAddress(), coapRequest.getMessageID());
        }
    }

    private void handleCodecException(ChannelHandlerContext ctx, InetSocketAddress remoteAddress,
                                      InternalExceptionMessage message) {

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


            sendCoapResponse(coapResponse, remoteAddress);

        }
        catch (InvalidHeaderException | InvalidMessageException e1) {
            log.error("This should never happen.", e1);
        }
    }

    private void sendCoapResponse(final CoapResponse coapResponse, final InetSocketAddress remoteAddress){

        //Write response
        log.info("Write response to {}: {}", remoteAddress, coapResponse);
        ChannelFuture future = channel.write(coapResponse, remoteAddress);

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
     * Prior to doing so this methods removes all registered {@link WebService} instances from the server, i.e.
     * invokes the {@link WebService#shutdown()} method of all registered services.
     */
    public void shutdown() throws InterruptedException {

        //remove all webservices
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

//        if(webService instanceof ObservableWebService){
//            InternalObservableResourceRegistrationMessage message =
//                    new InternalObservableResourceRegistrationMessage((ObservableWebService) webService);
//
//            ChannelFuture future = Channels.write(channel, message);
//            future.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    log.info("Registered {} at observable resource handler.", webService.getPath());
//                }
//            });
//        }

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

//        if(removedService != null && removedService instanceof ObservableWebService){
//            channel.write(new InternalServiceRemovedFromServerMessage(uriPath));
//            removedService.shutdown();
//        }

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