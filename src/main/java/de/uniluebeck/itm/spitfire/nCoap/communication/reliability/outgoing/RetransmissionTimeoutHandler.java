package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.EncodingFailedException;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutException;
import de.uniluebeck.itm.spitfire.nCoap.message.InvalidMessageException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.jboss.netty.channel.ExceptionEvent;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 30.11.12
 * Time: 15:34
 * To change this template use File | Settings | File Templates.
 */
public interface RetransmissionTimeoutHandler {

    public void handleRetransmissionTimout();


}
