package de.uniluebeck.itm.spitfire.nCoap.communication.core.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notifies the ObservableResourceHandler when a Service gets removed from a path.
 * 
 * @author Stefan Hueske
 */
public class InternalServiceRemovedFromPathMessage {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private String servicePath;

    public InternalServiceRemovedFromPathMessage(String servicePath) {
        this.servicePath = servicePath;

        log.info("Internal service removed from path message created for path " + servicePath);
    }

    public String getServicePath() {
        return servicePath;
    }
}
