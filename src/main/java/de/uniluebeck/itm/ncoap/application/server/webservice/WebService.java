package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.*;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;


import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * This is the interface to be implemented to realize a CoAP webservice. The generic type T means, that the object
 * that holds the status of the resource is of type T.
 *
 * Example: Assume, you want to realize a service representing a temperature with limited accuracy (integer values).
 * Then, your service class must implement WebService<Integer>.
 */
public interface WebService<T> {

    /**
     * Returns the (relative) path this service is listening at
     * @return relative path of the service (e.g. /path/to/service)
     */
    public String getPath();

    /**
     * Returns the object of type T that holds the actual status of the resource represented by this
     * {@link NotObservableWebService}.
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
     * Method to set the new status of the resource represented by this {@link WebService}. This method is the
     * one and only recommended way to change the status.
     *
     * Note, that this status is internal and thus independent from the payload of the {@link CoapResponse} to be
     * returned by the inherited method {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)}.
     *
     * Example: Assume this webservice represents a switch that has two states "on" and "off". The payload of the
     * previously mentioned {@link CoapResponse} could then be either "on" or "off". But since there are only
     * two possible states {@link T} could be of type {@link Boolean}.
     *
     * @param newStatus the object of type {@link T} representing the new status
     */
    public void setResourceStatus(T newStatus);

    /**
     * This method is automatically invoked by the nCoAP framework when this service instance is registered at a
     * {@link CoapServerApplication} instance (using {@link CoapServerApplication#registerService(WebService)}.
     * So, usually there is no need to set another {@link ScheduledExecutorService} instance manually.
     *
     * @param executorService a {@link ScheduledExecutorService} instance.
     */
    public void setScheduledExecutorService(ScheduledExecutorService executorService);

    public void setListeningExecutorService(ListeningExecutorService listeningExecutorService);

    /**
     * Returns the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     *
     * @return the {@link ScheduledExecutorService} instance which is used to schedule and execute any
     * web service related tasks.
     */
    public ScheduledExecutorService getScheduledExecutorService();

    public ListeningExecutorService getListeningExecutorService();

    /**
     * The max-age value represents the validity period (in seconds) of the actual status. The nCoap framework uses this
     * value to set the {@link OptionName#MAX_AGE} wherever necessary or useful. The framework does not change or remove
     * manually set max-age options in {@link CoapResponse} instances, i.e. using {@code response.setMaxAge(int)}.
     *
     * @return the max-age value of this {@link WebService} instance. If not set to another value implementing classes must
     * return {@link OptionRegistry#MAX_AGE_DEFAULT} as default value.
     */
    public long getMaxAge();


    /**
     * This method is called by the nCoAP framework when this {@link WebService} is removed from the
     * {@link CoapServerApplication} instance. If any one could e.g. try to cancel scheduled tasks. There might even
     * be no need to do anything at all, i.e. implement the method with empty body.
     *
     * If this {@link WebService} uses the default {@link ScheduledExecutorService} to execute tasks one MUST NOT
     * terminate this {@link ScheduledExecutorService} but only cancel scheduled tasks using there
     * {@link ScheduledFuture}.
     */
    public void shutdown();

    /**
     * Implementing classes must provide this method such that it returns <code>true</code> if
     * <ul>
     *  <li>
     *      the given object is a String that equals to the path of the URI representing the WebService
     *      instance, or
 *      </li>
     *  <li>
     *      the given object is a WebService instance which path equals to this WebService path.
     *  </li>
     * </ul>
     * In all other cases the equals method must return <code>false</code>.
     *
     * @param object The object to compare this WebService instance with
     * @return <code>true</code> if the given object is a String representing the path of the URI of this WebService or
     * if the given object is a WebService instance which path equals this WebService path
     */
    public boolean equals(Object object);

    /**
     * This method must return a hash value for the WebService instance based on the URI path of the webservice. Same
     * path must return the same hash value whereas different paths should have hash values as distinct as possible.
     */
    @Override
    public int hashCode();

    /**
     * Method to process an incoming {@link CoapRequest} asynchronously. The implementation of this method is dependant
     * on the concrete webservice. Processing a message might cause a new status of the resource or even the deletion
     * of the complete resource, i.e. this {@link WebService} instance.
     *
     * Implementing classes have to make sure that {@link SettableFuture<CoapResponse>#set(CoapResponse)} is invoked
     * after some time. Otherwise the {@link CoapServerApplication} will wait forever, even though non-blocking.
     *
     * The way to process the incoming request is basically to be implemented based on the {@link Code},
     * the {@link MsgType}, the contained {@link Option}s (if any) and (if any) the payload of the request.
     *
     * @param responseFuture the {@link SettableFuture} instance to set the {@link CoapResponse} which is the result
     *                       of the incoming {@link CoapRequest}. Use
     *                       {@link SettableFuture<CoapResponse>#set(CoapResponse)} to send it to the client.
     * @param request The {@link CoapRequest} to be processed by the {@link WebService} instance
     * @param remoteAddress The address of the sender of the request
     *
     */
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest request,
                                   InetSocketAddress remoteAddress);
}
