package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import java.net.InetSocketAddress;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;

/**
 * Instances of {@link RetransmissionTimeoutMessage} are sent upstream by the {@link OutgoingMessageReliabilityHandler}
 * if a {@link CoapMessage} of type {@link MsgType#CON} was not acknowledged despite the maximum number of
 * retransmission attempts.
 */
public class RetransmissionTimeoutMessage{

    private byte[] token;
    private InetSocketAddress remoteAddress;

    /**
     * @param token a byte[] representing the token of the outgoing confirmable {@link CoapMessage} that was not
     *              acknowledged by the recipient
     * @param remoteAddress the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that
     *                      did not acknowledge the reception
     */
    public RetransmissionTimeoutMessage(byte[] token, InetSocketAddress remoteAddress){
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
        return "RetransmissionTimeoutMessage: " + remoteAddress + " (remote address), "
                + Tools.toHexString(token) + " (token)";
    }
}
