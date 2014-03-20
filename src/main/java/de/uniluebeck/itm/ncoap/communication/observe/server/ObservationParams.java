package de.uniluebeck.itm.ncoap.communication.observe.server;

import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import de.uniluebeck.itm.ncoap.message.CoapRequest;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ObservationParams} is a wrapper class. Each instance contains all context information about a running
 * observation of an {@link Webservice}.
 *
 * @author Oliver Kleine
 */
public class ObservationParams {

    private InetSocketAddress remoteEndpoint;
    private Token token;
    private int latestUpdateNotificationMessageID;
    private String webservicePath;
    private long contentFormat;
    private Set<byte[]> etags;

    private AtomicLong notificationCount;


    /**
     * Creates a new instance of {@link ObservationParams}.
     *
     * @param remoteEndpoint the observing CoAP endpoint
     * @param token the {@link Token} to enable the observing CoAP endpoint to relate update notifications with
     *              the observation request
     * @param webservicePath the path of the {@link Webservice} that is to be observed
     * @param etags the ETAGs contained as {@link OptionValue.Name#ETAG} in the {@link CoapRequest} initiating the
     *              observation
     */
    public ObservationParams(InetSocketAddress remoteEndpoint, Token token, String webservicePath, Set<byte[]> etags){
        this.remoteEndpoint = remoteEndpoint;
        this.token = token;
        this.webservicePath = webservicePath;
        this.etags = etags;
        this.contentFormat = ContentFormat.UNDEFINED;
        this.notificationCount = new AtomicLong(0);
        this.latestUpdateNotificationMessageID = CoapMessage.MESSAGE_ID_UNDEFINED;
    }


    /**
     * Returns the socket address of the oberving CoAP endpoint
     *
     * @return the socket address of the oberving CoAP endpoint
     */
    public InetSocketAddress getRemoteEndpoint() {
        return this.remoteEndpoint;
    }


    /**
     * Returns the {@link Token} that in combination with {@link #getRemoteEndpoint()} uniquely identifies this
     * observation.
     *
     * @return the {@link Token} that in combination with {@link #getRemoteEndpoint()} uniquely identifies this
     * observation.
     */
    public Token getToken() {
        return this.token;
    }


    /**
     * Increments the sequence number to be used as {@link OptionValue.Name#OBSERVE} for update notifications by 1 and
     * returns the new value after incrementation.
     *
     * @return the new value after incrementation by 1
     */
    public long nextNotification(){
        return this.notificationCount.incrementAndGet();
    }


    /**
     * Returns the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications. This number
     * represents the content format of the update notifications content.
     *
     * @return the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications
     */
    public long getContentFormat() {
        return this.contentFormat;
    }


    /**
     * Sets the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications. This number
     * represents the content format of the update notifications content.
     *
     * @param contentFormat the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications.
     */
    public void setContentFormat(long contentFormat) {
        this.contentFormat = contentFormat;
    }


    /**
     * Returns the path of the {@link Webservice} to be observed
     *
     * @return the path of the {@link Webservice} to be observed
     */
    public String getWebservicePath() {
        return this.webservicePath;
    }


    /**
     * Returns the {@link Set} of ETAGs that was contained as {@link OptionValue.Name#ETAG} in the {@link CoapRequest}
     * initiating the observation.
     *
     * @return the {@link Set} of ETAGs that was contained as {@link OptionValue.Name#ETAG} in the {@link CoapRequest}
     * initiating the observation.
     */
    public Set<byte[]> getEtags() {
        return this.etags;
    }


    /**
     * Returns the message ID of the latest update notification that was sent to the observing CoAP endpoint
     *
     * @return the message ID of the latest update notification that was sent to the observing CoAP endpoint
     */
    public int getLatestUpdateNotificationMessageID() {
        return latestUpdateNotificationMessageID;
    }


    /**
     * Sets the message ID of the latest update notification that was sent to the observing CoAP endpoint
     *
     * @param latestUpdateNotificationMessageID the message ID of the latest update notification that was sent to the
     *                                          observing CoAP endpoint
     */
    public void setLatestUpdateNotificationMessageID(int latestUpdateNotificationMessageID) {
        this.latestUpdateNotificationMessageID = latestUpdateNotificationMessageID;
    }

}
