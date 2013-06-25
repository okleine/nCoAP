package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;

/**
 * This internal message is sent downstream when there is a new {@link ObservableWebService} instance registered
 * on the {@link CoapServerApplication} instance
 *
 * @author Oliver Kleine
 */
public class InternalObservableResourceRegistrationMessage {

    private ObservableWebService webService;

    /**
     * @param webService the newly registered {@link ObservableWebService} instance
     */
    public InternalObservableResourceRegistrationMessage(ObservableWebService webService){
        this.webService = webService;
    }

    /**
     * Returns the newly registered {@link ObservableWebService} instance
     * @return the newly registered {@link ObservableWebService} instance
     */
    public ObservableWebService getWebService() {
        return webService;
    }

    @Override
    public String toString(){
        return "[ObservableResourceRegistration] Path: " + webService.getPath();
    }
}
