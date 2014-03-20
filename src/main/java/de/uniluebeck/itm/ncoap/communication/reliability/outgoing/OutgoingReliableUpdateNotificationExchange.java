package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import de.uniluebeck.itm.ncoap.message.CoapResponse;

import java.net.InetSocketAddress;

/**
 * Created by olli on 18.03.14.
 */
public class OutgoingReliableUpdateNotificationExchange extends OutgoingReliableMessageExchange {

    private boolean changed;

    public OutgoingReliableUpdateNotificationExchange(InetSocketAddress remoteEndpoint,
                                                      CoapResponse updateNotification) {
        super(remoteEndpoint, updateNotification);
        this.changed = false;
    }


    public boolean isChanged() {
        return changed;
    }


    public void setChanged(boolean changed) {
        this.changed = changed;
    }


    @Override
    public CoapResponse getCoapMessage(){
        return (CoapResponse) super.getCoapMessage();
    }
}
