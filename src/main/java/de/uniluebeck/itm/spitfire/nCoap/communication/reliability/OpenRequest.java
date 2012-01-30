package de.uniluebeck.itm.spitfire.nCoap.communication.reliability;

import de.uniluebeck.itm.spitfire.nCoap.message.Message;

import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 30.01.12
 * Time: 14:22
 * To change this template use File | Settings | File Templates.
 */
public abstract class OpenRequest{

    private InetSocketAddress rcptSocketAddress;
    private Message message;
    protected long nextTransmitTime;


    public OpenRequest(InetSocketAddress rcptSocketAddress, Message message){
        this.message = message;
        this.rcptSocketAddress = rcptSocketAddress;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public long getNextTransmitTime(){
        return nextTransmitTime;
    }

    public InetSocketAddress getRcptSocketAddress() {
        return rcptSocketAddress;
    }

    public void setRcptSocketAddress(InetSocketAddress rcptSocketAddress) {
        this.rcptSocketAddress = rcptSocketAddress;
    }

    public abstract void setNextTransmitTime();

}
