package de.uniluebeck.itm.spitfire.nCoap.application;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;


/**
 * A service offers a resource to the CoapServerApplication.
 * The service will be registered at a path using
 * CoapServerApplication.registerService() 
 * The same service instance can be registered multiple times at different paths.
 * Observers of this resource will be notified by calling Service.notifyObservers().
 * 
 * @author Stefan Hueske
 */
public abstract class Service extends Observable {
    
    //Set of paths to which this service is registred to
    private Set<String> registeredPaths = new HashSet<String>();
    
    public abstract CoapResponse getStatus(CoapRequest request);
    
    /**
     * Notify observers.
     */
    @Override
    public void notifyObservers() {
        setChanged();
        super.notifyObservers();
    }
    
    /**
     * Internal method to add a registered path.
     * @param path 
     */
    synchronized void addPath(String path) {
        registeredPaths.add(path);
    }
    
    /**
     * Internal method to remove a registered path.
     * @param path 
     */
    synchronized boolean removePath(String path) {
        return registeredPaths.remove(path);
    }

    /**
     * Returns all paths this service is registered to.
     * @return set of paths
     */
    public synchronized Set<String> getRegisteredPaths() {
        return registeredPaths;
    }
}
