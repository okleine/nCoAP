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
package de.uzl.itm.ncoap.communication.blockwise.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.primitives.Bytes;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.client.BlockwiseResponseTransferFailedEvent;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.ResponseBlockReceivedEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>The {@link ClientBlock2Handler} handles the {@link Option#BLOCK_2} for
 * {@link de.uzl.itm.ncoap.application.client.CoapClient}s. The {@link de.uzl.itm.ncoap.application.client.CoapClient},
 * resp. the {@link de.uzl.itm.ncoap.application.client.ClientCallback} does not need to deal with any blockwise
 * transfer details for responses with content. This automatically handled by the {@link ClientBlock2Handler}.</p>

 * <p>The full payload (the cumulative blocks) is then set as the payload of the latest response. Only this response
 * (with full payload) is sent further upstream, i.e. to be processed by the
 * {@link de.uzl.itm.ncoap.application.client.ClientCallback}. Thus, from the
 * {@link de.uzl.itm.ncoap.application.client.ClientCallback}s perspective there is virtually no
 * difference between a blockwise transfer and a large payload in a single response.</p>
 *
 * @author Oliver Kleine
 */
public class ClientBlock2Handler extends AbstractCoapChannelHandler implements TokenReleasedEvent.Handler,
        RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ClientBlock2Handler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, ClientBlock2Helper> block2HelperTable;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of {@link ClientBlock2Handler}
     *
     * @param executor the {@link ScheduledExecutorService} for I/O operations
     */
    public ClientBlock2Handler(ScheduledExecutorService executor) {
        super(executor);
        this.block2HelperTable = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

        LOG.debug("HANDLE INBOUND MESSAGE: {}", coapMessage);

        if (coapMessage instanceof CoapResponse && coapMessage.getBlock2Szx() != UintOptionValue.UNDEFINED) {
            return handleInboundCoapResponseWithBlock2((CoapResponse) coapMessage, remoteSocket);
        } else {
            return true;
        }
    }

    private boolean handleInboundCoapResponseWithBlock2(CoapResponse coapResponse, InetSocketAddress remoteSocket) {

        Token token = coapResponse.getToken();
        ChannelBuffer responseBlock = coapResponse.getContent();
        byte[] etag = coapResponse.getEtag();

        byte[] received = addResponseBlock(remoteSocket, token, etag, responseBlock);

        if (received.length == 0) {
            LOG.error("Blockwise response transfer failed!");
            triggerEvent(new BlockwiseResponseTransferFailedEvent(remoteSocket, token), false);
            return false;
        }

        if (!coapResponse.isLastBlock2()) {
            long block2num = coapResponse.getBlock2Number();
            long block2szx = coapResponse.getBlock2Szx();

            long receivedLength = received.length;
            long expectedLength = coapResponse.getSize2();

            // fire internal event
            triggerEvent(new ResponseBlockReceivedEvent(remoteSocket, token, receivedLength, expectedLength), false);

            // send next request
            final CoapRequest nextRequest = getRequestForResponseBlock(remoteSocket, token, block2num + 1, block2szx);
            LOG.debug("Send CoAP request: {}", nextRequest);
            ChannelFuture future = sendCoapMessage(nextRequest, remoteSocket);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    LOG.debug("CoAP request sent: {}", nextRequest);
                }
            });

            return false;
        } else {
            coapResponse.setContent(received);
            resetHelper(remoteSocket, token);
            return true;
        }
    }

    private byte[] addResponseBlock(InetSocketAddress remoteSocket, Token token, byte[] etag,
                                           ChannelBuffer responsePayloadBlock) {
        try {
            this.lock.writeLock().lock();
            ClientBlock2Helper helper = this.block2HelperTable.get(remoteSocket, token);
            if (helper != null) {
                byte[] result =  helper.addResponseBlock(responsePayloadBlock, etag);
                LOG.debug("Payload (Concatenation of blocks received so far):\n{}", new String(result, CoapMessage.CHARSET));
                return result;
            } else {
//                return ChannelBuffers.EMPTY_BUFFER;
                return new byte[0];
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private CoapRequest getRequestForResponseBlock(InetSocketAddress remoteSocket, Token token,
                                                   long block2num, long block2szx) {
        try {
            this.lock.readLock().lock();
            ClientBlock2Helper helper = this.block2HelperTable.get(remoteSocket, token);
            if (helper != null) {
                return helper.getCoapRequestForResponseBlock(block2num, block2szx);
            } else {
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapRequest) {
            handleOutboundCoapRequest((CoapRequest) coapMessage, remoteSocket);
        }
        return true;
    }

    private void handleOutboundCoapRequest(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        // add new request to receive blockwise responses
        addHelper(coapRequest, remoteSocket);
    }


    private ClientBlock2Helper addHelper(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        try {
            this.lock.writeLock().lock();
            ClientBlock2Helper clientBlock2Helper = new ClientBlock2Helper(coapRequest);
            this.block2HelperTable.put(remoteSocket, coapRequest.getToken(), clientBlock2Helper);
            return clientBlock2Helper;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void resetHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            ClientBlock2Helper clientBlock2Helper = this.block2HelperTable.get(remoteSocket, token);
            if (clientBlock2Helper != null) {
                clientBlock2Helper.reset();
                LOG.debug("BLOCK2 helper reseted (Remote Socket: {}, Token: {})", remoteSocket, token);
            } else {
                LOG.debug("No BLOCK2 helper found to be reseted (Remote Socket: {}, Token: {})", remoteSocket, token);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void removeHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            if (this.block2HelperTable.remove(remoteSocket, token) == null) {
                LOG.debug("No BLOCK2 helper found to be removed (Remote Socket: {}, Token: {})", remoteSocket, token);
            } else {
                LOG.debug("Successfully removed BLOCK2 helper (Remote Socket: {}, Token: {})", remoteSocket, token);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void handleEvent(RemoteServerSocketChangedEvent event) {
        InetSocketAddress previous = event.getPreviousRemoteSocket();
        Token token = event.getToken();
        try {
            this.lock.readLock().lock();
            if (!this.block2HelperTable.contains(previous, token)) {
                return;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        InetSocketAddress actual = event.getRemoteSocket();
        try {
            this.lock.writeLock().lock();
            ClientBlock2Helper helper = this.block2HelperTable.remove(previous, token);
            this.block2HelperTable.put(event.getRemoteSocket(), token, helper);
            LOG.debug("Successfully updated remote socket (previous: {}, actual: {})", previous, actual);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        removeHelper(event.getRemoteSocket(), event.getToken());
    }


    private class ClientBlock2Helper {

        private CoapRequest coapRequest;
        private byte[] etag = null;
        private byte[] responseBlocks;

        private ClientBlock2Helper(CoapRequest coapRequest) {
            this.coapRequest = coapRequest;
            this.responseBlocks = new byte[0];
        }

        private void reset() {
            this.etag = null;
            this.responseBlocks = new byte[0];
        }

        private byte[] addResponseBlock(ChannelBuffer buffer, byte[] etag) {
            if (this.etag != null && etag == null) {
                // previous response block had an ETAG but current block has no ETAG
                return new byte[0];
            } else if (this.etag == null || Arrays.equals(this.etag, etag)) {
                // current block has same ETAG as previous blocks or previous blocks did not provide an ETAG
                this.etag = etag;
                byte[] block = new byte[buffer.readableBytes()];
                buffer.getBytes(0, block, 0, block.length);
                this.responseBlocks = Bytes.concat(this.responseBlocks, block);
                return this.responseBlocks;
            } else {
                return new byte[0];
//                return ChannelBuffers.EMPTY_BUFFER;
            }
        }


        private CoapRequest getCoapRequestForResponseBlock(long block2num, long block2szx) {
            if (block2num > 0) {
                coapRequest.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
                coapRequest.setContent(ChannelBuffers.EMPTY_BUFFER);
                coapRequest.removeOptions(Option.CONTENT_FORMAT);
                coapRequest.removeOptions(Option.BLOCK_1);
                coapRequest.setBlock2(block2num, block2szx);
            }

            return this.coapRequest;
        }
    }
}
