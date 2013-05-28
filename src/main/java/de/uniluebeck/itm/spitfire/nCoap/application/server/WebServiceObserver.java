package de.uniluebeck.itm.spitfire.nCoap.application.server;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 25.05.13
 * Time: 15:35
 * To change this template use File | Settings | File Templates.
 */
class WebServiceObserver {

    private InetSocketAddress observerAddress;
    private byte[] token;
    private MediaType mediaType;
    private int notificationCount = 1;

    public WebServiceObserver(InetSocketAddress observerAddress, byte[] token, MediaType mediaType){
        this.observerAddress = observerAddress;
        this.token = token;
        this.mediaType = mediaType;
    }

    WebServiceObserver(InetSocketAddress observerAddress){
        this.observerAddress = observerAddress;
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    public void increaseNotificationCount() {
        this.notificationCount++;
    }

    InetSocketAddress getObserverAddress() {
        return observerAddress;
    }

    byte[] getToken() {
        return this.token;
    }

    MediaType getMediaType() {
        return this.mediaType;
    }

    @Override
    public int hashCode(){
        return observerAddress.hashCode();
    }

    @Override
    public boolean equals(Object object){

        //Check for null
        if(object == null)
            return false;

        //Check if object is instance of WebServiceObserver
        if(!(object instanceof WebServiceObserver))
            return false;

        WebServiceObserver other = (WebServiceObserver) object;
        //Compare addresses
        if(!observerAddress.equals(((WebServiceObserver) object).getObserverAddress()))
            return false;

        return true;

    }
}
