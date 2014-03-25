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
package de.uniluebeck.itm.ncoap.application.server.webservice;

import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.application.server.WebserviceManager;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
public abstract class ObservableWebservice<T> extends Observable implements Webservice<T> {

    private static Logger log = LoggerFactory.getLogger(ObservableWebservice.class.getName());

    public static final int CONFIRMABLE_UPDATE_NOTIFICATION_INTERVALL_SECONDS = 86400;

    private WebserviceManager webserviceManager;
    private String path;

    private ReadWriteLock readWriteLock;

    private T resourceStatus;
    private long resourceStatusExpiryDate;

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

        setResourceStatus(initialStatus, lifetimeSeconds);
    }


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
        executorService.scheduleAtFixedRate(new Runnable(){

            @Override
            public void run() {
                try{
                    ObservableWebservice.this.setChanged();
                    ObservableWebservice.this.notifyObservers(MessageType.Name.CON);
                }
                catch(Exception ex){
                    log.error("Exception in webservice {} while sending periodical confirmable update notifications!",
                            ObservableWebservice.this.getPath());

                    log.error("Exception while sending periodical confirmable update notifications!", ex);
                }
            }

        }, CONFIRMABLE_UPDATE_NOTIFICATION_INTERVALL_SECONDS, CONFIRMABLE_UPDATE_NOTIFICATION_INTERVALL_SECONDS,
                TimeUnit.SECONDS);
    }


    @Override
    public ScheduledExecutorService getScheduledExecutorService(){
        return this.scheduledExecutorService;
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


    /**
     * This method is the one and only recommended way to retrieve the actual resource status that is used
     * for a {@link CoapResponse} to answer an incoming {@link CoapRequest}.
     *
     * Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link OptionValue.Name#ETAG}, resp.
     * {@link OptionValue.Name#MAX_AGE} in a {@link CoapResponse}. Such inconsistencies could happen in case of a
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
    public WrappedResourceStatus getWrappedResourceStatus(long contentFormat){
        try{
            this.readWriteLock.readLock().lock();

            byte[] serializedResourceStatus = getSerializedResourceStatus(contentFormat);

            if(serializedResourceStatus == null)
                return null;

            else
                return new WrappedResourceStatus(serializedResourceStatus, contentFormat,
                        this.getEtag(contentFormat), this.getMaxAge());
        }
        finally {
            this.readWriteLock.readLock().unlock();
        }
    }


    /**
     * This method is invoked by the framework for every observer after every resource update. Classes that extend
     * {@link ObservableWebservice} may implement this method just by returning one of {@link MessageType.Name#CON}
     * or {@link MessageType.Name#NON}. However, this method also gives {@link ObservableWebservice}s the opportunity
     * to e.g. distinguish between observers or have some other arbitrary logic...
     *
     * @param remoteEndpoint the remote CoAP endpoint that observes this {@link ObservableWebservice}
     * @param token the {@link Token} that (in combination with the remote endpoint address) uniquely identifies a
     *              running observation
     *
     * @return the message type for the next update notification for the observer identified by the given parameters
     */
    public abstract MessageType.Name getMessageTypeForUpdateNotification(InetSocketAddress remoteEndpoint, Token token);


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
