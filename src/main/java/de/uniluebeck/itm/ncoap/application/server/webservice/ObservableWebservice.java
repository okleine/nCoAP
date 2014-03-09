package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.annotations.Beta;
import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.application.server.WebserviceManager;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
* This is the abstract class to be extended by classes to represent an observable resource. The generic type T
* means, that the object that holds the resourceStatus of the resource is of type T.
*
* Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
* (integer values). Then, your service class should extend {@link NotObservableWebservice <Integer>}.
*
* @author Oliver Kleine, Stefan Hüske
*/
@Beta
public abstract class ObservableWebservice<T> extends Observable implements Webservice<T> {

    private static final long NO_RUNNING_OBSERVATION = -1;

    private static Logger log = LoggerFactory.getLogger(ObservableWebservice.class.getName());

    private WebserviceManager webserviceManager;
    private String path;

    private ReadWriteLock readWriteLock;

    private T resourceStatus;
    private long resourceStatusExpiryDate;

    private boolean isUpdateNotificationConfirmable = true;

    private HashBasedTable<InetSocketAddress, Token, Long> observations;

    private ScheduledExecutorService scheduledExecutorService;


    /**
     * Using this constructor is the same as {@link #ObservableWebservice(String, Object, long)} with parameter
     * <code>lifetimeSeconds</code> to {@link OptionValue#MAX_AGE_DEFAULT}.
     *
     * @param path the path this {@link ObservableWebservice} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebservice}.
     */
    protected ObservableWebservice(String path, T initialStatus){
        this(path, initialStatus, OptionValue.MAX_AGE_DEFAULT);
    }


    /**
     * @param path the path this {@link ObservableWebservice} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebservice}.
     * @param lifetimeSeconds the number of seconds the initial status may be considered fresh, i.e. cachable by
     *                        proxies or clients.
     */
    protected ObservableWebservice(String path, T initialStatus, long lifetimeSeconds){
        this.readWriteLock = new ReentrantReadWriteLock(false);
        this.path = path;
        this.observations = HashBasedTable.create();

        setResourceStatus(initialStatus, lifetimeSeconds);
    }


//    /**
//     * This method is called by the framework to pre-process incoming {@link CoapRequest}s, i.e. implementing classes
//     * should process any tasks that are not directly related to the creation of a {@link CoapResponse} here.
//     *
//     * Furthermore implementing classes are supposed to invoke
//     * {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} as the framework will not call
//     * that method at all.
//     *
//     * @param coapRequest The {@link CoapRequest} to be processed by the {@link Webservice} instance
//     * @param remoteSocketAddress The address of the sender of the request
//     */
//    public final void preprocessCoapRequest(final SettableFuture<CoapResponse> responseFuture,
//                                            final CoapRequest coapRequest, final InetSocketAddress remoteSocketAddress)
//            throws Exception{
//
//
//        if(observations.contains(remoteSocketAddress, coapRequest.getToken()))
//            removeObserver(remoteSocketAddress, coapRequest.getToken());
//
//        if(coapRequest.isObserveSet())
//            addObserver(remoteSocketAddress, coapRequest.getToken());
//
//
//        final SettableFuture<CoapResponse> responseFuture2 = SettableFuture.create();
//
//        responseFuture2.addListener(new Runnable(){
//
//            @Override
//            public void run() {
//                try {
//                    CoapResponse coapResponse = responseFuture2.get();
//
//                    if(coapRequest.isObserveSet()){
//                        //Stop the observation if the request processing caused an error message
//                        if(MessageCode.isErrorMessage(coapResponse.getMessageCode())){
//                            removeObserver(remoteSocketAddress, coapRequest.getToken());
//                        }
//
//                        //Set the proper observation sequence number
//                        else{
//                            long observationSequenceNumber =
//                                    getNextObservationSequenceNumber(remoteSocketAddress, coapRequest.getToken());
//                            coapResponse.setObservationSequenceNumber(observationSequenceNumber);
//                        }
//                    }
//
//                    responseFuture.set(coapResponse);
//
//                }
//                catch (Exception e) {
//                    log.error("This should never happen.", e);
//
//                    //Stop the observation if the request processing caused an error message
//                    if(coapRequest.isObserveSet())
//                        removeObserver(remoteSocketAddress, coapRequest.getToken());
//
//                    responseFuture.setException(e);
//                }
//            }
//        }, getScheduledExecutorService());
//
//        processCoapRequest(responseFuture2, coapRequest, remoteSocketAddress);
//    }


    private synchronized void removeObserver(InetSocketAddress remoteSocketAddress, Token token){
        if(observations.remove(remoteSocketAddress, token) != null){
            log.info("Removed {} with token {} as observer for service {}",
                    new Object[]{remoteSocketAddress, token, getPath()});
        }
    }


//    private synchronized void addObserver(InetSocketAddress remoteSocketAddress, Token token){
//        observations.put(remoteSocketAddress, token, 1L);
//        log.info("Added {} with tokeb {} as observer for service {}",
//                new Object[]{remoteSocketAddress, token, getPath()});
//    }

    private synchronized long getNextObservationSequenceNumber(InetSocketAddress remoteSocketAddress, Token token){

        Long actualSequenceNumber = observations.get(remoteSocketAddress, token);

        if(actualSequenceNumber != null){
            actualSequenceNumber += (OutgoingMessageReliabilityHandler.MAX_RETRANSMISSIONS + 1);
            observations.put(remoteSocketAddress, token, actualSequenceNumber);

            return actualSequenceNumber;
        }

        return NO_RUNNING_OBSERVATION;
    }

//    public final long getNextUpdateNotificationCount(InetSocketAddress remoteAddress, Token token){
//        return observations.get(remoteAddress, token);
//    }

    @Override
    public void setWebserviceManager(WebserviceManager webserviceManager){
        this.webserviceManager = webserviceManager;
    }


    @Override
    public WebserviceManager getWebserviceManager(){
        return this.webserviceManager;
    }


    @Override
    public final String getPath() {
        return this.path;
    }


    @Override
    public void setScheduledExecutorService(ScheduledExecutorService executorService){
        this.scheduledExecutorService = executorService;
    }


    @Override
    public ScheduledExecutorService getScheduledExecutorService(){
        return this.scheduledExecutorService;
    }


    @Override
    public final ReadWriteLock getReadWriteLock(){
        return this.readWriteLock;
    }


    @Override
    public final T getResourceStatus(){
        return this.resourceStatus;
    }


    @Override
    public synchronized final void setResourceStatus(T resourceStatus, long lifetimeSeconds){
        try{
            readWriteLock.writeLock().lock();
            this.resourceStatus = resourceStatus;
            this.resourceStatusExpiryDate = System.currentTimeMillis() + (lifetimeSeconds * 1000);

            this.updateEtag(resourceStatus);

            log.debug("New status of {} successfully set (expires in {} seconds).", this.path, lifetimeSeconds);

            //Notify observers (methods inherited from abstract class Observable)
            setChanged();
            notifyObservers(false);
        }

        finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void prepareShutdown(){
        setChanged();
        notifyObservers(true);
    }

//    private void notifyObservers(){
//
//    }

//    /**
//     * Returns the payload to be contained in {@link CoapResponse}s on incoming {@link CoapRequest}s. This method
//     * is invoked by the framework upon invocation of {@link #setResourceStatus(T, long)}. The implementation
//     * of this method is supposed to be fast, since it is invoked for every observer.
//     *
//     * @param contentFormat the number representing the format of the serialized resource status
//     *
//     * @return the serialized resource status
//     */
//    @Override
//    public abstract byte[] getSerializedResourceStatus(long contentFormat);


    /**
     * Returns the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients. The returned number is calculated using the parameter <code>lifetimeSeconds</code> on
     * invocation of {@link #setResourceStatus(Object, long)} or {@link #ObservableWebservice(String, Object, long)}
     * (which internally invokes {@link #setResourceStatus(Object, long)}).
     *
     * If the number of seconds passed after the last invocation of {@link #setResourceStatus(Object, long)} is larger
     * than the number of seconds given as parameter <code>lifetimeSeconds</code>, this method returns zero.
     *
     * @return the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients.
     */
    protected final long getMaxAge(){
        return Math.max(this.resourceStatusExpiryDate - System.currentTimeMillis(), 0) / 1000;
    }


    /**
     * Returns whether update notifications should be sent with {@link MessageType.Name#CON} or
     * {@link MessageType.Name#NON}.
     *
     * @return {@link MessageType.Name#CON} if update notifications should be sent confirmable or
     * {@link MessageType.Name#NON} otherwise. Default, i.e. if not set otherwise, is {@link MessageType.Name#CON}.
     */
    public final MessageType.Name getMessageTypeForUpdateNotifications(){
        if(isUpdateNotificationConfirmable)
            return MessageType.Name.CON;
        else
            return MessageType.Name.NON;
    }


    /**
     * Sets the {@link de.uniluebeck.itm.ncoap.message.MessageType} of update notifications.
     *
     * @param isConfirmable <code>true</code> if update notifications should be sent with {@link MessageType.Name#CON}
     *                      or <code>false</code> for {@link MessageType.Name#NON}. Default, i.e. if not set otherwise,
     *                      is {@link MessageType.Name#CON}.
     */
    public final void setUpdateNotificationConfirmable(boolean isConfirmable){
        this.isUpdateNotificationConfirmable = isConfirmable;
    }


    /**
     * The hash code of is {@link ObservableWebservice} instance is produced as {@code this.getPath().hashCode()}.
     * @return the hash code of this {@link ObservableWebservice} instance
     */
    @Override
    public int hashCode(){
        return this.getPath().hashCode();
    }

    @Override
    public final boolean equals(Object object){

        if(object == null){
            return false;
        }

        if(!(object instanceof String || object instanceof Webservice)){
            return false;
        }

        if(object instanceof String){
            return this.getPath().equals(object);
        }
        else{
            return this.getPath().equals(((Webservice) object).getPath());
        }
    }
}
