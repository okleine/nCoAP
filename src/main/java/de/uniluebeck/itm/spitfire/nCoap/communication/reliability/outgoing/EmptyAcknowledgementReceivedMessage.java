package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 27.08.12
 * Time: 18:22
 * To change this template use File | Settings | File Templates.
 */
public class EmptyAcknowledgementReceivedMessage {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ByteArrayWrapper token;

    public EmptyAcknowledgementReceivedMessage(byte[] token){
        this.token = new ByteArrayWrapper(token);
        log.info("Internal acknowledgement message created for token " + token);
    }

    public ByteArrayWrapper getToken(){
        return token;
    }
}
