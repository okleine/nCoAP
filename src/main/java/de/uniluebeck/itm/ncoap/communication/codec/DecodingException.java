package de.uniluebeck.itm.ncoap.communication.codec;

import de.uniluebeck.itm.ncoap.application.Token;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 07.12.13
 * Time: 14:02
 * To change this template use File | Settings | File Templates.
 */
public class DecodingException extends Exception {

    private InetSocketAddress remoteSocketAddress;
    private int messageID;
    private Token token;

    public DecodingException(InetSocketAddress remoteSocketAddress, int messageID, Token token, Throwable cause){
        super(cause);
        this.remoteSocketAddress = remoteSocketAddress;
        this.messageID = messageID;
        this.token = token;
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }

    public int getMessageID() {
        return messageID;
    }

    public Token getToken() {
        return token;
    }
}
