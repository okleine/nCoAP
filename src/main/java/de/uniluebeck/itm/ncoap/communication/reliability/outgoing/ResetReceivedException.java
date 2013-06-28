package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import java.net.InetSocketAddress;

public class ResetReceivedException extends Exception {
    private byte[] token;
    private InetSocketAddress rcptAddress;

    public ResetReceivedException(byte[] token, InetSocketAddress rcptAddress, String message){
        super(message);
        this.token = token;
        this.rcptAddress = rcptAddress;
    }

    public byte[] getToken() {
        return token;
    }

    public InetSocketAddress getRcptAddress() {
        return rcptAddress;
    }
}
