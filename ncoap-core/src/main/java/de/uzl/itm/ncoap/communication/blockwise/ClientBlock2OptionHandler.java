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
package de.uzl.itm.ncoap.communication.blockwise;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.ResponseBlockReceivedEvent;
import de.uzl.itm.ncoap.communication.events.client.TokenReleasedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 07.12.15.
 */
public class ClientBlock2OptionHandler extends AbstractCoapChannelHandler implements TokenReleasedEvent.Handler,
        RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(AbstractCoapChannelHandler.class.getName());

    private BlockSize maxBlockSize;

    // Outbound requests with no or incomplete response, yet
    private HashBasedTable<InetSocketAddress, Token, CoapRequest> outboundRequests;
    private HashBasedTable<InetSocketAddress, Token, ChannelBuffer> incompleteResponses;

    private ReentrantReadWriteLock lock;

    
    public ClientBlock2OptionHandler(ScheduledExecutorService executorService) {
        this(executorService, BlockSize.UNBOUND);
    }


    public ClientBlock2OptionHandler(ScheduledExecutorService executorService, BlockSize maxBlockSize) {
        super(executorService);
        this.maxBlockSize = maxBlockSize;
        this.outboundRequests = HashBasedTable.create();
        this.incompleteResponses = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteEndpoint) {
        if(coapMessage instanceof CoapResponse){
            return handleInboundCoapResponse((CoapResponse) coapMessage, remoteEndpoint);
        }
        return true;
    }

    private boolean handleInboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteEndpoint) {
        if(!coapResponse.isLastBlock2()) {
            Token token = coapResponse.getToken();

            // trigger event to indicate reception of a "partial" response
            ResponseBlockReceivedEvent event = new ResponseBlockReceivedEvent(remoteEndpoint, token,
                    coapResponse.getBlock2Number());
            this.triggerEvent(event, false);

            this.addResponsePayloadBlock(remoteEndpoint, token, coapResponse.getContent());

            // request next block
            long number = coapResponse.getBlock2Number() + 1;
            BlockSize size = BlockSize.getBlockSize(coapResponse.getBlock2EncodedSize());
            this.sendRequestForNextBlock(remoteEndpoint, token, number, size);

            return false;
        } else {
            Token token = coapResponse.getToken();
            this.removeCoapRequest(remoteEndpoint, token);
            ChannelBuffer payload = this.removeIncompleteResponse(remoteEndpoint, token);
            if(payload != null) {
                coapResponse.setContent(ChannelBuffers.wrappedBuffer(payload, coapResponse.getContent()));
            }
            return true;
        }
    }

    private void sendRequestForNextBlock(InetSocketAddress remoteEndpoint, Token token, long number, BlockSize size) {
        // send request for next block
        CoapRequest coapRequest = this.getCoapRequest(remoteEndpoint, token);
        coapRequest.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);

        // set block 2 option value
        coapRequest.setBlock2(number, false, size);

        ChannelFuture future = Channels.future(this.getContext().getChannel());
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    LOG.info("Successfully sent request for next block.");
                } else {
                    LOG.error("Error: {}", future.getCause());
                }
            }
        });

        Channels.write(this.getContext(), future, coapRequest, remoteEndpoint);
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteEndpoint) {

        if(coapMessage instanceof CoapRequest) {
            addCoapRequest(remoteEndpoint, (CoapRequest) coapMessage);
            if(this.maxBlockSize != BlockSize.UNBOUND) {
                ((CoapRequest) coapMessage).setBlock2(0, false, maxBlockSize);
            }
        }

        return true;
    }


    private void addCoapRequest(InetSocketAddress remoteEndpoint, CoapRequest coapRequest){
        try{
            this.lock.writeLock().lock();
            this.outboundRequests.put(remoteEndpoint, coapRequest.getToken(), coapRequest);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private CoapRequest getCoapRequest(InetSocketAddress remoteEndpoint, Token token){
        try{
            this.lock.readLock().lock();
            CoapRequest coapRequest = this.outboundRequests.get(remoteEndpoint, token);
            if(coapRequest == null) {
                LOG.error("No partial payload available (remote endpoint: {}, token: {}).", remoteEndpoint, token);
            }
            return coapRequest;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private CoapRequest removeCoapRequest(InetSocketAddress remoteEndpoint, Token token){
        try{
            this.lock.writeLock().lock();
            CoapRequest coapRequest = this.outboundRequests.remove(remoteEndpoint, token);
            if(coapRequest == null) {
                LOG.warn("No CoAP Request available (remote endpoint: {}, token: {}).", remoteEndpoint, token);
            }
            return coapRequest;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void addResponsePayloadBlock(InetSocketAddress remoteEndpoint, Token token, ChannelBuffer block) {
        try {
            this.lock.writeLock().lock();
            if(!this.incompleteResponses.contains(remoteEndpoint, token)) {
                this.incompleteResponses.put(remoteEndpoint, token, block);
                LOG.info("Added new partial payload (remote endpoint: {}, token: {}).", remoteEndpoint, token);
            } else {
                ChannelBuffer payload = this.incompleteResponses.get(remoteEndpoint, token);
                this.incompleteResponses.put(remoteEndpoint, token, ChannelBuffers.wrappedBuffer(payload, block));
                LOG.info("Added new block to partial payload (remote endpoint: {}, token: {})", remoteEndpoint, token);
            }

        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private ChannelBuffer removeIncompleteResponse(InetSocketAddress remoteEndpoint, Token token) {
        try{
            this.lock.writeLock().lock();
            ChannelBuffer buffer = this.incompleteResponses.remove(remoteEndpoint, token);
            if(buffer == null) {
                LOG.warn("No incomplete response available (remote endpoint: {}, token: {}).", remoteEndpoint, token);
            }
            return buffer;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        removeCoapRequest(event.getRemoteSocket(), event.getToken());
        removeIncompleteResponse(event.getRemoteSocket(), event.getToken());
    }

    @Override
    public void handleEvent(RemoteServerSocketChangedEvent event) {
        InetSocketAddress previousRemoteEndpoint = event.getPreviousRemoteSocket();
        Token token = event.getToken();

        try {
            this.lock.writeLock().lock();
            CoapRequest coapRequest = removeCoapRequest(previousRemoteEndpoint, token);
            if(coapRequest == null) {
                LOG.error("No CoAP Request available (remote endpoint: {}, token: {}).", previousRemoteEndpoint, token);
            } else {
                InetSocketAddress remoteEndpoint = event.getRemoteSocket();
                addCoapRequest(remoteEndpoint, coapRequest);
            }

            ChannelBuffer incompleteResponse = removeIncompleteResponse(previousRemoteEndpoint, token);
            if(incompleteResponse == null) {
                LOG.error("No incomplete response available (remote endpoint: {}, token: {}).", previousRemoteEndpoint, token);
            } else {
                InetSocketAddress remoteEndpoint = event.getRemoteSocket();
                addResponsePayloadBlock(remoteEndpoint, token, incompleteResponse);
            }

        } finally {
            this.lock.writeLock().unlock();
        }
    }

}
