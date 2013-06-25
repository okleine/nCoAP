package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import java.net.InetSocketAddress;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;

/**
 * Instances are sent upstream by the {@link OutgoingMessageReliabilityHandler} whenever there was a retransmission
 * of a confirmable {@link CoapMessage}.
 *
 * @author Oliver Kleine
 */
public class InternalMessageRetransmissionMessage {

    private InetSocketAddress remoteAddress;
    private ByteArrayWrapper token;

    /**
     * @param remoteAddress the recipient of the retransmitted message
     * @param token the token of the retransmitted message
     */
    public InternalMessageRetransmissionMessage(InetSocketAddress remoteAddress, byte[] token) {
        this.remoteAddress = remoteAddress;
        this.token = new ByteArrayWrapper(token);
    }

    /**
     * Returns the recipient of the retransmitted message
     * @return the recipient of the retransmitted message
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns the token of the retransmitted message
     * @return the token of the retransmitted message
     */
    public ByteArrayWrapper getToken() {
        return token;
    }

    @Override
    public String toString(){
        return "InternalMessageRetransmissionMessage: " + remoteAddress + " (remote address), "
                + token + " (token)";
    }
}
