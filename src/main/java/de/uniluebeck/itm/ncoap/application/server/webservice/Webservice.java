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

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.WebserviceManager;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReadWriteLock;

/**
* This is the interface to be implemented to realize a CoAP webservice. The generic type T means, that the object
* that holds the status of the resource is of type T.
*
* Example: Assume, you want to realize a service representing a temperature with limited accuracy (integer values).
* Then, your service class must implement Webservice<Integer>.
*/
public interface Webservice<T> {

    /**
     * Returns the (relative) path this service is registered at.
     *
     * @return the (relative) path this service is registered at.
     */
    public String getPath();


    /**
     * Returns the {@link ReadWriteLock} for this {@link Webservice} instance.
     *
     * @return the {@link ReadWriteLock} for this {@link Webservice} instance.
     */
    public ReadWriteLock getReadWriteLock();


    /**
     * This method is invoked by the framework to set the {@link WebserviceManager} of the CoAP server this
     * {@link Webservice} instance is registered at.
     *
     * @param webserviceManager the {@link WebserviceManager} of the CoAP server this
     * {@link Webservice} instance is registered at.
     */
    public void setWebserviceManager(WebserviceManager webserviceManager);


    /**
     * Returns the {@link WebserviceManager} this {@link Webservice} instance is registered at. This is useful, e.g.
     * to create and register or modify {@link Webservice} instances upon reception of a {@link CoapRequest} with
     * {@link MessageCode.Name#POST}.
     *
     * @return the {@link CoapServerApplication} this {@link Webservice} instance is registered at.
     */
    public WebserviceManager getWebserviceManager();


    /**
     * Returns the object of type T that holds the actual status of the resource represented by this
     * {@link NotObservableWebservice}.
     *
     * Note, that this status is internal and thus independent from the payload of the {@link CoapResponse} to be
     * computed by the inherited method {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)}.
     *
     * Example: Assume this webservice represents a switch that has two states "on" and "off". The payload of the
     * previously mentioned {@link CoapResponse} could then be either "on" or "off". But since there are only
     * two possible states {@link T} could be of type {@link Boolean}.
     *
     * @return the object of type T that holds the actual resourceStatus of the resource
     */
    public T getResourceStatus();


    /**
     * Method to set the new status of the resource represented by this {@link Webservice}.
     *
     * Example: Assume this webservice represents a switch that has two states "on" and "off". The payload of the
     * previously mentioned {@link CoapResponse} could then be either "on" or "off". But since there are only
     * two possible states {@link T} could be of type {@link Boolean}.
     *
     * @param newStatus the object of type {@link T} representing the new status
     * @param lifetimeSeconds the number of seconds this status is valid, i.e. cachable by clients or proxies.
     */
    public void setResourceStatus(T newStatus, long lifetimeSeconds);


    /**
     * This method is automatically invoked by the nCoAP framework when this service instance is registered at a
     * {@link CoapServerApplication} instance (using {@link WebserviceManager#registerService(Webservice)}.
     * So, usually there is no need to set another {@link ScheduledExecutorService} instance manually.
     *
     * @param executorService a {@link ScheduledExecutorService} instance.
     */
    public void setScheduledExecutorService(ScheduledExecutorService executorService);


    /**
     * Returns the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     *
     * @return the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     */
    public ScheduledExecutorService getScheduledExecutorService();


    /**
     * Returns the actual ETAG for the given content format (see {@link ContentFormat} for some pre-defined constants).
     *
     * @param contentFormat the number representing a content format (see {@link ContentFormat} for some pre-defined
     *                      constants).
     *
     * @return the actual ETAG for the given content format.
     */
    public byte[] getEtag(long contentFormat);


    /**
     * Sets the ETAG(s) of this {@link Webservice}. {@link Webservice}s can implement this method in several ways
     * depending on the desired strength of the ETAG.
     *
     * <ul>
     *     <li>
     *         <b>Strong ETAG:</b> Different ETAGs for every supported content format, i.e. an ETAG depends on both,
     *         the resource status and the content format.
     *     </li>
     *     <li>
     *         <b>Weak ETAG:</b> The same ETAG for all supported content formats, i.e. the ETAG only depends on
     *         the resource status
     *     </li>
     * </ul>
     *
     * @param resourceStatus the (abstract) resource status to be used to compute the new ETAG(s).
     */
    public void updateEtag(T resourceStatus);


    /**
     * This method is called by the nCoAP framework when this {@link Webservice} is removed from the
     * {@link CoapServerApplication} instance. If any one could e.g. try to cancel scheduled tasks. There might even
     * be no need to do anything at all, i.e. implement the method with empty body.
     *
     * If this {@link Webservice} uses the default {@link ScheduledExecutorService} to execute tasks one MUST NOT
     * terminate this {@link ScheduledExecutorService} but only cancel scheduled tasks using there
     * {@link ScheduledFuture}.
     */
    public void shutdown();

    /**
     * Returns <code>true</code> if this {@link Webservice} can be removed from the server with a {@link CoapRequest}
     * with {@link MessageCode.Name#DELETE} and <code>false</code> otherwise. This method is invoked by the framework
     * if such a request is addressed to the path this {@link Webservice} is registered at.
     *
     * @return <code>true</code> if this {@link Webservice} can be removed from the server with a {@link CoapRequest}
     * with {@link MessageCode.Name#DELETE} and <code>false</code> otherwise.
     */
    public boolean allowsDelete();


    /**
     * Method to process an incoming {@link CoapRequest} asynchronously. The implementation of this method is dependant
     * on the concrete webservice. Processing a message might cause a new status of the resource or even the deletion
     * of the complete resource, i.e. this {@link Webservice} instance.
     *
     * Implementing classes MUST make sure that {@link SettableFuture<CoapResponse>#set(CoapResponse)} is invoked
     * after some time. Furthermore implementing classes are supposed to lock the {@link ReadWriteLock#readLock()} from
     * {@link #getReadWriteLock()} while processing incoming reading requests. Both, {@link ObservableWebservice} and
     * {@link NotObservableWebservice} implement the method {@link #setResourceStatus(Object, long)} in a way, that
     * they lock the {@link ReadWriteLock#writeLock()} for the whole update process. That means, resource status updates are
     * only possible, when the {@link ReadWriteLock#readLock()} is not locked. However, multiple read locks are
     * possible to process multiple reading requests in parallel.
     *
     * @param responseFuture the {@link SettableFuture} instance to set the {@link CoapResponse} which is the result
     *                       of the incoming {@link CoapRequest}. Use
     *                       {@link SettableFuture<CoapResponse>#set(CoapResponse)} to send it to the client.
     * @param request The {@link CoapRequest} to be processed by the {@link Webservice} instance
     * @param remoteAddress The address of the sender of the request
     */
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest request,
                                   InetSocketAddress remoteAddress);


    /**
     * Returns a byte array that contains the serialized payload for a {@link CoapResponse} in the desired content
     * format.
     *
     * @param contentFormatNumber the number indicating the desired format of the returned content, see
     *                            {@link ContentFormat} for some pre-defined constants.
     *
     * @return a byte array that contains the serialized payload for a {@link CoapResponse} in the desired content
     * format.
     */
    public byte[] getSerializedResourceStatus(long contentFormatNumber);


    /**
     * Implementing classes must provide this method such that it returns <code>true</code> if
     * <ul>
     *  <li>
     *      the given object is a String that equals to the path of the URI representing the Webservice
     *      instance, or
*      </li>
     *  <li>
     *      the given object is a Webservice instance which path equals to this Webservice path.
     *  </li>
     * </ul>
     * In all other cases the equals method must return <code>false</code>.
     *
     * @param object The object to compare this Webservice instance with
     * @return <code>true</code> if the given object is a String representing the path of the URI of this Webservice or
     * if the given object is a Webservice instance which path equals this Webservice path
     */
    public boolean equals(Object object);


    /**
     * This method must return a hash value for the Webservice instance based on the URI path of the webservice. Same
     * path must return the same hash value whereas different paths should have hash values as distinct as possible.
     */
    @Override
    public int hashCode();

}
