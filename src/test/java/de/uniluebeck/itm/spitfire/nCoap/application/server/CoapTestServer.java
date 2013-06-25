package de.uniluebeck.itm.spitfire.nCoap.application.server;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import java.net.InetSocketAddress;
import java.util.*;

public class CoapTestServer extends CoapServerApplication {

    private HashBasedTable<InetSocketAddress, ByteArrayWrapper, Long> timeoutMessages = HashBasedTable.create();

    private Map<Integer, Long> requestReceptionTimes = Collections.synchronizedMap(new TreeMap<Integer, Long>());

    public CoapTestServer(int serverPort){
        super(serverPort);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent me){
        if(me.getMessage() instanceof CoapRequest){
           requestReceptionTimes.put(((CoapRequest) me.getMessage()).getMessageID(), System.currentTimeMillis());
        }

        super.messageReceived(ctx, me);

    }

    public Map<Integer, Long> getRequestReceptionTimes(){
        return this.requestReceptionTimes;
    }
}
