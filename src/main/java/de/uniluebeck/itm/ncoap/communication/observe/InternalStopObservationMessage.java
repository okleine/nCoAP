package de.uniluebeck.itm.ncoap.communication.observe;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 22.08.13
 * Time: 17:53
 * To change this template use File | Settings | File Templates.
 */
public class InternalStopObservationMessage {

    private InetSocketAddress remoteAddress;
    private byte[] token;

    public InternalStopObservationMessage(InetSocketAddress remoteAddress, byte[] token){
        this.remoteAddress = remoteAddress;

        this.token = token;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public byte[] getToken() {
        return token;
    }
}
