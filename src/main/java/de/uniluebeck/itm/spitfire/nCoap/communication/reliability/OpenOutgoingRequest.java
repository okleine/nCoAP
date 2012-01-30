package de.uniluebeck.itm.spitfire.nCoap.communication.reliability;

import de.uniluebeck.itm.spitfire.nCoap.communication.handler.OutgoingMessageReliabilityHandler;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;

import java.net.InetSocketAddress;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 30.01.12
 * Time: 14:41
 * To change this template use File | Settings | File Templates.
 */
public class OpenOutgoingRequest extends OpenRequest {
    private static Random random = new Random(System.currentTimeMillis());

    private double randomFactor;
    private int retransmissionCount;

    public OpenOutgoingRequest(InetSocketAddress rcptSocketAddress, Message message){
        super(rcptSocketAddress, message);
        this.randomFactor = 0.5 + 0.5 * random.nextDouble();
        this.retransmissionCount = 0;
        setNextTransmitTime();
    }

    @Override
    public void setNextTransmitTime() {
        nextTransmitTime = (long) (System.currentTimeMillis() +
            OutgoingMessageReliabilityHandler.RESPONSE_TIMEOUT * randomFactor * Math.pow(2, retransmissionCount));
    }

    public int getRetransmissionCount() {
        return retransmissionCount;
    }

    public void increaseRetransmissionCount() {
        retransmissionCount += 1;
    }

}
