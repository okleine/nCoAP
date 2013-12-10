package de.uniluebeck.itm.ncoap.communication.codec;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 03.12.13
 * Time: 16:47
 * To change this template use File | Settings | File Templates.
 */
public class MessageFormatDecodingException extends Exception {

    private InetSocketAddress remoteSocketAddress;
    private int messageID;

    public MessageFormatDecodingException(InetSocketAddress remoteSocketAddress, int messageID, String message){
        super(message);
        this.remoteSocketAddress = remoteSocketAddress;
        this.messageID = messageID;
    }

    public InetSocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }

    public int getMessageID() {
        return messageID;
    }
}
