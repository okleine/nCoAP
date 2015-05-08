package de.uniluebeck.itm.ncoap.communication.blockwise;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.communication.observing.ResourceStatusAge;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 08.05.15.
 */
public class Block2OptionHandler extends SimpleChannelHandler{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private HashBasedTable<InetSocketAddress, Token, CoapRequest> openRequests;
    private HashBasedTable<InetSocketAddress, Token, ChannelBuffer> partialResponses;

    private ReentrantReadWriteLock lock;

    public Block2OptionHandler(){
        this.openRequests = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapRequest){
            handleOutgoingCoapRequest(ctx, me);
        }
        else{
            ctx.sendDownstream(me);
        }
    }

    private void handleOutgoingCoapRequest(ChannelHandlerContext ctx, MessageEvent me) {
        CoapRequest coapRequest = (CoapRequest) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        if(addCoapRequest(remoteEndpoint, coapRequest)){
            ctx.sendDownstream(me);
        }
        else{
            //TODO: failed!
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapResponse){
            
        }
    }

    private boolean addCoapRequest(InetSocketAddress remoteEndpoint, CoapRequest coapRequest){
        Token token = coapRequest.getToken();

        try{
            this.lock.readLock().lock();
            if(this.openRequests.contains(remoteEndpoint, token)){
                log.error("Tried to override existing conversation (remote endpoint: {}, token: {}).",
                        remoteEndpoint, token);
                return false;
            }
        }
        finally{
            this.lock.readLock().unlock();
        }

        try{
            this.lock.writeLock().lock();
            if(this.openRequests.contains(remoteEndpoint, token)){
                log.error("Tried to override existing conversation (remote endpoint: {}, token: {}).",
                        remoteEndpoint, token);
            }

            else{
                this.openRequests.put(remoteEndpoint, token, coapRequest);
                log.info("New conversation added (remote endpoint: {}, token: {})", remoteEndpoint, token);
            }
        }
        finally{
            this.lock.writeLock().unlock();
        }
    }

}
