/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.application.server.resource;

import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.communication.dispatching.server.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* This is the abstract class to be extended by classes to represent a not observable resource.The generic type T
* means, that the object that holds the resourceStatus of the resource is of type T.
*
* Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
* (integer values). Then, your service class could e.g. extend {@link NotObservableWebresource <Integer>}.
*
* @author Oliver Kleine, Stefan Hüske
*/
public abstract class NotObservableWebresource<T> implements Webresource<T> {

    private static Logger LOG = LoggerFactory.getLogger(NotObservableWebresource.class.getName());

    private RequestDispatcher requestDispatcher;

    private String path;

    private LinkedHashMap<String, LinkParam> linkParams;

    private ReadWriteLock readWriteLock;

    private T resourceStatus;
    private long resourceStatusExpiryDate;

    private ScheduledExecutorService executor;

    protected NotObservableWebresource(String servicePath, T initialStatus, long lifetimeSeconds,
                                       ScheduledExecutorService executor) {
        this.path = servicePath;
        this.linkParams = new LinkedHashMap<>();

        this.readWriteLock = new ReentrantReadWriteLock(false);
        this.executor = executor;
        setResourceStatus(initialStatus, lifetimeSeconds);
    }


    @Override
    public void setLinkParam(LinkParam linkParam) {
        if (this.linkParams.containsKey(linkParam.getKeyName())) {
            removeLinkParams(linkParam.getKey());
        }

        this.linkParams.put(linkParam.getKeyName(), linkParam);
    }

    @Override
    public boolean removeLinkParams(LinkParam.Key key) {
        this.linkParams.remove(key.getKeyName());
        return (this.linkParams.get(key.getKeyName()) == null);
    }

    @Override
    public boolean hasLinkAttribute(LinkParam.Key key, String value) {
        LinkParam linkParam = this.linkParams.get(key.getKeyName());
        if(linkParam == null) {
            return false;
        } else if (linkParam.getValueType() == LinkParam.ValueType.EMPTY && value == null){
            return true;
        } else {
            return linkParam.contains(value);
        }
    }

    @Override
    public Collection<LinkParam> getLinkParams() {
        return this.linkParams.values();
    }


    @Override
    public final void setRequestDispatcher(RequestDispatcher requestDispatcher) {
        this.requestDispatcher = requestDispatcher;
    }


    @Override
    public final RequestDispatcher getRequestDispatcher() {
        return this.requestDispatcher;
    }


    @Override
    public final String getUriPath() {
        return this.path;
    }


    @Override
    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }


    /**
     * This method is the one and only recommended way to change the status.
     *
     * Invocation of this method write-locks the resource status, i.e. concurrent invocations of
     * {@link #getWrappedResourceStatus(long)} or this method wait for this method to
     * finish, i.e. to unlock the write-lock. This is e.g. to avoid inconsistencies between the content and
     * {@link de.uzl.itm.ncoap.message.options.Option#ETAG}, resp. {@link de.uzl.itm.ncoap.message.options.Option#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such
     * inconsistencies could happen in case of a resource update between calls of e.g.
     * {@link #getSerializedResourceStatus(long)} and {@link #getEtag(long)}, resp. {@link #getMaxAge()}.
     *
     * @param resourceStatus the new status of the resource
     * @param lifetimeSeconds the number of seconds this status is valid, i.e. cachable by clients or proxies.
     */
    @Override
    public final void setResourceStatus(T resourceStatus, long lifetimeSeconds) {
        try{
            readWriteLock.writeLock().lock();
            this.resourceStatus = resourceStatus;
            this.resourceStatusExpiryDate = System.currentTimeMillis() + (lifetimeSeconds * 1000);
            updateEtag(resourceStatus);

            LOG.debug("New status of {} set (expires in {} seconds).", this.path, lifetimeSeconds);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * This method is the one and only recommended way to retrieve the actual resource status that is used
     * for a {@link de.uzl.itm.ncoap.message.CoapResponse} to answer an inbound {@link de.uzl.itm.ncoap.message.CoapRequest}.
     *
     * Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link de.uzl.itm.ncoap.message.options.Option#ETAG}, resp.
     * {@link de.uzl.itm.ncoap.message.options.Option#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such inconsistencies could happen in case of a
     * resource update between calls of e.g. {@link #getSerializedResourceStatus(long)} and {@link #getEtag(long)},
     * resp. {@link #getMaxAge()}.
     *
     * However, concurrent invocations of this method are possible, as the resources read-lock can be locked multiple
     * times in parallel.
     *
     * @param contentFormat the number representing the desired content format of the serialized resource status
     *
     * @return a {@link WrappedResourceStatus} if the content format was supported or <code>null</code> if the
     * resource status could not be serialized to the desired content format.
     */
    public final WrappedResourceStatus getWrappedResourceStatus(long contentFormat) {
        try{
            this.readWriteLock.readLock().lock();

            byte[] serializedResourceStatus = getSerializedResourceStatus(contentFormat);

            if (serializedResourceStatus == null) {
                return null;
            } else {
                byte[] etag = this.getEtag(contentFormat);
                long maxAge = this.getMaxAge();
                return new WrappedResourceStatus(serializedResourceStatus, contentFormat, etag, maxAge);
            }
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * This method and {@link #getWrappedResourceStatus(long)} are the only recommended ways to retrieve
     * the actual resource status that is used for a {@link de.uzl.itm.ncoap.message.CoapResponse} to answer an inbound {@link de.uzl.itm.ncoap.message.CoapRequest}.
     *
     * Invocation of this method read-locks the resource status, i.e. concurrent invocations of
     * {@link #setResourceStatus(Object, long)} wait for this method to finish, i.e. the read-lock to be released.
     * This is to avoid inconsistencies between the content and {@link de.uzl.itm.ncoap.message.options.Option#ETAG}, resp.
     * {@link de.uzl.itm.ncoap.message.options.Option#MAX_AGE} in a {@link de.uzl.itm.ncoap.message.CoapResponse}. Such inconsistencies could happen in case of a
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
    @Override
    public final WrappedResourceStatus getWrappedResourceStatus(Set<Long> contentFormats) {
        try{
            this.readWriteLock.readLock().lock();

            WrappedResourceStatus result = null;

            for(long contentFormat : contentFormats) {
                result = getWrappedResourceStatus(contentFormat);

                if (result != null)
                    break;
            }

            return result;
        }
        finally {
            this.readWriteLock.readLock().unlock();
        }
    }


    @Override
    public final T getResourceStatus() {
        return this.resourceStatus;
    }


    /**
     * Returns the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients. The returned number is calculated using the parameter <code>lifetimeSeconds</code> on
     * invocation of {@link #setResourceStatus(Object, long)} or
     * {@link #NotObservableWebresource(String, Object, long, java.util.concurrent.ScheduledExecutorService)}
     * (which internally invokes {@link #setResourceStatus(Object, long)}).
     *
     * If the number of seconds passed after the last invocation of {@link #setResourceStatus(Object, long)} is larger
     * than the number of seconds given as parameter <code>lifetimeSeconds</code>, this method returns zero.
     *
     * @return the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients.
     */
    @Override
    public final long getMaxAge() {
        return Math.max(this.resourceStatusExpiryDate - System.currentTimeMillis(), 0) / 1000;
    }
}
