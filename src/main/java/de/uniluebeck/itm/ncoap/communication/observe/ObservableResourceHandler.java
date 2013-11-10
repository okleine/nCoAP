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
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.communication.observe;
//
//import com.google.common.collect.HashBasedTable;
//import de.uniluebeck.itm.ncoap.application.server.InternalServiceRemovedFromServerMessage;
//import de.uniluebeck.itm.ncoap.application.server.webservice.ContentFormatNotSupportedException;
//import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
//import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
//import de.uniluebeck.itm.ncoap.message.CoapRequest;
//import de.uniluebeck.itm.ncoap.message.CoapResponse;
//import de.uniluebeck.itm.ncoap.message.MessageCode;
//import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.jboss.netty.channel.*;
//import org.jboss.netty.channel.socket.DatagramChannel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import de.uniluebeck.itm.ncoap.message.MessageType;
//import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
//import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.ContentFormat;
//import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
//
//import java.net.InetSocketAddress;
//import java.util.*;
//import java.util.concurrent.ScheduledExecutorService;
//
//import static de.uniluebeck.itm.ncoap.message.MessageCode.GET;
//import static de.uniluebeck.itm.ncoap.message.MessageType.*;
//import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.OBSERVE_REQUEST;
//import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.OBSERVE_RESPONSE;
//
//
///**
// * This handler manages the observation of observable resources. Clients can register as observers of observable
// * resources by sending a request to the resource with the observe request option set. There are three ways for
// * clients to unregister as observer:
// * <ul>
// *  <li>
// *      send a new {@link MessageCode#GET} without {@link OptionRegistry.OptionName#OBSERVE_REQUEST} to the
// *      observed resource. A new {@link MessageCode#GET} with {@link OptionRegistry.OptionName#OBSERVE_REQUEST} options
// *      stops the current observation and starts a new one.
// *  </li>
// *  <li>
// *      send a {@link MessageType#RST} message within 2 minutes after reception of an update notification that echos
// *      the message ID of the update notification. This works for both kinds of update notification {@link MessageType#CON}
// *      and {@link MessageType#NON}. Later {@link MessageType#RST} messages are ignored.
// *  </li>
// *   <li>
// *      do not acknowledge an update notification with message type {@link MessageType#CON}.
// *  </li>
// * </ul>
// *
// * Clients are automatically unregistered in case of an error, i.e. when the resource is not longer available or
// * produces update notifications with a {@link MessageCode} where {@link MessageCode#isErrorMessage()} == true.
// *
// * @author Oliver Kleine, Stefan Hüske
//*/
//public class ObservableResourceHandler extends SimpleChannelHandler implements Observer{
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//    private HashBasedTable<InetSocketAddress, String, ObservationParameter> observations = HashBasedTable.create();
//    private DatagramChannel channel;
//
//    private ScheduledExecutorService executorService;
//
//    /**
//     * @param executorService the {@link ScheduledExecutorService} instance to execute the update notification tasks,
//     *                        i.e. send update notifications to all observers of an updated resource
//     */
//    public ObservableResourceHandler(ScheduledExecutorService executorService){
//        this.executorService = executorService;
//    }
//
//    /**
//     * @param channel the {@link DatagramChannel} instance to send update notifications to observers
//     */
//    public void setChannel(DatagramChannel channel){
//        this.channel = channel;
//    }
//
//    /**
//     * This method is automatically called by the Netty framework. It handles the following types of messages
//     * possibly contained in the given {@link MessageEvent}:
//     * <ul>
//     *     <li>
//     *         {@link InternalUpdateNotificationRejectedMessage}: remove the observer
//     *         {@link InternalUpdateNotificationRejectedMessage#getObserverAddress()} from the list of observers for
//     *         {@link InternalUpdateNotificationRejectedMessage#getServicePath()}.
//     *     </li>
//     *     <li>
//     *         {@link InternalUpdateNotificationRetransmissionMessage}: Increase the notification count for
//     *         {@link InternalUpdateNotificationRetransmissionMessage#getObserverAddress()} observing
//     *         {@link InternalUpdateNotificationRetransmissionMessage#getServicePath()}.
//     *     </li>
//     *     <li>
//     *         {@link de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage}:
//     *         Stop all observations of the observer that did not confirm a confirmable update notification
//     *     </li>
//     *     <li>
//     *         {@link CoapRequest}: Add {@link MessageEvent#getRemoteAddress()} as observer for
//     *         {@link CoapRequest#getTargetUri()} if the request contains the option
//     *         {@link OptionRegistry.OptionName#OBSERVE_REQUEST}.
//     *     </li>
//     * </ul>
//     *
//     * @param ctx the {@link ChannelHandlerContext} of this {@link ObservableResourceHandler} instance
//     * @param me the {@link MessageEvent} containing the message and other event specific parameters
//     * @throws Exception
//     */
//    @Override
//    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
//        log.debug("Incoming (from {}): {}", me.getRemoteAddress(), me.getMessage());
//
//        if(me.getMessage() instanceof InternalUpdateNotificationRejectedMessage){
//            InternalUpdateNotificationRejectedMessage message = (InternalUpdateNotificationRejectedMessage) me.getMessage();
//            removeObservation(message.getObserverAddress(), message.getServicePath());
//
//            me.getFuture().setSuccess();
//            return;
//        }
//
//        if(me.getMessage() instanceof InternalUpdateNotificationRetransmissionMessage){
//            InternalUpdateNotificationRetransmissionMessage message =
//                    ((InternalUpdateNotificationRetransmissionMessage) me.getMessage());
//
//            increaseNotificationCount(message.getObserverAddress(), message.getServicePath());
//
//            me.getFuture().setSuccess();
//            return;
//        }
//
//        if(me.getMessage() instanceof InternalRetransmissionTimeoutMessage){
//            InternalRetransmissionTimeoutMessage timeoutMessage = (InternalRetransmissionTimeoutMessage) me.getMessage();
//            removeAllObservations(timeoutMessage.getRemoteAddress());
//        }
//
//        if(me.getMessage() instanceof CoapRequest){
//            CoapRequest coapRequest = (CoapRequest) me.getMessage();
//
//            //If the remote address is registered as observer than stop the observation
//            if(coapRequest.getMessageCode() == GET){
//                if(observations.contains(me.getRemoteAddress(), coapRequest.getTargetUri().getPath())){
//                    removeObservation((InetSocketAddress) me.getRemoteAddress(), coapRequest.getTargetUri().getPath());
//                }
//            }
//
//            //Add remote address as observer if the observe request option is set
//            if(!coapRequest.getOption(OBSERVE_REQUEST).isEmpty()){
//                addObservation((InetSocketAddress) me.getRemoteAddress(), coapRequest.getTargetUri().getPath(),
//                        coapRequest.getToken());
//            }
//        }
//
//        ctx.sendUpstream(me);
//    }
//
//
//    /**
//     * This method is automatically called by the Netty framework. It handles the following types of messages
//     * possibly contained in the given {@link MessageEvent}:
//     * <ul>
//     *     <li>
//     *         {@link InternalObservableResourceRegistrationMessage}: If there is a new observer to be registered
//     *         for an instance of {@link ObservableWebService}.
//     *     </li>
//     *     <li>
//     *         {@link InternalServiceRemovedFromServerMessage}: If there was an instance of {@link ObservableWebService}
//     *         removed from the {@link CoapServerApplication}, all observations for this service are stopped
//     *         and observers are notified.
//     *     </li>
//     *     <li>
//     *         {@link CoapResponse} if there was a response by an instance of {@link ObservableWebService} and this
//     *         response is the result of a request from an observer of the service, then the running observation
//     *         is stopped. If the request contained the {@link OptionName#OBSERVE_REQUEST} option, a new observation
//     *         is started, i.e. notification count starts at 1 again.
//     *     </li>
//     * </ul>
//     *
//     * @param ctx the {@link ChannelHandlerContext} of this {@link ObservableWebService}
//     * @param me  the {@link MessageEvent} containing the message to be handled
//     *
//     * @throws Exception
//     */
//    @Override
//    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent me) throws Exception {
//        log.debug("Outgoing: {} to {}.", me.getMessage(), me.getRemoteAddress());
//
//        if(me.getMessage() instanceof InternalObservableResourceRegistrationMessage){
//            ((InternalObservableResourceRegistrationMessage) me.getMessage()).getWebService().addObserver(this);
//            me.getFuture().setSuccess();
//            return;
//        }
//
//        if(me.getMessage() instanceof InternalServiceRemovedFromServerMessage){
//            InternalServiceRemovedFromServerMessage message = (InternalServiceRemovedFromServerMessage) me.getMessage();
//
//            for(Object observerAddress : observations.column(message.getServicePath()).keySet().toArray()){
//                ObservationParameter parameter =
//                        observations.remove(observerAddress, message.getServicePath());
//
//                if(parameter != null){
//                    log.info("Removed {} as observer for service {}.", observerAddress, message.getServicePath());
//                    CoapResponse updateNotification = new CoapResponse(MessageCode.NOT_FOUND_404);
//                    updateNotification.getHeader().setMessageType(CON);
//
//                    updateNotification.setServicePath(message.getServicePath());
//                    updateNotification.setToken(parameter.getToken());
//
//                    sendUpdateNotification(updateNotification, (InetSocketAddress) observerAddress);
//                }
//            }
//
//            me.getFuture().setSuccess();
//            return;
//        }
//
//        if(me.getMessage() instanceof CoapResponse){
//            CoapResponse coapResponse = (CoapResponse) me.getMessage();
//            if(!coapResponse.isUpdateNotification()){
//                if(observations.contains(me.getRemoteAddress(), coapResponse.getServicePath())){
//                    removeObservation((InetSocketAddress) me.getRemoteAddress(), coapResponse.getServicePath());
//                }
//            }
//            else{
//                if(observations.contains(me.getRemoteAddress(), coapResponse.getServicePath())){
//
//                    if((Long) coapResponse.getOption(OBSERVE_RESPONSE).get(0).getDecodedValue() == 0){
//                        increaseNotificationCount((InetSocketAddress) me.getRemoteAddress(),
//                                coapResponse.getServicePath());
//                    }
//
//                    ObservationParameter parameter =
//                            observations.get(me.getRemoteAddress(), coapResponse.getServicePath());
//
//                    if(parameter != null){
//                        if(coapResponse.getContentType() != null){
//                            log.info("Set ContentFormat {} for {} observing {}", new Object[]{coapResponse.getContentType(),
//                                    me.getRemoteAddress(), coapResponse.getServicePath()});
//                            parameter.setAcceptedMediaType(coapResponse.getContentType());
//                        }
//
//                        //set the observer specific notification count of the running observation
//                        coapResponse.setObserveOptionValue(parameter.getNotificationCount());
//                    }
//                }
//            }
//        }
//
//        ctx.sendDownstream(me);
//    }
//
//    private void sendUpdateNotification(final CoapResponse updateNotification,
//                                        final InetSocketAddress remoteAddress){
//
//        ChannelFuture future = Channels.write(channel, updateNotification, remoteAddress);
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                log.info("Update notification for {} successfully sent to {}.",
//                        updateNotification.getServicePath(), remoteAddress);
//            }
//        });
//    }
//
//    private void removeAllObservations(InetSocketAddress observerAddress){
//        for(Object servicePath : observations.row(observerAddress).keySet().toArray()){
//            removeObservation(observerAddress, (String) servicePath);
//        }
//    }
//
//    private synchronized void removeObservation(InetSocketAddress observerAddress, String servicePath){
//        if(observations.remove(observerAddress, servicePath) != null){
//            log.info("Removed {} as observer of {}.", observerAddress, servicePath);
//        }
//    }
//
//    private synchronized void addObservation(InetSocketAddress observerAddress, String servicePath, byte[] token){
//        observations.put(observerAddress, servicePath,  new ObservationParameter(token));
//        log.info("Added {} as observer for {}.", observerAddress, servicePath);
//    }
//
//    private synchronized Map<InetSocketAddress, ObservationParameter> getObservations(String servicePath){
//        HashMap<InetSocketAddress, ObservationParameter> result =
//                new HashMap<InetSocketAddress, ObservationParameter>();
//
//        for(InetSocketAddress observerAddress : observations.column(servicePath).keySet()){
//            result.put(observerAddress, observations.get(observerAddress, servicePath));
//        }
//
//        if(log.isDebugEnabled()){
//            for(InetSocketAddress observerAddress : result.keySet()){
//                log.debug("Found {} as observer for {}.", observerAddress, servicePath);
//            }
//        }
//
//        return result;
//    }
//
//    private synchronized void increaseNotificationCount(InetSocketAddress observerAddress, String servicePath){
//        ObservationParameter parameter = observations.get(observerAddress, servicePath);
//        if(parameter != null){
//            parameter.increaseNotificationCount();
//        }
//    }
//
//
//    /**
//     * This method is automatically invoked by the framework if the status of an {@link ObservableWebService}
//     * instance changed. On invocation every observer gets an update notification.
//     *
//     * @param observable the {@link ObservableWebService} instance whose resource status changed
//     * @param arg null and thus ignored
//     */
//    @Override
//    public void update(final Observable observable, Object arg) {
//        ObservableWebService webService = (ObservableWebService) observable;
//        log.info("Observable service {} updated!", webService.getPath());
//
//        UpdateNotificationsSender updateNotificationSender =
//                new UpdateNotificationsSender(webService, getObservations(webService.getPath()));
//        executorService.submit(updateNotificationSender);
//    }
//
//
//    private class UpdateNotificationsSender implements Runnable{
//
//        private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//        private ObservableWebService webService;
//        private Map<InetSocketAddress, ObservationParameter> observations;
//
//        UpdateNotificationsSender(ObservableWebService webService,
//                                  Map<InetSocketAddress, ObservationParameter> observations){
//            this.webService = webService;
//            this.observations = observations;
//        }
//
//        @Override
//        public void run() {
//            final EnumMap<OptionRegistry.ContentFormat, ChannelBuffer> serializations =
//                    new EnumMap<OptionRegistry.ContentFormat, ChannelBuffer>(OptionRegistry.ContentFormat.class);
//
//            for(final InetSocketAddress observerAddress : observations.keySet()){
//
//                //get media type for the actual observation
//                ObservationParameter parameter = observations.get(observerAddress);
//                ContentFormat mediaType = parameter.getAcceptedMediaType();
//
//                //create update notification
//                CoapResponse updateNotification;
//                try{
//                    //check if payload for the media type is already available
//                    if(!serializations.containsKey(mediaType)){
//                        log.debug("Create {} payload for update notifications of {}.", mediaType, webService.getPath());
//
//                        //Create payload for updated resource
//                        byte[] payload = webService.getSerializedResourceStatus(mediaType);
//                        serializations.put(mediaType, ChannelBuffers.wrappedBuffer(payload));
//                        log.debug("Added {} payload for update notifications of {}.", mediaType, webService.getPath());
//                    }
//
//                    //Create update notification and set parameters properly
//                    updateNotification = new CoapResponse(MessageCode.CONTENT_205);
//                    updateNotification.getHeader().setMessageType(webService.getMessageTypeForUpdateNotifications());
//                    updateNotification.setContentType(mediaType);
//                    updateNotification.setContent(ChannelBuffers.copiedBuffer(serializations.get(mediaType)));
//                    updateNotification.setMaxAge(webService.getMaxAge());
//
//                    //update notification count for observation
//                    parameter.increaseNotificationCount();
//                    updateNotification.setObserveOptionValue(parameter.getNotificationCount());
//
//                    updateNotification.setServicePath(webService.getPath());
//
//                }
//                catch(Exception e){
//                    if(e instanceof ContentFormatNotSupportedException){
//                        ContentFormatNotSupportedException exception = (ContentFormatNotSupportedException) e;
//                        String message = "Media type(s)";
//                        for(ContentFormat unsupportedMediaType : exception.getUnsupportedContentFormats()){
//                            message += (" " + unsupportedMediaType);
//                        }
//                        message += " not anymore supported by service " + webService.getPath();
//
//                        log.warn(message);
//                    }
//                    else{
//                        log.error("This should never happen.", e);
//                    }
//
//                    //cancel observation due to error
//                    removeObservation(observerAddress, webService.getPath());
//
//                    //make update notification an error message
//                    updateNotification = new CoapResponse(MessageCode.INTERNAL_SERVER_ERROR_500);
//                    updateNotification.getHeader().setMessageType(MessageType.NON);
//                }
//
//                try{
//                    byte[] token = parameter.getToken();
//                    if(token.length > 0)
//                        updateNotification.setToken(token);
//                }
//                catch (Exception e){
//                    log.error("This should never happen.", e);
//                }
//
//                //Send update notification
//                sendUpdateNotification(updateNotification, observerAddress);
//            }
//        }
//    }
//}
