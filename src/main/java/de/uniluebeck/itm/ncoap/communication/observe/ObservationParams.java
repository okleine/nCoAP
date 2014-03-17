package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by olli on 17.03.14.
 */
public class ObservationParams {

    private InetSocketAddress remoteEndpoint;
    private Token token;
    private int latestUpdateNotificationMessageID;
    private String webservicePath;
    private long contentFormat;
    private Set<byte[]> etags;

    private AtomicLong notificationCount;


    public ObservationParams(InetSocketAddress remoteEndpoint, Token token, String webservicePath, Set<byte[]> etags) {
        this.remoteEndpoint = remoteEndpoint;
        this.token = token;
        this.webservicePath = webservicePath;
        this.etags = etags;
        this.contentFormat = ContentFormat.UNDEFINED;
        this.notificationCount = new AtomicLong(0);
        this.latestUpdateNotificationMessageID = CoapMessage.MESSAGE_ID_UNDEFINED;
    }


    public InetSocketAddress getRemoteEndpoint() {
        return this.remoteEndpoint;
    }

    public Token getToken() {
        return this.token;
    }


    public long nextNotification(){
        return this.notificationCount.incrementAndGet();
    }


    public long getContentFormat() {
        return this.contentFormat;
    }


    public void setContentFormat(long contentFormat) {
        this.contentFormat = contentFormat;
    }

    public String getWebservicePath() {
        return this.webservicePath;
    }

    public Set<byte[]> getEtags() {
        return this.etags;
    }


    public int getLatestUpdateNotificationMessageID() {
        return latestUpdateNotificationMessageID;
    }


    public void setLatestUpdateNotificationMessageID(int latestUpdateNotificationMessageID) {
        this.latestUpdateNotificationMessageID = latestUpdateNotificationMessageID;
    }

}
