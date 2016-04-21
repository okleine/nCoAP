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
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uzl.itm.ncoap.message.MessageCode.CONTINUE_231;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>The {@link ServerBlock2Handler} handles the {@link Option#BLOCK_2} for
 * {@link de.uzl.itm.ncoap.application.server.CoapServer}s. The {@link de.uzl.itm.ncoap.application.server.CoapServer},
 * resp. the {@link de.uzl.itm.ncoap.application.server.resource.Webresource} does not need to deal with any blockwise
 * transfer details for requests with content. This automatically handled by the {@link ServerBlock1Handler}.</p>

 * <p>The full payload (the cumulative blocks) is set as the payload of the latest request. Only this request
 * (with full payload) is sent further upstream, i.e. to be processed by the
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}. Thus, from the
 * {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s perspective there is virtually no
 * difference between a blockwise transfer and a large payload in a single request.</p>
 *
 * @author Oliver Kleine
 */
public class ServerBlock1Handler extends AbstractCoapChannelHandler {

    private static Logger LOG = LoggerFactory.getLogger(ServerBlock1Handler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, ChannelBuffer> receivedRequestBlocks;
    private ReentrantReadWriteLock lock;
    private BlockSize maxBlock1Size;

    /**
     * Creates a new instance of {@link ServerBlock1Handler}
     *
     * @param executor the {@link ScheduledExecutorService} for I/O operations
     * @param maxBlock1Size the maximum {@link BlockSize} for inbound {@link CoapRequest}s the
     * {@link de.uzl.itm.ncoap.application.server.CoapServer} is willing to process
     */
    public ServerBlock1Handler(ScheduledExecutorService executor, BlockSize maxBlock1Size) {
        super(executor);
        this.maxBlock1Size = maxBlock1Size;
        this.receivedRequestBlocks = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapRequest) {
            if (coapMessage.getBlock1Szx() != UintOptionValue.UNDEFINED) {
                return handleInboundCoapRequestWithBlock1((CoapRequest) coapMessage, remoteSocket);
            } else if (this.maxBlock1Size != BlockSize.UNBOUND &&
                    coapMessage.getContentLength() > this.maxBlock1Size.getSize()) {
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

            // lookup previously received blocks and append actual block
            Token token = coapRequest.getToken();
            ChannelBuffer receivedBlocks = this.receivedRequestBlocks.get(remoteSocket, token);
            if (receivedBlocks == null) {
                receivedBlocks = coapRequest.getContent();
            } else {
                receivedBlocks = ChannelBuffers.wrappedBuffer(receivedBlocks, coapRequest.getContent());
            }
            this.receivedRequestBlocks.put(remoteSocket, token, receivedBlocks);

            return receivedBlocks;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void removeRequestBlocks(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            if (this.receivedRequestBlocks.remove(remoteSocket, token) != null) {
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
        coapResponse.setBlock1(coapRequest.getBlock1Number(), this.maxBlock1Size.getSize());
        String message = "Try blockwise request transfer (" + this.maxBlock1Size.getSize() + " per block)";
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
            ChannelBuffer previousBlocks = this.receivedRequestBlocks.get(remoteSocket, coapRequest.getToken());
            if (previousBlocks == null) {
                return true;
            } else {
                long block1num = coapRequest.getBlock1Number();
                boolean expected = block1num == (previousBlocks.readableBytes() / coapRequest.getBlock1Size());
                if (!expected) {
                    sendEntityIncompleteResponse(coapRequest, remoteSocket, previousBlocks.readableBytes());
                }
                return expected;
            }
        } finally {
            this.lock.readLock().unlock();
        }

    }

    private void sendEntityIncompleteResponse(CoapRequest coapRequest, final InetSocketAddress remoteSocket,
                                              int receivedBytes) {

        final CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
                MessageCode.REQUEST_ENTITY_INCOMPLETE_408);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        String message = "BLOCK1 option out of sequence (NUM: " + coapRequest.getBlock1Number() +
                ", SZX: " + coapRequest.getBlock1Szx() + " (i.e. " + coapRequest.getBlock1Size() + " byte)" +
                ", previously received " + receivedBytes + " byte)";
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
        final CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), CONTINUE_231);
        coapResponse.setToken(coapRequest.getToken());
        coapResponse.setMessageID(coapRequest.getMessageID());

        if (maxBlock1Size == null || maxBlock1Size.getSzx() > coapRequest.getBlock1Szx()) {
            coapResponse.setBlock1(coapRequest.getBlock1Number(), coapRequest.getBlock1Szx());
        } else {
            coapResponse.setBlock1(coapRequest.getBlock1Number(), maxBlock1Size.getSzx());
        }

        // send response
        ChannelFuture future = sendCoapMessage(coapResponse, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Sent CONTINUE response to \"{}\": {}", remoteSocket, coapResponse);
                    } else {
                        LOG.info("Sent CONTINUE response to \"{}\".", remoteSocket);
                    }
                }
            }
        });
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        // nothing to do...
        return true;
    }
}
