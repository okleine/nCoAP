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
package de.uniluebeck.itm.ncoap.communication.codec;

import de.uniluebeck.itm.ncoap.application.Token;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
* A {@link CoapMessageDecoder} de-serializes incoming messages.
*
* @author Oliver Kleine
*/
public class CoapMessageDecoder extends SimpleChannelUpstreamHandler{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {

        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        final MessageEvent messageEvent = (MessageEvent) evt;
        Object originalMessage = messageEvent.getMessage();

        try{
            if (!(originalMessage instanceof ChannelBuffer)){
                ctx.sendUpstream(evt);
            }
            else{
                InetSocketAddress remoteSocketAddress = (InetSocketAddress) messageEvent.getRemoteAddress();
                CoapMessage coapMessage = decode(remoteSocketAddress, (ChannelBuffer) originalMessage);

                Channels.fireMessageReceived(ctx, coapMessage, remoteSocketAddress);
            }
        }
        catch(Exception e){
            messageEvent.getFuture().setFailure(e);
            throw e;
        }
    }


    private CoapMessage decode(InetSocketAddress remoteSocketAddress, ChannelBuffer buffer)
            throws MessageFormatDecodingException, OptionDecodingException, InvalidHeaderException {

        log.debug("Incoming message to be decoded (length: {})", buffer.readableBytes());

        //Decode the Message Header which must have a length of exactly 4 bytes
        if(buffer.readableBytes() < 4)
            throw new MessageFormatDecodingException(remoteSocketAddress, CoapMessage.MESSAGE_ID_UNDEFINED,
                    "Buffer must contain at least 4 readable bytes (but has " + buffer.readableBytes() + ")");


        //Decode the header values
        int encodedHeader = buffer.readInt();
        int version =     (encodedHeader >>> 30) & 0x03;
        int messageType = (encodedHeader >>> 28) & 0x03;
        int tokenLength = (encodedHeader >>> 24) & 0x0F;
        int messageCode = (encodedHeader >>> 16) & 0xFF;
        int messageID =   (encodedHeader)        & 0xFFFF;


        log.debug("Decoded Header: (T) {}, (TKL) {}, (C) {}, (ID) {}",
                new Object[]{messageType, tokenLength, messageCode, messageID});

        //Check whether the protocol version is supported (=1)
        if(version != CoapMessage.PROTOCOL_VERSION)
            throw new MessageFormatDecodingException(remoteSocketAddress, messageID,
                    "Unsupported CoAP protocol version: " + version);


        //Check whether TKL indicates a not allowed token length
        if(tokenLength > CoapMessage.MAX_TOKEN_LENGTH){
            byte[] tokenBytes = new byte[CoapMessage.MAX_TOKEN_LENGTH];
            buffer.readBytes(tokenBytes);
            Token token =  new Token(tokenBytes);

            throw new MessageFormatDecodingException(remoteSocketAddress, messageID, "TKL value to large (max: "
                    + CoapMessage.MAX_TOKEN_LENGTH + ", actual: " + tokenLength + "). First 8 bytes of token: "
                    + token);
        }

        //Check whether there are enough unread bytes left to read the token
        if(buffer.readableBytes() < tokenLength){
            byte[] tokenBytes = new byte[buffer.readableBytes()];
            buffer.readBytes(tokenBytes);
            Token token =  new Token(tokenBytes);
            throw new MessageFormatDecodingException(remoteSocketAddress, messageID,
                "TKL header value: " + tokenLength + ", Remaining bytes: " + token + ".");
        }

        //Read the token
        byte[] token = new byte[tokenLength];
        buffer.readBytes(token);


        //Check whether message code indicates empty message and either TKL is greater than 0 or there are unread bytes
        //following the header
        if(messageCode == MessageCode.Name.EMPTY.getNumber()){

            if(tokenLength > 0)
                throw new MessageFormatDecodingException(remoteSocketAddress, messageID,
                    "Invalid TKL header value for empty message: " + tokenLength);

            if(buffer.readableBytes() > 0)
                throw new MessageFormatDecodingException(remoteSocketAddress, messageID,
                    "Empty messages MUST NOT contain anything but the 4 bytes header (actual remaining bytes after "
                        + " header: " + buffer.readableBytes() + ")");

        }

        //Handle empty message
        if(messageCode == MessageCode.Name.EMPTY.getNumber()){

            if(messageType == MessageType.Name.ACK.getNumber())
                return CoapMessage.createEmptyAcknowledgement(messageID);

            else if(messageType == MessageType.Name.RST.getNumber())
                return CoapMessage.createEmptyReset(messageID);

            else if(messageType == MessageType.Name.CON.getNumber())
                return CoapMessage.createEmptyConfirmableMessage(messageID);

            else
                throw new MessageFormatDecodingException(remoteSocketAddress, messageID,
                    "Empty message received which is neither an ACK nor a RST (Type: " + messageType + ").");

        }

        //Handle non-empty messages (CON or NON)
        CoapMessage coapMessage;

        if(MessageCode.isRequest(messageCode))
            coapMessage = new CoapRequest(messageType, messageCode);

        else if(MessageCode.isResponse(messageCode)){
            coapMessage = new CoapResponse(messageType, messageCode);
            coapMessage.setMessageType(messageType);
        }

        else
            throw new MessageFormatDecodingException(remoteSocketAddress, messageID,
                "Unknown message code: " + messageCode);


        coapMessage.setMessageID(messageID);
        coapMessage.setToken(new Token(token));

        //Decode and set the options
        if(buffer.readableBytes() > 0){
            try {
                setOptions(coapMessage, buffer);
            }
            catch (OptionException e) {
                throw new OptionDecodingException(remoteSocketAddress, messageID, new Token(token), e);
            }
        }


        //The remaining bytes (if any) are the messages payload. If there is no payload, reader and writer index are
        //at the same position (buf.readableBytes() == 0).
        buffer.discardReadBytes();

        try {
            coapMessage.setContent(buffer);
        }
        catch (InvalidMessageException e) {
            log.warn("Message code {} does not allow content. Ignore {} bytes.", coapMessage.getMessageCode(),
                    buffer.readableBytes());
        }

        log.info("Decoded Message: {}", coapMessage);

        return coapMessage;
    }


    private void setOptions(CoapMessage coapMessage, ChannelBuffer buffer) throws OptionException{

        //Decode the options
        int previousOptionNumber = 0;
        int firstByte = buffer.readByte() & 0xFF;

        while(firstByte != 0xFF && buffer.readableBytes() > 0){
            log.debug("First byte: {} ({})", toBinaryString(firstByte), firstByte);
            int optionDelta =   (firstByte & 0xF0) >>> 4;
            int optionLength =   firstByte & 0x0F;
            log.debug("temp. delta: {}, temp. length {}", optionDelta, optionLength);

            if(optionDelta == 13)
                optionDelta += buffer.readByte() & 0xFF;

            else if(optionDelta == 14)
                optionDelta = 269 + ((buffer.readByte() & 0xFF) << 8) + (buffer.readByte() & 0xFF);

            else

            if(optionLength == 13){
                optionLength += buffer.readByte() & 0xFF;
            }

            else if(optionLength == 14)
                optionLength = 269 + ((buffer.readByte() & 0xFF) << 8) + (buffer.readByte() & 0xFF);


            log.debug("Previous option: {}, Option delta: {}", previousOptionNumber, optionDelta);

            int actualOptionNumber = previousOptionNumber + optionDelta;
            log.debug("Decode option no. {} with length of {} bytes.", actualOptionNumber, optionLength);

            try {
                byte[] optionValue = new byte[optionLength];
                buffer.readBytes(optionValue);

                switch(Option.getOptionType(actualOptionNumber)){

                    case Option.Type.EMPTY:
                        coapMessage.addOption(actualOptionNumber, new EmptyOption(actualOptionNumber));
                        break;

                    case Option.Type.OPAQUE:
                        coapMessage.addOption(actualOptionNumber, new OpaqueOption(actualOptionNumber, optionValue));
                        break;

                    case Option.Type.STRING:
                        coapMessage.addOption(actualOptionNumber, new StringOption(actualOptionNumber, optionValue));
                        break;

                    case Option.Type.UINT:
                        coapMessage.addOption(actualOptionNumber, new UintOption(actualOptionNumber, optionValue));
                        break;

                    default:
                        log.error("This should never happen!");
                        throw new UnknownOptionException(actualOptionNumber);
                }

            }
            catch (OptionException e) {

                if(MessageCode.isResponse(coapMessage.getMessageCode())){
                    log.warn("Silently ignore option no. " + e.getOptionNumber() + " in incoming response.");
                }

                else if(Option.isCritical(actualOptionNumber)){
                    throw e;
                }

                else{
                    log.warn("Silently ignore elective option no. " + e.getOptionNumber() + " in incoming request.");
                }

            }

            previousOptionNumber = actualOptionNumber;

            if(buffer.readableBytes() > 0)
                firstByte = buffer.readByte() & 0xFF;

            log.debug("{} readable bytes remaining.", buffer.readableBytes());
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent exceptionEvent){

        //Message Format Exceptions cause a RST message
        if(exceptionEvent.getCause() instanceof MessageFormatDecodingException){
            MessageFormatDecodingException ex = (MessageFormatDecodingException) exceptionEvent.getCause();

            if(ex.getMessageID() == CoapMessage.MESSAGE_ID_UNDEFINED){
                log.warn("Could not handle incoming malformed message (Reason: {})", ex.getCause().getMessage());
            }
            else{
                log.warn("Received malformed message (Reason: {})", ex.getCause().getMessage());

                try{
                    final CoapMessage emptyReset = CoapMessage.createEmptyReset(ex.getMessageID());
                    log.warn("Send empty RST: {}", emptyReset);
                    sendMessage(ctx, ex.getRemoteSocketAddress(), emptyReset);
                }
                catch(InvalidHeaderException e){
                    log.error("This should never happen!", e);
                }
            }
        }

        //Option Decoding Exceptions (only thrown for incoming requests) cause a BAD OPTION (402) response
        else if(exceptionEvent.getCause() instanceof OptionDecodingException){
            OptionDecodingException ex = (OptionDecodingException) exceptionEvent.getCause();
            log.warn("Received request with not-handable option (Reason: {})", ex.getCause().getMessage());

            try{
                final CoapResponse coapResponse =
                        new CoapResponse(MessageType.Name.NON, MessageCode.Name.BAD_OPTION_402);

                coapResponse.setMessageID(ex.getMessageID());
                coapResponse.setToken(ex.getToken());
                coapResponse.setContent(ex.getCause().getMessage().getBytes(CoapMessage.CHARSET),
                        ContentFormat.TEXT_PLAIN_UTF8);
                log.warn("Send BAD OPTION response: {}", coapResponse);
                sendMessage(ctx, ex.getRemoteSocketAddress(), coapResponse);
            }
            catch (InvalidHeaderException e) {
                log.error("This should never happen (Could not create error response for not-handable option)!", e);
            }
            catch (InvalidOptionException e) {
                log.error("This should never happen (Could not create error response for not-handable option)!", e);
            }
            catch (InvalidMessageException e) {
                log.error("This should never happen (Could not create error response for not-handable option)!", e);
            }
        }

        else{
            log.error("This should never happen (DecodingException with unsupported cause!", exceptionEvent.getCause());
            ctx.sendUpstream(exceptionEvent);
        }
    }

    private void sendMessage(ChannelHandlerContext ctx, final InetSocketAddress remoteSocketAddress,
                             final CoapMessage coapMessage){

        ChannelFuture future = Channels.future(ctx.getChannel());
        Channels.write(ctx, Channels.future(ctx.getChannel()), coapMessage, remoteSocketAddress);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Sent message to {}: {}", remoteSocketAddress, coapMessage);
            }
        });
    }

    private static String toBinaryString(int byteValue){
        StringBuffer buffer = new StringBuffer(8);

        //int actualValue = byteValue & 0xFF;

        for(int i = 7; i >= 0; i--){
            if((byteValue & (int) Math.pow(2, i)) > 0)
                buffer.append("1");
            else
                buffer.append("0");
        }

        return buffer.toString();
    }
}
