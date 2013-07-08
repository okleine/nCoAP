package de.uniluebeck.itm.ncoap.communication.blockwise;

import de.uniluebeck.itm.examples.performance.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.message.CoapResponse;

/**
 * The nCoAP framework only gives complete payload to the application, The
 * {@link CoapResponseProcessor#processCoapResponse(CoapResponse)} method is invoked when the data to be processed
 * is complete, i.e. all blocks are received.
 *
 * If the remote host decided to send its payload blockwise and one wants to be informed about the reception of
 * a new block, the {@link CoapResponseProcessor} instance must additionally implement
 * {@link InternalNextBlockReceivedMessageProcessor}.
 *
 * @author Oliver Kleine
 */
public interface InternalNextBlockReceivedMessageProcessor {

    /**
     * This message is invoked by the nCoAP framework whenever a new block of incoming data was received.
     *
     * <b>Note:</b> Implementing classes should e.g. have a counter to count the invokations of this method. It
     * provides no information about the number of blocks still to be received or any information on the payload.
     */
    public void receivedNextBlock();
}
