package de.uniluebeck.itm.spitfire.nCoap.application.webservice;

/**
 * This is the abstract class to be extended by classes to represent a not observable resource.The generic type T
 * means, that the object that holds the resourceStatus of the resource is of type T.
 *
 * Example: Assume, you want to realize a not observable service representing a temperature with limited accuracy
 * (integer values). Then, your service class must extend NotObservableWebService<Integer>.
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public abstract class NotObservableWebService<T> implements WebService<T> {
    
    private String path;
    private T resourceStatus;

    protected NotObservableWebService(String path, T initialStatus){
        setPath(path);
        setResourceStatus(initialStatus);
    }

    /**
     * Returns the (relative) path this service is listening at
     * @return relative path of the service (e.g. /path/to/service)
     */
    @Override
    public String getPath() {
        return this.path;
    }

    /**
     * Set the URI path (relative) this service should listen at
     * @param path relative path of the service (e.g. /path/to/service)
     */
    @Override
    public void setPath(String path) {
        this.path = path;
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
     * Method to set the new resourceStatus of the resource represented by this {@link WebService}.
     * @param newStatus
     */
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
