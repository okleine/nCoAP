package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances are sent upstream (i.e. to the application) by the {@link OutgoingMessageReliabilityHandler}
 * when there was an empty acknowledgement received indicating that a recipient received a a confirmable
 * message.
 *
 * @author Oliver Kleine
 */
public class InternalEmptyAcknowledgementReceivedMessage {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ByteArrayWrapper token;

    /**
     * @param token the token of the confirmed message
     */
    public InternalEmptyAcknowledgementReceivedMessage(byte[] token){
        this.token = new ByteArrayWrapper(token);
        log.debug("Internal acknowledgement message created for token {}.", Tools.toHexString(token));
    }

    /**
     * Returns the token of the confirmed message
     * @return the token of the confirmed message
     */
    public ByteArrayWrapper getToken(){
        return token;
    }

    @Override
    public String toString(){
        return "InternalEmptyAcknowledgementReceivedMessage: " + token + " (token)";
    }
}
