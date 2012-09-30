package de.uniluebeck.itm.spitfire.nCoap.communication.blockwise;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.BLOCK_2;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 12.09.12
 * Time: 17:50
 * To change this template use File | Settings | File Templates.
 */
public class BlockwiseTransferHandler extends SimpleChannelHandler{

    private static Logger log = LoggerFactory.getLogger(BlockwiseTransferHandler.class.getName());

    private Object incompleteResponseMonitor = new Object();
    private Object incompleteRequestMonitor = new Object();

    private HashMap<ByteArrayWrapper, BlockwiseTransfer> incompleteResponsePayload =
            new HashMap<ByteArrayWrapper, BlockwiseTransfer>();

    private HashMap<ByteArrayWrapper, ChannelBuffer> incompleteRequestPayload =
            new HashMap<ByteArrayWrapper, ChannelBuffer>();

    private static BlockwiseTransferHandler instance = new BlockwiseTransferHandler();

    private BlockwiseTransferHandler(){

    }

    public static BlockwiseTransferHandler getInstance(){
        return instance;
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(!(me.getMessage() instanceof CoapMessage)){
            ctx.sendDownstream(me);
            return;
        }

        if(me.getMessage() instanceof CoapRequest){
            CoapRequest request = (CoapRequest) me.getMessage();
            byte[] token = request.getToken();

            //There was no ResponseCallback attached to the response and thus there is no token
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

        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        if(coapMessage.getCode().isError() || coapMessage.getMessageType().equals(MsgType.RST)){
            errorMessageReceived(ctx, me);
            return;
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse response = (CoapResponse) me.getMessage();

            final byte[] token = response.getToken();

            BlockwiseTransfer transfer;
            //Add latest received payload to already received payload
            synchronized (incompleteResponseMonitor){
                transfer = incompleteResponsePayload.get(new ByteArrayWrapper(token));
                if(transfer != null){
                    try {
                        if(response.getBlockNumber(BLOCK_2) == transfer.getNextBlockNumber()){
                            log.debug("Received response (Token: " + (new ByteArrayWrapper(token).toHexString()) +
                                      " , Block: " + response.getBlockNumber(BLOCK_2) + "), ");

                            if (log.isDebugEnabled()){
                                //Copy Payload
                                ChannelBuffer payloadCopy = ChannelBuffers.copiedBuffer(response.getPayload());
                                byte[] bytes = new byte[payloadCopy.readableBytes()];
                                payloadCopy.getBytes(0, bytes);
                                log.debug("Payload Hex: " + new ByteArrayWrapper(bytes).toHexString());
                            }

                            transfer.getPartialPayload()
                                    .writeBytes(response.getPayload(), 0, response.getPayload().readableBytes());
                            transfer.setNextBlockNumber(transfer.getNextBlockNumber() + 1);
                        }
                        else{
                            log.debug("Received unexpected response (Token: " + (new ByteArrayWrapper(token).toHexString()) +
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
                                new ByteArrayWrapper(token).toHexString() +
                                "  received. Payload complete. Forward to client application.");

                        response.getOptionList().removeAllOptions(BLOCK_2);

                        response.setPayload(transfer.getPartialPayload());
                        MessageEvent event = new UpstreamMessageEvent(me.getChannel(), response, me.getRemoteAddress());
                        ctx.sendUpstream(event);

                        synchronized (incompleteResponseMonitor){
                            if(incompleteResponsePayload.remove(new ByteArrayWrapper(token)) == null){
                                log.error("This should never happen! No incomplete payload found for token " +
                                    new ByteArrayWrapper(token).toHexString());
                            }
                            else{
                                log.debug("Deleted not anymore incomplete payload for token " +
                                        new ByteArrayWrapper(token).toHexString() + " from list");
                            }
                        }
                        return;

                    }
                    else{
                        final long receivedBlockNumber = response.getBlockNumber(BLOCK_2);

                        log.debug("Block " + receivedBlockNumber + " for response with token " +
                                new ByteArrayWrapper(token).toHexString() +
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
                                    new ByteArrayWrapper(token).toHexString() + " sent succesfully.");

                            }
                        });
                        MessageEvent event = new DownstreamMessageEvent(me.getChannel(),
                                future, nextCoapRequest, me.getRemoteAddress());

                        log.debug("Send request for block " + (receivedBlockNumber + 1) + " for token " +
                                new ByteArrayWrapper(token).toHexString() + ".");

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

        ctx.sendUpstream(me);
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
