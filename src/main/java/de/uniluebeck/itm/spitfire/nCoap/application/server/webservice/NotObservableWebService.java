package de.uniluebeck.itm.spitfire.nCoap.application.server.webservice;

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

    protected NotObservableWebService(String servicePath, T initialStatus){
        this.path = servicePath;
        this.resourceStatus = initialStatus;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    public final T getResourceStatus(){
        return this.resourceStatus;
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
