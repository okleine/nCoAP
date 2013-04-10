package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

import de.uniluebeck.itm.spitfire.nCoap.application.webservice.ObservableWebService;

/**
 * This message will be passed down the pipeline if a registered service updates.
 * 
 * @author Stefan Hueske, Oliver Kleine
 */
public class ObservableWebServiceUpdate implements InternalMessage {
    private ObservableWebService service;

    public ObservableWebServiceUpdate(ObservableWebService service) {
        this.service = service;
    }

    /**
     * Returns the content of the internal ObservableWebServiceUpdate message which is the updated WebService instance
     * itself.
     * @return the updated {@link ObservableWebService} instance (may be cast from Object)
     */
    @Override
    public Object getContent() {
        return service;
    }
}
