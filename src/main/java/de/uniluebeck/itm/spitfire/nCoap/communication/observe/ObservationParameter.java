package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 23.05.13
 * Time: 17:05
 * To change this template use File | Settings | File Templates.
 */
class ObservationParameter {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private byte[] token;
    private MediaType acceptedMediaType;
    private int notificationCount = 0;

    public ObservationParameter(byte[] token){
        this.token = token;
    }

    public MediaType getAcceptedMediaType() {
        if(acceptedMediaType != null){
            return acceptedMediaType;
        }
        return null;
    }

    public void setAcceptedMediaType(MediaType acceptedMediaType){
        this.acceptedMediaType = acceptedMediaType;
    }
    public int getNotificationCount() {
        return notificationCount;
    }

    public void increaseNotificationCount() {
        this.notificationCount++;
        log.debug("Notificaton count set to {}.", notificationCount);
    }

    public byte[] getToken() {
        return token;
    }
}
