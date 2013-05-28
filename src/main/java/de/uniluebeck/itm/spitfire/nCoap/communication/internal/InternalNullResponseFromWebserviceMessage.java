package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 17.05.13
 * Time: 16:41
 * To change this template use File | Settings | File Templates.
 */
public class InternalNullResponseFromWebserviceMessage {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private InetSocketAddress remoteAddress;
    private int messageID;

    public InternalNullResponseFromWebserviceMessage(InetSocketAddress remoteAddress, int messageID){
        this.remoteAddress = remoteAddress;
        this.messageID = messageID;
        log.info("Internal null response message created for message ID " + messageID + " from " + remoteAddress);
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public int getMessageID() {
        return messageID;
    }
}
