package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import java.net.InetSocketAddress;

import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;

/**
 * This internal message is sent upstream when a confirmable update notification was re-transmitted due to a missing
 * acknowledgement from the observer.
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public class InternalUpdateNotificationRetransmissionMessage {

    private String servicePath;
    private InetSocketAddress observerAddress;

    /**
     * @param observerAddress the address of the observer that did not confirm the reception of a confirmable
     *                        update notification
     * @param servicePath the path of the {@link ObservableWebService} instance whose confirmable update notification
     *                    was not yet confirmed by the observer
     */
    public InternalUpdateNotificationRetransmissionMessage(InetSocketAddress observerAddress, String servicePath) {
        this.observerAddress = observerAddress;
        this.servicePath = servicePath;
    }

    /**
     * Returns the address of the observer that did not confirm the reception of the confirmable update notification
     * @return the address of the observer that did not confirm the reception of the confirmable update notification
     */
    public InetSocketAddress getObserverAddress() {
        return this.observerAddress;
    }

    /**
     * Returns the path of the {@link ObservableWebService} instance whose confirmable update notification was not yet
     * confirmed by the observer
     * @return the path of the {@link ObservableWebService} instance whose confirmable update notification was not yet
     * confirmed by the observer
     */
    public String getServicePath() {
        return servicePath;
    }

    @Override
    public String toString(){
        return "[UpdateNotificationRetransmissionMessage] observer: " + observerAddress + ", service: " + servicePath;
    }
}
