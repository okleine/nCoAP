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

package de.uniluebeck.itm.ncoap.communication.observing.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WrappedResourceStatus;
import de.uniluebeck.itm.ncoap.communication.events.MessageIDAssignedEvent;
import de.uniluebeck.itm.ncoap.communication.events.ResetReceivedEvent;
import de.uniluebeck.itm.ncoap.communication.events.server.ObservableWebserviceDeregistrationEvent;
import de.uniluebeck.itm.ncoap.communication.events.server.ObservableWebserviceRegistrationEvent;
import de.uniluebeck.itm.ncoap.communication.events.TransmissionTimeoutEvent;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import de.uniluebeck.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* The {@link ServerObservationHandler} is responsible to inform observations1 whenever the status of an
* {@link ObservableWebservice} changes. It itself registers as {@link Observer} on all instances of
* {@link ObservableWebservice} instances running on this {@link CoapServerApplication} instance.
*
* @author Oliver Kleine
*/
public class ServerObservationHandler extends SimpleChannelHandler implements Observer {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Table<InetSocketAddress, Token, ObservationParams> observations1;
    private Multimap<String, ObservationParams> observations2;
    private ReentrantReadWriteLock lock;
    private ScheduledExecutorService executor;

    private ChannelHandlerContext ctx;
    private Map<String, ScheduledFuture> updateTransmissionFutures;

    /**
     * Creates a new instance of {@link ServerObservationHandler}.
     */
    public ServerObservationHandler(ScheduledExecutorService executor){
        this.executor = executor;
        this.observations1 = HashBasedTable.create();
        this.observations2 = HashMultimap.create();
        this.lock = new ReentrantReadWriteLock();
        this.updateTransmissionFutures = Collections.synchronizedMap(new HashMap<String, ScheduledFuture>());
    }


    /**
     * Sets the {@link ChannelHandlerContext} of this {@link ServerObservationHandler} on the
     * {@link DatagramChannel}
     *
     * @param ctx the {@link ChannelHandlerContext} of this {@link ServerObservationHandler} on the
     * {@link DatagramChannel}
     */
    public void setChannelHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        if((me.getMessage() instanceof CoapRequest)){
            if(((CoapRequest) me.getMessage()).getObserve() == 0)
                handleIncomingObserverRegistrationRequest(ctx, me);

            else if(((CoapRequest) me.getMessage()).getObserve() == 1)
                handleIncomingObserverDeregistrationRequest(ctx, me);

            else
                ctx.sendUpstream(me);
        }

        else if(me.getMessage() instanceof MessageIDAssignedEvent){
            handleMessageIDAssignedEvent(ctx, me);
        }

        else if(me.getMessage() instanceof ResetReceivedEvent)
            handleInternalResetReceivedMessage(ctx, me);

        else if(me.getMessage() instanceof TransmissionTimeoutEvent)
            handleTransmissionTimeoutEvent(ctx, me);

        else
            ctx.sendUpstream(me);
    }

    private void handleMessageIDAssignedEvent(ChannelHandlerContext ctx, MessageEvent me) {
        MessageIDAssignedEvent event = (MessageIDAssignedEvent) me.getMessage();
        InetSocketAddress remoteEndpoint = event.getRemoteEndpoint();
        Token token = event.getToken();

        try{
            lock.readLock().lock();
            if(!observations1.contains(remoteEndpoint, token)){
                ctx.sendUpstream(me);
                return;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();
            if(observations1.contains(remoteEndpoint, token)){
                ObservationParams obsParams = observations1.get(remoteEndpoint, token);
                obsParams.setLatestMessageID(event.getMessageID());
                ctx.sendUpstream(me);
            }
        }
        finally {
            lock.writeLock().unlock();
        }


    }


    private void handleIncomingObserverDeregistrationRequest(ChannelHandlerContext ctx, MessageEvent me){
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapRequest coapRequest = (CoapRequest) me.getMessage();
        Token token = coapRequest.getToken();

        try{
            lock.readLock().lock();
            if(!observations1.contains(remoteEndpoint, token)){
                ctx.sendUpstream(me);
                return;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();

            ObservationParams params = observations1.remove(remoteEndpoint, coapRequest.getToken());
            if(!(params == null)){
                observations2.remove(params.getWebservicePath(), params);
                log.info("Removed {} as observer for \"{}\" because of de-registration request!",
                        params.getRemoteEndpoint(), params.getWebservicePath());
            }

            ctx.sendUpstream(me);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void handleIncomingObserverRegistrationRequest(ChannelHandlerContext ctx, MessageEvent me) {

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapRequest coapRequest = (CoapRequest) me.getMessage();

        ObservationParams params = new ObservationParams(remoteEndpoint, coapRequest.getToken(),
                coapRequest.getUriPath(), coapRequest.getEtags());

        String webservicePath = coapRequest.getUriPath();
        Token token = coapRequest.getToken();

        //a new observation request with the same token replaces an already running observation
        try{
            lock.writeLock().lock();

            ObservationParams oldParams = observations1.put(remoteEndpoint, token, params);
            if(oldParams != null){
                observations2.remove(webservicePath, oldParams);
            }
            observations2.put(webservicePath, params);

            log.info("Registered new observer for {} (remote endpoint: {}, token: {})",
                    new Object[]{webservicePath, remoteEndpoint, token});

            ctx.sendUpstream(me);
        }
        finally{
            lock.writeLock().unlock();
        }
    }


    private void handleTransmissionTimeoutEvent(ChannelHandlerContext ctx, MessageEvent me) {

        TransmissionTimeoutEvent event = (TransmissionTimeoutEvent) me.getMessage();
        InetSocketAddress remoteEndpoint = event.getRemoteEndpoint();
        Token token = event.getToken();

        try{
            lock.readLock().lock();
            ObservationParams params = this.observations1.get(remoteEndpoint, token);
            if(params == null || params.getLatestMessageID() != event.getMessageID()){
                ctx.sendDownstream(me);
                return;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();
            ObservationParams params = removeObserver(event.getRemoteEndpoint(), event.getToken());
            if(params != null){
                log.warn("Stopped observation of service {} by {} with token {} due to retransmission timeout.",
                        new Object[]{params.getWebservicePath(), params.getRemoteEndpoint(), params.getToken()});
            }
            ctx.sendUpstream(me);
        }
        finally {
            lock.writeLock().unlock();
        }

    }


    private void handleInternalResetReceivedMessage(ChannelHandlerContext ctx, MessageEvent me) {

        ResetReceivedEvent resetMessage = (ResetReceivedEvent) me.getMessage();

        try{
            lock.readLock().lock();
            if(!observations1.contains(resetMessage.getRemoteEndpoint(), resetMessage.getToken())){
                ctx.sendUpstream(me);
                return;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();
            ObservationParams params = removeObserver(resetMessage.getRemoteEndpoint(), resetMessage.getToken());

            if(params != null){
                log.warn("Stopped observation of service {} by {} with token {} due to RST message.",
                        new Object[]{params.getWebservicePath(), params.getRemoteEndpoint(), params.getToken()});
            }

            ctx.sendUpstream(me);
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    private ObservationParams removeObserver(InetSocketAddress remoteEndpoint, Token token){
        try{
            lock.readLock().lock();
            if(!observations1.contains(remoteEndpoint, token)){
                return null;
            }
        }
        finally{
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();
            if(observations1.contains(remoteEndpoint, token)){
                ObservationParams params = observations1.get(remoteEndpoint, token);
                observations2.remove(params.getWebservicePath(), params);
                return params;
            }
            else{
                return null;
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapResponse)
            handleOutgoingCoapResponse(ctx, me);

        else if(me.getMessage() instanceof ObservableWebserviceRegistrationEvent)
            handleInternalObservableWebserviceRegistrationMessage(me);

        else if(me.getMessage() instanceof ObservableWebserviceDeregistrationEvent)
            handleInternalServiceRemovedFromServerMessage(ctx, me);

        else
            ctx.sendDownstream(me);
    }



    private void handleInternalServiceRemovedFromServerMessage(ChannelHandlerContext ctx, MessageEvent me) {

        ObservableWebserviceDeregistrationEvent event = (ObservableWebserviceDeregistrationEvent) me.getMessage();

        try{
            lock.writeLock().lock();

            Collection<ObservationParams> tmp = observations2.get(event.getServicePath());
            ObservationParams[] observations = tmp.toArray(new ObservationParams[tmp.size()]);

            for(ObservationParams params : observations){
                CoapResponse coapResponse = new CoapResponse(MessageType.Name.NON, MessageCode.Name.NOT_FOUND_404);
                String message = "Service \"" + params.getWebservicePath() + "\" was removed from server!";
                coapResponse.setToken(params.getToken());
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

                Channels.write(ctx.getChannel(), coapResponse, params.getRemoteEndpoint());
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    private void handleInternalObservableWebserviceRegistrationMessage(MessageEvent me) {

        ObservableWebserviceRegistrationEvent registrationMessage =
                (ObservableWebserviceRegistrationEvent) me.getMessage();

        registrationMessage.getWebservice().addObserver(this);
    }


    private void handleOutgoingCoapResponse(ChannelHandlerContext ctx, MessageEvent me) {

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapResponse coapResponse = (CoapResponse) me.getMessage();

        try{
            lock.readLock().lock();
            if(!observations1.contains(remoteEndpoint, coapResponse.getToken())){
                ctx.sendDownstream(me);
                return;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();
            if(observations1.contains(remoteEndpoint, coapResponse.getToken())){
                Token token = coapResponse.getToken();
                if(!coapResponse.isUpdateNotification()){
                    ObservationParams params = observations1.remove(remoteEndpoint, token);
                    observations2.remove(params.getWebservicePath(), params);
                }

                else{
                    observations1.get(remoteEndpoint, token).setContentFormat(coapResponse.getContentFormat());
                    coapResponse.setObserve();
                }

                ctx.sendDownstream(me);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public void update(Observable observable, final Object arg) {
        final ObservableWebservice webservice = (ObservableWebservice) observable;
        String path = webservice.getPath();

        if(updateTransmissionFutures.containsKey(path)){
            updateTransmissionFutures.remove(path).cancel(true);
        }

        ScheduledFuture future = executor.schedule(new Runnable(){

            @Override
            public void run() {
                Map<Long, WrappedResourceStatus> statusCache = new HashMap<>();

                Collection<ObservationParams> tmp = observations2.get(webservice.getPath());
                ObservationParams[] observations = tmp.toArray(new ObservationParams[tmp.size()]);

                for(final ObservationParams params : observations){

                    long contentFormat = params.getContentFormat();
                    if(contentFormat == UintOptionValue.UNDEFINED){
                        log.warn("Undefined content format for observation (path: {}, remote endpoint: {}, token: {})",
                                new Object[]{params.getWebservicePath(), params.getRemoteEndpoint(), params.getToken()});
                        continue;
                    }

                    if(!statusCache.containsKey(contentFormat))
                        statusCache.put(contentFormat, webservice.getWrappedResourceStatus(contentFormat));

                    WrappedResourceStatus wrappedResourceStatus = statusCache.get(contentFormat);

                    //Determine the message type for the update notification
                    MessageType.Name messageType;
                    if(arg != null && arg instanceof MessageType.Name)
                        messageType = (MessageType.Name) arg;
                    else
                        messageType =
                                webservice.getMessageTypeForUpdateNotification(params.getRemoteEndpoint(), params.getToken());

                    //Determine the message code for the update notification (depends on the ETAG options sent with the
                    //request that started the observation
                    final CoapResponse updateNotification;
                    if(params.getEtags().contains(wrappedResourceStatus.getEtag())){
                        updateNotification = new CoapResponse(messageType, MessageCode.Name.VALID_203);
                    }

                    else{
                        updateNotification = new CoapResponse(messageType, MessageCode.Name.CONTENT_205);
                        updateNotification.setContent(wrappedResourceStatus.getContent(), contentFormat);
                    }

                    //Set content related options for the update notification
                    updateNotification.setMaxAge(wrappedResourceStatus.getMaxAge());

                    byte[] etag = wrappedResourceStatus.getEtag();
                    if(etag.length > 0)
                        updateNotification.setEtag(wrappedResourceStatus.getEtag());

                    updateNotification.setObserve();

                    updateNotification.setMessageID(params.getLatestMessageID());
                    updateNotification.setToken(params.getToken());


                    //Send the update notification
                    ChannelFuture future =
                            Channels.write(ctx.getChannel(), updateNotification, params.getRemoteEndpoint());

                    future.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if(!future.isSuccess()){
                                String path = params.getWebservicePath();
                                InetSocketAddress remoteEndpoint = params.getRemoteEndpoint();
                                Token token = params.getToken();
                                log.error("Could NOT SEND update notification for {} (remote endpoint: {}, token: {})",
                                        new Object[]{path, remoteEndpoint, token});
                            }
                            else{
                                if(log.isInfoEnabled()){
                                    String path = params.getWebservicePath();
                                    InetSocketAddress remoteEndpoint = params.getRemoteEndpoint();
                                    Token token = params.getToken();
                                    log.info("Update notification for {} sent (remote endpoint: {}, token: {})",
                                            new Object[]{path, remoteEndpoint, token});
                                }
                            }
                        }
                    });
                }
            }

        }, 0, TimeUnit.MILLISECONDS);

        updateTransmissionFutures.put(path, future);
    }
}
