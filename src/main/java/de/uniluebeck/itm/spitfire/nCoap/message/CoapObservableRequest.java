package de.uniluebeck.itm.spitfire.nCoap.message;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;

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

    public void notifyObserver(CoapNotificationResponse coapResponse) {
        try {
            coapResponse.setObserveOptionResponse(sequenceNumber++);
            coapResponse.setMessageID(coapRequest.getMessageID());
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
    }

    public CoapRequest getCoapRequest() {
        return coapRequest;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }


}
