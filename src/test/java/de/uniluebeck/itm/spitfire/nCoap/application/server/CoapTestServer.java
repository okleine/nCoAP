package de.uniluebeck.itm.spitfire.nCoap.application.server;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import java.net.InetSocketAddress;
import java.util.*;

public class CoapTestServer extends CoapServerApplication {

    private HashBasedTable<InetSocketAddress, ByteArrayWrapper, Long> timeoutMessages = HashBasedTable.create();

    private Map<Long, Integer> requestReceptionTimes =
            Collections.synchronizedMap(new TreeMap<Long, Integer>());

    public CoapTestServer(int numberOfThreads, int serverPort){
        super(numberOfThreads, serverPort);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent me){
        if(me.getMessage() instanceof CoapRequest)
            requestReceptionTimes.put(System.currentTimeMillis(), ((CoapRequest) me.getMessage()).getMessageID());

        super.messageReceived(ctx, me);
    }

    public Map<Long, Integer> getRequestReceptionTimes(){
        return this.requestReceptionTimes;
    }

    @Override
    public void processRetransmissionTimeout(RetransmissionTimeoutMessage timeoutMessage) {
        timeoutMessages.put(timeoutMessage.getRemoteAddress(), new ByteArrayWrapper(timeoutMessage.getToken()),
                System.currentTimeMillis());
    }
}
