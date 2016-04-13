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

package de.uzl.itm.ncoap.application.server.resource;

import com.google.common.collect.LinkedHashMultimap;
import de.uzl.itm.ncoap.application.linkformat.EmptyLinkAttribute;
import de.uzl.itm.ncoap.application.linkformat.LinkAttribute;
import de.uzl.itm.ncoap.communication.dispatching.server.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static de.uzl.itm.ncoap.message.options.OptionValue.MAX_AGE_DEFAULT;

/**
* This is the abstract class to be extended by classes to represent an observable resource. The generic type T
* means, that the object that holds the status of the resource is of type T.
*
* Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
* (integer values). Then, your service class should extend {@link ObservableWebresource<Integer>}.
*
* @author Oliver Kleine, Stefan Hüske
*/
public abstract class ObservableWebresource<T> extends Observable implements Webresource<T> {

    private static Logger log = LoggerFactory.getLogger(ObservableWebresource.class.getName());

    public static final boolean SHUTDOWN = true;
    public static final boolean UPDATE = false;

    private RequestDispatcher requestDispatcher;
    private String uriPath;
    private LinkedHashMultimap<String, LinkAttribute> linkAttributes;

    private T status;
    private long statusExpiryDate;
    private ReentrantReadWriteLock statusLock;

    private ScheduledExecutorService executor;


    /**
     * Using this constructor is the same as
     * {@link #ObservableWebresource(String, Object, long, java.util.concurrent.ScheduledExecutorService)}
     * with parameter <code>lifetimeSeconds</code> set to
     * {@link de.uzl.itm.ncoap.message.options.OptionValue#MAX_AGE_DEFAULT}.
     *
     * @param uriPath the uriPath this {@link ObservableWebresource} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebresource}.
     */
    protected ObservableWebresource(String uriPath, T initialStatus, ScheduledExecutorService executor) {
        this(uriPath, initialStatus, MAX_AGE_DEFAULT, executor);
        this.setLinkAttribute(new EmptyLinkAttribute(EmptyLinkAttribute.OBSERVABLE));
    }


    /**
     * @param uriPath the uriPath this {@link ObservableWebresource} is registered at.
     * @param initialStatus the initial status of this {@link ObservableWebresource}.
     * @param lifetime the number of seconds the initial status may be considered fresh, i.e. cachable by
     *                        proxies or clients.
     */
    protected ObservableWebresource(String uriPath, T initialStatus, long lifetime, ScheduledExecutorService executor) {
        this.uriPath = uriPath;
        this.linkAttributes = LinkedHashMultimap.create();
        this.statusLock = new ReentrantReadWriteLock();
        this.executor = executor;
        setResourceStatus(initialStatus, lifetime);
    }


    @Override
    public void setLinkAttribute(LinkAttribute linkAttribute) {
        if (this.linkAttributes.containsKey(linkAttribute.getKey())) {

            if (!LinkAttribute.allowsMultipleValues(linkAttribute.getKey()))
                removeLinkAttribute(linkAttribute.getKey());
        }

        this.linkAttributes.put(linkAttribute.getKey(), linkAttribute);
    }


    @Override
    public boolean removeLinkAttribute(String attributeKey) {
        return !this.linkAttributes.removeAll(attributeKey).isEmpty();
    }

    @Override
    public boolean hasLinkAttribute(LinkAttribute linkAttribute) {
        return this.linkAttributes.get(linkAttribute.getKey()).contains(linkAttribute);
    }

    @Override
    public Collection<LinkAttribute> getLinkAttributes() {
        return this.linkAttributes.values();
    }


    @Override
    public void setRequestDispatcher(RequestDispatcher requestDispatcher) {
        this.requestDispatcher = requestDispatcher;
    }


    @Override
    public RequestDispatcher getRequestDispatcher() {
        return this.requestDispatcher;
    }


    @Override
    public final String getUriPath() {
        return this.uriPath;
    }


    @Override
    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }


    /**
     * {@inheritDoc}
     *
     * <p><b>Note: </b>Do not use this method for status retrieval when processing an inbound
     * {@link de.uzl.itm.ncoap.message.CoapRequest}. Use
     * {@link #getWrappedResourceStatus(java.util.Set)} instead to avoid synchronization issues. However,
     * this method is safely called within {@link #getWrappedResourceStatus(java.util.Set)}.</p>
     */
    @Override
    public final T getResourceStatus() {
        return this.status;
    }


    @Override
    public synchronized final void setResourceStatus(final T status, final long lifetime) {
        this.executor.submit(new Runnable() {

            @Override
            public void run() {
                try{
                    statusLock.writeLock().lock();

                    ObservableWebresource.this.status = status;
                    ObservableWebresource.this.statusExpiryDate = System.currentTimeMillis() + (lifetime * 1000);
                    ObservableWebresource.this.updateEtag(status);

                    log.debug("New status of {} successfully set (expires in {} seconds).",
                            ObservableWebresource.this.getUriPath(), lifetime);

                    setChanged();
                    notifyObservers(UPDATE);
                }
                catch(Exception ex) {
                    log.error("Exception while setting new resource status for \"{}\"!",
                            ObservableWebresource.this.getUriPath(), ex);
                }

                finally {
                    statusLock.writeLock().unlock();
                }
            }
        });
    }


    /**
     * This method and {@link #getWrappedResourceStatus(java.util.Set)} are the only recommended way to retrieve
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
     * @param contentFormat the number representing the desired content format of the serialized resource status
     *
     * @return a {@link WrappedResourceStatus} if the content format was supported or <code>null</code> if the
     * resource status could not be serialized to the desired content format.
     */
    public final WrappedResourceStatus getWrappedResourceStatus(long contentFormat) {
        try{
            this.statusLock.readLock().lock();

            byte[] serializedResourceStatus = getSerializedResourceStatus(contentFormat);

            if (serializedResourceStatus == null)
                return null;

            else
                return new WrappedResourceStatus(serializedResourceStatus, contentFormat,
                        this.getEtag(contentFormat), this.getMaxAge());
        }
        finally {
            this.statusLock.readLock().unlock();
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
            this.statusLock.readLock().lock();

            WrappedResourceStatus result = null;

            for(long contentFormat : contentFormats) {
                result = getWrappedResourceStatus(contentFormat);

                if (result != null)
                    break;
            }

            return result;
        }
        finally {
            this.statusLock.readLock().unlock();
        }
    }


    /**
     * This method is invoked by the framework for every observer after every resource update. Classes that extend
     * {@link ObservableWebresource} may implement this method just by returning one of
     * {@link de.uzl.itm.ncoap.message.MessageType#CON} or {@link de.uzl.itm.ncoap.message.MessageType#NON}.
     * However, this method also gives {@link ObservableWebresource}s the opportunity
     * to e.g. distinguish between observers or have some other arbitrary logic...
     *
     * @param remoteSocket the remote CoAP endpoints that observes this {@link ObservableWebresource}
     *
     * @return the message type for the next update notification for the observer identified by the given parameters
     */
    public abstract boolean isUpdateNotificationConfirmable(InetSocketAddress remoteSocket);


    /**
     * Returns the number of seconds the actual resource state can be considered fresh for status caching on proxies
     * or clients. The returned number is calculated using the parameter <code>lifetimeSeconds</code> on
     * invocation of {@link #setResourceStatus(Object, long)} or
     * {@link #ObservableWebresource(String, Object, long, ScheduledExecutorService)}
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
        return Math.max(this.statusExpiryDate - System.currentTimeMillis(), 0) / 1000;
    }


    /**
     * <p>This method is called by the nCoAP framework within the unregistration process of this
     * {@link de.uzl.itm.ncoap.application.server.resource.ObservableWebresource} instance.</p>
     *
     * <p><b>Important:</b> Make sure to invoke <code>super.shutdown()</code> if you override this method in an extending
     * class.</p>
     *
     * <p><b>Note:</b> Do NOT INVOKE this method directly! Use
     * {@link de.uzl.itm.ncoap.application.server.CoapServer#
     *  shutdownWebresource(de.uzl.itm.ncoap.application.server.webresource.Webresource)
     * } or
     * {@link de.uzl.itm.ncoap.application.endpoint.CoapEndpoint#
     *  shutdownWebresource(de.uzl.itm.ncoap.application.server.webresource.Webresource)
     * }
     * to shutdown a resource!</p>
     */
    @Override
    public void shutdown() {
        getExecutor().submit(new Runnable() {
            @Override
            public void run() {
                log.warn("Shutdown service \"{}\"!", getUriPath());
                statusLock.writeLock().lock();
                setChanged();
                notifyObservers(SHUTDOWN);
            }
        });
    }
}
