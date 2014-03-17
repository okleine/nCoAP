package de.uniluebeck.itm.ncoap.communication.observe;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WrappedWebserviceStatus;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalResetReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

/**
* Created by olli on 17.03.14.
*/
public class WebserviceObservationHandler extends SimpleChannelHandler implements Observer {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private final Object monitor = new Object();
    private Table<InetSocketAddress, Token, ObservationParams> observationsPerObserver;
    private Multimap<String, ObservationParams> observationsPerService;

    private ChannelHandlerContext ctx;


    public WebserviceObservationHandler(){
        this.observationsPerObserver = HashBasedTable.create();
        this.observationsPerService = HashMultimap.create();
    }


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
            handleInternalObservableWebserviceRegistrationMessage(ctx, me);

        else
            ctx.sendDownstream(me);
    }

    private void handleInternalObservableWebserviceRegistrationMessage(ChannelHandlerContext ctx, MessageEvent me) {

        InternalObservableWebserviceRegistrationMessage registrationMessage =
                (InternalObservableWebserviceRegistrationMessage) me.getMessage();

        registrationMessage.getWebservice().addObserver(this);
    }


    private void handleOutgoingCoapResponse(ChannelHandlerContext ctx, MessageEvent me) {

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        CoapResponse coapResponse = (CoapResponse) me.getMessage();

        if(observationsPerObserver.contains(remoteEndpoint, coapResponse.getToken())){
            if(!coapResponse.isUpdateNotification()){
                synchronized (monitor){
                    ObservationParams params = observationsPerObserver.remove(remoteEndpoint, coapResponse.getToken());
                    observationsPerService.remove(params.getWebservicePath(), params);
                }
            }

            else{
                ObservationParams params = observationsPerObserver.get(remoteEndpoint, coapResponse.getToken());

                if(params.getLatestUpdateNotificationMessageID() != coapResponse.getMessageID())
                    params.setLatestUpdateNotificationMessageID(coapResponse.getMessageID());

                if(params.getContentFormat() == ContentFormat.UNDEFINED)
                    params.setContentFormat(coapResponse.getContentFormat());

                coapResponse.setObserveOption(params.nextNotification());
            }
        }

        ctx.sendDownstream(me);
    }


    @Override
    public void update(Observable observable, Object arg) {
        ObservableWebservice webservice = (ObservableWebservice) observable;

        Map<Long, WrappedWebserviceStatus> statusCache = new HashMap<>();

        Collection<ObservationParams> tmp = observationsPerService.get(webservice.getPath());
        ObservationParams[] observations = tmp.toArray(new ObservationParams[tmp.size()]);

        for(ObservationParams params : observations){
            long contentFormat = params.getContentFormat();

            if(!statusCache.containsKey(contentFormat))
                statusCache.put(contentFormat, webservice.getWrappedWebserviceStatus(contentFormat));

            WrappedWebserviceStatus wrappedWebserviceStatus = statusCache.get(contentFormat);
            MessageType.Name messageType =
                    webservice.getMessageTypeForUpdateNotification(params.getRemoteEndpoint());

            CoapResponse updateNotification;
            if(params.getEtags().contains(wrappedWebserviceStatus.getEtag())){
                updateNotification = new CoapResponse(messageType, MessageCode.Name.VALID_203);
            }

            else{
                updateNotification = new CoapResponse(messageType, MessageCode.Name.CONTENT_205);
                updateNotification.setContent(wrappedWebserviceStatus.getContent(), contentFormat);
            }

            updateNotification.setMaxAge(wrappedWebserviceStatus.getMaxAge());
            updateNotification.setObserveOption(0);

            updateNotification.setMessageID(params.getLatestUpdateNotificationMessageID());
            updateNotification.setToken(params.getToken());

            Channels.write(this.ctx.getChannel(), updateNotification, params.getRemoteEndpoint());
        }
    }
}
