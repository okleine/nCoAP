package de.uniluebeck.itm.spitfire.nCoap.communication.blockwise;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 12.09.12
 * Time: 17:50
 * To change this template use File | Settings | File Templates.
 */
public class BlockwiseTransferHandler extends SimpleChannelHandler{

    private static BlockwiseTransferHandler instance = new BlockwiseTransferHandler();

    private BlockwiseTransferHandler(){

    }

    public static BlockwiseTransferHandler getInstance(){
        return instance;
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        super.writeRequested(ctx, me);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) throws Exception {
        super.messageReceived(ctx, me);
    }
}
