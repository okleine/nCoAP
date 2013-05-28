package de.uniluebeck.itm.spitfire.nCoap.communication.observe;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.Option;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.ACCEPT;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 23.05.13
 * Time: 17:05
 * To change this template use File | Settings | File Templates.
 */
class ObservationParameter {

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
    }

    public byte[] getToken() {
        return token;
    }
}
