package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of {@link ObservationParameter} contain meta-information about a running observation of
 * a local resource by a (remote) observer.
 *
 * @author Oliver Kleine
 */
class ObservationParameter {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private byte[] token;
    private MediaType acceptedMediaType;
    private int notificationCount = 0;

    /**
     * @param token The token to be included in every update notification for the observer
     */
    public ObservationParameter(byte[] token){
        this.token = token;
    }

    /**
     * Returns the {@link MediaType} for the observation. The payload of all update notifications for the
     * observer must have this {@link MediaType}.
     *
     * @return the {@link MediaType} for the observation
     */
    public MediaType getAcceptedMediaType() {
        if(acceptedMediaType != null){
            return acceptedMediaType;
        }
        return null;
    }

    /**
     * Set the {@link MediaType} for the observation. The payload of all update notifications for the
     * observer must have this {@link MediaType}.
     *
     * @param acceptedMediaType the {@link MediaType} for the observation
     */
    public void setAcceptedMediaType(MediaType acceptedMediaType){
        this.acceptedMediaType = acceptedMediaType;
    }

    /**
     * Returns the number of update notifications already sent to the observer.
     * @return the number of update notifications already sent to the observer.
     */
    public int getNotificationCount() {
        return notificationCount;
    }

    /**
     * Increases the notification count for this observation by 1.
     */
    public void increaseNotificationCount() {
        this.notificationCount++;
        log.debug("Notificaton count set to {}.", notificationCount);
    }

    /**
     * Returns the token to be included in every update notification for the observer
     * @return the token to be included in every update notification for the observer
     */
    public byte[] getToken() {
        return token;
    }
}
