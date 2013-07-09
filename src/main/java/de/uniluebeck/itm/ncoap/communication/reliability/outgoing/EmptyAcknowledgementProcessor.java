package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;

/**
 * Interface to be implemented by instances of {@link CoapResponseProcessor} to get the information that a
 * request was received by a server but that the server needs some more time to proceed the request.
 *
 * @author Oliver Kleine
 */
public interface EmptyAcknowledgementProcessor {

    /**
     * This method is invoked for an incoming empty
     * acknowledgement. If the client application is e.g. a browser, one could e.g. display a message in the
     * browser windows telling the user that the server has received the request but needs some time to
     * process it.
     */
    public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message);
}
