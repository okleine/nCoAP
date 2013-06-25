package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

/**
 * Interface to handle not-acknowledged outgoing confirmable messages.
 */
public interface RetransmissionTimeoutProcessor {

    /**
     * Method invoked by the nCoAP framework when there was an outgoing
     */
    public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage);
}
