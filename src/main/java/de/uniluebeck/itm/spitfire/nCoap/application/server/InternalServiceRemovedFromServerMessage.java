package de.uniluebeck.itm.spitfire.nCoap.application.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.WebService;

/**
 * This internal message is sent downstream when a {@link WebService} instance was removed from the
 * {@link CoapServerApplication} instance.
 * 
 * @author Oliver Kleine, Stefan Hüske
 */
public class InternalServiceRemovedFromServerMessage {

    private static Logger log = LoggerFactory.getLogger(InternalServiceRemovedFromServerMessage.class.getName());
    private String servicePath;

    /**
     * @param servicePath the path of the removed {@link WebService} instance
     */
    public InternalServiceRemovedFromServerMessage(String servicePath) {
        this.servicePath = servicePath;

        log.info("Internal service removed from path message created for path " + servicePath);
    }

    /**
     * Returns the path of the removed {@link WebService} instance
     * @return the path of the removed {@link WebService} instance
     */
    public String getServicePath() {
        return servicePath;
    }

    @Override
    public String toString(){
        return "[Internal message] Service " + servicePath + " was removed from server.";
    }
}
