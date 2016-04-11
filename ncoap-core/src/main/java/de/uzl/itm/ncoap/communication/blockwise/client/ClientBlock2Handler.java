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
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This handler is only used on the client side and deals with blockwise transfers in a rather simple manner.
 * If a response contains the BLOCK2 option (i.e. just a portion of the complete payload) the client accepts the
 * block size (as given in the SZX part of the response option) and requests the next blocks with the same size until
 * the full payload was received (as indicated by the M part of the (final) response option).
 *
 * The full payload (the cumulative blocks) is then set as the payload of the latest response. Only this response
 * (with full payload) is sent further upstream, i.e. to be processed by the
 * {@link de.uzl.itm.ncoap.application.client.ClientCallback}. Thus, from the
 * {@link de.uzl.itm.ncoap.application.client.ClientCallback}s perspective there is virtually no
 * difference between a blockwise transfer and a large payload in a single response.
 *
 * @author Oliver Kleine
 */
public class ClientBlock2Handler extends AbstractCoapChannelHandler implements TokenReleasedEvent.Handler,
        RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ClientBlock2Handler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, Block2Helper> block2helpers;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of {@link ClientBlock2Handler}.
     */
    public ClientBlock2Handler(ScheduledExecutorService executor){
        super(executor);
        this.block2helpers = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if(coapMessage instanceof CoapResponse && coapMessage.getBlock2SZX() != UintOptionValue.UNDEFINED) {
            return handleInboundCoapResponseWithBlock2((CoapResponse) coapMessage, remoteSocket);
        } else {
            return true;
        }
    }

    private boolean handleInboundCoapResponseWithBlock2(CoapResponse coapResponse, InetSocketAddress remoteSocket) {

        ChannelBuffer payload = addResponseBlock(remoteSocket, coapResponse.getToken(), coapResponse.getContent());

        if(!coapResponse.isLastBlock2()) {
            Token token = coapResponse.getToken();
            long block2num = coapResponse.getBlock2Number();
            long block2szx = coapResponse.getBlock2SZX();

            // fire internal event
            triggerEvent(new ResponseBlockReceivedEvent(remoteSocket, token, block2num), false);

            // send next request
            final CoapRequest nextRequest = getRequestForResponseBlock(remoteSocket, token, block2num + 1, block2szx);
            ChannelFuture future = sendCoapMessage(nextRequest, remoteSocket);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    LOG.debug("Sent CoAP request (BLOCK): {}", nextRequest);
                }
            });

            return false;
        } else {
            coapResponse.setContent(payload);
            return true;
        }
    }

    private ChannelBuffer addResponseBlock(InetSocketAddress remoteSocket, Token token,
                                           ChannelBuffer responsePayloadBlock) {
        try {
            this.lock.writeLock().lock();
            Block2Helper helper = this.block2helpers.get(remoteSocket, token);
            if(helper != null) {
                return helper.addResponseBlock(responsePayloadBlock);
            } else {
                return null;
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private CoapRequest getRequestForResponseBlock(InetSocketAddress remoteSocket, Token token,
                                                   long block2num, long block2szx) {
        try {
            this.lock.readLock().lock();
            Block2Helper helper = this.block2helpers.get(remoteSocket, token);
            if(helper != null) {
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
        if (coapMessage instanceof CoapRequest && coapMessage.getBlock2SZX() != UintOptionValue.UNDEFINED) {
            handleOutboundCoapRequestWithBlock2((CoapRequest) coapMessage, remoteSocket);
        }
        return true;
    }

    private void handleOutboundCoapRequestWithBlock2(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        // add new request to receive blockwise responses
        addHelper(coapRequest, remoteSocket);
    }


    private Block2Helper addHelper(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        try {
            this.lock.writeLock().lock();
            Block2Helper block2helper = new Block2Helper(coapRequest);
            this.block2helpers.put(remoteSocket, coapRequest.getToken(), block2helper);
            return block2helper;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void removeHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            if(this.block2helpers.remove(remoteSocket, token) == null) {
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
            if(!this.block2helpers.contains(previous, token)) {
                return;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        InetSocketAddress actual = event.getRemoteSocket();
        try {
            this.lock.writeLock().lock();
            Block2Helper helper = this.block2helpers.remove(previous, token);
            this.block2helpers.put(event.getRemoteSocket(), token, helper);
            LOG.debug("Successfully updated remote socket (previous: {}, actual: {})", previous, actual);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        removeHelper(event.getRemoteSocket(), event.getToken());
    }


    private class Block2Helper {

        private CoapRequest coapRequest;
        private ChannelBuffer responseBlocks;

        private Block2Helper(CoapRequest coapRequest) {
            this.coapRequest = coapRequest;
            this.responseBlocks = ChannelBuffers.EMPTY_BUFFER;
        }


        private ChannelBuffer addResponseBlock(ChannelBuffer buffer) {
            this.responseBlocks = ChannelBuffers.wrappedBuffer(responseBlocks, buffer);
            return this.responseBlocks;
        }


        private ChannelBuffer getResponseBlocks() {
            return this.responseBlocks;
        }

        private CoapRequest getCoapRequestForResponseBlock(long block2num, long block2szx) {
            if(block2num > 0) {
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
