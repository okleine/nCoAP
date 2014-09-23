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
package de.uniluebeck.itm.ncoap.communication.observe.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WrappedResourceStatus;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.ResetReceptionEvent;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.TransmissionTimeoutEvent;
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

/**
 * The {@link WebserviceObservationHandler} is responsible to inform observers whenever the status of an
 * {@link ObservableWebservice} changes. It itself registers as {@link Observer} on all instances of
 * {@link ObservableWebservice} instances running on this {@link CoapServerApplication} instance.
 *
 * @author Oliver Kleine
 */
public class WebserviceObservationHandler extends SimpleChannelHandler implements Observer {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private final Object monitor = new Object();
    private Table<InetSocketAddress, Token, ObservationParams> observationsPerObserver;
    private Multimap<String, ObservationParams> observationsPerService;

    private ChannelHandlerContext ctx;


    /**
     * Creates a new instance of {@link WebserviceObservationHandler}.
     */
    public WebserviceObservationHandler(){
        this.observationsPerObserver = HashBasedTable.create();
        this.observationsPerService = HashMultimap.create();
    }


    /**
     * Sets the {@link ChannelHandlerContext} of this {@link WebserviceObservationHandler} on the
     * {@link DatagramChannel}
     *
     * @param ctx the {@link ChannelHandlerContext} of this {@link WebserviceObservationHandler} on the
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

        else if(me.getMessage() instanceof ResetReceptionEvent)
            handleInternalResetReceivedMessage(ctx, me);

        else if(me.getMessage() instanceof TransmissionTimeoutEvent)
            handleInternalRetransmissionTimeoutMessage(ctx, me);

        else
            ctx.sendUpstream(me);
    }


    private void handleIncomingObserverDeregistrationRequest(ChannelHandlerContext ctx, MessageEvent me){
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapRequest coapRequest = (CoapRequest) me.getMessage();

        synchronized (monitor){
            ObservationParams params = observationsPerObserver.remove(remoteEndpoint, coapRequest.getToken());
            if(!(params == null)){
                observationsPerService.remove(params.getWebservicePath(), params);
                log.info("Removed {} as observer for \"{}\" because of de-registration request!",
                        params.getRemoteEndpoint(), params.getWebservicePath());
            }

        }

        ctx.sendUpstream(me);
    }

    private void handleIncomingObserverRegistrationRequest(ChannelHandlerContext ctx, MessageEvent me) {

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapRequest coapRequest = (CoapRequest) me.getMessage();


        ObservationParams params = new ObservationParams(remoteEndpoint, coapRequest.getToken(),
                coapRequest.getUriPath(), coapRequest.getEtags());

        //a new observation request with the same token replaces an already running observation
        synchronized (monitor){
            ObservationParams oldParams = observationsPerObserver.put(remoteEndpoint, coapRequest.getToken(), params);

            if(oldParams != null)
                observationsPerService.remove(coapRequest.getUriPath(), oldParams);

            observationsPerService.put(coapRequest.getUriPath(), params);
        }

        ctx.sendUpstream(me);
    }


    private void handleInternalRetransmissionTimeoutMessage(ChannelHandlerContext ctx, MessageEvent me) {

        TransmissionTimeoutEvent timeoutMessage = (TransmissionTimeoutEvent) me.getMessage();

        if(observationsPerObserver.contains(timeoutMessage.getRemoteEndpoint(), timeoutMessage.getToken())){
            ObservationParams params = removeObserver(timeoutMessage.getRemoteEndpoint(), timeoutMessage.getToken());

            if(params != null)
                log.warn("Stopped observation of service {} by {} with token {} due to retransmission timeout.",
                        new Object[]{params.getWebservicePath(), params.getRemoteEndpoint(), params.getToken()});
        }

        ctx.sendUpstream(me);
    }


    private void handleInternalResetReceivedMessage(ChannelHandlerContext ctx, MessageEvent me) {

        ResetReceptionEvent resetMessage = (ResetReceptionEvent) me.getMessage();

        if(observationsPerObserver.contains(resetMessage.getRemoteEndpoint(), resetMessage.getToken())){
            ObservationParams params = removeObserver(resetMessage.getRemoteEndpoint(), resetMessage.getToken());

            if(params != null)
                log.warn("Stopped observation of service {} by {} with token {} due to RST message.",
                        new Object[]{params.getWebservicePath(), params.getRemoteEndpoint(), params.getToken()});
        }

        ctx.sendUpstream(me);
    }


    private ObservationParams removeObserver(InetSocketAddress remoteEndpoint, Token token){
        ObservationParams params = null;

        synchronized (monitor){
            if(observationsPerObserver.contains(remoteEndpoint, token)){
                params = observationsPerObserver.get(remoteEndpoint, token);
                observationsPerService.remove(params.getWebservicePath(), params);
            }
        }

        return params;
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

        ObservableWebserviceDeregistrationEvent internalMessage =
                (ObservableWebserviceDeregistrationEvent) me.getMessage();

        synchronized (monitor){

            Collection<ObservationParams> tmp = observationsPerService.get(internalMessage.getServicePath());
            ObservationParams[] observations = tmp.toArray(new ObservationParams[tmp.size()]);

            for(ObservationParams params : observations){
                CoapResponse coapResponse = new CoapResponse(MessageType.Name.NON, MessageCode.Name.NOT_FOUND_404);
                String message = "Service \"" + params.getWebservicePath() + "\" was removed from server!";
                coapResponse.setToken(params.getToken());
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

                Channels.write(ctx.getChannel(), coapResponse, params.getRemoteEndpoint());
            }
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

        if(observationsPerObserver.contains(remoteEndpoint, coapResponse.getToken())){
            synchronized (monitor){
                if(!coapResponse.isUpdateNotification()){
                    ObservationParams params = observationsPerObserver.remove(remoteEndpoint, coapResponse.getToken());
                    observationsPerService.remove(params.getWebservicePath(), params);
                }

                else{
                    ObservationParams params = observationsPerObserver.get(remoteEndpoint, coapResponse.getToken());
                    if(params.getNotifiationCount() == 0){
                        params.setInitialSequenceNumber(coapResponse.getObservationSequenceNumber());
                    }
                    if(params.getLatestUpdateNotificationMessageID() != coapResponse.getMessageID()){
                        log.debug("Set latest message ID for observation to {}", coapResponse.getMessageID());
                        params.setLatestUpdateNotificationMessageID(coapResponse.getMessageID());
                    }

                    if(params.getContentFormat() == ContentFormat.UNDEFINED)
                        params.setContentFormat(coapResponse.getContentFormat());

                    coapResponse.setObserveOption(params.getNextSequenceNumber());
                }
            }
        }

        ctx.sendDownstream(me);
    }


    @Override
    public void update(Observable observable, Object arg) {
        ObservableWebservice webservice = (ObservableWebservice) observable;

        Map<Long, WrappedResourceStatus> statusCache = new HashMap<>();

        Collection<ObservationParams> tmp = observationsPerService.get(webservice.getPath());
        ObservationParams[] observations = tmp.toArray(new ObservationParams[tmp.size()]);

        for(final ObservationParams params : observations){
            if(params.getNotifiationCount() == 0){
                continue;
            }

            long contentFormat = params.getContentFormat();

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

            updateNotification.setObserveOption(UintOptionValue.UNDEFINED);


            updateNotification.setMessageID(params.getLatestUpdateNotificationMessageID());
            updateNotification.setToken(params.getToken());


            //Send the update notification
            ChannelFuture future =
                    Channels.write(this.ctx.getChannel(), updateNotification, params.getRemoteEndpoint());

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess())
                        log.error("Could not send update notification for {} with token {} " +
                                "(observing \"{}\").", new Object[]{params.getRemoteEndpoint(), params.getToken(),
                                params.getWebservicePath(), future.getCause()});
                    else
                        log.debug("Update notification sent to {}: {}", params.getRemoteEndpoint(), updateNotification);
                }
            });
        }
    }
}
