package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import java.net.InetSocketAddress;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.toolbox.Tools;

/**
 * Instances of {@link InternalRetransmissionTimeoutMessage} are sent upstream by the {@link OutgoingMessageReliabilityHandler}
 * if a {@link CoapMessage} of type {@link MsgType#CON} was not acknowledged despite the maximum number of
 * retransmission attempts.
 */
public class InternalRetransmissionTimeoutMessage {

    private byte[] token;
    private InetSocketAddress remoteAddress;

    /**
     * @param token a byte[] representing the token of the outgoing confirmable {@link CoapMessage} that was not
     *              acknowledged by the recipient
     * @param remoteAddress the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that
     *                      did not acknowledge the reception
     */
    public InternalRetransmissionTimeoutMessage(byte[] token, InetSocketAddress remoteAddress){
        this.token = token;
        this.remoteAddress = remoteAddress;
    }

    /**
     * Returns the token of the outgoing confirmable {@link CoapMessage} that was not acknowledged by the recipient
     * @return the token of the outgoing confirmable {@link CoapMessage} that was not acknowledged by the recipient
     */
    public byte[] getToken() {
        return token;
    }

    /**
     * Returns the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that did not
     * acknowledge the reception.
     * @return the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that did not
     * acknowledge the reception.
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public String toString(){
        return "InternalRetransmissionTimeoutMessage: " + remoteAddress + " (remote address), "
                + Tools.toHexString(token) + " (token)";
    }
}
