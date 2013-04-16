package de.uniluebeck.itm.spitfire.nCoap.application.webservice;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;

import java.net.InetSocketAddress;

/**
 * This is the interface to be implemented to realize a CoAP webservice. The generic type T means, that the object
 * that holds the status of the resource is of type T.
 *
 * Example: Assume, you want to realize a service representing a temperature with limited accuracy (integer values).
 * Then, your service class must implement WebService<Integer>.
 */
public interface WebService<T> {

    /**
     * Set the URI path (relative) this service should listen at
     * @param path relative path of the service (e.g. /path/to/service)
     */
    public void setPath(String path);

    /**
     * Returns the (relative) path this service is listening at
     * @return relative path of the service (e.g. /path/to/service)
     */
    public String getPath();

    /**
     * Method to set the new status of the resource represented by this {@link WebService}.
     * @param newStatus
     */
    public void setResourceStatus(T newStatus);

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
     * path must return the same hash value whereas different path should have hash values as distinct as possible.
     */
    @Override
    public int hashCode();

    /**
     * Method to process an incoming {@link CoapRequest}. The implementation of this method is dependant on the
     * concrete webservice. Processing a message might cause a new status of the resource or even the deletion of the
     * complete resource, resp. the webservice instance.
     *
     * @param request The {@link CoapRequest} to be processed by the {@link WebService} instance
     * @param remoteAddress The address of the sender of the request
     * @return a proper {@link CoapResponse} instance. The returned response must contain a proper {@link Code} and (if
     * the response contains payload) a {@link MediaType}.
     */
    public CoapResponse processMessage(CoapRequest request, InetSocketAddress remoteAddress);

}
