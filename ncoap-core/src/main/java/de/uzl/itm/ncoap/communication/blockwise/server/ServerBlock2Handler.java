package de.uzl.itm.ncoap.communication.blockwise.server;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uzl.itm.ncoap.message.MessageCode.*;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 12.04.16.
 */
public class ServerBlock2Handler extends AbstractCoapChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(ServerBlock2Handler.class.getName());

    private BlockSize maxBlock2Size;
    private HashBasedTable<InetSocketAddress, Token, Block2Helper> block2Helpers;
    private ReentrantReadWriteLock lock;

    public ServerBlock2Handler(ScheduledExecutorService executor, BlockSize maxBlock2Size) {
        super(executor);
        this.maxBlock2Size = maxBlock2Size;
        this.block2Helpers = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if(coapMessage instanceof CoapRequest && coapMessage.getBlock2Szx() != BlockSize.UNDEFINED) {
            return handleInboundCoapRequestWithBlock2((CoapRequest) coapMessage, remoteSocket);
        } else {
            return true;
        }
    }


    private boolean handleInboundCoapRequestWithBlock2(CoapRequest coapRequest, final InetSocketAddress remoteSocket) {

        Block2Helper helper = this.getBlock2Helper(remoteSocket, coapRequest.getToken());
        if(helper == null && coapRequest.getBlock2Number() > 0) {
            sendPreconditionFailed(coapRequest, remoteSocket);
            return false;
        }
        else if (helper != null && coapRequest.getBlock2Number() > 0){
            // determine actual BLOCK 2 number according to (new) BLOCK 2 szx
            long block2Num = helper.getBlock2Szx() == BlockSize.UNDEFINED ? coapRequest.getBlock2Number() :
                getNextBlockNumber(coapRequest.getBlock2Number(), helper.getBlock2Szx(), coapRequest.getBlock2Szx());
            long block2Szx = coapRequest.getBlock2Szx();

            // create response with representation portion
            int messageID = coapRequest.getMessageID();
            final CoapResponse coapResponse = helper.getResponseWithPayloadBlock(messageID, block2Num, block2Szx);
            coapResponse.setMessageID(coapRequest.getMessageID());

            ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    LOG.debug("Sent response to {}: {}", remoteSocket, coapResponse);
                }
            });

            // delete blockwise transfer after last block
            if(coapResponse.isLastBlock2()) {
                this.removeHelper(remoteSocket, coapResponse.getToken());
            }

            return false;
        } else {
            return true;
        }
    }


    private void sendPreconditionFailed(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        final CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), PRECONDITION_FAILED_412);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        String message = "Request for " + coapRequest.getBlock2Number() + " without prior request for block 0";
        coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.debug("Sent {}: {}", MessageCode.asString(PRECONDITION_FAILED_412), coapResponse);
            }
        });
    }

    private Block2Helper getBlock2Helper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            return this.block2Helpers.get(remoteSocket, token);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if(coapMessage instanceof CoapResponse) {
            // set the BLOCK 2 option if necessary and not yet present
            if (coapMessage.getContentLength() > this.maxBlock2Size.getSize() &&
                    coapMessage.getBlock2Szx() == UintOptionValue.UNDEFINED) {
                ((CoapResponse) coapMessage).setPreferedBlock2Size(this.maxBlock2Size);
            }

            // handle responses with BLOCK 2 option
            if (coapMessage.containsOption(Option.BLOCK_2)) {
                handleOutboundCoapResponseWithBlock2((CoapResponse) coapMessage, remoteSocket);
                return false;
            }
        }

        return true;
    }


    private void handleOutboundCoapResponseWithBlock2(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
        if(coapResponse.getBlock2Size() > this.maxBlock2Size.getSize()) {
            coapResponse.setPreferedBlock2Size(this.maxBlock2Size);
        }

        Block2Helper helper = addHelper(coapResponse, remoteSocket);
        long block2Szx = BlockSize.min(coapResponse.getBlock2Szx(), helper.getBlock2Szx());
        int messageID = coapResponse.getMessageID();

        final CoapResponse firstBlock = helper.getResponseWithPayloadBlock(messageID, 0L, block2Szx);
        ChannelFuture future = sendCoapMessage(firstBlock, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.debug("Sent response block #{}: {}", firstBlock.getBlock2Number(), firstBlock);
            }
        });

        if(firstBlock.isLastBlock2()) {
            this.removeHelper(remoteSocket, firstBlock.getToken());
        }
    }


    private Block2Helper addHelper(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
        try {
            this.lock.writeLock().lock();
            // add new response to be sent blockwise
            Block2Helper helper = new Block2Helper(coapResponse);
            this.block2Helpers.put(remoteSocket, coapResponse.getToken(), helper);
            return helper;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void removeHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            // remove response to be sent blockwise
            if(this.block2Helpers.remove(remoteSocket, token) != null) {
                LOG.debug("Removed response blocks (remote socket: {}, token: {})", remoteSocket, token);
            } else {
                LOG.warn("Could not remove response blocks (remote socket: {}, token: {})", remoteSocket, token);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private static long getNextBlockNumber(long acknowledgedNumber, long oldSZX, long newSZX) {
        return getNextBlockNumber(acknowledgedNumber, BlockSize.getBlockSize(oldSZX), BlockSize.getBlockSize(newSZX));
    }

    private static long getNextBlockNumber(long acknowledgedNumber, BlockSize oldSize, BlockSize newSize) {
        return oldSize.getSize() * acknowledgedNumber / newSize.getSize();
    }

    private class Block2Helper {

        private long block2Szx;
        private CoapResponse coapResponse;
        private ChannelBuffer completeRepresentation;


        public Block2Helper(CoapResponse coapResponse) {
            this.coapResponse = coapResponse;
            this.completeRepresentation = coapResponse.getContent();

            // determine initial BLOCK 2 size
            long block2Szx = coapResponse.getBlock2Szx();
            if(block2Szx == UintOptionValue.UNDEFINED || block2Szx >= maxBlock2Size.getSzx()) {
                this.block2Szx = maxBlock2Size.getSzx();
            } else {
                this.block2Szx = block2Szx;
            }

            // set the SIZE 2 option (length of complete representation in bytes)
            this.coapResponse.setSize2(this.completeRepresentation.readableBytes());
        }

        public long getBlock2Szx() {
            return this.block2Szx;
        }

        public CoapResponse getResponseWithPayloadBlock(int messageID, long block2Num, long block2Szx) {

            this.coapResponse.setMessageID(messageID);

            // set block 2 option and proper payload
            int block2Size = BlockSize.getSize(block2Szx);
            int startIndex = (int) block2Num * block2Size;
            int remaining = this.completeRepresentation.readableBytes() - startIndex;
            boolean block2more = (remaining > block2Size);
            this.coapResponse.setBlock2(block2Num, block2more, block2Szx);

            //set the payload block
            if(block2more) {
                this.coapResponse.setContent(this.completeRepresentation.slice(startIndex, block2Size));
            } else {
                this.coapResponse.setContent(this.completeRepresentation.slice(startIndex, remaining));
            }

            return coapResponse;
        }
    }
}
