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

package de.uniluebeck.itm.spitfire.nCoap.application;

import de.uniluebeck.itm.spitfire.nCoap.application.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.application.webservice.WebService;
import de.uniluebeck.itm.spitfire.nCoap.application.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalServiceRemovedFromPath;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.ObservableWebServiceUpdate;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutHandler;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;

/**
 * Abstract class to be extended by a CoAP server application. Even though the communication is based on the Netty
 * framework, a developer of such a server doesn't have to go into details regarding the architecture. The whole
 * architecture is hidden from the users perspective. Technically speaking, the extending class will be the
 * topmost {@link ChannelUpstreamHandler} of the automatically generated netty handler stack.
 *
 * @author Oliver Kleine
 */
public abstract class CoapServerApplication extends SimpleChannelUpstreamHandler
                                            implements RetransmissionTimeoutHandler, Observer {

    public static final int DEFAULT_COAP_SERVER_PORT = 5683;

    private static Logger log = LoggerFactory.getLogger(CoapServerApplication.class.getName());

    private DatagramChannel channel;

    //This map holds all registered services (key: URI path, value: WebService instance)
    private ConcurrentHashMap<String, WebService> registeredServices = new ConcurrentHashMap<String, WebService>();

    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on the given port
     * and already provides the default /.well-known/core resource
     */
    protected CoapServerApplication(int serverPort){
        channel = new CoapServerDatagramChannelFactory(this, serverPort).getChannel();
        log.info("New server created. Listening on port " + this.getServerPort() + ".");
        registerService(new WellKnownCoreResource(registeredServices));
    }
    /**
     * Constructor to create a new instance of {@link CoapServerApplication}. The server listens on port 5683
     * and already provides the default /.well-known/core resource
     */
    protected CoapServerApplication(){
        this(DEFAULT_COAP_SERVER_PORT);
    }


//    /**
//     * Blocks until current channel is closed and binds the channel to a new ChannelFactory.
//     */
//    public void rebindChannel() {
//        ChannelFuture future = channel.close();
//        future.awaitUninterruptibly();
//        if (!future.isSuccess()) {
//            throw new InternalError("Failed to close channel!");
//        }
//        channel = new CoapServerDatagramChannelFactory(this).getChannel();
//    }
    
    /**
     * This method is called by the Netty framework whenever a new message is received to be processed by the server.
     * For each incoming request a new Thread is created to handle the request (by invoking the method
     * <code>receiveCoapRequest</code>).
     *
     * @param ctx The {@link ChannelHandlerContext} connecting relating this class (which implements the
     * {@link ChannelUpstreamHandler} interface) to the channel that received the message.
     * @param me the {@link MessageEvent} containing the actual message
     */
    @Override
    public final void messageReceived(ChannelHandlerContext ctx, final MessageEvent me){
        if(me.getMessage() instanceof CoapRequest){
            me.getFuture().setSuccess();
            try{
                final CoapRequest coapRequest = (CoapRequest) me.getMessage();
                final InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

                Object response = null;
                WebService webService = registeredServices.get(coapRequest.getTargetUri().getPath());

                if(webService == null){
                    //Error response if there is no such webservice instance registered at this server instance
                    response = new CoapResponse(Code.NOT_FOUND_404);

                }
                else if (!coapRequest.getOption(OBSERVE_REQUEST).isEmpty()) {
                    //Handle request with observe option
                    if(webService instanceof ObservableWebService){
                        response = new ObservableWebServiceUpdate((ObservableWebService) webService);
                    }
                }
                else{
                    //handle request without observe option
                    response = webService.processMessage(coapRequest, remoteAddress);

                    //Set message ID and token to match the request
                    log.debug("Message ID of incoming request: " + coapRequest.getMessageID());
                    ((CoapResponse) response).setMessageID(coapRequest.getMessageID());

                    if(coapRequest.getToken().length > 0){
                        ((CoapResponse) response).setToken(coapRequest.getToken());
                    }
                }

                //Write response
                ChannelFuture future = Channels.write(channel, response, remoteAddress);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.debug("Sending of response to recipient " +
                                remoteAddress + " with message ID " + coapRequest.getMessageID() + " and " +
                                "token " + Tools.toHexString(coapRequest.getToken()) + " completed.");
                    }
                });

                return;
            }
            catch (InvalidOptionException e) {
                log.error("This should never happen.", e);
            }
            catch (ToManyOptionsException e) {
                log.error("This should never happen.", e);
            }
            catch (InvalidHeaderException e) {
                log.error("This should never happen.", e);
            }
        }
        ctx.sendUpstream(me);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
        if(e.getCause() instanceof RetransmissionTimeoutHandler)
            handleRetransmissionTimout();
        else
            log.info("Exception while processing I/O task.", e.getCause());
    }

    /**
     * Shuts the server down by closing the channel which includes to unbind the channel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public void shutdown(){
        //Close the datagram channel (includes unbind)
        ChannelFuture future = channel.close();

        //remove all services
        for(WebService service : registeredServices.values()){
            removeService(service.getPath());
        }

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Channel closed.");

                channel.getFactory().releaseExternalResources();
                log.info("External resources released. Shutdown completed.");
            }
        });

        future.awaitUninterruptibly();
    }
    
//    /**
//     * Searches for a service to respond to the passed request.
//     * If no matching service was found a 404 Not Found response will be returned.
//     *
//     * @param coapRequest request
//     * @param senderAddress requests remote address
//     * @return resource from service or 404 Not Found
//     */
//    private CoapResponse receiveCoapRequest(CoapRequest coapRequest, InetSocketAddress senderAddress) {
//        String uriPath = coapRequest.getTargetUri().getPath();
//        //TODO .well-known/core
//        WebService service = registeredServices.get(uriPath);
//        if (service != null) {
//            return service.processMessage(coapRequest);
//        } else {
//            return new CoapResponse(Code.NOT_FOUND_404);
//        }
//    }
    
    /**
     * Registers a WebService instance at the server. After registration the service will be available at the path
     * given using <code>service.getPath()</code>.
     *
     * It is not possible to register multiple services at a single path. If a new service is registered at the server
     * with a path from another already registered service, then the new service replaces the old one.
     *      *
     * @param service A {@link WebService} instance to be registered at the server
     */
    public void registerService(WebService service) {
        registeredServices.put(service.getPath(), service);
        log.info("Registered new service at " + service.getPath());
        if(service instanceof ObservableWebService){
            ((ObservableWebService) service).addObserver(this);
        }
    }
    
    /**
     * Removes a service from the server
     * 
     * @param uriPath service path
     * @return true if a registered service was removed
     */
    public boolean removeService(String uriPath) {
        WebService removedService = registeredServices.remove(uriPath);

        if(removedService != null && removedService instanceof ObservableWebService){
                channel.write(new InternalServiceRemovedFromPath(uriPath));
        }

        if(removedService != null){
            log.info("Service " + uriPath + " removed from server (port: " + this.getServerPort() + ").");
        }
        else{
            log.info("Service " + uriPath + " does not exist and thus could not be removed.");
        }

        return removedService == null;
    }
    
    /**
     * Removes all services from server.
     */
    public void removeAllServices() {
        for (String path : registeredServices.keySet()) {
            removeService(path);
        }
        if(!registeredServices.isEmpty()){
            log.error("All services should be removed but there are " + registeredServices.size() + " left.");
        }
    }

    public int getServerPort(){
        return channel.getLocalAddress().getPort();
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof ObservableWebService) {
            //write internal message on channel if service updates
            channel.write(new ObservableWebServiceUpdate((ObservableWebService) observable));
        }
    }

}