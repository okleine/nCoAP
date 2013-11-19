package de.uniluebeck.itm.ncoap.communication.observe;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.application.TokenFactory;
import de.uniluebeck.itm.ncoap.application.server.webservice.ContentFormatNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingMessageReliabilityHandler;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 18.11.13
* Time: 16:41
* To change this template use File | Settings | File Templates.
*/
public class ObservableResourceHandler extends SimpleChannelHandler implements Observer {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //This table is to relate the first response on a observation request with the service path
    private HashBasedTable<InetSocketAddress, Long, String> observationsPerToken;

    //This table is to relate the observers socket address and the service path to a running observation
    private HashBasedTable<InetSocketAddress, String, ObservationParameter> observationsPerPath;

//    //This table relates observer socket addresses and the message ID used for the latest update notification
//    //to the path of the observed service
//    private HashBasedTable<InetSocketAddress, Integer, String> latestMessageIDs;

    private ScheduledExecutorService executorService;

    public ObservableResourceHandler(ScheduledExecutorService executorService){
        this.executorService = executorService;
        observationsPerPath = HashBasedTable.create();
        observationsPerToken = HashBasedTable.create();
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception {

        if(me.getMessage() instanceof CoapMessage){
            CoapMessage coapMessage = (CoapMessage) me.getMessage();
            InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();


            if(coapMessage.getMessageTypeName() == MessageType.Name.RST){
                int resetedMessageID = coapMessage.getMessageID();

                Map<String, ObservationParameter> observationsForRemoteHost =
                        observationsPerPath.row((InetSocketAddress) me.getRemoteAddress());

                for(String servicePath : observationsForRemoteHost.keySet()){
                    ObservationParameter observationParameter = observationsForRemoteHost.get(servicePath);

                    if(observationParameter.getLatestMessageID() == resetedMessageID){
                        stopObservation(ctx, remoteSocketAddress, servicePath);
                        break;
                    }
                }
            }

            else if(coapMessage instanceof CoapRequest){
                CoapRequest coapRequest = (CoapRequest) coapMessage;
                stopObservation(ctx, remoteSocketAddress, coapRequest.getUriPath());

                if(coapRequest.isObserveSet())
                    addObservationPerToken((InetSocketAddress) me.getRemoteAddress(), coapRequest.getToken(),
                            coapRequest.getUriPath());
            }
        }

        ctx.sendUpstream(me);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(me.getMessage() instanceof InternalObservableResourceRegistrationMessage){
            InternalObservableResourceRegistrationMessage message =
                    (InternalObservableResourceRegistrationMessage) me.getMessage();

            message.getWebservice().addObserver(this);
            return;
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse coapResponse = (CoapResponse) me.getMessage();

            InetSocketAddress remoteSocketAddress = (InetSocketAddress) me.getRemoteAddress();
            long token = coapResponse.getToken();
            int messageID = coapResponse.getMessageID();

            String webservicePath = this.observationsPerToken.get(remoteSocketAddress, token);

            //Start new observation (otherwise webservice path will be null)
            if(coapResponse.isUpdateNotification()){
                if(webservicePath != null){
                    if(!this.observationsPerPath.contains(remoteSocketAddress, webservicePath)){
                        //This is a new observation
                        log.debug("Start observation of service {} (observer: {}, token: {}, messageID: {})",
                                new Object[]{webservicePath, remoteSocketAddress, TokenFactory.toHexString(token),
                                             messageID
                                });

                        ObservationParameter observationParameter =
                                new ObservationParameter(coapResponse.getMessageID(), token,
                                        coapResponse.getContentFormat(), ctx.getChannel());

                        addObservation(remoteSocketAddress, webservicePath, observationParameter);
                    }

                    else{
                        log.debug("Update observation of service {} (observer: {}, token: {}, messageID: {})",
                                new Object[]{webservicePath, remoteSocketAddress, TokenFactory.toHexString(token),
                                             messageID
                                });

                        //Update the the latest message ID to the one set by the Outgoing Reliability Handler
                        updateLatestMessageID(remoteSocketAddress, token, messageID);
                    }
                }

                long observationSequenceNumber = coapResponse.getObservationSequenceNumber();
                coapResponse.removeOptions(Option.Name.OBSERVE);
                coapResponse.setObservationSequenceNumber(observationSequenceNumber + 1);
            }

            else{
                stopObservation(ctx, remoteSocketAddress, coapResponse.getToken());
            }




        }

        ctx.sendDownstream(me);
    }

    private synchronized void updateLatestMessageID(InetSocketAddress remoteSocketAddress, long token, int messageID){
        String servicePath = this.observationsPerToken.get(remoteSocketAddress, token);
        ObservationParameter observationParameter = this.observationsPerPath.get(remoteSocketAddress, servicePath);

        observationParameter.setLatestMessageID(messageID);
    }

    @Override
    public void update(Observable object, Object arg) {
        if(!(object instanceof ObservableWebservice))
            return;

        ObservableWebservice webservice = (ObservableWebservice) object;
        String webservicePath = webservice.getPath();

        Map<InetSocketAddress, ObservationParameter> applyingObservations = observationsPerPath.column(webservicePath);
        Map<Long, ChannelBuffer> formattedContent = new HashMap<>();

        for(InetSocketAddress remoteSocketAddress : applyingObservations.keySet()){
            ObservationParameter observationParameter = applyingObservations.get(remoteSocketAddress);
            observationParameter.nextUpdateNotification();

            long contentFormat = observationParameter.getContentFormat();

            try{
                ChannelBuffer serializedResourceStatus;
                if(formattedContent.containsKey(contentFormat)){
                    serializedResourceStatus = ChannelBuffers.copiedBuffer(formattedContent.get(contentFormat));
                }
                else{
                    serializedResourceStatus =
                            ChannelBuffers.wrappedBuffer(webservice.getSerializedResourceStatus(contentFormat));
                    formattedContent.put(contentFormat, ChannelBuffers.copiedBuffer(serializedResourceStatus));
                }

                CoapResponse updateNotification = new CoapResponse(MessageCode.Name.CONTENT_205);
                updateNotification.setMessageType(webservice.getMessageTypeForUpdateNotifications().getNumber());
                updateNotification.setToken(observationParameter.getToken());
                updateNotification.setObservationSequenceNumber(observationParameter.getNotificationCount());
                updateNotification.setContent(serializedResourceStatus, contentFormat);

                executorService.schedule(new UpdateNotificationSender(remoteSocketAddress, updateNotification,
                        observationParameter.getLatestMessageID(), observationParameter.getChannel()
                ), 0, TimeUnit.MILLISECONDS);
            }

            catch (ContentFormatNotSupportedException e) {
                handleContentFormatNotSupportedException(remoteSocketAddress, observationParameter, e);
                continue;
            }
            catch (InvalidHeaderException | InvalidMessageException | InvalidOptionException e) {
                log.error("This should never happen.", e);
                continue;
            }

        }
    }

    private void handleContentFormatNotSupportedException(InetSocketAddress remoteSocketAddress,
                ObservationParameter observationParameter, ContentFormatNotSupportedException e){
        try {
            CoapResponse updateNotification = new CoapResponse(MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415);
            String message = "Content Format No. " + e.getUnsupportedContentFormatsAsString();
            updateNotification.setContent(message.getBytes(CoapMessage.CHARSET));

//            executorService.schedule(new UpdateNotificationSender(remoteSocketAddress, updateNotification,
//                    observationIDs.get(remoteSocketAddress, observationParameter.getToken()),
//                    observationParameter.getChannel()
//            ), 0, TimeUnit.MILLISECONDS);
        }
        catch (InvalidHeaderException | InvalidMessageException e1) {
            log.error("This should never happen.", e1);
        }

    }

    private synchronized void addObservation(InetSocketAddress remoteSocketAddress, String path,
                                             ObservationParameter observationParameter){

        this.observationsPerPath.put(remoteSocketAddress, path, observationParameter);
        log.info("Added {} as observer for service {}", remoteSocketAddress, path);
    }


    private synchronized void stopObservation(ChannelHandlerContext ctx, InetSocketAddress remoteSocketAddress,
                                                 long token){

        String servicePath = this.observationsPerToken.remove(remoteSocketAddress, token);

        if(servicePath != null){
            ObservationParameter observationParameter =
                    this.observationsPerPath.remove(remoteSocketAddress, servicePath);

            if(observationParameter != null){
                InternalStopRetransmissionMessage stopRetransmissionMessage =
                        new InternalStopRetransmissionMessage(remoteSocketAddress,
                                observationParameter.getLatestMessageID());

                ctx.sendUpstream(new UpstreamMessageEvent(ctx.getChannel(), stopRetransmissionMessage, null));

                this.observationsPerPath.remove(remoteSocketAddress, servicePath);
            }
        }
    }

    private synchronized void stopObservation(ChannelHandlerContext ctx, InetSocketAddress remoteSocketAddress,
                                                 String path){

        if(this.observationsPerPath.remove(remoteSocketAddress, path) == null)
            log.error("Could not remove {} as observer for service {}", remoteSocketAddress, path);
        else
            log.info("Removed {} as observer for service {}", remoteSocketAddress, path);
    }

    private synchronized void addObservationPerToken(InetSocketAddress remoteSocketAddress, long token,
                                                     String webservicePath){

        this.observationsPerToken.put(remoteSocketAddress, token, webservicePath);
        log.info("Received new observation request from {} for service {} with token {}",
                new Object[]{remoteSocketAddress, webservicePath, TokenFactory.toHexString(token)});
    }



//    private synchronized String removeNotAnsweredObservationRequest(InetSocketAddress remoteSocketAddress, long token){
//        String result = this.observationsPerToken.remove(remoteSocketAddress, token);
//        if(result != null){
//            log.info("Received first update notification for observer {} for service {}", remoteSocketAddress, result);
//        }
//        else{
//            log.error("No not answered observe request found for token {} from {}", TokenFactory.toHexString(token),
//                    remoteSocketAddress);
//        }
//        return result;
//    }

//    private synchronized void updateObservationIDs(InetSocketAddress remoteSocketAddress, long token, int latestMessageID){
//        this.observationIDs.put(remoteSocketAddress, token, latestMessageID);
//    }
//
//    private synchronized void removeObservationIDs(InetSocketAddress remoteSocketAddress, long token){
//        Integer messageID = this.observationIDs.remove(remoteSocketAddress, token);
//
//        if(messageID != null)
//            log.debug("Removed observation IDs: {} (Remote Socket Address), {} (token), {} (messageID)",
//                    new Object[]{remoteSocketAddress, TokenFactory.toHexString(token), messageID});
//        else
//            log.warn("Could not remove observation ID for {} and token {}", remoteSocketAddress,
//                    TokenFactory.toHexString(token));
//    }


    private class UpdateNotificationSender implements Runnable{

        private InetSocketAddress remoteSocketAddress;
        private CoapResponse updateNotification;
        private int latestMessageID;
        private Channel channel;

        public UpdateNotificationSender(InetSocketAddress remoteSocketAddress, CoapResponse updateNotification,
                                        int latestMessageID, Channel channel){

            this.remoteSocketAddress = remoteSocketAddress;
            this.updateNotification = updateNotification;
            this.latestMessageID = latestMessageID;
            this.channel = channel;
        }

        @Override
        public void run() {

            //Stop potentially running retranmissions
            InternalStopRetransmissionMessage stopRetransmissionMessage =
                    new InternalStopRetransmissionMessage(remoteSocketAddress, latestMessageID);

            ChannelHandlerContext ctx1 = channel.getPipeline().getContext(ObservableResourceHandler.class);
            ctx1.sendUpstream(new UpstreamMessageEvent(channel, stopRetransmissionMessage, null));

            //Send new update notification
            ChannelHandlerContext ctx2 =
                    channel.getPipeline().getContext(IncomingMessageReliabilityHandler.class);

            ChannelFuture future = Channels.future(channel);
            Channels.write(ctx2, future, updateNotification, remoteSocketAddress);
        }
    }
}
