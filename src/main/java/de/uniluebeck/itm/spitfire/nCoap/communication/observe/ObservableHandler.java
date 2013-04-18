package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.spitfire.nCoap.application.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalObserveOptionUpdate;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalServiceRemovedFromPath;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.ObservableWebServiceUpdate;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.ResetReceivedException;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutException;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;



/**
 * This handler manages observable resources and observer.
 * Observers will be registered when receiving passing observable requests.
 * Observers will be removed when either a Reset-Message or a InternalErrorMessage is received.
 * In reaction to a received ObservableWebServiceUpdate notifications will be send to all corresponding observers.
 * 
 * @author Stefan Hueske
 */
public class ObservableHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(ObservableHandler.class.getName());

    //each observer is represented by its initial observable request --> CoapRequestForObservableResource object
    //both data structures hold the same set of observable requests
    //[Token, RemoteAddress] --> [Observer]
    private HashBasedTable<byte[], SocketAddress, CoapRequestForObservableResource> addressTokenMappedToObservableRequests
            = HashBasedTable.create();
    //[UriPath] --> [List of Observers]
    private Multimap<String, CoapRequestForObservableResource> pathMappedToObservableRequests = LinkedListMultimap.create();
        
    //this cache maps the last used notification message id to an observer, which is needed to match a reset message
    //each cache entry has a lifetime of 2 minutes
    //[Notification message id] --> [Observer]
    private Cache<Integer, CoapRequestForObservableResource> responseMessageIdCache;

    private ScheduledExecutorService executorService;
    
    private Map<CoapRequestForObservableResource, ScheduledFuture> scheduledMaxAgeNotifications =
            new ConcurrentHashMap<CoapRequestForObservableResource, ScheduledFuture>();

    public ObservableHandler(ScheduledExecutorService executorService) {
        responseMessageIdCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();
        this.executorService = executorService;
    }
    
    @Override
    public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

        if (e.getMessage() instanceof ObservableWebServiceUpdate) {
            //a service has updated, notify all observers which observe the same uri path
            final ObservableWebService service =
                    (ObservableWebService) ((ObservableWebServiceUpdate) e.getMessage()).getContent();

            log.debug("Updated observed service: " + service.getPath());

            String uriPath = service.getPath();

            //Send notifications to all registered observing clients
            for (final CoapRequestForObservableResource observableRequest : pathMappedToObservableRequests.get(uriPath)) {

                //remove scheduled Max-Age notification
                ScheduledFuture future = scheduledMaxAgeNotifications.remove(observableRequest);
                if (future != null) {
                    future.cancel(false);
                }

                //Send notification to observer
                executorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CoapResponse coapResponse =
                                    service.processMessage(observableRequest, observableRequest.getRemoteAddress());
                            setupResponse(coapResponse, observableRequest);

//                            ChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), false);
//                            ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future,
//                                               coapResponse, observableRequest.getRemoteAddress()));

                            log.debug("Try to send " + coapResponse + " to " + observableRequest.getRemoteAddress());
                            Channels.write(ctx.getChannel(), coapResponse, observableRequest.getRemoteAddress());

                            scheduleMaxAgeNotification(observableRequest, coapResponse, service, ctx);
                        } catch (Exception ex) {
                            log.error("Unexpected exception in scheduled notification! " + ex.getMessage());
                        }
                    }
                }, 0, TimeUnit.SECONDS);

            }
            return;
        }

        if (e.getMessage() instanceof CoapResponse) {
            //CoapResponse received, check if it responds to a observable request
            CoapResponse coapResponse = (CoapResponse) e.getMessage();
            CoapRequestForObservableResource observableRequest = addressTokenMappedToObservableRequests.get(coapResponse.getToken(),
                    e.getRemoteAddress());
            if (observableRequest == null) {
                //remove observe option
                coapResponse.getOptionList().removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
            }
        }
        if (e.getMessage() instanceof InternalServiceRemovedFromPath) {
            String removedPath = ((InternalServiceRemovedFromPath)e.getMessage()).getServicePath();
            for (final CoapRequestForObservableResource observableRequest : pathMappedToObservableRequests.get(removedPath)) {
                executorService.schedule(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            CoapResponse coapResponse = new CoapResponse(Code.NOT_FOUND_404);
                            coapResponse.getHeader().setMsgType(MsgType.CON);
                            coapResponse.setToken(observableRequest.getToken());
                            coapResponse.getOptionList().removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);

//                            ChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), false);
//                            ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future,
//                                    coapResponse, observableRequest.getRemoteAddress()));

                            log.debug("Try to send " + coapResponse + " to " + observableRequest.getRemoteAddress());
                            if(ctx.getChannel().isBound()){
                                Channels.write(ctx.getChannel(), coapResponse, observableRequest.getRemoteAddress());
                            }
                            else{
                                log.info("Could not write " + coapResponse + ". Channel is closed.");
                            }

                        } catch (Exception ex) {
                            log.error("Unexpected exception in scheduled notification! " + ex.getMessage());
                        }
                    }
                }, 0, TimeUnit.SECONDS);
                removeObservableRequest(observableRequest);
            }
            return;
        }
        ctx.sendDownstream(e);
    }

    private void scheduleMaxAgeNotification(final CoapRequestForObservableResource observableRequest,
            final CoapResponse currentCoapResponse, final ObservableWebService service,
            final ChannelHandlerContext ctx) {

        //schedule Max-Age notification
        int maxAge = OptionRegistry.MAX_AGE_DEFAULT; //dafault Max-Age value
        if (!currentCoapResponse.getOption(OptionRegistry.OptionName.MAX_AGE).isEmpty()) {
            maxAge = ((UintOption)currentCoapResponse.getOption(OptionRegistry.OptionName.MAX_AGE)
                    .get(0)).getDecodedValue().intValue();
        }
        scheduledMaxAgeNotifications.put(observableRequest, executorService.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    CoapResponse coapResponse =
                            service.processMessage(observableRequest, observableRequest.getRemoteAddress());

                    setupResponse(coapResponse, observableRequest);

//                    ChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), false);
//                    ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future,
//                            coapResponse, observableRequest.getRemoteAddress()));

                    log.debug("Try to send " + coapResponse + " to " + observableRequest.getRemoteAddress());
                    Channels.write(ctx.getChannel(), coapResponse, observableRequest.getRemoteAddress());

                    scheduleMaxAgeNotification(observableRequest, coapResponse, service, ctx);
                } catch (Exception ex) {
                    log.error("Unexpected exception in scheduled notification! " + ex.getMessage());
                }
            }
        }, maxAge, TimeUnit.SECONDS));
    }
    
    /**
     * Remove Observer.
     * @param observableRequest Observer to remove
     */
    private void removeObservableRequest(CoapRequestForObservableResource observableRequest) {
        if (observableRequest == null) {
            return;
        }
        addressTokenMappedToObservableRequests.remove(observableRequest.getToken(),
                observableRequest.getRemoteAddress());
        pathMappedToObservableRequests.remove(observableRequest.getTargetUri().getPath(),
                observableRequest);
        //cancel scheduled MaxAge Auto-Notification
        ScheduledFuture maxAgeNotificationFuture = scheduledMaxAgeNotifications.remove(observableRequest);
        if (maxAgeNotificationFuture != null) {
            maxAgeNotificationFuture.cancel(true);
        }
        log.info(String.format("Observer for path %s from %s removed! ", 
                observableRequest.getTargetUri().getPath(), observableRequest.getRemoteAddress()));
    }
    
    /**
     * Remove Observers.
     * @param observableRequests Observers to remove
     */
    private void removeObservableRequests(List<CoapRequestForObservableResource> observableRequests) {
        for (CoapRequestForObservableResource request : observableRequests) {
            removeObservableRequest(request);
        }
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//        if (e.getMessage() instanceof CoapMessage && ((CoapMessage)e.getMessage()).getMessageType() == MsgType.RST) {
//            //Reset message received, remove observer if it reponds to a previous notification
//            int rstMessageId = ((CoapMessage)e.getMessage()).getMessageID();
//            removeObservableRequest(responseMessageIdCache.getIfPresent(rstMessageId));
//        }
        //TODO fix InternalErrorMessage?
//        if (e.getMessage() instanceof InternalErrorMessage) {
//            //CON msg Timeout received, remove observer if the CON msg was a notification
//            InternalErrorMessage conTimeoutMsg = (InternalErrorMessage) e.getMessage();
//            CoapRequestForObservableResource observableRequest = addressTokenMappedToObservableRequests.get(conTimeoutMsg.getToken(),
//                    e.getRemoteAddress());
//            removeObservableRequest(observableRequest);
//        }
        if (e.getMessage() instanceof CoapRequest) {
            CoapRequest coapRequest = (CoapRequest) e.getMessage();
            if (!coapRequest.getOption(OptionRegistry.OptionName.OBSERVE_REQUEST).isEmpty()) {
                //Observable request received, register new observer
                CoapRequestForObservableResource observableRequest
                        = new CoapRequestForObservableResource(coapRequest, (InetSocketAddress) e.getRemoteAddress());
                addressTokenMappedToObservableRequests.put(coapRequest.getToken(), e.getRemoteAddress(), 
                        observableRequest);
                pathMappedToObservableRequests.put(coapRequest.getTargetUri().getPath(), observableRequest);
                log.info("%s is now observer for service %s.", observableRequest.getRemoteAddress(),
                        observableRequest.getTargetUri().getPath());
            } else {
                //request without observe option received
                String requestPath = coapRequest.getTargetUri().getPath();
                List<CoapRequestForObservableResource> observableRequestsToRemove = new LinkedList<CoapRequestForObservableResource>();
                for (CoapRequestForObservableResource observableRequest : addressTokenMappedToObservableRequests
                        .column(e.getRemoteAddress()).values()) {
                    //iterate over all observers from e.getRemoteAddress()
                    if (requestPath.equals(observableRequest.getTargetUri().getPath())) {
                        //remove observer if new request targets same path
                        observableRequestsToRemove.add(observableRequest);
                    }
                }
                removeObservableRequests(observableRequestsToRemove);
            }
        }
        if (e.getMessage() instanceof InternalObserveOptionUpdate) {
            InternalObserveOptionUpdate optionUpdate = (InternalObserveOptionUpdate) e.getMessage();
            CoapRequestForObservableResource observer = addressTokenMappedToObservableRequests
                    .get(optionUpdate.getToken(), optionUpdate.getRemoteAddress());
            if (observer != null) {
                observer.setResponseCount(optionUpdate.getObserveOptionValue() + 1);
            }
            
            return;
        }
        ctx.sendUpstream(e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Throwable cause = e.getCause();
        if(cause instanceof RetransmissionTimeoutException){
            RetransmissionTimeoutException ex = (RetransmissionTimeoutException) e.getCause();
            removeObservableRequest(addressTokenMappedToObservableRequests.get(ex.getToken(), ex.getRcptAddress()));
        } else if (cause instanceof ResetReceivedException) {
            ResetReceivedException ex = (ResetReceivedException) e.getCause();
            removeObservableRequest(addressTokenMappedToObservableRequests.get(ex.getToken(), ex.getRcptAddress()));
        }
        ctx.sendUpstream(e);
    }
    
    /**
     * Sets the msg id, msg type, token and observe response option.
     * 
     * @param coapResponse response to setup
     * @param observableRequest corresponding observer
     * @throws InvalidHeaderException
     * @throws ToManyOptionsException
     * @throws InvalidOptionException 
     */
    private void setupResponse(CoapResponse coapResponse, CoapRequestForObservableResource observableRequest)
            throws InvalidHeaderException, ToManyOptionsException, InvalidOptionException {
        
        int responseCount = observableRequest.updateResponseCount();
        if (responseCount == 1) {
            //first response
            coapResponse.setMessageID(observableRequest.getMessageID());
            coapResponse.getHeader().setMsgType(MsgType.ACK);
        } else {
            //second or later
            //coapResponse.setMessageID(MessageIDFactory.nextMessageID());
            coapResponse.getHeader().setMsgType(MsgType.CON);
            responseMessageIdCache.put(coapResponse.getMessageID(), observableRequest);
        }
        coapResponse.setToken(observableRequest.getToken());
        coapResponse.setObserveOptionResponse(responseCount);
    }
    
}