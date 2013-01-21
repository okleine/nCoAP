package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

/**
 * Notifies the ObservableHandler when a Service gets removed from a path.
 * 
 * @author Stefan Hueske
 */
public class InternalServiceRemovedFromPath {
    String servicePath;

    public InternalServiceRemovedFromPath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getServicePath() {
        return servicePath;
    }
}
