package de.uzl.itm.ncoap.communication.blockwise.server;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 11.04.16.
 */
public class ServerBlock1Handler extends AbstractCoapChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(ServerBlock1Handler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, ChannelBuffer> previousRequestBlocks;
    private ReentrantReadWriteLock lock;
    private BlockSize defaultBlock1Size;

    public ServerBlock1Handler(ScheduledExecutorService executor, BlockSize defaultBlock1Size){
        super(executor);
        this.defaultBlock1Size = defaultBlock1Size;
        this.previousRequestBlocks = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if(coapMessage instanceof CoapRequest) {
            if (coapMessage.getBlock1SZX() != UintOptionValue.UNDEFINED) {
                return handleInboundCoapRequestWithBlock1((CoapRequest) coapMessage, remoteSocket);
            } else if (this.defaultBlock1Size != BlockSize.UNBOUND &&
                    coapMessage.getContentLength() > this.defaultBlock1Size.getDecodedSize()) {
                // request content is larger than maximum block size
                sendRequestEntityTooLarge((CoapRequest) coapMessage, remoteSocket);
                return false;
            }
        }

        return true;
    }


    private boolean handleInboundCoapRequestWithBlock1(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        if (!containsExpectedBlock(coapRequest, remoteSocket)) {
            this.removeRequestBlocks(remoteSocket, coapRequest.getToken());
            return false;
        } else {
            ChannelBuffer content = addRequestBlock(coapRequest, remoteSocket);
            if (!coapRequest.isLastBlock1()) {
                sendContinueResponse(coapRequest, remoteSocket);
                return false;
            } else {
                this.removeRequestBlocks(remoteSocket, coapRequest.getToken());
                coapRequest.setContent(content);
                return true;
            }
        }
    }


    private ChannelBuffer addRequestBlock(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        try {
            this.lock.writeLock().lock();

            // lookup previous blocks and append actual block
            Token token = coapRequest.getToken();
            ChannelBuffer previousBlocks = this.previousRequestBlocks.get(remoteSocket, token);
            if(previousBlocks == null) {
                previousBlocks = coapRequest.getContent();
            } else {
                previousBlocks = ChannelBuffers.wrappedBuffer(previousBlocks, coapRequest.getContent());
            }
            this.previousRequestBlocks.put(remoteSocket, token, previousBlocks);

            return previousBlocks;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void removeRequestBlocks(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            if(this.previousRequestBlocks.remove(remoteSocket, token) != null) {
                LOG.debug("Removed previous request blocks (remote socket: {}, token: {})", remoteSocket, token);
            } else {
                LOG.warn("No previous request blocks found (remote socket: {}, token: {})", remoteSocket, token);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void sendRequestEntityTooLarge(CoapRequest coapRequest, final InetSocketAddress remoteSocket) {
        // create error response
        int messageType = coapRequest.getMessageType();
        final CoapResponse coapResponse = new CoapResponse(messageType, MessageCode.REQUEST_ENTITY_TOO_LARGE_413);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        // set options and content (error message)
        coapResponse.setSize1(this.defaultBlock1Size.getDecodedSize());
        String message = "Try blockwise request transfer (" + this.defaultBlock1Size.getDecodedSize() + " per block)";
        coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        // send response
        ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.warn("Sent ERROR response to {}: {}", remoteSocket, coapResponse);
            }
        });
    }

    private boolean containsExpectedBlock(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        try {
            this.lock.readLock().lock();
            ChannelBuffer previousBlocks = this.previousRequestBlocks.get(remoteSocket, coapRequest.getToken());
            if(previousBlocks == null) {
                return true;
            } else {
                long block1num = coapRequest.getBlock1Number();
                boolean expected = block1num == (previousBlocks.readableBytes() / coapRequest.getBlock1Size());
                if(!expected) {
                    sendEntityIncompleteResponse(coapRequest, remoteSocket, previousBlocks.readableBytes());
                }
                return expected;
            }
        } finally {
            this.lock.readLock().unlock();
        }

    }

    private void sendEntityIncompleteResponse(CoapRequest coapRequest, final InetSocketAddress remoteSocket,
                                              int previousBytes) {

        final CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
                MessageCode.REQUEST_ENTITY_INCOMPLETE_408);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        String message = "BLOCK1 option out of sequence (NUM: " + coapRequest.getBlock1Number() +
                ", SZX: " + coapRequest.getBlock1SZX() + " (i.e. " + coapRequest.getBlock1Size() + " byte)" +
                ", previously received " + previousBytes + " byte)";
        coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        // send response
        ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.warn("Sent REQUEST_ENTITY_INCOMPLETE response to {}: {}", remoteSocket, coapResponse);
            }
        });
    }

    private void sendContinueResponse(CoapRequest coapRequest, final InetSocketAddress remoteSocket) {
        final CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTINUE_231);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        if(defaultBlock1Size == null || defaultBlock1Size.getEncodedSize() > coapRequest.getBlock1SZX()) {
            coapResponse.setBlock1(coapRequest.getBlock1Number(), coapRequest.getBlock1SZX());
        } else {
            coapResponse.setBlock1(coapRequest.getBlock1Number(), defaultBlock1Size.getEncodedSize());
        }

        // send response
        ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.warn("Sent CONTINUE response to {}: {}", remoteSocket, coapResponse);
            }
        });
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        // nothing to do...
        return true;
    }
}
