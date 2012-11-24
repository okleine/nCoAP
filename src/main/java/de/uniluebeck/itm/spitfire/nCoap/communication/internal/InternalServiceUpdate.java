package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

import de.uniluebeck.itm.spitfire.nCoap.application.Service;

/**
 * This message will be passed down the pipeline if a registered service updates.
 * 
 * @author Stefan Hueske
 */
public class InternalServiceUpdate implements InternalMessage {
    private Service service;

    public InternalServiceUpdate(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    @Override
    public Object getContent() {
        return service;
    }
}
