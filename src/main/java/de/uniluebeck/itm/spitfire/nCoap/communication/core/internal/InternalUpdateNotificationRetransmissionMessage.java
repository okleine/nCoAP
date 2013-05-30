package de.uniluebeck.itm.spitfire.nCoap.communication.core.internal;

import java.net.InetSocketAddress;

import de.uniluebeck.itm.spitfire.nCoap.communication.observe.ObservableResourceHandler;

/**
 * This message notifies the {@link ObservableResourceHandler} instance about the retransmission of an
 * update notification. This causes the notification count of this observation to be increased by 1.
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public class InternalUpdateNotificationRetransmissionMessage {

    private String servicePath;
    private InetSocketAddress observerAddress;

    public InternalUpdateNotificationRetransmissionMessage(InetSocketAddress observerAddress, String servicePath) {
        this.observerAddress = observerAddress;
        this.servicePath = servicePath;
    }

    public InetSocketAddress getObserverAddress() {
        return this.observerAddress;
    }

    public String getServicePath() {
        return servicePath;
    }

    @Override
    public String toString(){
        return "[UpdateNotificationRetransmissionMessage] observer: " + observerAddress + ", service: " + servicePath;
    }
}
