package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;

import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;

/**
 * A {@link RetransmissionSchedule} represents the schedule regarding the retransmissions of a confirmable
 * {@link CoapMessage}.
 *
 * @author Oliver Kleine
 */
public class RetransmissionSchedule {
    private LinkedList<ScheduledFuture> retransmissionFutures = new LinkedList<ScheduledFuture>();
    private ScheduledFuture timeoutNotificationFuture;

    private CoapMessage coapMessage;


    public boolean addRetransmissionFuture(ScheduledFuture future) {
        return retransmissionFutures.add(future);
    }

    public LinkedList<ScheduledFuture> getRetransmissionFutures() {
        return retransmissionFutures;
    }

    public CoapMessage getCoapMessage() {
        return coapMessage;
    }

    public void setCoapMessage(CoapMessage coapMessage) {
        this.coapMessage = coapMessage;
    }

    public ScheduledFuture getTimeoutNotificationFuture() {
        return timeoutNotificationFuture;
    }

    public void setTimeoutNotificationFuture(ScheduledFuture timeoutNotificationFuture) {
        this.timeoutNotificationFuture = timeoutNotificationFuture;
    }
}
