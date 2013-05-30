package de.uniluebeck.itm.spitfire.nCoap.communication.core.internal;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 27.05.13
 * Time: 21:06
 * To change this template use File | Settings | File Templates.
 */
public class UpdateNotificationRejectedMessage {

    private InetSocketAddress observerAddress;
    private String servicePath;

    public UpdateNotificationRejectedMessage(InetSocketAddress observerAddress, String servicePath){
        this.observerAddress = observerAddress;
        this.servicePath = servicePath;
    }

    public InetSocketAddress getObserverAddress() {
        return observerAddress;
    }

    public String getServicePath() {
        return servicePath;
    }

    @Override
    public String toString(){
        return "[UpdateNotificationRejectedMessage] Remote address " + observerAddress + ", observable web service " +
                servicePath + ".";
    }

}
