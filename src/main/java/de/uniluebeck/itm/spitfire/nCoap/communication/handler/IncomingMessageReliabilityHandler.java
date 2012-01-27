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
 * Time: 10:35
 * To change this template use File | Settings | File Templates.
 */
public class IncomingMessageReliabilityHandler extends SimpleChannelHandler {

    private static IncomingMessageReliabilityHandler instance = new IncomingMessageReliabilityHandler();

    private HashMultimap<InetSocketAddress, byte[]> openRequests = HashMultimap.create();

    public static IncomingMessageReliabilityHandler getInstance(){
        return instance;
    }

    private IncomingMessageReliabilityHandler(){

    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        //TODO: Memorize token in open requests
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{
        if(!(me.getMessage() instanceof Message)){
            super.messageReceived(ctx, me);
        }

        //TODO: Delete token from open requests
    }
}
