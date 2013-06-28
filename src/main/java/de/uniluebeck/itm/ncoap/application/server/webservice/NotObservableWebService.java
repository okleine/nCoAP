package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.collect.HashBasedTable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This is the abstract class to be extended by classes to represent a not observable resource.The generic type T
 * means, that the object that holds the resourceStatus of the resource is of type T.
 *
 * Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
 * (integer values). Then, your service class could e.g. extend {@link NotObservableWebService<Integer>}.
 * 
 * @author Oliver Kleine, Stefan Hüske
 */
public abstract class NotObservableWebService<T> implements WebService<T> {
    
    private String path;
    private T resourceStatus;

    private long maxAge = OptionRegistry.MAX_AGE_DEFAULT;

    private ScheduledExecutorService scheduledExecutorService;
    private ListeningExecutorService listeningExecutorService;

    private HashBasedTable<InetSocketAddress, ByteArrayWrapper, ListenableFuture<CoapResponse>> openRequests;

    protected NotObservableWebService(String servicePath, T initialStatus){
        this.path = servicePath;
        this.resourceStatus = initialStatus;
        this.openRequests = HashBasedTable.create();
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
    public void setScheduledExecutorService(ScheduledExecutorService executorService){
        this.scheduledExecutorService = executorService;
    }

    @Override
    public void setListeningExecutorService(ListeningExecutorService executorService) {
        this.listeningExecutorService = executorService;
    }

    @Override
    public ListeningExecutorService getListeningExecutorService() {
        return listeningExecutorService;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService(){
        return this.scheduledExecutorService;
    }

    @Override
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * The max-age value represents the validity period (in seconds) of the actual status. The nCoap framework uses this
     * value as default value for the  {@link OptionRegistry.OptionName#MAX_AGE} option for outgoing
     * {@link CoapResponse}s, if there was no such option set manually.
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
