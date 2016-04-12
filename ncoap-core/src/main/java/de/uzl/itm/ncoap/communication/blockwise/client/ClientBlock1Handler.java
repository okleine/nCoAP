/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
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
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 07.12.15.
 */
public class ClientBlock1Handler extends AbstractCoapChannelHandler implements TokenReleasedEvent.Handler,
        RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ClientBlock1Handler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, Block1Helper> block1helpers;
    private ReentrantReadWriteLock lock;

    public ClientBlock1Handler(ScheduledExecutorService executorService) {
        super(executorService);
        this.block1helpers = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapResponse && coapMessage.getBlock1SZX() != UintOptionValue.UNDEFINED) {
            return handleInboundCoapResponseWithBlock1((CoapResponse) coapMessage, remoteSocket);
        } else {
            return true;
        }
    }

    private boolean handleInboundCoapResponseWithBlock1(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
        if(coapResponse.getMessageCode() == MessageCode.CONTINUE_231) {
            Token token = coapResponse.getToken();
            long block1num = coapResponse.getBlock1Number() + 1;
            long block1szx = coapResponse.getBlock1SZX();
            final CoapRequest nextRequest = getCoapRequestWithPayloadBlock(remoteSocket, token, block1num, block1szx);
            if(nextRequest != null) {
                nextRequest.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
                ChannelFuture future = sendCoapMessage(nextRequest, remoteSocket);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(future.isSuccess()) {
                            LOG.debug("Sent CoAP request: {}", nextRequest);
                        }
                    }
                });
            } else {
                LOG.warn("No blockwise outbound request found (Remote Socket: {}, Token: {})", remoteSocket, token);
            }
            return false;
        } else {
            return true;
        }
    }

    private CoapRequest getCoapRequestWithPayloadBlock(InetSocketAddress remoteSocket, Token token, long block1num,
                                                       long block1szx) {

        try {
            this.lock.readLock().lock();
            Block1Helper helper = this.block1helpers.get(remoteSocket, token);
            if (helper != null) {
                if(helper.getBlock1SZX() > block1szx) {
                    int oldSize = BlockSize.getSize(helper.getBlock1SZX());
                    int newSize = BlockSize.getSize(block1szx);
                    block1num =  oldSize * block1num / newSize;
                }
                return helper.getCoapRequestWithPayloadBlock(block1num , block1szx);
            } else {
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if(coapMessage instanceof CoapRequest && coapMessage.getBlock1SZX() != UintOptionValue.UNDEFINED) {
            handleOutboundCoapRequestWithBlock1((CoapRequest) coapMessage, remoteSocket);
            return false;
        } else {
            return true;
        }
    }

    private void handleOutboundCoapRequestWithBlock1(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        // add new request to be sent blockwise
        Block1Helper helper = addHelper(coapRequest, remoteSocket);

        // send first block
        final CoapRequest firstBlock = helper.getCoapRequestWithPayloadBlock(0L, coapRequest.getBlock1SZX());
        ChannelFuture future = sendCoapMessage(firstBlock, remoteSocket);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.debug("Sent CoAP request (BLOCK): {}", firstBlock);
            }
        });

    }

    private Block1Helper addHelper(CoapRequest coapRequest, InetSocketAddress remoteSocket) {

        try {
            this.lock.writeLock().lock();
            Block1Helper block1helper = new Block1Helper(coapRequest);
            this.block1helpers.put(remoteSocket, coapRequest.getToken(), block1helper);
            return block1helper;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void removeHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            if(this.block1helpers.remove(remoteSocket, token) == null) {
                LOG.debug("No BLOCK1 helper found to be removed (Remote Socket: {}, Token: {})", remoteSocket, token);
            } else {
                LOG.debug("Successfully removed BLOCK1 helper (Remote Socket: {}, Token: {})", remoteSocket, token);
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
            if(!this.block1helpers.contains(previous, token)) {
                return;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        InetSocketAddress actual = event.getRemoteSocket();
        try {
            this.lock.writeLock().lock();
            Block1Helper helper = this.block1helpers.remove(previous, token);
            this.block1helpers.put(event.getRemoteSocket(), token, helper);
            LOG.debug("Successfully updated remote socket (previous: {}, actual: {})", previous, actual);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        removeHelper(event.getRemoteSocket(), event.getToken());
    }


    private class Block1Helper {

        private long block1szx;
        private CoapRequest coapRequest;
        private ChannelBuffer completePayload;
        private long block2szx;

        private Block1Helper(CoapRequest coapRequest) {
            this.coapRequest = coapRequest;
            this.completePayload = coapRequest.getContent();
            this.block2szx = coapRequest.getBlock2Szx();
            if(this.block2szx != UintOptionValue.UNDEFINED) {
                this.coapRequest.removeOptions(Option.BLOCK_2);
            }
            this.block1szx = coapRequest.getBlock1SZX();

            // set size 1 option (size of complete payload in bytes)
            this.coapRequest.setSize1(coapRequest.getContentLength());
        }

        public long getBlock1SZX() {
            return this.block1szx;
        }

        public CoapRequest getCoapRequestWithPayloadBlock(long block1num, long block1szx) {

            this.block1szx = block1szx;
            int block1Size = BlockSize.getSize(block1szx);

            // set block 1 option and proper payload
            int startIndex = (int) block1num * block1Size;
            int remaining = completePayload.readableBytes() - startIndex;
            boolean block1more = (remaining > block1Size);
            this.coapRequest.setBlock1(block1num, block1more, block1szx);

            //set the payload block
            if(block1more) {
                this.coapRequest.setContent(this.completePayload.slice(startIndex, block1Size));
            } else {
                this.coapRequest.setContent(this.completePayload.slice(startIndex, remaining));
                if(this.block2szx != UintOptionValue.UNDEFINED) {
                    this.coapRequest.setBlock2(0L, this.block2szx);
                }
            }

            return coapRequest;
        }
    }
}
