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
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.client.ContinueResponseReceivedEvent;
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
 * The {@link ClientBlock1Handler} handles the {@link Option#BLOCK_1} for
 * {@link de.uzl.itm.ncoap.application.client.CoapClient}s. The {@link de.uzl.itm.ncoap.application.client.CoapClient},
 * resp. the {@link de.uzl.itm.ncoap.application.client.ClientCallback} does not need to deal with any blockwise
 * transfer details for requests with content. This automatically handled by the {@link ClientBlock1Handler}.
 *
 * @author Oliver Kleine
 */
public class ClientBlock1Handler extends AbstractCoapChannelHandler implements TokenReleasedEvent.Handler,
        RemoteServerSocketChangedEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ClientBlock1Handler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, ClientBlock1Helper> block1Helpers;
    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of {@link ClientBlock1Handler}
     *
     * @param executor the {@link ScheduledExecutorService} for I/O operations
     */
    public ClientBlock1Handler(ScheduledExecutorService executor) {
        super(executor);
        this.block1Helpers = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapResponse && coapMessage.getBlock1Szx() != UintOptionValue.UNDEFINED) {
            return handleInboundCoapResponseWithBlock1((CoapResponse) coapMessage, remoteSocket);
        } else {
            return true;
        }
    }

    private boolean handleInboundCoapResponseWithBlock1(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
        if (coapResponse.getMessageCode() == MessageCode.CONTINUE_231) {
            Token token = coapResponse.getToken();
            ClientBlock1Helper helper = this.getBlock1Helper(remoteSocket, token);

            // trigger event for successful request block delivery
            triggerEvent(new ContinueResponseReceivedEvent(remoteSocket, token, helper.getblock1Szx()), false);

            // determine next BLOCK 1 number according to (possibly changed) BLOCK 1 SZX
            long block1Num;
            if (helper.getblock1Szx() == coapResponse.getBlock1Szx()) {
                block1Num = coapResponse.getBlock1Number() + 1;
            } else {
                int oldSize = BlockSize.getSize(helper.getblock1Szx());
                int newSize = BlockSize.getSize(coapResponse.getBlock1Szx());
                block1Num = oldSize * (coapResponse.getBlock1Number() + 1) / newSize;
            }

            long block1Szx = coapResponse.getBlock1Szx();

            // write next request block
            helper.writeCoapRequestWithPayloadBlock(block1Num, block1Szx);
            return false;
        } else {
            return true;
        }
    }

    private ClientBlock1Helper getBlock1Helper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            return this.block1Helpers.get(remoteSocket, token);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapRequest && coapMessage.getBlock1Szx() != UintOptionValue.UNDEFINED) {
            handleOutboundCoapRequestWithBlock1((CoapRequest) coapMessage, remoteSocket);
            return false;
        } else {
            return true;
        }
    }

    private void handleOutboundCoapRequestWithBlock1(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
        // add new request to be sent blockwise
        ClientBlock1Helper helper = addHelper(coapRequest, remoteSocket);

        // send first block
        helper.writeCoapRequestWithPayloadBlock(0L, coapRequest.getBlock1Szx());
    }

    private ClientBlock1Helper addHelper(CoapRequest coapRequest, InetSocketAddress remoteSocket) {

        try {
            this.lock.writeLock().lock();
            ClientBlock1Helper clientBlock1Helper = new ClientBlock1Helper(coapRequest, remoteSocket);
            this.block1Helpers.put(remoteSocket, coapRequest.getToken(), clientBlock1Helper);
            return clientBlock1Helper;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void removeHelper(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.writeLock().lock();
            if (this.block1Helpers.remove(remoteSocket, token) == null) {
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
            if (!this.block1Helpers.contains(previous, token)) {
                return;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        InetSocketAddress actual = event.getRemoteSocket();
        try {
            this.lock.writeLock().lock();
            ClientBlock1Helper helper = this.block1Helpers.remove(previous, token);
            this.block1Helpers.put(event.getRemoteSocket(), token, helper);
            LOG.debug("Successfully updated remote socket (previous: {}, actual: {})", previous, actual);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        removeHelper(event.getRemoteSocket(), event.getToken());
    }


    private class ClientBlock1Helper {

        private long block1Szx;
        private long block2Szx;
        private InetSocketAddress remoteSocket;
        private CoapRequest coapRequest;
        private ChannelBuffer completePayload;


        private ClientBlock1Helper(CoapRequest coapRequest, InetSocketAddress remoteSocket) {
            this.remoteSocket = remoteSocket;
            this.coapRequest = coapRequest;
            this.completePayload = coapRequest.getContent();
            this.block2Szx = coapRequest.getBlock2Szx();
            if (this.block2Szx != UintOptionValue.UNDEFINED) {
                this.coapRequest.removeOptions(Option.BLOCK_2);
            }
            this.block1Szx = coapRequest.getBlock1Szx();

            // set size 1 option (size of complete payload in bytes)
            this.coapRequest.setSize1(coapRequest.getContentLength());
        }

        public long getblock1Szx() {
            return this.block1Szx;
        }

        public void writeCoapRequestWithPayloadBlock(long block1Num, long block1Szx) {

            this.block1Szx = block1Szx;
            int block1Size = BlockSize.getSize(block1Szx);

            // set block 1 option and proper payload
            int startIndex = (int) block1Num * block1Size;
            int remaining = completePayload.readableBytes() - startIndex;
            boolean block1More = (remaining > block1Size);
            this.coapRequest.setBlock1(block1Num, block1More, block1Szx);

            //set the payload block
            if (block1More) {
                this.coapRequest.setContent(this.completePayload.slice(startIndex, block1Size));
            } else {
                this.coapRequest.setContent(this.completePayload.slice(startIndex, remaining));
                if (this.block2Szx != UintOptionValue.UNDEFINED) {
                    this.coapRequest.setBlock2(0L, this.block2Szx);
                }
            }

            this.coapRequest.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
            ChannelFuture future = sendCoapMessage(this.coapRequest, this.remoteSocket);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    LOG.debug("Sent to \"{}\": {}", remoteSocket, coapRequest);
                }
            });
        }
    }
}
