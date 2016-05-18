/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
 * <p>The {@link ServerBlock2Handler} handles the {@link Option#BLOCK_2} for
 * {@link de.uzl.itm.ncoap.application.server.CoapServer}s. The {@link de.uzl.itm.ncoap.application.server.CoapServer},
 * resp. the {@link de.uzl.itm.ncoap.application.server.resource.Webresource} does not need to deal with any blockwise
 * transfer details for responses with content. This is automatically handled by the {@link ServerBlock2Handler}.</p>
 *
 * <p>This is particularly useful for resources with frequently changing states. The {@link ServerBlock2Handler}
 * ensures that all response blocks refer to the resource state the time of the first block.</p>
 *
 * @author Oliver Kleine
 */
public class ServerBlock2Handler extends AbstractCoapChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(ServerBlock2Handler.class.getName());

    private BlockSize maxBlock2Size;
    private HashBasedTable<InetSocketAddress, Token, ServerBlock2Helper> block2Helpers;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of {@link ServerBlock2Handler}
     *
     * @param executor the {@link ScheduledExecutorService} for I/O operations
     * @param maxBlock2Size the maximum {@link BlockSize} for outbound {@link CoapResponse}s
     */
    public ServerBlock2Handler(ScheduledExecutorService executor, BlockSize maxBlock2Size) {
        super(executor);
        this.maxBlock2Size = maxBlock2Size;
        this.block2Helpers = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapRequest && coapMessage.getBlock2Szx() != BlockSize.UNDEFINED) {
            return handleInboundCoapRequestWithBlock2((CoapRequest) coapMessage, remoteSocket);
        } else {
            return true;
        }
    }


    private boolean handleInboundCoapRequestWithBlock2(CoapRequest coapRequest, final InetSocketAddress remoteSocket) {

        ServerBlock2Helper helper = this.getBlock2Helper(remoteSocket, coapRequest.getToken());

        if (helper == null && coapRequest.getBlock2Number() > 0) {
            writePreconditionFailedResponse(coapRequest, remoteSocket);
            return false;
        } else if (helper != null && coapRequest.getBlock2Number() > 0) {

            // determine next BLOCK 2 number according to (possibly changed) BLOCK 2 SZX
            long block2Num;
            long block2Szx;
            if (helper.getBlock2Szx() == BlockSize.UNDEFINED || helper.getBlock2Szx() == coapRequest.getBlock2Szx()) {
                block2Num = coapRequest.getBlock2Number();
                block2Szx = coapRequest.getBlock2Szx();
            } else {
                BlockSize oldSize = BlockSize.getBlockSize(helper.getBlock2Szx());
                BlockSize newSize = BlockSize.getBlockSize(coapRequest.getBlock2Szx());
                if (newSize.getSize() > oldSize.getSize()) {
                    // this is for "buggy" clients that try to request a larger block size than previously negotiated
                    newSize = oldSize;
                }
                block2Num = oldSize.getSize() * coapRequest.getBlock2Number() / newSize.getSize();
                block2Szx = newSize.getSzx();
            }

            // send response with next representation portion
            int messageID = coapRequest.getMessageID();
            helper.writeResponseWithPayloadBlock(messageID, block2Num, block2Szx);
            return false;
        } else {
            return true;
        }
    }


    private void writePreconditionFailedResponse(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        final CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), PRECONDITION_FAILED_412);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        String message = "Request for block " + coapRequest.getBlock2Number() + " without prior request for block 0";
        coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.debug("Sent {}: {}", MessageCode.asString(PRECONDITION_FAILED_412), coapResponse);
            }
        });
    }


    private ServerBlock2Helper getBlock2Helper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            return this.block2Helpers.get(remoteSocket, token);
        } finally {
            this.lock.readLock().unlock();
        }
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapResponse) {
            // set the BLOCK 2 option if necessary and not yet present
            if (coapMessage.getContentLength() > this.maxBlock2Size.getSize() &&
                    coapMessage.getBlock2Szx() == UintOptionValue.UNDEFINED) {
                ((CoapResponse) coapMessage).setPreferredBlock2Size(this.maxBlock2Size);
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
        if (coapResponse.getBlock2Size() > this.maxBlock2Size.getSize()) {
            coapResponse.setPreferredBlock2Size(this.maxBlock2Size);
        }

        ServerBlock2Helper helper = addHelper(coapResponse, remoteSocket);
        try {
            long block2Szx = BlockSize.min(coapResponse.getBlock2Szx(), helper.getBlock2Szx());
            int messageID = coapResponse.getMessageID();

            helper.writeResponseWithPayloadBlock(messageID, 0L, block2Szx);
        } catch (IllegalArgumentException ex) {
            LOG.error("This should never happen!", ex);
            throw ex;
        }
    }


    private ServerBlock2Helper addHelper(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
        try {
            this.lock.writeLock().lock();
            // add new response to be sent blockwise
            ServerBlock2Helper helper = new ServerBlock2Helper(coapResponse, remoteSocket);
            this.block2Helpers.put(remoteSocket, coapResponse.getToken(), helper);
            LOG.debug("Added Block2 Helper (Remote Socket: {}, Token: {})", remoteSocket, coapResponse.getToken());
            return helper;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void removeHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            // remove response to be sent blockwise
            if (this.block2Helpers.remove(remoteSocket, token) != null) {
                LOG.debug("Removed response blocks (remote socket: {}, token: {})", remoteSocket, token);
            } else {
                LOG.warn("Could not remove response blocks (remote socket: {}, token: {})", remoteSocket, token);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private class ServerBlock2Helper {

        private long block2Szx;
        private CoapResponse coapResponse;
        private ChannelBuffer completeRepresentation;
        private InetSocketAddress remoteSocket;

        public ServerBlock2Helper(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
            this.remoteSocket = remoteSocket;
            this.coapResponse = coapResponse;
            this.completeRepresentation = coapResponse.getContent();

            // determine initial BLOCK 2 size
            long block2Szx = coapResponse.getBlock2Szx();
            if (block2Szx == UintOptionValue.UNDEFINED || block2Szx >= maxBlock2Size.getSzx()) {
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

        public void writeResponseWithPayloadBlock(int messageID, long block2Num, long block2Szx) {

            this.coapResponse.setMessageID(messageID);

            // set block 2 option and proper payload
            int block2Size = BlockSize.getSize(block2Szx);
            int startIndex = (int) block2Num * block2Size;
            int remaining = this.completeRepresentation.readableBytes() - startIndex;
            boolean block2more = (remaining > block2Size);
            this.coapResponse.setBlock2(block2Num, block2more, block2Szx);

            //set the payload block
            if (block2more) {
                this.coapResponse.setContent(this.completeRepresentation.slice(startIndex, block2Size));
            } else {
                this.coapResponse.setContent(this.completeRepresentation.slice(startIndex, remaining));
            }

            ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    LOG.debug("Sent response to {}: {}", remoteSocket, coapResponse);
                }
            });

            // delete blockwise transfer after last block
            if (coapResponse.isLastBlock2()) {
                removeHelper(remoteSocket, coapResponse.getToken());
            }
        }
    }
}
