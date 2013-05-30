package de.uniluebeck.itm.spitfire.nCoap.application.server.webservice;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This is the abstract class to be extended by classes to represent a not observable resource.The generic type T
 * means, that the object that holds the resourceStatus of the resource is of type T.
 *
 * Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
 * (integer values). Then, your service class could e.g. extend {@link NotObservableWebService<Integer>}.
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public abstract class NotObservableWebService<T> implements WebService<T> {
    
    private String path;
    private T resourceStatus;

    private long maxAge = OptionRegistry.MAX_AGE_DEFAULT;

    private ScheduledExecutorService executorService;

    protected NotObservableWebService(String servicePath, T initialStatus){
        this.path = servicePath;
        this.resourceStatus = initialStatus;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public final T getResourceStatus(){
        return this.resourceStatus;
    }

    @Override
    public void setExecutorService(ScheduledExecutorService executorService){
        this.executorService = executorService;
    }

    @Override
    public ScheduledExecutorService getExecutorService(){
        return this.executorService;
    }

    @Override
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * The max-age value represents the validity period (in seconds) of the actual status. The nCoap framework uses this
     * value to set the {@link OptionRegistry.OptionName#MAX_AGE} option in every {@link CoapResponse} which is
     * returned by {@link #processMessage(CoapRequest, InetSocketAddress)}. This does not hold for error messages with
     * {@code message.getCode().isErrorMessage() == true}.
     *
     * @param maxAge the new max age value
     */
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public final void setResourceStatus(T newStatus){
        this.resourceStatus = newStatus;
    }

    @Override
    public int hashCode(){
        return this.getPath().hashCode();
    }

    @Override
    public boolean equals(Object object){

        if(object == null){
            return false;
        }

        if(!(object instanceof String || object instanceof WebService)){
            return false;
        }

        if(object instanceof String){
            return (this.getPath().equals(object));
        }
        else{
            return (this.getPath().equals(((WebService) object).getPath()));
        }
    }
}
