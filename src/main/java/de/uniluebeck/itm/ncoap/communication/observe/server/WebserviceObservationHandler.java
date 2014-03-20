package de.uniluebeck.itm.ncoap.communication.observe.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.application.server.InternalServiceRemovedFromServerMessage;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WrappedResourceStatus;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalResetReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
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
        if((me.getMessage() instanceof CoapRequest) && ((CoapRequest) me.getMessage()).isObserveSet())
            handleIncomingCoapObserveRequest(ctx, me);

        else if(me.getMessage() instanceof InternalResetReceivedMessage)
            handleInternalResetReceivedMessage(ctx, me);

        else if(me.getMessage() instanceof InternalRetransmissionTimeoutMessage)
            handleInternalRetransmissionTimeoutMessage(ctx, me);

        else
            ctx.sendUpstream(me);
    }


    private void handleIncomingCoapObserveRequest(ChannelHandlerContext ctx, MessageEvent me) {

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

        InternalRetransmissionTimeoutMessage timeoutMessage = (InternalRetransmissionTimeoutMessage) me.getMessage();

        if(observationsPerObserver.contains(timeoutMessage.getRemoteEndpoint(), timeoutMessage.getToken())){
            ObservationParams params = removeObserver(timeoutMessage.getRemoteEndpoint(), timeoutMessage.getToken());

            if(params != null)
                log.warn("Stopped observation of service {} by {} with token {} due to retransmission timeout.",
                        new Object[]{params.getWebservicePath(), params.getRemoteEndpoint(), params.getToken()});
        }

        ctx.sendUpstream(me);
    }


    private void handleInternalResetReceivedMessage(ChannelHandlerContext ctx, MessageEvent me) {

        InternalResetReceivedMessage resetMessage = (InternalResetReceivedMessage) me.getMessage();

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

        else if(me.getMessage() instanceof InternalObservableWebserviceRegistrationMessage)
            handleInternalObservableWebserviceRegistrationMessage(me);

        else if(me.getMessage() instanceof InternalServiceRemovedFromServerMessage)
            handleInternalServiceRemovedFromServerMessage(ctx, me);

        else
            ctx.sendDownstream(me);
    }



    private void handleInternalServiceRemovedFromServerMessage(ChannelHandlerContext ctx, MessageEvent me) {

        InternalServiceRemovedFromServerMessage internalMessage =
                (InternalServiceRemovedFromServerMessage) me.getMessage();

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

        InternalObservableWebserviceRegistrationMessage registrationMessage =
                (InternalObservableWebserviceRegistrationMessage) me.getMessage();

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

                    if(params.getLatestUpdateNotificationMessageID() != coapResponse.getMessageID()){
                        log.debug("Set latest message ID for observation to {}", coapResponse.getMessageID());
                        params.setLatestUpdateNotificationMessageID(coapResponse.getMessageID());
                    }

                    if(params.getContentFormat() == ContentFormat.UNDEFINED)
                        params.setContentFormat(coapResponse.getContentFormat());

                    coapResponse.setObserveOption(params.nextNotification());
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
                        log.warn("No message ID available for update notification for {} with token {} " +
                                "(observing \"{}\".", new Object[]{params.getRemoteEndpoint(), params.getToken(),
                                params.getWebservicePath()});
                    else
                        log.info("Update notification sent: {}", updateNotification);
                }
            });
        }
    }
}
