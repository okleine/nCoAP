package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

import java.net.SocketAddress;

/**
 * This message notifies the ObservableHandler of an external ObserveOption update.
 * 
 * @author Stefan Hueske
 */
public class InternalObserveOptionUpdate {
    byte[] token;
    SocketAddress remoteAddress;
    Long observeOptionValue;

    public InternalObserveOptionUpdate(byte[] token, SocketAddress remoteAddress, Long observeOptionValue) {
        this.token = token;
        this.remoteAddress = remoteAddress;
        this.observeOptionValue = observeOptionValue;
    }

    public byte[] getToken() {
        return token;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public Long getObserveOptionValue() {
        return observeOptionValue;
    }
}
