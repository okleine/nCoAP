package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import java.net.InetSocketAddress;

/**
 * Created by olli on 06.03.14.
 */
public class RetransmissionsAlreadyScheduledException extends Exception {

    private final InetSocketAddress remoteSocketAddress;
    private final int messageID;

    public RetransmissionsAlreadyScheduledException(InetSocketAddress remoteSocketAddress, int messageID){

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
