package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.internal.InternalServiceRemovedFromPathMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.internal.InternalUpdateNotificationRetransmissionMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.internal.ObservableResourceRegistrationMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.internal.UpdateNotificationRejectedMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.GET;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.CON;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.NON;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_REQUEST;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.OBSERVE_RESPONSE;


/**
 * This handler manages the observation of observable resources. Clients can register as observers of observable
 * resources by sending a request to the resource with the observe request option set. There are two ways for
 * clients to unregister as observer:
 * <ul>
 *  <li>
 *      send a new {@link Code#GET} without {@link OptionRegistry.OptionName#OBSERVE_REQUEST} to the
 *      observed resource. A new {@link Code#GET} with {@link OptionRegistry.OptionName#OBSERVE_REQUEST} options
 *      stops the current observation and starts a new one.
 *  </li>
 *  <li>
 *      send a {@link MsgType#RST} message within 2 minutes after reception of an update notification that echos
 *      the message ID of the update notification. This works for both kinds of update notification {@link MsgType#CON}
 *      and {@link MsgType#NON}. Later {@link MsgType#RST} messages are ignored.
 *  </li>
 * </ul>
 *
 * Clients are automatically unregistered in case of an error, i.e. when the resource is not longer available or
 * produces update notifications with a {@link Code} where {@link Code#isErrorMessage()} == true.
 *
 *  @author Oliver Kleine, Stefan Hueske
*/
public class ObservableResourceHandler extends SimpleChannelHandler implements Observer{

    private static Logger log = LoggerFactory.getLogger(ObservableResourceHandler.class.getName());

    private HashBasedTable<InetSocketAddress, String, ObservationParameter> observations = HashBasedTable.create();
    private DatagramChannel channel;

    private ScheduledExecutorService executorService;

    /**
     * Constructor for a new instance
     * @param executorService the {@link ScheduledExecutorService} instance to execute the update notification tasks,
     *                        i.e. send update notifications to all observers of an updated resource
     */
    public ObservableResourceHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
    }

    public void setChannel(DatagramChannel channel){
        this.channel = channel;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        log.debug("Incoming (from {}): {}", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof UpdateNotificationRejectedMessage){
            UpdateNotificationRejectedMessage message = (UpdateNotificationRejectedMessage) me.getMessage();
            removeObserver(message.getObserverAddress(), message.getServicePath());

            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalUpdateNotificationRetransmissionMessage){
            InternalUpdateNotificationRetransmissionMessage message =
                    ((InternalUpdateNotificationRetransmissionMessage) me.getMessage());

            increaseNotificationCount(message.getObserverAddress(), message.getServicePath());

            me.getFuture().setSuccess();
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
        log.debug("Outgoing: {} to {}.", me.getMessage(), me.getRemoteAddress());

        if(me.getMessage() instanceof ObservableResourceRegistrationMessage){
            ((ObservableResourceRegistrationMessage) me.getMessage()).getWebService().addObserver(this);
            me.getFuture().setSuccess();
            return;
        }

        if(me.getMessage() instanceof InternalServiceRemovedFromPathMessage){
            InternalServiceRemovedFromPathMessage message = (InternalServiceRemovedFromPathMessage) me.getMessage();

            for(Object observerAddress : observations.column(message.getServicePath()).keySet().toArray()){
                ObservationParameter parameter =
                        observations.remove(observerAddress, message.getServicePath());

                if(parameter != null){
                    CoapResponse updateNotification =
                            new CoapResponse(Code.NOT_FOUND_404);
                    updateNotification.setServicePath(message.getServicePath());
                    updateNotification.setToken(parameter.getToken());
                    updateNotification.getHeader().setMsgType(NON);
                    sendUpdateNotification(updateNotification, (InetSocketAddress) observerAddress);
                }
            }

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
                if(observations.contains(me.getRemoteAddress(), coapResponse.getServicePath())){
                    increaseNotificationCount((InetSocketAddress) me.getRemoteAddress(),
                            coapResponse.getServicePath());

                    ObservationParameter status = observations.get(me.getRemoteAddress(), coapResponse.getServicePath());

                    if(status != null){
                        if(coapResponse.getContentType() != null){
                            log.info("Set MediaType {} for {} observing {}", new Object[]{coapResponse.getContentType(),
                                    me.getRemoteAddress(), coapResponse.getServicePath()});
                            status.setAcceptedMediaType(coapResponse.getContentType());
                        }

                        //set the observer specific notification count of the running observation
                        coapResponse.setObserveOptionResponse(status.getNotificationCount());
                    }
                }
            }
        }

        ctx.sendDownstream(me);
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

        if(log.isDebugEnabled()){
            for(InetSocketAddress observerAddress : result.keySet()){
                log.debug("Found {} as observer for {}.", observerAddress, servicePath);
            }
        }

        return result;
    }

    private synchronized void increaseNotificationCount(InetSocketAddress observerAddress, String servicePath){
        ObservationParameter parameter = observations.get(observerAddress, servicePath);
        if(parameter != null){
            parameter.increaseNotificationCount();
            log.debug("Notificaton count for {} observing {} set to {}.",
                    new Object[]{observerAddress, servicePath, parameter.getNotificationCount()});
        }
    }

    private void sendUpdateNotification(final CoapResponse updateNotification,
                                        final InetSocketAddress remoteAddress){

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

                    EnumMap<MediaType, ChannelBuffer> payload = new EnumMap<MediaType, ChannelBuffer>(MediaType.class);

                    Map<InetSocketAddress, ObservationParameter> observations= getObservations(webService.getPath());

                    for(InetSocketAddress observerAddress : observations.keySet()){
                        ObservationParameter parameter = observations.get(observerAddress);

                        if(!payload.containsKey(parameter.getAcceptedMediaType())){
                            //Create payload for updated resource
                            CoapRequest coapRequest =
                                    new CoapRequest(NON, Code.GET, new URI("coap://localhost" + webService.getPath()));
                            CoapResponse coapResponse = webService.processMessage(coapRequest, new InetSocketAddress(0));

                            if(coapResponse.getContentType() == null)
                                coapResponse.setContentType(MediaType.TEXT_PLAIN_UTF8);

                            if(coapResponse.getCode().isErrorMessage()){
                                //If resource produced an error stop the observation
                                removeObserver(observerAddress, webService.getPath());

                                //Send error message
                                CoapResponse updateNotification =
                                        new CoapResponse(coapResponse.getCode());
                                updateNotification.setServicePath(webService.getPath());
                                updateNotification.setPayload(coapResponse.getPayload());
                                sendUpdateNotification(updateNotification, observerAddress);
                                return;
                            }
                            else{
                                payload.put(coapResponse.getContentType(), coapResponse.getPayload());
                            }
                        }

                        //Create update notification
                        CoapResponse updateNotification = new CoapResponse(Code.CONTENT_205);
                        updateNotification.setServicePath(webService.getPath());

                        //reliability
                        if(webService.isUpdateNotificationConfirmable())
                            updateNotification.getHeader().setMsgType(CON);
                        else
                            updateNotification.getHeader().setMsgType(NON);

                        //set options
                        updateNotification.setMaxAge(webService.getMaxAge());
                        updateNotification.setToken(parameter.getToken());
                        parameter.increaseNotificationCount();
                        updateNotification.setObserveOptionResponse(parameter.getNotificationCount());

                        //set payload
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