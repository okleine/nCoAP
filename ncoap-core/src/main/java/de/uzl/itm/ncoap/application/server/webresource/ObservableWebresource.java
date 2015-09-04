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

package de.uzl.itm.ncoap.application.server.webresource;

import com.google.common.collect.LinkedHashMultimap;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.EmptyLinkAttribute;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.OptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
* This is the abstract class to be extended by classes to represent an observable resource. The generic type T
* means, that the object that holds the status of the resource is of type T.
*
* Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
* (integer values). Then, your service class should extend {@link ObservableWebresource<Integer>}.
*
* @author Oliver Kleine, Stefan Hüske
*/
public abstract class ObservableWebresource<T> extends Observable implements Webresource<T> {

    private static Logger log = LoggerFactory.getLogger(ObservableWebresource.class.getName());

    public static final boolean SHUTDOWN = true;
    public static final boolean UPDATE = false;

    private WebresourceManager webresourceManager;
    private String uriPath;
    private LinkedHashMultimap<String, LinkAttribute> linkAttributes;

//    private HashBasedTable<InetSocketAddress, Token, Observation> observations;
//    private ReentrantReadWriteLock observationsLock;

    private T status;
    private long statusExpiryDate;
    private ReentrantReadWriteLock statusLock;

    private ScheduledExecutorService executor;
    //private Future notifyAllObserversFuture;


    /**
     * Using this constructor is the same as
     * {@link #ObservableWebresource(String, Object, long, java.util.concurrent.ScheduledExecutorService)} with
     * parameter <code>lifetimeSeconds</code> set to {@link OptionValue#MAX_AGE_DEFAULT}.
     *
     * @param uriPath the uriPath this {@link ObservableWebresource} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebresource}.
     */
    protected ObservableWebresource(String uriPath, T initialStatus, ScheduledExecutorService executor){
        this(uriPath, initialStatus, OptionValue.MAX_AGE_DEFAULT, executor);
        this.setLinkAttribute(new EmptyLinkAttribute(EmptyLinkAttribute.OBSERVABLE));
    }


    /**
     * @param uriPath the uriPath this {@link ObservableWebresource} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebresource}.
     * @param lifetime the number of seconds the initial status may be considered fresh, i.e. cachable by
     *                        proxies or clients.
     */
    protected ObservableWebresource(String uriPath, T initialStatus, long lifetime, ScheduledExecutorService executor){
        this.uriPath = uriPath;
        this.linkAttributes = LinkedHashMultimap.create();
        this.statusLock = new ReentrantReadWriteLock();
//        this.observations = HashBasedTable.create();
//        this.observationsLock = new ReentrantReadWriteLock();
        this.executor = executor;
        setResourceStatus(initialStatus, lifetime);
    }


//    /**
//     * Adds a new observer to observe this observable Webresource
//     * @param remoteEndpoint the remote endpoint (i.e. the observers socket)
//     * @param token the {@link de.uzl.itm.ncoap.communication.dispatching.client.Token} identifying this
//     *              observation
//     * @param contentFormat the number representing the format of the update notifications payload
//     */
//    public void addObservation(InetSocketAddress remoteEndpoint, Token token, long contentFormat, byte[] endpointID){
//        try{
//            this.observationsLock.writeLock().lock();
//            Observation observation = new Observation(remoteEndpoint, token, contentFormat, endpointID);
//            NotifySingleObserverTask heartbeatTask = new NotifySingleObserverTask(observation, true);
//            observation.setHeartbeatFuture(this.executor.schedule(heartbeatTask, 24, TimeUnit.HOURS));
//            this.observations.put(remoteEndpoint, token, observation);
//            log.info("Added new observation (remote endpoint: {}, token: {}, content format: {})",
//                    new Object[]{remoteEndpoint, token, contentFormat});
//        }
//        finally{
//            this.observationsLock.writeLock().unlock();
//        }
//    }
//
//
//    public boolean removeObservation(InetSocketAddress remoteEndpoint, Token token){
//        try{
//            this.observationsLock.writeLock().lock();
//            return this.observations.remove(remoteEndpoint, token) != null;
//        }
//        finally{
//            this.observationsLock.writeLock().unlock();
//        }
//    }

    @Override
    public void setLinkAttribute(LinkAttribute linkAttribute){
        if(this.linkAttributes.containsKey(linkAttribute.getKey())){

            if(!LinkAttribute.allowsMultipleValues(linkAttribute.getKey()))
                removeLinkAttribute(linkAttribute.getKey());
        }

        this.linkAttributes.put(linkAttribute.getKey(), linkAttribute);
    }


    @Override
    public boolean removeLinkAttribute(String attributeKey){
        return !this.linkAttributes.removeAll(attributeKey).isEmpty();
    }

    @Override
    public boolean hasLinkAttribute(LinkAttribute linkAttribute){
        return this.linkAttributes.get(linkAttribute.getKey()).contains(linkAttribute);
    }

    @Override
    public Collection<LinkAttribute> getLinkAttributes(){
        return this.linkAttributes.values();
    }


    @Override
    public void setWebresourceManager(WebresourceManager webresourceManager){
        this.webresourceManager = webresourceManager;
    }


    @Override
    public WebresourceManager getWebresourceManager(){
        return this.webresourceManager;
    }


    @Override
    public final String getUriPath() {
        return this.uriPath;
    }


    @Override
    public ScheduledExecutorService getExecutor(){
        return this.executor;
    }


    /**
     * {@inheritDoc}
     *
     * <p><b>Note: </b>Do not use this method for status retrieval when processing an inbound
     * {@link de.uzl.itm.ncoap.message.CoapRequest}. Use
     * {@link #getWrappedResourceStatus(java.util.Set)} instead to avoid synchronization issues. However,
     * this method is safely called within {@link #getWrappedResourceStatus(java.util.Set)}.</p>
     */
    @Override
    public final T getStatus(){
        return this.status;
    }


    @Override
    public synchronized final void setResourceStatus(final T status, final long lifetime){
        this.executor.submit(new Runnable(){

            @Override
            public void run() {
                try{
                    statusLock.writeLock().lock();

//                    if(ObservableWebresource.this.notifyAllObserversFuture != null){
//                        ObservableWebresource.this.notifyAllObserversFuture.cancel(true);
//                    }

                    ObservableWebresource.this.status = status;
                    ObservableWebresource.this.statusExpiryDate = System.currentTimeMillis() + (lifetime * 1000);
                    ObservableWebresource.this.updateEtag(status);

                    log.debug("New status of {} successfully set (expires in {} seconds).",
                            ObservableWebresource.this.getUriPath(), lifetime);

                    setChanged();
                    notifyObservers(UPDATE);

//                    ObservableWebresource.this.notifyAllObserversFuture =
//                            ObservableWebresource.this.executor.submit(new NotifyAllObserversTask());
                }
                catch(Exception ex){
                    log.error("Exception while setting new resource status for \"{}\"!",
                            ObservableWebresource.this.getUriPath(), ex);
                }

                finally {
                    statusLock.writeLock().unlock();
                }
            }
        });
    }


//    public void handleMessageTransferEvent(MessageTransferEvent event){
//        if(event instanceof MessageIDAssignedEvent){
//            this.handleMessageIDAssignedEvent(event.getRemoteEndpoint(), event.getToken(), event.getMessageID());
//        }
//
//        else if(event instanceof EmptyAckReceivedEvent){
//            this.handleEmptyAckReceived(event.getRemoteEndpoint(), event.getToken(), event.getMessageID());
//        }
//
//        else if(event instanceof ResetReceivedEvent){
//            this.handleResetReceived(event.getRemoteEndpoint(), event.getToken(), event.getMessageID());
//        }
//
//        else{
//            log.info("Unsupported event: {}", event);
//        }
//    }
//
//
//    private void handleMessageIDAssignedEvent(InetSocketAddress remoteEndpoint, Token token, int messageID){
//
//        try{
//            this.observationsLock.readLock().lock();
//            if(!this.observations.contains(remoteEndpoint, token)){
//                return;
//            }
//        }
//        finally {
//            this.observationsLock.readLock().unlock();
//        }
//
//        try{
//            this.observationsLock.writeLock().lock();
//            if(!this.observations.contains(remoteEndpoint, token)){
//                return;
//            }
//
//            Observation observation = this.observations.get(remoteEndpoint, token);
//            observation.setMessageID(messageID);
//            log.info("Observation of \"{}\" (remote endpoint: {}, token: {}) can now be canceled with RST and message " +
//                    "ID {}", new Object[]{this.uriPath, remoteEndpoint, token, messageID});
//        }
//        finally {
//            this.observationsLock.writeLock().unlock();
//        }
//    }
//
//
//    private void handleEmptyAckReceived(InetSocketAddress remoteEndpoint, Token token, int messageID){
//
//        try{
//            this.observationsLock.readLock().lock();
//            if(!this.observations.contains(remoteEndpoint, token)){
//                return;
//            }
//        }
//        finally {
//            this.observationsLock.readLock().unlock();
//        }
//
//        try{
//            this.observationsLock.writeLock().lock();
//            if(!this.observations.contains(remoteEndpoint, token)){
//                return;
//            }
//
//            Observation observation = this.observations.get(remoteEndpoint, token);
//            if(observation.getMessageID() == messageID){
//                observation.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
//            }
//        }
//        finally {
//            this.observationsLock.writeLock().unlock();
//        }
//    }
//
//
//    private void handleResetReceived(InetSocketAddress remoteEndpoint, Token token, int messageID){
//        try{
//            this.observationsLock.readLock().lock();
//            if(!this.observations.contains(remoteEndpoint, token)){
//                log.debug("No observation of \"{}\" found to be cancelled with RST (remote endpoint: {}, token: {})",
//                        new Object[]{this.uriPath, remoteEndpoint, token});
//                return;
//            }
//        }
//        finally {
//            this.observationsLock.readLock().unlock();
//        }
//
//        try{
//            this.observationsLock.writeLock().lock();
//            Observation observation = this.observations.get(remoteEndpoint, token);
//            if(observation == null){
//                log.debug("No observation of \"{}\" found to be cancelled with RST (remote endpoint: {}, token: {})",
//                        new Object[]{this.uriPath, remoteEndpoint, token});
//            }
//
//            else if(observation.getMessageID() == messageID){
//                this.observations.remove(remoteEndpoint, token);
//                log.info("Stopped observation of \"{}\" (remote endpoint: {}, token: {}) due to RST.",
//                        new Object[]{this.uriPath, remoteEndpoint, token});
//            }
//
//            else{
//                log.warn("Could not cancel observation (remote endpoint: {}, token: {}) with RST due to wrong message" +
//                        "ID (expected: {}, actual: {})", new Object[]{remoteEndpoint, token, observation.getMessageID(),
//                        messageID});
//            }
//        }
//        finally {
//            this.observationsLock.writeLock().unlock();
//        }
//    }

    /**
     * This method and {@link #getWrappedResourceStatus(java.util.Set)} are the only recommended way to retrieve
     * the actual resource status that is used for a {@link de.uzl.itm.ncoap.message.CoapResponse} to answer an inbound {@link de.uzl.itm.ncoap.message.CoapRequest}.
     *
     * Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link OptionValue.Name#ETAG}, resp.
     * {@link OptionValue.Name#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such inconsistencies could happen in case of a
     * resource update between calls of e.g. {@link #getSerializedResourceStatus(long)} and {@link #getEtag(long)},
     * resp. {@link #getMaxAge()}.
     *
     * However, concurrent invocations of this method are possible, as the resources read-lock can be locked multiple
     * times in parallel and {@link #setResourceStatus(Object, long)} waits for all read-locks to be released.
     *
     * @param contentFormat the number representing the desired content format of the serialized resource status
     *
     * @return a {@link WrappedResourceStatus} if the content format was supported or <code>null</code> if the
     * resource status could not be serialized to the desired content format.
     */
    public final WrappedResourceStatus getWrappedResourceStatus(long contentFormat){
        try{
            this.statusLock.readLock().lock();

            byte[] serializedResourceStatus = getSerializedResourceStatus(contentFormat);

            if(serializedResourceStatus == null)
                return null;

            else
                return new WrappedResourceStatus(serializedResourceStatus, contentFormat,
                        this.getEtag(contentFormat), this.getMaxAge());
        }
        finally {
            this.statusLock.readLock().unlock();
        }
    }


    /**
     * This method and {@link #getWrappedResourceStatus(long)} are the only recommended ways to retrieve
     * the actual resource status that is used for a {@link de.uzl.itm.ncoap.message.CoapResponse} to answer an inbound {@link de.uzl.itm.ncoap.message.CoapRequest}.
     *
     * Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link OptionValue.Name#ETAG}, resp.
     * {@link OptionValue.Name#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such inconsistencies could happen in case of a
     * resource update between calls of e.g. {@link #getSerializedResourceStatus(long)} and {@link #getEtag(long)},
     * resp. {@link #getMaxAge()}.
     *
     * However, concurrent invocations of this method are possible, as the resources read-lock can be locked multiple
     * times in parallel and {@link #setResourceStatus(Object, long)} waits for all read-locks to be released.
     *
     * <b>Note:</b> This method iterates over the given {@link Set} and tries to serialize the status in the order
     * given by the {@link java.util.Set#iterator()}. The first supported content format, i.e. where
     * {@link #getSerializedResourceStatus(long)} does return a value other than <code>null</code> is the content
     * format of the {@link WrappedResourceStatus} returned by this method.
     *
     * @param contentFormats A {@link Set} containing the numbers representing the accepted content formats
     *
     * @return a {@link WrappedResourceStatus} if any of the given content formats was supported or
     * <code>null</code> if the resource status could not be serialized to any accepted content format.
     */
    public final WrappedResourceStatus getWrappedResourceStatus(Set<Long> contentFormats){
        try{
            this.statusLock.readLock().lock();

            WrappedResourceStatus result = null;

            for(long contentFormat : contentFormats){
                result = getWrappedResourceStatus(contentFormat);

                if(result != null)
                    break;
            }

            return result;
        }
        finally {
            this.statusLock.readLock().unlock();
        }
    }


    /**
     * This method is invoked by the framework for every observer after every resource update. Classes that extend
     * {@link ObservableWebresource} may implement this method just by returning one of {@link de.uzl.itm.ncoap.message.MessageType.Name#CON}
     * or {@link de.uzl.itm.ncoap.message.MessageType.Name#NON}. However, this method also gives {@link ObservableWebresource}s the opportunity
     * to e.g. distinguish between observers or have some other arbitrary logic...
     *
     * @param remoteEndpoint the remote CoAP endpoints that observes this {@link ObservableWebresource}
     *
     * @return the message type for the next update notification for the observer identified by the given parameters
     */
    public abstract boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint);


    /**
     * Returns the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients. The returned number is calculated using the parameter <code>lifetimeSeconds</code> on
     * invocation of {@link #setResourceStatus(Object, long)} or
     * {@link #ObservableWebresource(String, Object, long, ScheduledExecutorService)}
     * (which internally invokes {@link #setResourceStatus(Object, long)}).
     *
     * If the number of seconds passed after the last invocation of {@link #setResourceStatus(Object, long)} is larger
     * than the number of seconds given as parameter <code>lifetimeSeconds</code>, this method returns zero.
     *
     * @return the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients.
     */
    protected final long getMaxAge(){
        return Math.max(this.statusExpiryDate - System.currentTimeMillis(), 0) / 1000;
    }


    /**
     * This method is called by the nCoAP framework within the unregistration process of this
     * {@link de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource} instance.
     *
     * Note: Do NOT INVOKE this method directly! Use
     * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication#
     *  shutdownWebresource(de.uzl.itm.ncoap.application.server.webresource.Webresource)
     * } or
     * {@link de.uzl.itm.ncoap.application.peer.CoapPeerApplication#
     *  shutdownWebresource(de.uzl.itm.ncoap.application.server.webresource.Webresource)
     * }
     * to shutdown a resource!
     */
    @Override
    public void shutdown(){
        log.warn("Shutdown service \"{}\"!", this.getUriPath());
        this.statusLock.writeLock().lock();
        setChanged();
        notifyObservers(SHUTDOWN);
//        try{
//            this.observationsLock.writeLock().lock();
//            String message = "Webservice \"" + this.getUriPath() + "\" no longer available!";
//
//            Channel channel = this.webresourceManager.getChannel();
//
//            for(Observation observation : this.observations.values()){
//                final InetSocketAddress remoteEndpoint = observation.getRemoteEndpoint();
//                final Token token = observation.getToken();
//
//                CoapResponse coapResponse = CoapResponse.createErrorResponse(MessageType.Name.NON,
//                        MessageCode.Name.NOT_FOUND_404, message);
//                coapResponse.setToken(token);
//
//                ChannelFuture future =  Channels.write(channel, coapResponse, remoteEndpoint);
//
//                future.addListener(new ChannelFutureListener() {
//                    @Override
//                    public void operationComplete(ChannelFuture future) throws Exception {
//                        if(future.isSuccess()){
//                            log.info("NOT FOUND notification sent (remote endpoint: {}, token: {})",
//                                    remoteEndpoint, token);
//                        }
//                        else{
//                             log.error("Could not sent NOT FOUND notification (remote endpoint: {}, token: {})",
//                                    new Object[]{remoteEndpoint, token, future.getCause()});
//                        }
//                    }
//                });
//
//                MessageExchangeFinishedEvent event = new MessageExchangeFinishedEvent(
//                        remoteEndpoint, -1, token, observation.getEndpointID2ForUpdateNotifications()
//                );
//                Channels.write(channel, event);
//            }
//        }
//        finally {
//            //Do NOT unlock the "write lock" to avoid new registrations!
//            log.warn("Keep WRITE LOCK for service \"{}\" to avoid new registrations for observation!", this.uriPath);
//
//        }
    }

//    private class NotifySingleObserverTask implements Runnable {
//
//        private Observation observation;
//        private WrappedResourceStatus wrappedStatus;
//        private MessageType.Name messageType;
//
//
//        private NotifySingleObserverTask(Observation observation, boolean confirmable) {
//            this(observation, getWrappedResourceStatus(observation.getContentFormat()));
//            this.messageType = confirmable ? MessageType.Name.CON : MessageType.Name.NON;
//        }
//
//
//        private NotifySingleObserverTask(Observation observation, WrappedResourceStatus wrappedStatus){
//            this.observation = observation;
//            this.wrappedStatus = wrappedStatus;
//            boolean confirmable = ObservableWebresource.this.isUpdateNotificationConfirmable(
//                    observation.getRemoteEndpoint(), observation.getToken());
//            this.messageType = confirmable ? MessageType.Name.CON : MessageType.Name.NON;
//        }
//
//
//        @Override
//        public void run(){
//            InetSocketAddress remoteEndpoint = this.observation.getRemoteEndpoint();
//            Token token = this.observation.getToken();
//            int messageID = this.observation.getMessageID();
//
//            try{
//                ObservableWebresource.this.observationsLock.writeLock().lock();
//
//                if(wrappedStatus == null){
//                    MessageCode.Name messageCode = MessageCode.Name.BAD_REQUEST_400;
//                    CoapResponse updateNotification = new CoapResponse(messageType, messageCode);
//
//                    updateNotification.setToken(token);
//                    updateNotification.setMessageID(messageID);
//
//                    String message = "Format (" + observation.getContentFormat() + ") is not anymore supported!";
//                    updateNotification.setContent(message.getBytes(CoapMessage.CHARSET),
//                            ContentFormat.TEXT_PLAIN_UTF8);
//
//                    Channels.write(getWebresourceManager().getChannel(), updateNotification, remoteEndpoint);
//
//                    removeObservation(remoteEndpoint, token);
//                }
//
//                else{
//                    MessageCode.Name messageCode = observation.getEtags().contains(wrappedStatus.getEtag()) ?
//                            MessageCode.Name.VALID_203 : MessageCode.Name.CONTENT_205;
//
//                    CoapResponse updateNotification = new CoapResponse(messageType, messageCode);
//
//                    updateNotification.setToken(token);
//                    updateNotification.setMessageID(messageID);
//                    updateNotification.setEtag(wrappedStatus.getEtag());
//
//                    if(messageCode == MessageCode.Name.CONTENT_205){
//                        updateNotification.setContent(wrappedStatus.getContent(), wrappedStatus.getContentFormat());
//                    }
//
//                    updateNotification.setObserve();
//
//                    Channels.write(getWebresourceManager().getChannel(), updateNotification, remoteEndpoint);
//
//                    if(messageType == MessageType.Name.CON){
//                        if(observation.getHeartbeatFuture().cancel(false)){
//                            log.info("Cancelled heartbeat notification (remote endpoint: {}, token: {})",
//                                    remoteEndpoint, token);
//                        }
//                        NotifySingleObserverTask heartbeat = new NotifySingleObserverTask(observation, true);
//                        ScheduledFuture heartbeatFuture = ObservableWebresource.this.executor.schedule(heartbeat,
//                                24, TimeUnit.HOURS);
//                        observation.setHeartbeatFuture(heartbeatFuture);
//                        log.info("Scheduled new heartbeat (remote endpoint: {}, token: {})", remoteEndpoint, token);
//                    }
//                }
//
//            }
//            catch(Exception ex){
//                MessageCode.Name messageCode = MessageCode.Name.INTERNAL_SERVER_ERROR_500;
//                CoapResponse updateNotification = CoapResponse.createErrorResponse(messageType, messageCode, ex);
//                log.error("Exception while processing notification task!", ex);
//                Channels.write(getWebresourceManager().getChannel(), updateNotification, remoteEndpoint);
//
//                removeObservation(remoteEndpoint, token);
//            }
//            finally{
//                ObservableWebresource.this.observationsLock.writeLock().unlock();
//            }
//        }
//    }
//
//
//    private class NotifyAllObserversTask implements Runnable{
//
//        @Override
//        public void run() {
//
//            List<NotifySingleObserverTask> notificationTasks = new ArrayList<>();
//
//            try{
//                ObservableWebresource.this.observationsLock.readLock().lock();
//
//                Map<Long, WrappedResourceStatus> serializedStates = new HashMap<>();
//
//                for (Observation observation : ObservableWebresource.this.observations.values()) {
//                    long contentFormat = observation.getContentFormat();
//
//                    if (!serializedStates.containsKey(contentFormat)) {
//                        serializedStates.put(contentFormat, getWrappedResourceStatus(contentFormat));
//                    }
//
//                    WrappedResourceStatus wrappedStatus = serializedStates.get(contentFormat);
//                    notificationTasks.add(new NotifySingleObserverTask(observation, wrappedStatus));
//                }
//            }
//            finally {
//                ObservableWebresource.this.observationsLock.readLock().unlock();
//            }
//
//            for(NotifySingleObserverTask notificationTask : notificationTasks){
//                notificationTask.run();
//            }
//        }
//    }
}
