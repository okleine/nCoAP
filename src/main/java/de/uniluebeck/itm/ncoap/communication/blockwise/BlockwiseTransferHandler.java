/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.communication.blockwise;

import com.google.common.annotations.Beta;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.BLOCK_2;

/**
 * The {@link BlockwiseTransferHandler} currently provides the ability to receive {@link CoapResponse}s blockwise, i.e.
 * if a server decided to split up the payload on several {@link CoapResponse}s, the {@link BlockwiseTransferHandler}
 * communicates with the server to receive follow-up blocks until the whole payload is complete.
 *
 * Upon completion it sends a {@link CoapResponse} containg the whole payload upstream
 */

@Beta
public class BlockwiseTransferHandler extends SimpleChannelHandler{

    private static Logger log = LoggerFactory.getLogger(BlockwiseTransferHandler.class.getName());

    private Object incompleteResponseMonitor = new Object();
    private Object incompleteRequestMonitor = new Object();

    private HashMap<ByteArrayWrapper, BlockwiseTransfer> incompleteResponsePayload =
            new HashMap<ByteArrayWrapper, BlockwiseTransfer>();

    private HashMap<ByteArrayWrapper, ChannelBuffer> incompleteRequestPayload =
            new HashMap<ByteArrayWrapper, ChannelBuffer>();


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendDownstream(me);
            return;
        }

        if(me.getMessage() instanceof CoapRequest){
            CoapRequest request = (CoapRequest) me.getMessage();
            byte[] token = request.getToken();

            //There was no CoapResponseProcessor attached to the response and thus there is no token
            if(token.length == 0){
                ctx.sendDownstream(me);
                return;
            }

            BlockwiseTransfer transfer =
                    new BlockwiseTransfer(request, ChannelBuffers.dynamicBuffer());

            synchronized (incompleteResponseMonitor){
                incompleteResponsePayload.put(new ByteArrayWrapper(token), transfer);
            }

            ctx.sendDownstream(me);

            //TODO handle blockwise transfer for outgoing requests with payload

        }
        else{
            //TODO handle blockwise transfer for outgoing responses with payload
            ctx.sendDownstream(me);
        }


    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendUpstream(me);
            return;
        }

        log.info("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        if(coapMessage.getCode().isErrorMessage() || coapMessage.getMessageType().equals(MsgType.RST)){
            errorMessageReceived(ctx, me);
            return;
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse response = (CoapResponse) me.getMessage();

            //Check if there is a BLOCK_2 option contained
            if(response.getMaxBlocksizeForResponse() == null){
                ctx.sendUpstream(me);
                return;
            }

            final byte[] token = response.getToken();

            BlockwiseTransfer transfer;
            //Add latest received payload to already received payload
            synchronized (incompleteResponseMonitor){
                transfer = incompleteResponsePayload.get(new ByteArrayWrapper(token));
                if(transfer != null){
                    try {
                        if(response.getBlockNumber(BLOCK_2) == transfer.getNextBlockNumber()){
                            log.debug("Received response (Token: " + (new ByteArrayWrapper(token).toString()) +
                                    " , Block: " + response.getBlockNumber(BLOCK_2) + "), ");

                            if (log.isDebugEnabled()){
                                //Copy Payload
                                ChannelBuffer payloadCopy = ChannelBuffers.copiedBuffer(response.getPayload());
                                byte[] bytes = new byte[payloadCopy.readableBytes()];
                                payloadCopy.getBytes(0, bytes);
                                log.debug("Payload Hex: " + new ByteArrayWrapper(bytes).toString());
                            }

                            transfer.getPartialPayload()
                                    .writeBytes(response.getPayload(), 0, response.getPayload().readableBytes());
                            transfer.setNextBlockNumber(transfer.getNextBlockNumber() + 1);

                            sendInternalNextBlockReceivedMessage(token, (InetSocketAddress) me.getRemoteAddress(), ctx);

                        }
                        else{
                            log.debug("Received unexpected response (Token: " + (new ByteArrayWrapper(token).toString()) +
                                    " , Block: " + response.getBlockNumber(BLOCK_2) + "). IGNORE!");
                            me.getFuture().setSuccess();
                            return;
                        }
                    }
                    catch (InvalidOptionException e) {
                        log.error("This should never happen!", e);
                    }
                }
            }

            //Check whether payload of the response is complete
            if(transfer != null){
                try {
                    if(response.isLastBlock(BLOCK_2)){

                        //Send response with complete payload to application
                        log.debug("Block " + response.getBlockNumber(BLOCK_2) + " for response with token " +
                                new ByteArrayWrapper(token).toString() +
                                "  received. Payload complete. Forward to client application.");

                        response.getOptionList().removeAllOptions(BLOCK_2);

                        response.setPayload(transfer.getPartialPayload());
                        MessageEvent event = new UpstreamMessageEvent(me.getChannel(), response, me.getRemoteAddress());
                        ctx.sendUpstream(event);

                        synchronized (incompleteResponseMonitor){
                            if(incompleteResponsePayload.remove(new ByteArrayWrapper(token)) == null){
                                log.error("This should never happen! No incomplete payload found for token " +
                                        new ByteArrayWrapper(token).toString());
                            }
                            else{
                                log.debug("Deleted not anymore incomplete payload for token " +
                                        new ByteArrayWrapper(token).toString() + " from list");
                            }
                        }
                        return;

                    }
                    else{
                        final long receivedBlockNumber = response.getBlockNumber(BLOCK_2);

                        log.debug("Block " + receivedBlockNumber + " for response with token " +
                                new ByteArrayWrapper(token).toString() +
                                "  received. Payload (still) incomplete.");

                        CoapRequest nextCoapRequest = (CoapRequest) transfer.getCoapMessage();
                        nextCoapRequest.setMessageID(-1);
                        nextCoapRequest.setBlockOption(BLOCK_2, receivedBlockNumber + 1,
                                false, response.getMaxBlocksizeForResponse());


                        ChannelFuture future = Channels.future(me.getChannel());

                        future.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                log.debug("Request for block " + (receivedBlockNumber + 1) + " for token " +
                                        new ByteArrayWrapper(token).toString() + " sent succesfully.");

                            }
                        });
                        MessageEvent event = new DownstreamMessageEvent(me.getChannel(),
                                future, nextCoapRequest, me.getRemoteAddress());

                        log.debug("Send request for block " + (receivedBlockNumber + 1) + " for token " +
                                new ByteArrayWrapper(token).toString() + ".");

                        ctx.sendDownstream(event);
                        return;
                    }
                }
                catch (InvalidOptionException e) {
                    log.error("This should never happen!", e);
                }
                catch (MessageDoesNotAllowPayloadException e) {
                    log.error("This should never happen!", e);
                }
                catch (ToManyOptionsException e){
                    log.error("This should never happen!", e);
                }
                catch (InvalidHeaderException e) {
                    log.error("This should never happen!", e);
                }
            }
        }

        log.info("Incoming 2(from {}): {}.", me.getRemoteAddress(), me.getMessage());
        ctx.sendUpstream(me);
    }

    private void sendInternalNextBlockReceivedMessage(byte[] token, InetSocketAddress remoteAddress,
                                                      ChannelHandlerContext ctx){
        InternalNextBlockReceivedMessage message = new InternalNextBlockReceivedMessage(token);

        UpstreamMessageEvent event = new UpstreamMessageEvent(ctx.getChannel(), message, remoteAddress);
        ctx.sendUpstream(event);
    }

    private void errorMessageReceived(ChannelHandlerContext ctx, MessageEvent me){
        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        synchronized (incompleteResponseMonitor){
            incompleteResponsePayload.remove(coapMessage.getToken());
        }
        ctx.sendUpstream(me);
    }

    private class BlockwiseTransfer {

        private CoapMessage coapMessage;
        private ChannelBuffer partialPayload;
        private int nextBlockNumber = 0;

        public BlockwiseTransfer(CoapMessage coapMessage, ChannelBuffer partialPayload){
            this.coapMessage = coapMessage;
            this.partialPayload = partialPayload;
        }

        public CoapMessage getCoapMessage() throws InvalidHeaderException {
            return coapMessage;
        }

        public ChannelBuffer getPartialPayload() {
            return partialPayload;
        }

        public void setNextBlockNumber(int nextBlockNumber){
            this.nextBlockNumber = nextBlockNumber;
        }

        public int getNextBlockNumber(){
            return this.nextBlockNumber;
        }
    }
}