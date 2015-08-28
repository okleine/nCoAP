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

import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute;
import de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

/**
* This is the interface to be implemented to realize a CoAP webresource. The generic type T means, that the object
* that holds the status of the resource is of type T.
*
* Example: Assume, you want to realize a service representing a temperature with limited accuracy (integer values).
* Then, your service class must implement Webservice<Integer>.
*/
public interface Webresource<T> {

    /**
     * Returns the (relative) path this service is registered at.
     *
     * @return the (relative) path this service is registered at.
     */
    public String getUriPath();


    /**
     * This method is invoked by the framework to set the {@link de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager} of the CoAP server this
     * {@link Webresource} instance is registered at.
     *
     * @param webresourceManager the {@link de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager} of the CoAP server this
     * {@link Webresource} instance is registered at.
     */
    public void setWebresourceManager(WebresourceManager webresourceManager);


    /**
     * Returns the {@link de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager} this
     * {@link Webresource} instance is registered at. This is
     * useful, e.g. to create and register or modify
     * {@link Webresource} instances upon reception of a
     * {@link de.uzl.itm.ncoap.message.CoapRequest} with
     * {@link de.uzl.itm.ncoap.message.MessageCode.Name#POST}.
     *
     * @return the {@link de.uzl.itm.ncoap.application.server.CoapServerApplication} this {@link Webresource} instance is registered at.
     */
    public WebresourceManager getWebresourceManager();


    /**
     * <p>Returns the object of type T that holds the actual status of the resource represented by this
     * {@link NotObservableWebresource}. Note, that this status is internal and just the base to create the
     * payload of a {@link de.uzl.itm.ncoap.message.CoapResponse}.</p>
     *
     * <p>Example: Assume this webresource represents a switch that has two states "on" and "off". The payload of the
     * previously mentioned {@link de.uzl.itm.ncoap.message.CoapResponse} could then be either "on" or "off". But since there are only
     * two possible states the generic <code>T</code> could be of type {@link java.lang.Boolean}.</p>
     *
     * @return the object of type T that holds the actual resourceStatus of the resource
     */
    public T getStatus();


    /**
     * Method to set the new status of the resource represented by this {@link Webresource}.
     *
     * Example: Assume this webresource represents a switch that has two states "on" and "off". The payload of the
     * previously mentioned {@link de.uzl.itm.ncoap.message.CoapResponse} could then be either "on" or "off". But since there are only
     * two possible states <code>T</code> could be of type {@link Boolean}.
     *
     * @param newStatus the object of type <code>T</code> representing the new status
     * @param lifetimeSeconds the number of seconds this status is valid, i.e. cachable by clients or proxies.
     */
    public void setResourceStatus(T newStatus, long lifetimeSeconds);


    /**
     * Returns the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     *
     * @return the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     */
    public ScheduledExecutorService getExecutor();


    /**
     * Returns the actual ETAG for the given content format (see {@link de.uzl.itm.ncoap.message.options.ContentFormat} for some pre-defined constants).
     *
     * @param contentFormat the number representing a content format (see {@link de.uzl.itm.ncoap.message.options.ContentFormat} for some pre-defined
     *                      constants).
     *
     * @return the actual ETAG for the given content format.
     */
    public byte[] getEtag(long contentFormat);


    /**
     * Sets the ETAG(s) of this {@link Webresource}. {@link Webresource}s can implement this method in several ways
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
     * <p>This method is called by the nCoAP framework when this
     * {@link Webresource} is removed from the
     * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication} instance. One could e.g. try to
     * cancel scheduled tasks if any. There might even be no need to do anything at all, i.e. implement the method with
     * empty body.</p>
     *
     * <p><b>Note: </b>If this {@link Webresource} uses the
     * {@link java.util.concurrent.ScheduledExecutorService} returned by
     * {@link de.uzl.itm.ncoap.application.server.CoapServerApplication#getExecutor()} one MUST NOT
     * terminate this {@link java.util.concurrent.ScheduledExecutorService} but only cancel scheduled tasks using there
     * {@link java.util.concurrent.ScheduledFuture}.</p>
     */
    public void shutdown();


    /**
     * <p>Method to process an inbound {@link de.uzl.itm.ncoap.message.CoapRequest} asynchronously. The
     * implementation of this method is dependant on the concrete webresource. Processing a message might cause a new
     * status of the resource or even the deletion of the complete resource, i.e. this
     * {@link Webresource} instance.</p>
     *
     * <p><b>Note:</b>Implementing classes MUST make sure that the given
     * {@link com.google.common.util.concurrent.SettableFuture} is eventually set with either a
     * {@link de.uzl.itm.ncoap.message.CoapResponse} or an {@link java.lang.Exception} or throw an
     * {@link java.lang.Exception}. Both, setting the {@link com.google.common.util.concurrent.SettableFuture} with an
     * {@link java.lang.Exception} or throw one make the framework to send
     * {@link de.uzl.itm.ncoap.message.CoapResponse} with
     * {@link de.uzl.itm.ncoap.message.MessageCode.Name#INTERNAL_SERVER_ERROR_500} and
     * {@link java.lang.Exception#getMessage()} as content.</p>
     *
     * @param responseFuture the {@link com.google.common.util.concurrent.SettableFuture} instance to be set with the
     *                       {@link de.uzl.itm.ncoap.message.CoapResponse} which is the result of the inbound
     *                       {@link de.uzl.itm.ncoap.message.CoapRequest}
     *
     * @param coapRequest The {@link de.uzl.itm.ncoap.message.CoapRequest} to be processed by the
     *                    {@link Webresource} instance
     * @param remoteEndpoint The {@link java.net.InetSocketAddress} of the sender of the request
     *
     * @throws Exception if an error occurred while processing the {@link de.uzl.itm.ncoap.message.CoapRequest}.
     */
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception;


    /**
     * Returns a byte array that contains the serialized payload for a
     * {@link de.uzl.itm.ncoap.message.CoapResponse} in the desired content format or <code>null</code> if the
     * given content format is not supported.
     *
     * @param contentFormat the number indicating the desired format of the returned content, see
     *                      {@link de.uzl.itm.ncoap.message.options.ContentFormat} for some pre-defined
     *                      constants.
     *
     * @return a byte array that contains the serialized payload for a
     * {@link de.uzl.itm.ncoap.message.CoapResponse} in the desired content format or <code>null</code> if the
     * given content format is not supported.
     */
    public byte[] getSerializedResourceStatus(long contentFormat);


    /**
     * <p>Sets the given {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute} for this
     * {@link Webresource} instance.</p>
     *
     * <p><b>Note:</b> Implementing classes MUST ensure that attributes keys that do only allow a single
     * value are not set multiple times</p>
     *
     * @param attribute the {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute} to
     *                  be set for this {@link Webresource}
     *                  instance
     */
    public void setLinkAttribute(LinkAttribute attribute);


    /**
     * Removes all {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute}s for the
     * given key.
     *
     * @param attributeKey the attribute key to remove all attributes of
     *
     * @return <code>true</code> if there were any (previously set) attributes removed, <code>false</code>
     * otherwise
     */
    public boolean removeLinkAttribute(String attributeKey);

    /**
     * Returns <code>true</code> if this {@link Webresource}
     * instance has the given {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute}
     * and <code>false</code> otherwise
     *
     * @param linkAttribute the {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute}
     *                      to check for existence
     *
     * @return code>true</code> if this {@link Webresource}
     * instance has the given {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute}
     * and <code>false</code> otherwise
     */
    public boolean hasLinkAttribute(LinkAttribute linkAttribute);

    /**
     * Returns a {@link java.util.Collection} containing all
     * {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute}s of this
     * {@link Webresource} instance
     *
     * @return a {@link java.util.Collection} containing all
     * {@link de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute}s of this
     * {@link Webresource} instance
     */
    public Collection<LinkAttribute> getLinkAttributes();

//    /**
//     * Implementing classes must provide this method such that it returns <code>true</code> if
//     * <ul>
//     *  <li>
//     *      the given object is a String that equals to the path of the URI representing the Webservice
//     *      instance, or
//*      </li>
//     *  <li>
//     *      the given object is a Webservice instance which path equals to this Webservice path.
//     *  </li>
//     * </ul>
//     * In all other cases the equals method must return <code>false</code>.
//     *
//     * @param object The object to compare this Webservice instance with
//     * @return <code>true</code> if the given object is a String representing the path of the URI of this Webservice or
//     * if the given object is a Webservice instance which path equals this Webservice path
//     */
//    public boolean equals(Object object);
//
//
//    @Override
//    public int hashCode();

}
