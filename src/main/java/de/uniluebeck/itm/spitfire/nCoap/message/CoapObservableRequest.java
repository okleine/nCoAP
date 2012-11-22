package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.MessageIDFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import java.net.InetSocketAddress;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapObservableRequest {

    private static Logger log = LoggerFactory.getLogger(CoapObservableRequest.class.getName());

    CoapRequest coapRequest;
    InetSocketAddress remoteAddress;
    DatagramChannel channel;

    int sequenceNumber = 1;

    public CoapObservableRequest(CoapRequest coapRequest, InetSocketAddress remoteAddress, DatagramChannel channel) {
        this.coapRequest = coapRequest;
        this.remoteAddress = remoteAddress;
        this.channel = channel;
    }

    public synchronized int notifyObserver(CoapNotificationResponse coapResponse) {
        int responseMessageID = -1;
        try {
            if (sequenceNumber == 1) {
                //is first notification
                responseMessageID = coapRequest.getMessageID();
                coapResponse.getHeader().setMsgType(MsgType.ACK);
                coapResponse.setMessageID(responseMessageID);
            } else {
                //response is 2nd notification or later
                responseMessageID = MessageIDFactory.nextMessageID();
                coapResponse.setMessageID(responseMessageID);
                coapResponse.getHeader().setMsgType(MsgType.CON);
            }
            coapResponse.setObserveOptionResponse(sequenceNumber++);
            if(coapRequest.getToken().length > 0){
                coapResponse.setToken(coapRequest.getToken());
            }
            //Write response
            ChannelFuture future = Channels.write(channel, coapResponse, remoteAddress);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.debug("CoapRequestExecutor] Sending of observe notification to recipient " +
                        remoteAddress + " with message ID " + coapRequest.getMessageID() + " and " +
                        "token " + Tools.toHexString(coapRequest.getToken()) + " completed.");
                }
            });
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.error("Exception while creating observable-request response. This should " +
                    " never happen!", e);
            e.printStackTrace();
        }
        return responseMessageID;
    }

    public CoapRequest getCoapRequest() {
        return coapRequest;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }


}
