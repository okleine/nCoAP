package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.application.client.*;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;

/**
 * Interface to be implemented by instances of {@link CoapResponseProcessor} that want to somehow know about each
 * transmission attempt of the related {@link CoapRequest}, e.g. in order to print it on screen or log it.
 *
 * @author Oliver Kleine
 */

public interface RetransmissionProcessor {

    /**
     * Method invoked by the {@link CoapClientApplication} for each attempt to send the {@link de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest} related
     * to this {@link CoapResponseProcessor}.
     *
     * This happens 5 times at maximum, once for the original request and 4 times for retransmission attempts if the
     * {@link de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest} was confirmable.
     */
    public void requestSent();
}
