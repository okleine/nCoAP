package de.uniluebeck.itm.spitfire.nCoap.communication.handler;

import com.google.common.collect.HashMultimap;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 16.01.12
 * Time: 10:48
 * To change this template use File | Settings | File Templates.
 */
public class IncomingMessageDuplicateHandler extends SimpleChannelHandler {

    private static IncomingMessageDuplicateHandler instance = new IncomingMessageDuplicateHandler();

    private HashMultimap<InetSocketAddress, Integer> receivedMessages = HashMultimap.create();

    private IncomingMessageDuplicateHandler(){
        //Thread to send empty ACKs if there wasn't a response from the application yet

    }

    public IncomingMessageDuplicateHandler getInstance(){
        return instance;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        ctx.sendUpstream(me);
        //TODO memorize message IDs of incoming messages and only forward non duplicates
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        ctx.sendDownstream(me);
    }


}
