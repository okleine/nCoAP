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

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalServiceRemovedFromPath;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalServiceUpdate;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutHandler;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
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
public abstract class CoapServerApplication extends SimpleChannelUpstreamHandler implements RetransmissionTimeoutHandler, Observer {

    private static Logger log = LoggerFactory.getLogger(CoapServerApplication.class.getName());

    protected final DatagramChannel channel;

    protected CoapServerApplication(){
        channel = new CoapServerDatagramChannelFactory(this).getChannel();
    }

    //This map holds all registered services
    //[uri path] --> [Service]
    ConcurrentHashMap<String, Service> registeredServices = new ConcurrentHashMap<String, Service>();
    
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
            try{
                final CoapRequest coapRequest = (CoapRequest) me.getMessage();
                final InetSocketAddress remoteAddress = (InetSocketAddress) me.getRemoteAddress();

                if (!coapRequest.getOption(OBSERVE_REQUEST).isEmpty()) {
                    //request has observe option, send InternalServiceUpdate instead of CoapResponse
                    Service service = registeredServices.get(coapRequest.getTargetUri().getPath());
                    if (service != null) {
                        InternalServiceUpdate serviceUpdate = new InternalServiceUpdate(service);                    
                        Channels.write(channel, serviceUpdate, remoteAddress);
                        return;
                    }
                }
                
                //Create the response
                CoapResponse coapResponse = receiveCoapRequest(coapRequest, remoteAddress);

                //Set message ID and token to match the request
                log.debug("Message ID of incoming request: " + coapRequest.getMessageID());
                coapResponse.setMessageID(coapRequest.getMessageID());

                if(coapRequest.getToken().length > 0){
                    coapResponse.setToken(coapRequest.getToken());
                }

                //Write response
                ChannelFuture future = Channels.write(channel, coapResponse, remoteAddress);

                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.debug("CoapRequestExecutor] Sending of response to recipient " +
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
            log.info("Exception while processing I/O task.", e);
    }

    /**
     * Shuts the server down by closing the channel which includes to unbind the channel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public void shutdown(){
        //Close the datagram channel (includes unbind)
        ChannelFuture future = channel.close();

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
    
    /**
     * Searches for a service to respond to the passed request.
     * If no matching service was found a 404 Not Found response will be returned.
     * 
     * @param coapRequest request
     * @param senderAddress requests remote address
     * @return resource from service or 404 Not Found
     */
    private CoapResponse receiveCoapRequest(CoapRequest coapRequest, InetSocketAddress senderAddress) {
        String uriPath = coapRequest.getTargetUri().getPath();
        //TODO .well-known/core        
        Service service = registeredServices.get(uriPath);
        if (service != null) {
            return service.getStatus(coapRequest);
        } else {
            return new CoapResponse(Code.NOT_FOUND_404);
        }     
    }
    
    /**
     * Register a service to a path.
     * It is not possible to register multiple services at a single path.
     * However the same service can be registered multiple times at different paths.
     * 
     * @param uriPath uri path
     * @param service resource providing service
     */
    public void registerService(String uriPath, Service service) {
        if (registeredServices.containsKey(uriPath)) {
            removeService(uriPath);
        }
        registeredServices.put(uriPath, service);
        service.addObserver(this);
        service.addPath(uriPath);
    }
    
    /**
     * Removes a service from a path.
     * 
     * @param uriPath service path
     * @return true if a registered service was removed
     */
    public boolean removeService(String uriPath) {
        Service service = registeredServices.remove(uriPath);
        if (service != null) {
            service.deleteObserver(this);
            service.removePath(uriPath);
            channel.write(new InternalServiceRemovedFromPath(uriPath));
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Removes all services. 
     */
    public void removeAllServices() {
        for (String path : registeredServices.keySet()) {
            //remove observer and path from service
            Service service = registeredServices.get(path);
            service.deleteObserver(this);
            service.removePath(path);
        }
        registeredServices.clear();
    }
    
    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof Service) {
            //write internal message on channel if service updates
            channel.write(new InternalServiceUpdate((Service) o));
        }
    }
}