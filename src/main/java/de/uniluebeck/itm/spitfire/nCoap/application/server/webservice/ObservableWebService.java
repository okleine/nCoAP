package de.uniluebeck.itm.spitfire.nCoap.application.server.webservice;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Observable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;


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
    private ScheduledFuture maxAgeFuture;

    private int maxAge = OptionRegistry.MAX_AGE_DEFAULT;

    protected ObservableWebService(String path, T initialStatus){
        this.path = path;
        this.resourceStatus = initialStatus;
    }

    public final void setExecutorService(ScheduledExecutorService executorService){
        this.executorService = executorService;
        scheduleMaxAgeNotifications();
    }

    @Override
    public abstract CoapResponse processMessage(CoapRequest request, InetSocketAddress remoteAddress);

    /**
     * Returns whether update notifications should be sent with {@link MsgType#CON} or {@link MsgType#NON}
     * @return <code>true</code> if update notifications should be sent with {@link MsgType#CON} or
*              <code>false</code> for {@link MsgType#NON}. Default, i.e. if not set otherwise, is {@link MsgType#CON}.
     */
    public final boolean isUpdateNotificationConfirmable(){
        return this.isUpdateNotificationConfirmable;
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
    public final synchronized String getPath() {
       return this.path;
    }

    @Override
    public final T getResourceStatus(){
        return this.resourceStatus;
    }

    @Override
    public synchronized final void setResourceStatus(T newStatus){
        this.resourceStatus = newStatus;

        if(maxAgeFuture.cancel(false)){
            log.info("Update of {} before max age.", getPath());
        }

        //Notify observers (methods inherited from abstract class Observable)
        setChanged();
        notifyObservers();

        scheduleMaxAgeNotifications();
    }

    public void scheduleMaxAgeNotifications(){
        maxAgeFuture = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                log.info("Send max age notifications for {}.", getPath());

                synchronized (ObservableWebService.this){
                    setChanged();
                    notifyObservers();
                }

                scheduleMaxAgeNotifications();
            }
        }, getMaxAge(), TimeUnit.SECONDS);
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public int hashCode(){
        return this.getPath().hashCode();
    }

    @Override
    public boolean equals(Object object){

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
