package de.uniluebeck.itm.ncoap.communication.reliability;

import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 12.11.13
 * Time: 13:37
 * To change this template use File | Settings | File Templates.
 */
public class ReliabilityHandler extends SimpleChannelHandler {

    public static final int ACK_TIMEOUT_MILLIS = 2000;
    public static final double ACK_RANDOM_FACTOR = 1.5;
    public static final int MAX_RETRANSMIT = 4;
    public static final int NSTART = 1;


}
