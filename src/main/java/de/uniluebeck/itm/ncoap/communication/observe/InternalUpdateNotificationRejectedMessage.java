package de.uniluebeck.itm.ncoap.communication.observe;

import java.net.InetSocketAddress;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.ncoap.message.header.MsgType;

/**
 * This internal message is created and send upstream when an observer of an {@link ObservableWebService} instance
 * rejects the reception of an update notification with a {@link MsgType#RST}.
 *
 * @author Oliver Kleine
 */
public class InternalUpdateNotificationRejectedMessage {

    private InetSocketAddress observerAddress;
    private String servicePath;

    /**
     * @param observerAddress the address of the rejecting observer
     * @param servicePath the path of the {@link ObservableWebService} instance whose update notification was rejected
     */
    public InternalUpdateNotificationRejectedMessage(InetSocketAddress observerAddress, String servicePath){
        this.observerAddress = observerAddress;
        this.servicePath = servicePath;
    }

    /**
     * Returns the address of the observer that rejected the update notification
     * @return the address of the observer that rejected the update notification
     */
    public InetSocketAddress getObserverAddress() {
        return observerAddress;
    }

    /**
     * Returns the path of the service whose update notification was rejected
     * @return the path of the service whose update notification was rejected
     */
    public String getServicePath() {
        return servicePath;
    }

    @Override
    public String toString(){
        return "[InternalUpdateNotificationRejectedMessage] Remote address " + observerAddress +
                ", observable web service " + servicePath + ".";
    }

}
