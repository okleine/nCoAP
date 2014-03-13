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
package de.uniluebeck.itm.ncoap.communication.observe;

//import com.google.common.collect.HashBasedTable;
//import de.uniluebeck.itm.ncoap.application.client.Token;
//import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
//import de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingMessageReliabilityHandler;
//import de.uniluebeck.itm.ncoap.message.*;
//import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.jboss.netty.channel.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.util.*;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
///**
//* Created with IntelliJ IDEA.
//* User: olli
//* Date: 18.11.13
//* Time: 16:41
//* To change this template use File | Settings | File Templates.
//*/
//public class WebserviceObservationHandler extends SimpleChannelHandler implements Observer {
//
//    private Logger log = LoggerFactory.getLogger(WebserviceObservationHandler.class.getName());
//
//    //This table is to relate the first response on a observation request with the service path
//    private HashBasedTable<InetSocketAddress, Token, String> observationsPerToken;
//
//    //This table is to relate the observers socket address and the service path to a running observation
//    private HashBasedTable<InetSocketAddress, String, ObservationParameter> observationsPerPath;
//
//    private ScheduledExecutorService executorService;
//
//    public WebserviceObservationHandler(ScheduledExecutorService executorService){
//        this.executorService = executorService;
//        observationsPerPath = HashBasedTable.create();
//        observationsPerToken = HashBasedTable.create();
//    }
//
//
//    @Override
//    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
//
//        if(me.getMessage() instanceof CoapMessage){
//            CoapMessage coapMessage = (CoapMessage) me.getMessage();
//            InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteEndpoint();
//
//            //Running observations can be cancelled by observing clients by responding with an RST message upon
//            //reception of an update notification. The RST must contain the message ID of the latest update
//            //notification
//            if(coapMessage.getMessageTypeName() == MessageType.Name.RST){
//                int resetedMessageID = coapMessage.getMessageID();
//
//                //Find running observation by the remote host
//                Map<String, ObservationParameter> observationsForRemoteHost =
//                        observationsPerPath.row(remoteEndpoint);
//
//                //Check if the message ID contained in the RST matches any of the running observations
//                for(String servicePath : observationsForRemoteHost.keySet()){
//                    ObservationParameter observationParameter = observationsForRemoteHost.get(servicePath);
//
//                    if(observationParameter.getLatestMessageID() == resetedMessageID){
//                        stopObservation(remoteEndpoint, servicePath);
//                        break;
//                    }
//                }
//            }
//
//            //If the message is no RST message but a CoAP request then
//            else if(coapMessage instanceof CoapRequest){
//                CoapRequest coapRequest = (CoapRequest) coapMessage;
//                Token token = coapRequest.getToken();
//
//                //Stop observation if there is an already running observation from the remote host with the same token
//                if(observationsPerToken.contains(remoteEndpoint, token))
//                    stopObservation(ctx, remoteEndpoint, token);
//
//                //Start new observation if the received CoAP request contains the observe option
//                if(coapRequest.isObserveSet())
//                    addObservationPerToken((InetSocketAddress) me.getRemoteEndpoint(), coapRequest.getToken(),
//                            coapRequest.getUriPath());
//            }
//        }
//
//        ctx.sendUpstream(me);
//    }
//
//    @Override
//    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
//
//        if(me.getMessage() instanceof InternalObservableWebserviceRegistrationMessage){
//            registerObservableWebservice(
//                    ((InternalObservableWebserviceRegistrationMessage) me.getMessage()).getWebservice()
//            );
//
//            me.getFuture().setSuccess();
//            return;
//        }
//
//        else if(me.getMessage() instanceof CoapResponse){
//            CoapResponse coapResponse = (CoapResponse) me.getMessage();
//
//            InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteEndpoint();
//            Token token = coapResponse.getToken();
//            int messageID = coapResponse.getMessageID();
//
//            String webservicePath = this.observationsPerToken.get(remoteEndpoint, token);
//
//            //Start new observation (otherwise webservice path will be null)
//            if(webservicePath != null){
//                if(!this.observationsPerPath.contains(remoteEndpoint, webservicePath)){
//                    //This is a new observation
//                    log.debug("Start observation of service {} (observer: {}, token: {}, messageID: {})",
//                            new Object[]{webservicePath, remoteEndpoint, token,
//                                         messageID
//                            });
//
//                    ObservationParameter observationParameter =
//                            new ObservationParameter(coapResponse.getMessageID(), token,
//                                    coapResponse.getContentFormat(), ctx.getChannel());
//
//                    addObservation(remoteEndpoint, webservicePath, observationParameter);
//                }
//
//                else{
//                    log.debug("Update observation of service {} (observer: {}, token: {}, messageID: {})",
//                            new Object[]{webservicePath, remoteEndpoint, token, messageID});
//
//                    //Update the the latest message ID to the one set by the Outgoing Reliability Handler
//                    updateLatestMessageID(remoteEndpoint, token, messageID);
//                }
//
//                coapResponse.setObserveOption(getNextUpdateNotificationCount(remoteEndpoint, token));
//            }
//
//            else{
//                stopObservation(ctx, remoteEndpoint, coapResponse.getToken());
//            }
//        }
//
//        ctx.sendDownstream(me);
//    }
//
//
//    private void registerObservableWebservice(ObservableWebservice webservice){
//        webservice.addObserver(this);
//    }
//
//
//    private synchronized void updateLatestMessageID(InetSocketAddress remoteEndpoint, Token token, int messageID){
//        String servicePath = this.observationsPerToken.get(remoteEndpoint, token);
//        ObservationParameter observationParameter = this.observationsPerPath.get(remoteEndpoint, servicePath);
//
//        observationParameter.setLatestMessageID(messageID);
//    }
//
//    private synchronized long getNextUpdateNotificationCount(InetSocketAddress remoteEndpoint, Token token){
//        String servicePath = this.observationsPerToken.get(remoteEndpoint, token);
//        ObservationParameter observationParameter = this.observationsPerPath.get(remoteEndpoint, servicePath);
//
//        return observationParameter.getNextUpdateNotificationTransmissionCount();
//    }
//
//    @Override
//    public void update(Observable object, Object serviceShutdown) {
//        if(!(object instanceof ObservableWebservice && serviceShutdown instanceof Boolean))
//            return;
//
//        ObservableWebservice webservice = (ObservableWebservice) object;
//        String webservicePath = webservice.getPath();
//
//        log.info("UPDATE of service {}", webservicePath);
//
//        Map<InetSocketAddress, ObservationParameter> applyingObservations = observationsPerPath.column(webservicePath);
//        log.info("Found {} observers for service {}.", applyingObservations.size(), webservicePath);
//
//        if(!((Boolean) serviceShutdown))
//            sendUpdateNotification(applyingObservations, webservice);
//        else
//            sendErrorMessage(applyingObservations, webservice);
//    }
//
//
//    private void sendErrorMessage(Map<InetSocketAddress, ObservationParameter> runningObservations,
//                                  ObservableWebservice webservice){
//
//        for(InetSocketAddress remoteEndpoint : runningObservations.keySet()){
//            log.debug("Try to notify {}.", remoteEndpoint);
//
//            ObservationParameter observationParameter = runningObservations.get(remoteEndpoint);
//
//            try{
//                CoapResponse coapResponse = new CoapResponse(MessageType.Name.NON, MessageCode.Name.NOT_FOUND_404);
//                coapResponse.setMessageType(webservice.getMessageTypeForUpdateNotifications().getNumber());
//
//                String message = "Sorry, the service \"" + webservice.getPath() + "\" is not anymore available";
//                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
//                coapResponse.setToken(observationParameter.getToken());
//
//                executorService.schedule(new UpdateNotificationSender(remoteEndpoint, coapResponse,
//                        observationParameter.getLatestMessageID(), observationParameter.getChannel()
//                ), 0, TimeUnit.MILLISECONDS);
//
//                stopObservation(remoteEndpoint, webservice.getPath());
//            }
//            catch(Exception e){
//                log.error("This should never happen!", e);
//            }
//
//        }
//    }
//
//    private void sendUpdateNotification(Map<InetSocketAddress, ObservationParameter> runningObservations,
//                                        ObservableWebservice webservice){
//
//        Map<Long, ChannelBuffer> formattedContent = new HashMap<Long, ChannelBuffer>();
//
//        for(InetSocketAddress remoteEndpoint : runningObservations.keySet()){
//            log.debug("Try to notify {}.", remoteEndpoint);
//
//            ObservationParameter observationParameter = runningObservations.get(remoteEndpoint);
//            observationParameter.nextResourceUpdate();
//
//            long contentFormat = observationParameter.getContentFormat();
//
//            try{
//                ChannelBuffer serializedResourceStatus;
//                if(formattedContent.containsKey(contentFormat)){
//                    serializedResourceStatus = ChannelBuffers.copiedBuffer(formattedContent.get(contentFormat));
//                }
//                else{
//                    serializedResourceStatus =
//                            ChannelBuffers.wrappedBuffer(webservice.getSerializedResourceStatus(contentFormat));
//                    formattedContent.put(contentFormat, ChannelBuffers.copiedBuffer(serializedResourceStatus));
//                }
//
//                MessageType.Name messageType = webservice.getMessageTypeForUpdateNotifications();
//                CoapResponse updateNotification = new CoapResponse(messageType, MessageCode.Name.CONTENT_205);
//
//                updateNotification.setToken(observationParameter.getToken());
//                updateNotification.setEtag(webservice.getEtag(contentFormat));
//                updateNotification.setObserveOption(observationParameter.getNotificationCount());
//                updateNotification.setContent(serializedResourceStatus, contentFormat);
//
//                executorService.schedule(new UpdateNotificationSender(remoteEndpoint, updateNotification,
//                        observationParameter.getLatestMessageID(), observationParameter.getChannel()
//                ), 0, TimeUnit.MILLISECONDS);
//
//                log.debug("Update notification for {} scheduled.", remoteEndpoint);
//            }
//
//            catch (Exception e) {
//                log.error("Error while trying to create and schedule update notification.", e);
//            }
//        }
//    }
//
////    private void handleContentFormatNotSupportedException(InetSocketAddress remoteEndpoint,
////                ObservationParameter observationParameter, AcceptedContentFormatNotSupportedException e){
////
////        try {
////            String webservice = observationsPerToken.get(remoteEndpoint, observationParameter.getToken());
////            stopObservation(remoteEndpoint, webservice);
////
////            CoapResponse updateNotification = new CoapResponse(MessageCode.Name.NOT_IMPLEMENTED_501);
////            String message = "Not supported anymore: Content Format No. " + e.getUnsupportedContentFormatsAsString();
////            updateNotification.setContent(message.getBytes(CoapMessage.CHARSET));
////
////            executorService.schedule(new UpdateNotificationSender(remoteEndpoint, updateNotification,
////                    observationParameter.getLatestMessageID(), observationParameter.getChannel()
////            ), 0, TimeUnit.MILLISECONDS);
////        }
////        catch (InvalidHeaderException e1) {
////            log.error("This should never happen.", e1);
////        }
////        catch (InvalidMessageException e1) {
////            log.error("This should never happen.", e1);
////        }
////
////    }
//
//    private synchronized void addObservation(InetSocketAddress remoteEndpoint, String path,
//                                             ObservationParameter observationParameter){
//
//        this.observationsPerPath.put(remoteEndpoint, path, observationParameter);
//        log.info("Added {} as observer for service {}", remoteEndpoint, path);
//    }
//
//
////    private synchronized void stopObservation(InetSocketAddress remoteEndpoint, Token token){
////
////        String servicePath = this.observationsPerToken.remove(remoteEndpoint, token);
////
////        if(servicePath != null)
////            this.observationsPerPath.remove(remoteEndpoint, servicePath);
////    }
//
//    private synchronized void stopObservation(ChannelHandlerContext ctx, InetSocketAddress remoteEndpoint,
//                                              Token token){
//
//        String servicePath = this.observationsPerToken.remove(remoteEndpoint, token);
//
//        if(servicePath != null){
//            ObservationParameter observationParameter =
//                    this.observationsPerPath.remove(remoteEndpoint, servicePath);
//
//            if(observationParameter != null){
//                InternalStopUpdateNotificationRetransmissionMessage stopRetransmissionMessage =
//                        new InternalStopUpdateNotificationRetransmissionMessage(remoteEndpoint,
//                                observationParameter.getLatestMessageID());
//
//                ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), stopRetransmissionMessage, null));
//
//                this.observationsPerPath.remove(remoteEndpoint, servicePath);
//            }
//        }
//    }
//
//    private synchronized void stopObservation(InetSocketAddress remoteEndpoint, String webservicePath){
//
//        ObservationParameter observationParameter =
//                this.observationsPerPath.remove(remoteEndpoint, webservicePath);
//
//            if(observationParameter == null){
//                log.error("No observation parameters found (remote socket: {}, service: {})", remoteEndpoint, webservicePath);
//            }
//            else{
//                if(observationsPerToken.remove(remoteEndpoint, observationParameter.getToken()) != null){
//                    log.info("Removed {} as observer for service {}", remoteEndpoint, webservicePath);
//                }
//                else{
//                    log.error("Could not remove {} as observer for service {}", remoteEndpoint, webservicePath);
//                }
//            }
//    }
//
//    private synchronized void addObservationPerToken(InetSocketAddress remoteEndpoint, Token token,
//                                                     String webservicePath){
//
//        this.observationsPerToken.put(remoteEndpoint, token, webservicePath);
//        log.info("Received new observation request from {} for service {} with token {}",
//                new Object[]{remoteEndpoint, webservicePath, token});
//    }
//
//
//    private class UpdateNotificationSender implements Runnable{
//
//        private InetSocketAddress remoteEndpoint;
//        private CoapResponse updateNotification;
//        private int latestMessageID;
//        private Channel channel;
//
//        public UpdateNotificationSender(InetSocketAddress remoteEndpoint, CoapResponse updateNotification,
//                                        int latestMessageID, Channel channel){
//
//            this.remoteEndpoint = remoteEndpoint;
//            this.updateNotification = updateNotification;
//            this.latestMessageID = latestMessageID;
//            this.channel = channel;
//        }
//
//        @Override
//        public void run() {
//
//            log.info("Start transmission of update notification: {}", updateNotification);
//
//            //Stop potentially running retranmissions
//            InternalStopUpdateNotificationRetransmissionMessage stopRetransmissionMessage =
//                    new InternalStopUpdateNotificationRetransmissionMessage(remoteEndpoint, latestMessageID);
//
//            ChannelHandlerContext ctx1 = channel.getPipeline().getContext(WebserviceObservationHandler.class);
//            ctx1.sendUpstream(new UpstreamMessageEvent(channel, stopRetransmissionMessage, null));
//
//            //Send new update notification
//            ChannelHandlerContext ctx2 =
//                    channel.getPipeline().getContext(IncomingMessageReliabilityHandler.class);
//
//            ChannelFuture future = Channels.future(channel);
//            Channels.write(ctx2, future, updateNotification, remoteEndpoint);
//        }
//    }
//}
