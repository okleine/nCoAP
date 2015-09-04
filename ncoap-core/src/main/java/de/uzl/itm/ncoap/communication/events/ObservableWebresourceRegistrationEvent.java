package de.uzl.itm.ncoap.communication.events;

import de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource;

/**
 * Created by olli on 04.09.15.
 */
public class ObservableWebresourceRegistrationEvent {

    private ObservableWebresource webresource;

    public ObservableWebresourceRegistrationEvent(ObservableWebresource webresource){
        this.webresource = webresource;
    }

    public ObservableWebresource getWebresource() {
        return webresource;
    }

    public interface Handler {
        public void handleObservableWebresourceRegistrationEvent(ObservableWebresourceRegistrationEvent event);
    }
}
