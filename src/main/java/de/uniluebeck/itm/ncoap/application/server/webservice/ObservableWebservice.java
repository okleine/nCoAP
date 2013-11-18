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
/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.UnknownOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Observable;
import java.util.concurrent.ScheduledExecutorService;


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

    private String path;
    private T resourceStatus;

    private int etagLength = Option.ETAG_LENGTH_DEFAULT;
    private byte[] etag;

    private boolean isUpdateNotificationConfirmable = true;

    private ScheduledExecutorService scheduledExecutorService;
    private ListeningExecutorService listeningExecutorService;
    private long maxAge = Option.MAX_AGE_DEFAULT;

    //private ScheduledFuture maxAgeFuture;

    protected ObservableWebservice(String path, T initialStatus){
        this.path = path;
        this.resourceStatus = initialStatus;
    }

    /**
     * This method is automatically invoked by the nCoAP framework when this service instance is registered at a
     * {@link CoapServerApplication} instance (using {@link CoapServerApplication#registerService(Webservice)}.
     * So, usually there is no need to set another {@link ScheduledExecutorService} instance manually.
     *
     * @param executorService a {@link ScheduledExecutorService} instance.
     */
    @Override
    public void setScheduledExecutorService(ScheduledExecutorService executorService){
        if(this.scheduledExecutorService != null)
            this.scheduledExecutorService.shutdownNow();

        this.scheduledExecutorService = executorService;
        this.listeningExecutorService = MoreExecutors.listeningDecorator(executorService);
        //scheduleMaxAgeNotifications();
    }


    @Override
    public ScheduledExecutorService getScheduledExecutorService(){
        return this.scheduledExecutorService;
    }


    @Override
    public ListeningExecutorService getListeningExecutorService() {
        return listeningExecutorService;
    }

    @Override
    public abstract void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                            InetSocketAddress remoteAddress);

    /**
     * Returns the payload to be contained in {@link CoapResponse}s on incoming {@link CoapRequest}s. This method
     * is invoked by the framework upon invocation of {@link #setResourceStatus(Object)}. The implementation
     * of this method is supposed to be fast, since it is invoked for every observer.
     *
     * @param contentFormat the number representing the format of the serialized resource status
     *
     * @return the serialized resource status
     */
    public abstract byte[] getSerializedResourceStatus(long contentFormat) throws ContentFormatNotSupportedException;


    /**
     * Returns whether update notifications should be sent with {@link MessageType.Name#CON} or
     * {@link MessageType.Name#NON}.
     *
     * @return {@link MessageType.Name#CON} if update notifications should be sent confirmable or
     * {@link MessageType.Name#NON} otherwise. Default, i.e. if not set otherwise, is {@link MessageType.Name#CON}.
     */
    public final int getMessageTypeForUpdateNotifications(){
        if(isUpdateNotificationConfirmable)
            return MessageType.Name.CON.getNumber();
        else
            return MessageType.Name.NON.getNumber();
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


    @Override
    public final String getPath() {
       return this.path;
    }

    @Override
    public final T getResourceStatus(){
        return this.resourceStatus;
    }

    @Override
    public synchronized final void setResourceStatus(T newStatus, long lifetimeSeconds){
        this.resourceStatus = newStatus;

//        try{
//            if(maxAgeFuture.cancel(false))
//                log.info("Max-age notification cancelled for {}.", getPath());
//        }
//        catch(NullPointerException ex){
//            log.info("Max-age notifiation for {} not yet scheduled. This should only happen once!", getPath());
//        }

        //Notify observers (methods inherited from abstract class Observable)
        setChanged();
        notifyObservers();

        //scheduleMaxAgeNotifications();

        try{
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            this.etag =
                    Arrays.copyOfRange(messageDigest.digest(newStatus.toString().getBytes(CoapMessage.CHARSET)), 0,
                            etagLength);
        }
        catch (NoSuchAlgorithmException e) {
            log.error("This should never happen.", e);
        }
    }

    @Override
    public void setEtagLength(int etagLength) throws IllegalArgumentException {
        try{
            if(etagLength > Option.getMaxLength(Option.Name.ETAG))
                throw new IllegalArgumentException("Maximum length for ETAG option is " +
                        Option.getMaxLength(Option.Name.ETAG));

            if(etagLength < Option.getMinLength(Option.Name.ETAG))
                throw new IllegalArgumentException("Minimum length for ETAG option is " +
                        Option.getMinLength(Option.Name.ETAG));

            this.etagLength = etagLength;
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    @Override
    public byte[] getEtag(){
        return this.etag;
    }

    /**
     * The max age value represents the validity period (in seconds) of the actual status. With
     * {@link ObservableWebservice} instances the nCoAP framework uses this value
     * <ul>
     *     <li>
     *          to set the {@link Option.Name#MAX_AGE} option in every {@link CoapResponse} that was produced by
     *          {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)}
     *          (if not set to another value before {@link SettableFuture<CoapResponse>#set(CoapResponse)}).
     *     </li>
     *     <li>
     *         to send update notifications to all observers of this service every {@link #maxAge} seconds.
     *     </li>
     * </ul>
     *
     * The default (if not set otherwise) is {@link Option#MAX_AGE_DEFAULT}
     *
     * @return the current value of {@link #maxAge}
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * The max age value represents the validity period (in seconds) of the actual status. With
     * {@link ObservableWebservice} instances the nCoAP framework uses this value
     * <ul>
     *     <li>
     *         to set the {@link Option.Name#MAX_AGE} option in every {@link CoapResponse} that was produced by
     *          {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)}
     *          (if not set to another value before {@link SettableFuture<CoapResponse>#set(CoapResponse)}).
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

//    private void scheduleMaxAgeNotifications(){
//        maxAgeFuture = scheduledExecutorService.schedule(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (ObservableWebservice.this){
//                    log.info("Send max-age notifications for {} with status {}.", getPath(), getResourceStatus());
//
//                    setChanged();
//                    notifyObservers();
//
//                    scheduleMaxAgeNotifications();
//                }
//            }
//        }, getMaxAge(), TimeUnit.SECONDS);
//    }

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
