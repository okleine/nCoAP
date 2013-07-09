package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;

/**
 * Interface to be implemented by instances of {@link CoapResponseProcessor} to get informed if an outgoing
 * {@link CoapRequest} was neither acknowledged nor reseted by the intended recipient.
 *
 * @author Oliver Kleine
 */
public interface RetransmissionTimeoutProcessor {

    /**
     * Method invoked by the nCoAP framework when there was an outgoing
     */
    public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage);
}
