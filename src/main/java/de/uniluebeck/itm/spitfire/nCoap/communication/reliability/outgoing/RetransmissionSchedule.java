package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;

import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;

/**
 * A {@link RetransmissionSchedule} represents the schedule regarding the retransmissions of a confirmable
 * {@link CoapMessage}.
 *
 * @author Oliver Kleine
 */
public class RetransmissionSchedule {

    private static Logger log = LoggerFactory.getLogger(RetransmissionSchedule.class.getName());

    private LinkedList<ScheduledFuture> retransmissionFutures = new LinkedList<ScheduledFuture>();
    private ScheduledFuture timeoutNotificationFuture;
    private CoapMessage coapMessage;

    public boolean addRetransmissionFuture(ScheduledFuture retransmissionFuture) {
        return retransmissionFutures.add(retransmissionFuture);
    }

    public RetransmissionSchedule(CoapMessage coapMessage){
        this.coapMessage = coapMessage;
    }

    /**
     * The retransmission futures represent the state of the scheduled retransmissions for the {@link CoapMessage}
     * instance accessable using {@link #getCoapMessage()}.
     *
     * @return a {@link LinkedList<ScheduledFuture>} instance containing the futures represent the states of the
     * scheduled retransmissions
     */
    public LinkedList<ScheduledFuture> getRetransmissionFutures() {
        return retransmissionFutures;
    }

    /**
     * A shortcut for {@link #getCoapMessage().getToken()}
     * @return the byte array representing the token of the {@link CoapMessage} this {@link RetransmissionSchedule} is
     * for
     */
    public byte[] getToken(){
        return coapMessage.getToken();
    }

    /**
     * Returns the {@link CoapMessage} instance this {@link RetransmissionSchedule} is for
     * @return the {@link CoapMessage} instance this {@link RetransmissionSchedule} is for
     */
    public CoapMessage getCoapMessage() {
        return coapMessage;
    }

    /**
     * Sets the {@link CoapMessage} instance this schedule is for, i.e. this is the message to be retransmitted
     * according to this {@link RetransmissionSchedule} instance.
     *
     * @param coapMessage the {@link CoapMessage} instance this schedule is for
     */
    public void setCoapMessage(CoapMessage coapMessage) {
        this.coapMessage = coapMessage;
    }

    /**
     * Sets the {@link ScheduledFuture} instance to represent the state of the task to send a timeout notification
     * to the message initiator,  i.e. the local {@link CoapClientApplication} or
     * {@link CoapServerApplication} instance.
     *
     * The task itself causes the nCoAP framework to invoke the method
     * {@link RetransmissionTimeoutHandler#handleRetransmissionTimeout(RetransmissionTimeoutMessage)}.
     *
     * @param timeoutNotificationFuture
     */
    public void setTimeoutNotificationFuture(ScheduledFuture timeoutNotificationFuture) {
        this.timeoutNotificationFuture = timeoutNotificationFuture;
    }

    /**
     * Tries to cancel all remaining retransmission tasks and the timeout notification for the initiating instance, i.e.
     * the origin of the message which is either the local {@link CoapClientApplication} or
     * {@link CoapServerApplication} instance.
     */
    public void stopScheduledTasks(){
        int counter = 0;
        for(ScheduledFuture retransmissionFuture : retransmissionFutures){
            if(retransmissionFuture.cancel(false))
                counter++;
        }

        log.debug("Canceled {} retransmissions of {}.", counter, coapMessage);

        if(timeoutNotificationFuture.cancel(false))
            log.debug("Canceled timeout notification for {}.", coapMessage);
    }
}
