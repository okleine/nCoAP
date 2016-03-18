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
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uzl.itm.ncoap.communication.blockwise;
//
//import com.google.common.collect.HashBasedTable;
//import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
//import de.uzl.itm.ncoap.communication.dispatching.client.Token;
//import de.uzl.itm.ncoap.communication.events.PartialContentReceivedEvent;
//import de.uzl.itm.ncoap.communication.events.client.RemoteServerSocketChangedEvent;
//import de.uzl.itm.ncoap.communication.events.TransmissionTimeoutEvent;
//import de.uzl.itm.ncoap.message.CoapMessage;
//import de.uzl.itm.ncoap.message.CoapRequest;
//import de.uzl.itm.ncoap.message.CoapResponse;
//import de.uzl.itm.ncoap.message.options.UintOptionValue;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.jboss.netty.channel.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetSocketAddress;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//
///**
// * This handler is only used on the client side and deals with blockwise transfers in a rather simple manner.
// * If a response contains the BLOCK2 option (i.e. just a portion of the complete payload) the client accepts the
// * block size (as given in the SZX part of the response option) and requests the next blocks with the same size until
// * the full payload was received (as indicated by the M part of the (final) response option).
// *
// * The full payload (the cumulative blocks) is then set as the payload of the latest response. Only this response
// * (with full payload) is sent further upstream. Thus, from the
// * {@link de.uzl.itm.ncoap.application.client.ClientCallback}s perspective there is virtually no
// * difference between a blockwise transfer and a large payload in a single response.
// *
// * @author Oliver Kleine
// */
//public class OldClientBlockOptionsHandler extends AbstractCoapChannelHandler implements TransmissionTimeoutEvent.Handler,
//        RemoteServerSocketChangedEvent.Handler {
//
//    private static Logger LOG = LoggerFactory.getLogger(OldClientBlockOptionsHandler.class.getName());
//
//    private HashBasedTable<InetSocketAddress, Token, CoapRequest> openRequests;
//    private HashBasedTable<InetSocketAddress, Token, ChannelBuffer> partialResponses;
//
//    private ReentrantReadWriteLock lock;
//
//    /**
//     * Creates a new instance of {@link OldClientBlockOptionsHandler}.
//     */
//    public OldClientBlockOptionsHandler(ScheduledExecutorService executor){
//        super(executor);
//        this.openRequests = HashBasedTable.create();
//        this.partialResponses = HashBasedTable.create();
//        this.lock = new ReentrantReadWriteLock();
//    }
//
//    @Override
//    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
//        LOG.debug("INBOUND: {}", coapMessage);
//        if(coapMessage instanceof CoapResponse) {
//            handleIncomingCoapResponse((CoapResponse) coapMessage, remoteSocket);
//        }
//        return true;
//    }
//
//
//    @Override
//    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
//        LOG.debug("OUTBOUND: {}", coapMessage);
//        return !(coapMessage instanceof CoapRequest && !addCoapRequest(remoteSocket, (CoapRequest) coapMessage));
//    }
//
//    @Override
//    public void handleEvent(TransmissionTimeoutEvent event) {
//        removeCoapRequest(event.getRemoteSocket(), event.getToken());
//    }
//
//
//    @Override
//    public void handleEvent(RemoteServerSocketChangedEvent event) {
//        InetSocketAddress previousRemoteSocket = event.getPreviousRemoteSocket();
//        Token token = event.getToken();
//
//        try{
//            this.lock.readLock().lock();
//            if(!this.openRequests.contains(previousRemoteSocket, token)){
//                return;
//            }
//        }
//        finally {
//            this.lock.readLock().unlock();
//        }
//
//        try{
//            this.lock.writeLock().lock();
//            CoapRequest coapRequest = this.openRequests.remove(previousRemoteSocket, token);
//            if(coapRequest == null){
//                return;
//            }
//
//            InetSocketAddress remoteSocket = event.getRemoteSocket();
//            this.openRequests.put(remoteSocket, token, coapRequest);
//            ChannelBuffer partialContent = this.partialResponses.remove(previousRemoteSocket, token);
//            this.partialResponses.put(remoteSocket, token, partialContent);
//        }
//        finally {
//            this.lock.writeLock().unlock();
//        }
//    }
//
//
//    private void handleIncomingCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
//
//        Token token = coapResponse.getToken();
//        LOG.info("Received response from {}: {}", remoteSocket, coapResponse);
//
//        if(coapResponse.getBlock2Number() == UintOptionValue.UNDEFINED){
//            removePartialPayload(remoteSocket, token);
//            removeCoapRequest(remoteSocket, token);
//        } else {
//            addPayloadBlock(remoteSocket, token, coapResponse.getContent());
//            if(coapResponse.isLastBlock2()){
//                coapResponse.setContent(getPayload(remoteSocket, token));
//                removePartialPayload(remoteSocket, token);
//                removeCoapRequest(remoteSocket, token);
//            } else {
//                long blockNumber = coapResponse.getBlock2Number();
//                long encBlockSize = coapResponse.getBlock2EncodedSize();
//                requestNextBlock(getContext(), remoteSocket, coapResponse.getToken(), blockNumber + 1, encBlockSize);
//
//                // send internal event
//                triggerEvent(new PartialContentReceivedEvent(
//                    remoteSocket, token, blockNumber, (long) Math.pow(2, encBlockSize + 4)
//                ), false);
//            }
//        }
//    }
//
//
//    private void requestNextBlock(ChannelHandlerContext ctx, InetSocketAddress remoteSocket, Token token,
//            long blockNumber, long encodedBlockSize) {
//
//        CoapRequest coapRequest = this.getCoapRequest(remoteSocket, token);
//        coapRequest.setMessageID(CoapMessage.UNDEFINED_MESSAGE_ID);
//        coapRequest.setBlock2(blockNumber, false, encodedBlockSize);
//
//        ChannelFuture future = Channels.future(ctx.getChannel());
//        future.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                if(future.isSuccess()){
//                    LOG.info("Successfully sent request for next block.");
//                }
//                else{
//                    LOG.error("Error: {}", future.getCause());
//                }
//            }
//        });
//
//        Channels.write(ctx, future, coapRequest, remoteSocket);
//    }
//
//    private void removePartialPayload(InetSocketAddress remoteEndpoint, Token token){
//        try{
//            this.lock.writeLock().lock();
//            this.partialResponses.remove(remoteEndpoint, token);
//        }
//        finally {
//            this.lock.writeLock().unlock();
//        }
//    }
//
//    /**
//     * Returns the {@link de.uzl.itm.ncoap.message.CoapRequest} used to initiate this conversation. This request
//     * is modified (the BLOCK2 option) and used to request the next block
//     *
//     * @param remoteEndpoint the server socket
//     * @param token the token used for this transfer
//     *
//     * @return the {@link de.uzl.itm.ncoap.message.CoapRequest} used to initiate this conversation
//     */
//    private CoapRequest getCoapRequest(InetSocketAddress remoteEndpoint, Token token){
//        try{
//            this.lock.readLock().lock();
//            if(!this.openRequests.contains(remoteEndpoint, token)){
//                LOG.error("No partial payload available (remote endpoint: {}, token: {}).",
//                        remoteEndpoint, token);
//                return null;
//            }
//
//            else{
//                return this.openRequests.get(remoteEndpoint, token);
//            }
//        }
//        finally{
//            this.lock.readLock().unlock();
//        }
//    }
//
//    /**
//     * Returns the cumulative payload received so far in previous blocks
//     *
//     * @param remoteEndpoint the server socket
//     * @param token the token used for this transfer
//     *
//     * @return the cumulative payload received so far in previous blocks
//     */
//    private ChannelBuffer getPayload(InetSocketAddress remoteEndpoint, Token token){
//        try{
//            this.lock.writeLock().lock();
//            if(!this.partialResponses.contains(remoteEndpoint, token)){
//                LOG.error("No partial payload available (remote endpoint: {}, token: {}).",
//                        remoteEndpoint, token);
//                return ChannelBuffers.EMPTY_BUFFER;
//            }
//
//            else{
//                return this.partialResponses.remove(remoteEndpoint, token);
//            }
//        }
//        finally{
//            this.lock.writeLock().unlock();
//        }
//    }
//
//
//    private void addPayloadBlock(InetSocketAddress remoteEndpoint, Token token, ChannelBuffer payloadBlock){
//        try{
//            this.lock.writeLock().lock();
//            if(!this.partialResponses.contains(remoteEndpoint, token)){
//                this.partialResponses.put(remoteEndpoint, token, payloadBlock);
//                LOG.info("Added new partial payload (remote endpoint: {}, token: {}).", remoteEndpoint, token);
//            }
//
//            else{
//                ChannelBuffer previous = this.partialResponses.get(remoteEndpoint, token);
//                this.partialResponses.put(remoteEndpoint, token, ChannelBuffers.wrappedBuffer(previous, payloadBlock));
//                LOG.info("Added new block to partial payload (remote endpoint: {}, token: {})", remoteEndpoint, token);
//            }
//        }
//        finally{
//            this.lock.writeLock().unlock();
//        }
//    }
//
//
//    private CoapRequest removeCoapRequest(InetSocketAddress remoteEndpoint, Token token){
//        try{
//            this.lock.readLock().lock();
//            if(!this.openRequests.contains(remoteEndpoint, token)){
//                return null;
//            }
//        }
//        finally {
//            this.lock.readLock().unlock();
//        }
//
//        try{
//            this.lock.writeLock().lock();
//            if(!this.openRequests.contains(remoteEndpoint, token)){
//                LOG.error("No open request found (remote endpoint: {}, token: {}).",
//                        remoteEndpoint, token);
//                return null;
//            } else {
//                CoapRequest result = this.openRequests.remove(remoteEndpoint, token);
//                LOG.info("Removed open request (remote endpoint: {}, token: {})", remoteEndpoint, token);
//                return result;
//            }
//        }
//        finally{
//            this.lock.writeLock().unlock();
//        }
//    }
//
//    private boolean addCoapRequest(InetSocketAddress remoteEndpoint, CoapRequest coapRequest){
//        Token token = coapRequest.getToken();
//        try{
//            this.lock.writeLock().lock();
//            if(this.openRequests.contains(remoteEndpoint, token)){
//                LOG.error("Tried to override existing conversation (remote endpoint: {}, token: {}).",
//                        remoteEndpoint, token);
//                return false;
//            }
//
//            else{
//                this.openRequests.put(remoteEndpoint, token, coapRequest);
//                LOG.info("New conversation added (remote endpoint: {}, token: {})", remoteEndpoint, token);
//                return true;
//            }
//        }
//        finally{
//            this.lock.writeLock().unlock();
//        }
//    }
//}
