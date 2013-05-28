package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * This message notifies the ObservableResourceHandler of an external ObserveOption update.
 * 
 * @author Stefan Hueske
 */
public class InternalObserveOptionUpdateMessage {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private byte[] token;
    private SocketAddress remoteAddress;
    private Long observeOptionValue;

    public InternalObserveOptionUpdateMessage(byte[] token, SocketAddress remoteAddress, Long observeOptionValue) {
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
