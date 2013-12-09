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
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.UnknownOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

/**
* This is the abstract class to be extended by classes to represent a not observable resource.The generic type T
* means, that the object that holds the resourceStatus of the resource is of type T.
*
* Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
* (integer values). Then, your service class could e.g. extend {@link NotObservableWebservice <Integer>}.
*
* @author Oliver Kleine, Stefan Hüske
*/
public abstract class NotObservableWebservice<T> implements Webservice<T> {

    public static long SECONDS_PER_YEAR = 31556926;

    private static Logger log = LoggerFactory.getLogger(NotObservableWebservice.class.getName());

    private CoapServerApplication coapServerApplication;

    private String path;
    private T resourceStatus;
    private long resourceStatusExpiryDate;


    private int etagLength;
    private byte[] etag;

    private ScheduledExecutorService scheduledExecutorService;
    private ListeningExecutorService listeningExecutorService;


    protected NotObservableWebservice(String servicePath, T initialStatus, long lifetimeSeconds){
        this.path = servicePath;
        this.etagLength = Option.ETAG_LENGTH_DEFAULT;
        setResourceStatus(initialStatus, lifetimeSeconds);
    }

    public void setCoapServerApplication(CoapServerApplication serverApplication){
        this.coapServerApplication = serverApplication;
    }

    public CoapServerApplication getCoapServerApplication(){
        return this.coapServerApplication;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public final synchronized T getResourceStatus(){
        return this.resourceStatus;
    }

    @Override
    public final void setScheduledExecutorService(ScheduledExecutorService executorService){
        this.scheduledExecutorService = executorService;
        this.listeningExecutorService = MoreExecutors.listeningDecorator(executorService);
    }


    /**
     * Returns an instance of {@link ListeningExecutorService} to execute asynchronous webservice internal tasks.
     * Actually, the underlying {@link java.util.concurrent.ExecutorService} is the same instance as returned by
     * {@link #getScheduledExecutorService()} but decorated using {@link MoreExecutors#listeningDecorator}.
     *
     * @return an instance of {@link ListeningExecutorService} to execute asynchronous webservice internal tasks
     */
    public final ListeningExecutorService getListeningExecutorService() {
        return listeningExecutorService;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService(){
        return this.scheduledExecutorService;
    }

    @Override
    public final synchronized long getMaxAge() {
        return Math.max((this.resourceStatusExpiryDate - System.currentTimeMillis()) / 1000, 0);
    }

//    /**
//     * The max-age value represents the validity period (in seconds) of the actual status. The nCoap framework uses this
//     * value as default value for the  {@link Option.Name#MAX_AGE} option for outgoing
//     * {@link CoapResponse}s, if there was no such option set manually.
//     *
//     * @param maxAge the new max age value
//     */
//    public void setMaxAge(int maxAge) {
//        this.maxAge = maxAge;
//    }


    @Override
    public final synchronized void setResourceStatus(T newStatus, long lifetimeSeconds){
        this.resourceStatus = newStatus;
        this.resourceStatusExpiryDate = System.currentTimeMillis() + (lifetimeSeconds * 1000);

        log.debug("New resource status: {}, expires in {} seconds", newStatus.toString(), lifetimeSeconds);
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
    public synchronized byte[] getEtag(){
        return this.etag;
    }

    @Override
    public int hashCode(){
        return this.getPath().hashCode();
    }

    @Override
    public boolean equals(Object object){
        if(object == null)
            return false;

        if(!(object instanceof String || object instanceof Webservice))
            return false;

        if(object instanceof String)
            return (this.getPath().equals(object));

        return (this.getPath().equals(((Webservice) object).getPath()));
    }
}
