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

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.server.WebserviceManager;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LinkAttribute;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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
    public void setExecutor(ScheduledExecutorService executorService);


    /**
     * Returns the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     *
     * @return the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     */
    public ScheduledExecutorService getExecutor();


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
     * Method to process an inbound {@link CoapRequest} asynchronously. The implementation of this method is dependant
     * on the concrete webservice. Processing a message might cause a new status of the resource or even the deletion
     * of the complete resource, i.e. this {@link Webservice} instance.
     *
     * <b>Note:</b>Implementing classes MUST make sure that the given {@link SettableFuture} is eventually set with
     * either a {@link CoapResponse} or an {@link Exception} or throw an {@link Exception}. Both, setting the
     * {@link SettableFuture} with an {@link Exception} or throw one make the framework to send {@link CoapResponse}
     * with {@link MessageCode.Name#INTERNAL_SERVER_ERROR_500} and {@link Exception#getMessage()} as content.
     *
     * @param responseFuture the {@link SettableFuture} instance to set the {@link CoapResponse} which is the result
     *                       of the inbound {@link CoapRequest}.
     *                       {@link SettableFuture<CoapResponse>#set(CoapResponse)} to send it to the client.
     * @param coapRequest The {@link CoapRequest} to be processed by the {@link Webservice} instance
     * @param remoteEndpoint The address of the sender of the request
     *
     * @throws Exception if an error occurred while processing the {@link CoapRequest}.
     */
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception;


    /**
     * Returns a byte array that contains the serialized payload for a {@link CoapResponse} in the desired content
     * format or <code>null</code> if the given content format is not supported.
     *
     * @param contentFormat the number indicating the desired format of the returned content, see
     *                      {@link ContentFormat} for some pre-defined constants.
     *
     * @return a byte array that contains the serialized payload for a {@link CoapResponse} in the desired content
     * format or <code>null</code> if the given content format is not supported.
     */
    public byte[] getSerializedResourceStatus(long contentFormat);

    /**
     * Sets the given {@link LinkAttribute} for this {@link Webservice} instance.
     *
     * <b>Note:</b> Implementing classes MUST ensure that attributes keys that do only allow a single
     * value are not set multiple times
     *
     * @param attribute the {@link LinkAttribute} to be set for this {@link Webservice} instance
     */
    public void setLinkAttribute(LinkAttribute attribute);

    /**
     * Removes all {@link LinkAttribute}s for the given key.
     *
     * @param attributeKey the attribute key to remove all attributes of
     *
     * @return <code>true</code> if there were any (previously set) attributes removed, <code>false</code>
     * otherwise
     */
    public boolean removeLinkAttribute(String attributeKey);

    /**
     * Returns <code>true</code> if this {@link Webservice} instance has the given {@link LinkAttribute} and
     * <code>false</code> otherwise
     *
     * @param linkAttribute the {@link LinkAttribute} to check
     *
     * @return code>true</code> if this {@link Webservice} instance has the given {@link LinkAttribute} and
     * <code>false</code> otherwise
     */
    public boolean hasLinkAttribute(LinkAttribute linkAttribute);

    /**
     * Returns a {@link Collection} containing all {@link LinkAttribute}s of this {@link Webservice} instance
     * @return a {@link Collection} containing all {@link LinkAttribute}s of this {@link Webservice} instance
     */
    public Collection<LinkAttribute> getLinkAttributes();

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


    @Override
    public int hashCode();

}
