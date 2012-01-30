package de.uniluebeck.itm.spitfire.nCoap.communication.reliability;

import de.uniluebeck.itm.spitfire.nCoap.message.Message;

import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 30.01.12
 * Time: 14:42
 * To change this template use File | Settings | File Templates.
 */
public class OpenIncomingRequest extends OpenRequest {

    public OpenIncomingRequest(InetSocketAddress rcptSocketAddress, Message message){
        super(rcptSocketAddress, message);
    }

    @Override
    public void setNextTransmitTime() {
        this.nextTransmitTime = System.currentTimeMillis() + 2000;
    }
}
