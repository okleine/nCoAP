package de.uniluebeck.itm.spitfire.nCoap.application.webservice;

import java.util.Observable;

/**
 * This is the abstract class to be extended by classes to represent an observable resource.The generic type T
 * means, that the object that holds the resourceStatus of the resource is of type T.
 *
 * Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
 * (integer values). Then, your service class must extend NotObservableWebService<Integer>.
 *
 * @author Oliver Kleine, Stefan Hueske
 */
public abstract class ObservableWebService<T> extends Observable implements WebService<T> {

    private String path;
    private T resourceStatus;

    protected ObservableWebService(String path, T initialStatus){
        setPath(path);
        setResourceStatus(initialStatus);
    }

    /**
     * Set the URI path (relative) this service should listen at
     * @param path relative path of the service (e.g. /path/to/service)
     */
    @Override
    public final void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the (relative) path this service is listening at
     * @return relative path of the service (e.g. /path/to/service)
     */
    @Override
    public final synchronized String getPath() {
       return this.path;
    }

    /**
     * Returns the object of type T that holds the actual resourceStatus of the resource represented by this
     * {@link NotObservableWebService}
     * @return the object of type T that holds the actual resourceStatus of the resource
     */
    protected final T getResourceStatus(){
        return this.resourceStatus;
    }

    /**
     * Use this method to set the resourceStatus of an observable resource. By using this method to update a resource, every
     * observer is automatically notfied about the new resourceStatus of the resource.
     *
     * @param newStatus An instance of class T that holds the new resourceStatus if the resource represented by this
     *                  {@link WebService} instance.
     */
    @Override
    public final void setResourceStatus(T newStatus){
        this.resourceStatus = newStatus;

        //Notify observers (methods inherited from abstract class Observable)
        setChanged();
        notifyObservers();
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
            return this.getPath().equals(object);
        }
        else{
            return this.getPath().equals(((WebService) object).getPath());
        }
    }
}
