package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 30.11.12
 * Time: 14:37
 * To change this template use File | Settings | File Templates.
 */
public class RetransmissionTimeoutException extends Exception{

    private byte[] token;
    private InetSocketAddress rcptAddress;

    public RetransmissionTimeoutException(byte[] token, InetSocketAddress rcptAddress, String message){
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
