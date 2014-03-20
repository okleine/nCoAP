package de.uniluebeck.itm.ncoap.communication.observe.server;

import de.uniluebeck.itm.ncoap.application.client.Token;

import java.net.InetSocketAddress;

/**
 * Created by olli on 20.03.14.
 */
public class InternalStopObservationMessage {

    private InetSocketAddress remoteEndpoint;
    private Token token;


    public InternalStopObservationMessage(InetSocketAddress remoteEndpoint, Token token) {
        this.remoteEndpoint = remoteEndpoint;
        this.token = token;
    }


    public InetSocketAddress getRemoteEndpoint() {
        return remoteEndpoint;
    }


    public Token getToken() {
        return token;
    }
}
