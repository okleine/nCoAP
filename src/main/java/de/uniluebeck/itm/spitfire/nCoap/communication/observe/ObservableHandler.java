package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.spitfire.nCoap.application.Service;
import de.uniluebeck.itm.spitfire.nCoap.communication.callback.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapExecutorService;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalObserveOptionUpdate;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalServiceRemovedFromPath;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalServiceUpdate;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.MessageIDFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.ResetReceivedException;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutException;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This handler manages observable resources and observer.
 * Observers will be registered when receiving passing observable requests.
 * Observers will be removed when either a Reset-Message or a InternalErrorMessage is received.
 * In reaction to a received InternalServiceUpdate notifications will be send to all corresponding observers.
 * 
 * @author Stefan Hueske
 */
public class ObservableHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(ObservableHandler.class.getName());

    //each observer is represented by its initial observable request --> ObservableRequest object
    //both data structures hold the same set of observable requests
    //[Token, RemoteAddress] --> [Observer]
    HashBasedTable<byte[], SocketAddress, ObservableRequest> addressTokenMappedToObservableRequests 
            = HashBasedTable.create();
    //[UriPath] --> [List of Observers]
    Multimap<String, ObservableRequest> pathMappedToObservableRequests = LinkedListMultimap.create();
        
    //this cache maps the last used notification message id to an observer, which is needed to match a reset message
    //each cache entry has a lifetime of 2 minutes
    //[Notification message id] --> [Observer]
    Cache<Integer, ObservableRequest> responseMessageIdCache;
    
    Map<ObservableRequest, ScheduledFuture> scheduledMaxAgeNotifications = 
            new ConcurrentHashMap<ObservableRequest, ScheduledFuture>();

    private static ObservableHandler instance = new ObservableHandler();
    
    public static ObservableHandler getInstance() {
        return instance;
    }

    private ObservableHandler() {
        responseMessageIdCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();
    }
    
    @Override
    public void writeRequested(final ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof InternalServiceUpdate) {
            //a service has updated, notify all observers which observe the same uri path
            final Service service = ((InternalServiceUpdate) e.getMessage()).getService();
            for (String uriPath : service.getRegisteredPaths()) {
                for (final ObservableRequest observableRequest : pathMappedToObservableRequests.get(uriPath)) {
                    //remove scheduled Max-Age notification
                    ScheduledFuture future = scheduledMaxAgeNotifications.remove(observableRequest);
                    if (future != null) {
                        future.cancel(false);
                    }
                    CoapExecutorService.schedule(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                CoapResponse coapResponse = service.getStatus(observableRequest.getRequest());
                                setupResponse(coapResponse, observableRequest);
                                ChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), false);
                                ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, 
                                        coapResponse, observableRequest.getRemoteAddress()));
                                scheduleMaxAgeNotification(observableRequest, coapResponse, service, ctx);                                
                            } catch (Exception ex) {
                                log.error("Unexpected exception in scheduled notification! " + ex.getMessage());
                            }
                        }
                    }, 0, TimeUnit.SECONDS);
                    
                }
            }
            return;
        }
        if (e.getMessage() instanceof CoapResponse) {
            //CoapResponse received, check if it responds to a observable request
            CoapResponse coapResponse = (CoapResponse) e.getMessage();
            ObservableRequest observableRequest = addressTokenMappedToObservableRequests.get(coapResponse.getToken(), 
                    e.getRemoteAddress());
            if (observableRequest == null) {
                //remove observe option
                coapResponse.getOptionList().removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
            }
        }
        if (e.getMessage() instanceof InternalServiceRemovedFromPath) {
            String removedPath = ((InternalServiceRemovedFromPath)e.getMessage()).getServicePath();
            for (final ObservableRequest observer : pathMappedToObservableRequests.get(removedPath)) {
                CoapExecutorService.schedule(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            CoapResponse coapResponse = new CoapResponse(Code.NOT_FOUND_404);
                            coapResponse.getHeader().setMsgType(MsgType.CON);
                            coapResponse.setToken(observer.getRequest().getToken());
                            coapResponse.getOptionList().removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
                            ChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), false);
                            ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, 
                                    coapResponse, observer.getRemoteAddress()));
                        } catch (Exception ex) {
                            log.error("Unexpected exception in scheduled notification! " + ex.getMessage());
                        }
                    }
                }, 0, TimeUnit.SECONDS);
                addressTokenMappedToObservableRequests
                        .remove(observer.getRequest().getToken(), observer.getRemoteAddress());
            }
            pathMappedToObservableRequests.removeAll(removedPath);
            return;
        }
        ctx.sendDownstream(e);
    }

    private void scheduleMaxAgeNotification(final ObservableRequest observableRequest, 
            final CoapResponse currentCoapResponse, final Service service, final ChannelHandlerContext ctx) {
        //schedule Max-Age notification
        int maxAge = 60; //dafault Max-Age value
        if (!currentCoapResponse.getOption(OptionRegistry.OptionName.MAX_AGE).isEmpty()) {
            maxAge = ((UintOption)currentCoapResponse.getOption(OptionRegistry.OptionName.MAX_AGE)
                    .get(0)).getDecodedValue().intValue();
        }
        scheduledMaxAgeNotifications.put(observableRequest, 
                CoapExecutorService.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    CoapResponse futureCoapResponse = service.getStatus(observableRequest.getRequest());
                    setupResponse(futureCoapResponse, observableRequest);
                    ChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), false);
                    ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, 
                            futureCoapResponse, observableRequest.getRemoteAddress()));
                    scheduleMaxAgeNotification(observableRequest, futureCoapResponse, service, ctx);
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
    private void removeObservableRequest(ObservableRequest observableRequest) {
        if (observableRequest == null) {
            return;
        }
        addressTokenMappedToObservableRequests.remove(observableRequest.getRequest().getToken(), 
                observableRequest.getRemoteAddress());
        pathMappedToObservableRequests.remove(observableRequest.getRequest().getTargetUri().getPath(), 
                observableRequest);
        log.info(String.format("Observer for path %s from %s removed! ", 
                observableRequest.getRequest().getTargetUri().getPath(), observableRequest.getRemoteAddress()));
    }
    
    /**
     * Remove Observers.
     * @param observableRequests Observers to remove
     */
    private void removeObservableRequests(List<ObservableRequest> observableRequests) {
        for (ObservableRequest request : observableRequests) {
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
//            ObservableRequest observableRequest = addressTokenMappedToObservableRequests.get(conTimeoutMsg.getToken(), 
//                    e.getRemoteAddress());
//            removeObservableRequest(observableRequest);
//        }
        if (e.getMessage() instanceof CoapRequest) {
            CoapRequest coapRequest = (CoapRequest) e.getMessage();
            if (!coapRequest.getOption(OptionRegistry.OptionName.OBSERVE_REQUEST).isEmpty()) {
                //Observable request received, register new observer
                ObservableRequest observableRequest = new ObservableRequest(coapRequest, e.getRemoteAddress());
                addressTokenMappedToObservableRequests.put(coapRequest.getToken(), e.getRemoteAddress(), 
                        observableRequest);
                pathMappedToObservableRequests.put(coapRequest.getTargetUri().getPath(), observableRequest);
                log.info(String.format("Observer for path %s from %s registered! ", 
                observableRequest.getRequest().getTargetUri().getPath(), observableRequest.getRemoteAddress()));
            } else {
                //request without observe option received
                String requestPath = coapRequest.getTargetUri().getPath();
                List<ObservableRequest> observableRequestsToRemove = new LinkedList<ObservableRequest>();
                for (ObservableRequest observer : addressTokenMappedToObservableRequests
                        .column(e.getRemoteAddress()).values()) {
                    //iterate over all observers from e.getRemoteAddress()
                    if (requestPath.equals(observer.getRequest().getTargetUri().getPath())) {
                        //remove observer if new request targets same path
                        observableRequestsToRemove.add(observer);
                    }
                }
                removeObservableRequests(observableRequestsToRemove);
            }
        }
        if (e.getMessage() instanceof InternalObserveOptionUpdate) {
            InternalObserveOptionUpdate optionUpdate = (InternalObserveOptionUpdate) e.getMessage();
            ObservableRequest observer = addressTokenMappedToObservableRequests
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
    private void setupResponse(CoapResponse coapResponse, ObservableRequest observableRequest) 
            throws InvalidHeaderException, ToManyOptionsException, InvalidOptionException {
        
        int responseCount = observableRequest.updateResponseCount();
        if (responseCount == 1) {
            //first response
            coapResponse.setMessageID(observableRequest.getRequest().getMessageID());
            coapResponse.getHeader().setMsgType(MsgType.ACK);
        } else {
            //second or later
//            coapResponse.setMessageID(MessageIDFactory.nextMessageID());
            coapResponse.getHeader().setMsgType(MsgType.CON);
            responseMessageIdCache.put(coapResponse.getMessageID(), observableRequest);
        }
        coapResponse.setToken(observableRequest.getRequest().getToken());
        coapResponse.setObserveOptionResponse(responseCount);
    }
    
}
/**
 * Represents a observer.
 * Holds the original request, remote address and a notification counter.
 */
class ObservableRequest {
    private CoapRequest request;
    private SocketAddress remoteAddress;
    private int responseCount = 1;

    /**
     * Creates a new instance.
     * @param request observable request
     * @param remoteAddress observer remote address
     */
    public ObservableRequest(CoapRequest request, SocketAddress remoteAddress) {
        this.request = request;
        this.remoteAddress = remoteAddress;
    }
    
    public int updateResponseCount() {
        return responseCount++;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public CoapRequest getRequest() {
        return request;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setResponseCount(long observeOptionValue) {
        responseCount = (int)observeOptionValue;
    }
}