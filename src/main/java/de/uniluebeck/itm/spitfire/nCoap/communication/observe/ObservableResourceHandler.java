package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledExecutorService;

import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.GET;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.CON;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.NON;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_REQUEST;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_RESPONSE;



/**
* This handler manages observable resources and observer.
* Observers will be registered when receiving passing observable requests.
* Observers will be removed when either a Reset-Message or a InternalErrorMessage is received.
* In reaction to a received ObservableWebServiceUpdate notifications will be send to all corresponding observers.
*
* @author Stefan Hueske
*/
public class ObservableResourceHandler extends SimpleChannelHandler implements Observer{

    private static Logger log = LoggerFactory.getLogger(ObservableResourceHandler.class.getName());

    private HashBasedTable<InetSocketAddress, String, ObservationParameter> observations
            = HashBasedTable.create();

    private Object monitor = new Object();

    private ScheduledExecutorService executorService;

    public ObservableResourceHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
    }

    private synchronized void removeObserver(InetSocketAddress observerAddress, String servicePath){
        if(observations.remove(observerAddress, servicePath) != null){
            log.info("Removed {} as observer of {}.", observerAddress, servicePath);
        }
    }

    private synchronized void addObserver(InetSocketAddress observerAddress, String servicePath, byte[] token){
        observations.put(observerAddress, servicePath,  new ObservationParameter(token));
        log.info("Added {} as observer for {}.", observerAddress, servicePath);
    }

    private synchronized Map<InetSocketAddress, ObservationParameter> getObservations(String servicePath){
        HashMap<InetSocketAddress, ObservationParameter> result =
                new HashMap<InetSocketAddress, ObservationParameter>();

        for(InetSocketAddress observerAddress : observations.column(servicePath).keySet()){
            result.put(observerAddress, observations.get(observerAddress, servicePath));
        }

        log.info("Found {} observers for {}.", result.size(), servicePath);
        return result;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        log.info("Incoming (from {}): {}", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof UpdateNotificationRejectedMessage){
            UpdateNotificationRejectedMessage message = (UpdateNotificationRejectedMessage) me.getMessage();
            removeObserver(message.getObserverAddress(), message.getServicePath());
            return;
        }

        if(me.getMessage() instanceof CoapRequest){
            CoapRequest coapRequest = (CoapRequest) me.getMessage();

            //If the remote address is registered as observer than stop the observation
            if(coapRequest.getCode() == GET){
                if(observations.contains(me.getRemoteAddress(), coapRequest.getTargetUri().getPath())){
                    removeObserver((InetSocketAddress) me.getRemoteAddress(), coapRequest.getTargetUri().getPath());
                }
            }

            //Add remote address as observer if the observe request option is set
            if(!coapRequest.getOption(OBSERVE_REQUEST).isEmpty()){
                addObserver((InetSocketAddress) me.getRemoteAddress(), coapRequest.getTargetUri().getPath(),
                        coapRequest.getToken());
            }
        }

        ctx.sendUpstream(me);
    }


    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me) throws Exception {

        log.info("Outgoing: {} to {}.", me.getMessage(), me.getRemoteAddress());

        if(me.getMessage() instanceof ObservableResourceRegistrationMessage){
            ((ObservableResourceRegistrationMessage) me.getMessage()).getWebService().addObserver(this);
            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse coapResponse = (CoapResponse) me.getMessage();
            if(coapResponse.getOption(OBSERVE_RESPONSE).isEmpty()){
                if(observations.contains(me.getRemoteAddress(), coapResponse.getServicePath())){
                    removeObserver((InetSocketAddress) me.getRemoteAddress(), coapResponse.getServicePath());
                }
            }
            else{
                ObservationParameter status = observations.get(me.getRemoteAddress(), coapResponse.getServicePath());
                if(status != null){
                    if(coapResponse.getContentType() != null){
                        log.info("Set MediaType {} for {} observing {}", new Object[]{coapResponse.getContentType(),
                                me.getRemoteAddress(), coapResponse.getServicePath()});
                        status.setAcceptedMediaType(coapResponse.getContentType());
                    }

                    //set the observer specific notification count of the running observation
                    status.increaseNotificationCount();
                    coapResponse.setObserveOptionResponse(status.getNotificationCount());
                }
            }
        }


        ctx.sendDownstream(me);
    }

    private void sendUpdateNotification(final UpdateNotification updateNotification,
                                        final InetSocketAddress remoteAddress){

        DatagramChannel channel = CoapServerDatagramChannelFactory.getChannel(5683);


        ChannelFuture future = Channels.write(channel, updateNotification, remoteAddress);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Update notification for {} successfully sent to {}.",
                        updateNotification.getServicePath(), remoteAddress);
            }
        });
    }

    @Override
    public void update(final Observable observable, Object arg) {
        executorService.submit(new Runnable(){
            @Override
            public void run() {
                try{
                    ObservableWebService webService = (ObservableWebService) observable;
                    log.info("Observable service {} updated!", webService.getPath());

                    HashMap<MediaType, ChannelBuffer> payload = new HashMap<MediaType, ChannelBuffer>();

                    Map<InetSocketAddress, ObservationParameter> observations= getObservations(webService.getPath());

                    for(InetSocketAddress observerAddress : observations.keySet()){
                        ObservationParameter parameter = observations.get(observerAddress);

                        if(!payload.containsKey(parameter.getAcceptedMediaType())){
                            //Create payload for updated resource
                            CoapRequest coapRequest =
                                    new CoapRequest(NON, Code.GET, new URI("coap://localhost" + webService.getPath()));
                            CoapResponse coapResponse = webService.processMessage(coapRequest, new InetSocketAddress(0));


                            if(coapResponse.getCode().isErrorMessage()){
                                //If resource produced an error stop the observation
                                removeObserver(observerAddress, webService.getPath());

                                //Send error message
                                UpdateNotification updateNotification =
                                        new UpdateNotification(coapResponse.getCode(), webService.getPath());
                                updateNotification.setPayload(coapResponse.getPayload());
                                sendUpdateNotification(updateNotification, observerAddress);
                                return;
                            }
                            else{
                                payload.put(coapResponse.getContentType(), coapResponse.getPayload());
                            }
                        }

                        //Create update notification
                        UpdateNotification updateNotification = new UpdateNotification(Code.CONTENT_205, webService.getPath());

                        if(webService.isUpdateNotificationConfirmable())
                            updateNotification.getHeader().setMsgType(CON);
                        else
                            updateNotification.getHeader().setMsgType(NON);

                        parameter.increaseNotificationCount();
                        updateNotification.setObserveOptionResponse(parameter.getNotificationCount());
                        updateNotification.setToken(parameter.getToken());
                        updateNotification.setPayload(ChannelBuffers.copiedBuffer(payload.get(parameter.getAcceptedMediaType())));

                        sendUpdateNotification(updateNotification, observerAddress);
                    }
                }
                catch(Exception e){
                    log.error("This should never happen!", e);
                }
            }
        });

    }
}