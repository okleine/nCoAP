package de.uniluebeck.itm.ncoap.communication.blockwise;

import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;

/**
 * Instances of {@link InternalNextBlockReceivedMessage} are created by the {@link BlockwiseTransferHandler} if
 * the data to be processed by the recipient is split up on several messages.
 *
 * This could, e.g. be the case if a client sends a request to the service and the server holding this service
 * is located in constrained environment in terms of bandwith or MTU size. Thus, the server splits up the response
 * on several responses.
 *
 * @author Oliver Kleine
 */
public class InternalNextBlockReceivedMessage {

    private byte[] token;

    /**
      * @param token the token that was contained in the received message containing partial payload
     */
    public InternalNextBlockReceivedMessage(byte[] token) {
        this.token = token;
    }

    /**
     * Returns the token of the not yet completely received message
     * @return the token of the not yet completely received message
     */
    public ByteArrayWrapper getToken(){
        return new ByteArrayWrapper(this.token);
    }
}
