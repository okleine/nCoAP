package de.uniluebeck.itm.spitfire.nCoap.application.server.webservice;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * This is the abstract class to be extended by classes to represent an observable resource.The generic type T
 * means, that the object that holds the resourceStatus of the resource is of type T.
 *
 * Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
 * (integer values). Then, your service class must extend {@link NotObservableWebService<Integer>}.
 *
 * @author Oliver Kleine, Stefan Hueske
 */
public abstract class ObservableWebService<T> extends Observable implements WebService<T> {

    private static Logger log = LoggerFactory.getLogger(ObservableWebService.class.getName());

    private String path;
    private T resourceStatus;
    private boolean isUpdateNotificationConfirmable = true;

    private ScheduledExecutorService executorService;
    private long maxAge = OptionRegistry.MAX_AGE_DEFAULT;

    private ScheduledFuture maxAgeFuture;

    protected ObservableWebService(String path, T initialStatus){
        this.path = path;
        this.resourceStatus = initialStatus;
    }

    /**
     * This method is automatically invoked by the nCoAP framework when this service instance is registered at a
     * {@link de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication} instance (using {@link de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication#registerService(WebService)}.
     * So, usually there is no need to set another {@link ScheduledExecutorService} instance manually.
     *
     * @param executorService a {@link ScheduledExecutorService} instance.
     */
    @Override
    public void setExecutorService(ScheduledExecutorService executorService){
        this.executorService = executorService;
    }


    @Override
    public ScheduledExecutorService getExecutorService(){
        return this.executorService;
    }

    @Override
    public abstract CoapResponse processMessage(CoapRequest coapRequest, InetSocketAddress remoteAddress);

    /**
     * Returns whether update notifications should be sent with {@link MsgType#CON} or {@link MsgType#NON}
     * @return {@link MsgType#CON} if update notifications should be sent confirmable or {@link MsgType#NON}
     * otherwise. Default, i.e. if not set otherwise, is {@link MsgType#CON}.
     */
    public final MsgType getMessageTypeForUpdateNotifications(){
        if(isUpdateNotificationConfirmable)
            return MsgType.CON;
        else
            return MsgType.NON;
    }

    /**
     * Sets the {@link MsgType} of update notifications
     * @param isConfirmable <code>true</code> if update notifications should be sent with {@link MsgType#CON} or
     *                      <code>false</code> for {@link MsgType#NON}. Default, i.e. if not set otherwise, is
     *                      {@link MsgType#CON}.
     */
    public final void setUpdateNotificationConfirmable(boolean isConfirmable){
        this.isUpdateNotificationConfirmable = isConfirmable;
    }

    @Override
    public final String getPath() {
       return this.path;
    }

    @Override
    public final T getResourceStatus(){
        return this.resourceStatus;
    }

    @Override
    public synchronized final void setResourceStatus(T newStatus){
        this.resourceStatus = newStatus;

        try{
            if(maxAgeFuture.cancel(false))
                log.info("Max-age notification cancelled for {}.", getPath());
        }
        catch(NullPointerException ex){
            log.info("Max-age notifiation for {} not yet scheduled. This should only happen once!", getPath());
        }

        //Notify observers (methods inherited from abstract class Observable)
        setChanged();
        notifyObservers();

        scheduleMaxAgeNotifications();
    }

    /**
     * The max age value represents the validity period (in seconds) of the actual status. With
     * {@link ObservableWebService} instances the nCoAP framework uses this value
     * <ul>
     *     <li>
     *          to set the {@link OptionName#MAX_AGE} option in every {@link CoapResponse} that was returned by
     *          {@link #processMessage(CoapRequest, InetSocketAddress)} (if not set set to another value manually) and
     *     </li>
     *     <li>
     *         to send update notifications to all observers of this service every {@link #maxAge} seconds.
     *     </li>
     * </ul>
     *
     * The default (if not set otherwise) is {@link OptionRegistry#MAX_AGE_DEFAULT}
     *
     * @return the current value of {@link #maxAge}
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * The max age value represents the validity period (in seconds) of the actual status. With
     * {@link ObservableWebService} instances the nCoAP framework uses this value
     * <ul>
     *     <li>
     *          to set the {@link OptionName#MAX_AGE} option in every {@link CoapResponse} that was returned by
     *          {@link #processMessage(CoapRequest, InetSocketAddress)} (if not set set to another value manually) and
     *     </li>
     *     <li>
     *         to send update notifications to all observers of this service every {@link #maxAge} seconds.
     *     </li>
     * </ul>
     *
     * @param maxAge  the new max age value
     */
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    private void scheduleMaxAgeNotifications(){
        maxAgeFuture = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (ObservableWebService.this){
                    log.info("Send max-age notifications for {} with status {}.", getPath(), getResourceStatus());

                    setChanged();
                    notifyObservers();

                    scheduleMaxAgeNotifications();
                }
            }
        }, getMaxAge(), TimeUnit.SECONDS);
    }

    /**
     * The hash code of is {@link ObservableWebService} instance is produced as {@code this.getPath().hashCode()}.
     * @return the hash code of this {@link ObservableWebService} instance
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

        if(!(object instanceof String || object instanceof WebService)){
            return false;
        }

        if(object instanceof String){
            return this.getPath().equals(object);
        }
        else{
            return this.getPath().equals(((WebService) object).getPath());
        }
    }
}
