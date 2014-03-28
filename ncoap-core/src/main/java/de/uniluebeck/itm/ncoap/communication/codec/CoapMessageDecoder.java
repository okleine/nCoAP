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

import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * A {@link CoapMessageDecoder} de-serializes incoming messages. Please note the following:
 * <ul>
 *     <li>
 *          If the incoming message is a {@link CoapResponse} then malformed or unknown options are silently
 *          ignored and the {@link CoapResponse} is further processed without these options.
 *     </li>
 *     <li>
 *          If the incoming messge is a {@link CoapRequest}, then malformed or unsupported, i.e. unknown
 *          non-critical options are silently ignored but critical options lead to an immediate
 *          {@link CoapResponse} with {@link MessageCode.Name#BAD_OPTION_402} being sent to the remote CoAP endpoints.
 *     </li>
 *     <li>
 *          Malformed incoming {@link CoapMessage}s with malformed header, e.g. a TKL field that does not correspond to
 *          the actual tokens length, lead to an immediate {@link CoapMessage} with {@link MessageType.Name#RST} being
 *          sent to the remote CoAP endpoints.
 *     </li>
 *     <li>
 *         For incoming {@link CoapMessage}s with {@link MessageCode.Name#EMPTY} only the header, i.e. the first 4
 *         bytes are decoded and further processed. Any following bytes contained in the same encoded message are
 *         ignored.
 *     </li>
 * </ul>
 *
 * @author Oliver Kleine
 */
public class CoapMessageDecoder extends SimpleChannelUpstreamHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());


    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {

        if (!(evt instanceof MessageEvent) || !(((MessageEvent) evt).getMessage() instanceof ChannelBuffer)) {
            ctx.sendUpstream(evt);
            return;
        }

        final MessageEvent messageEvent = (MessageEvent) evt;
        messageEvent.getFuture().setSuccess();

        InetSocketAddress remoteEndpoint = (InetSocketAddress) messageEvent.getRemoteAddress();
        CoapMessage coapMessage = decode(remoteEndpoint, (ChannelBuffer) messageEvent.getMessage());

        if(coapMessage != null)
            Channels.fireMessageReceived(ctx, coapMessage, remoteEndpoint);
    }


    private CoapMessage decode(InetSocketAddress remoteEndpoint, ChannelBuffer buffer)
            throws InvalidHeaderException, InvalidOptionException {

        log.debug("Incoming message to be decoded (length: {})", buffer.readableBytes());

        //Decode the Message Header which must have a length of exactly 4 bytes
        if(buffer.readableBytes() < 4)
            throw new InvalidHeaderException(CoapMessage.MESSAGE_ID_UNDEFINED, remoteEndpoint);


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
            throw new InvalidHeaderException(messageID, remoteEndpoint);


        //Check whether TKL indicates a not allowed token length
        if(tokenLength > CoapMessage.MAX_TOKEN_LENGTH)
            throw new InvalidHeaderException(messageID, remoteEndpoint);


        //Check whether there are enough unread bytes left to read the token
        if(buffer.readableBytes() < tokenLength)
            throw new InvalidHeaderException(messageID, remoteEndpoint);


        //Handle empty message (ignore everything but the first 4 bytes)
        if(messageCode == MessageCode.Name.EMPTY.getNumber()){

            if(messageType == MessageType.Name.ACK.getNumber())
                return CoapMessage.createEmptyAcknowledgement(messageID);

            else if(messageType == MessageType.Name.RST.getNumber())
                return CoapMessage.createEmptyReset(messageID);

            else if(messageType == MessageType.Name.CON.getNumber())
                return CoapMessage.createEmptyConfirmableMessage(messageID);

            //There is no empty NON message defined, so send a RST
            else
                throw new InvalidHeaderException(messageID, remoteEndpoint);
        }


        //Read the token
        byte[] token = new byte[tokenLength];
        buffer.readBytes(token);

        //Handle non-empty messages (CON, NON or ACK)
        CoapMessage coapMessage;

        if(MessageCode.isRequest(messageCode))
            coapMessage = new CoapRequest(messageType, messageCode);

        else{
            coapMessage = new CoapResponse(messageType, messageCode);
            coapMessage.setMessageType(messageType);
        }

        coapMessage.setMessageID(messageID);
        coapMessage.setToken(new Token(token));

        //Decode and set the options
        if(buffer.readableBytes() > 0){
            try {
                setOptions(coapMessage, buffer);
            }
            catch (InvalidOptionException e) {
                e.setMessageID(messageID);
                e.setToken(new Token(token));
                e.setRemoteEndpoint(remoteEndpoint);
                e.setMessageType(messageType);

                throw e;
            }
        }


        //The remaining bytes (if any) are the messages payload. If there is no payload, reader and writer index are
        //at the same position (buf.readableBytes() == 0).
        buffer.discardReadBytes();

        try {
            coapMessage.setContent(buffer);
        }
        catch (IllegalArgumentException e) {
            log.warn("Message code {} does not allow content. Ignore {} bytes.", coapMessage.getMessageCode(),
                    buffer.readableBytes());
        }

        log.info("Decoded Message: {}", coapMessage);

        return coapMessage;
    }


    private void setOptions(CoapMessage coapMessage, ChannelBuffer buffer) throws InvalidOptionException{

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

                switch(OptionValue.getOptionType(actualOptionNumber)){

                    case OptionValue.Type.EMPTY:
                        coapMessage.addOption(actualOptionNumber, new EmptyOptionValue(actualOptionNumber));
                        break;

                    case OptionValue.Type.OPAQUE:
                        coapMessage.addOption(actualOptionNumber, new OpaqueOptionValue(actualOptionNumber, optionValue));
                        break;

                    case OptionValue.Type.STRING:
                        coapMessage.addOption(actualOptionNumber, new StringOptionValue(actualOptionNumber, optionValue));
                        break;

                    case OptionValue.Type.UINT:
                        coapMessage.addOption(actualOptionNumber, new UintOptionValue(actualOptionNumber, optionValue));
                        break;

                    default:
                        log.error("This should never happen!");
                        throw new RuntimeException("This should never happen!");
                }

            }

            //failed option creation leads to an illegal argument exception
            catch (IllegalArgumentException e) {

                //Malformed options in responses are silently ignored...
                if(MessageCode.isResponse(coapMessage.getMessageCode()))
                    log.warn("Silently ignore malformed option no. {} in incoming response.", actualOptionNumber);
                
                //Critical malformed options in requests cause an exception
                else if(OptionValue.isCritical(actualOptionNumber))
                    throw new InvalidOptionException(actualOptionNumber);
                
                //Not critical malformed options in requests are silently ignored... 
                else
                    log.warn("Silently ignore elective option no. {} in incoming request.", actualOptionNumber);
                

            }

            previousOptionNumber = actualOptionNumber;

            if(buffer.readableBytes() > 0)
                firstByte = buffer.readByte() & 0xFF;

            log.debug("{} readable bytes remaining.", buffer.readableBytes());
        }
    }




    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent exceptionEvent){

        Throwable cause = exceptionEvent.getCause();

        //Invalid Header Exceptions cause a RST
        if(cause instanceof InvalidHeaderException){
            InvalidHeaderException ex = (InvalidHeaderException) cause;

            if (ex.getMessageID() != CoapMessage.MESSAGE_ID_UNDEFINED)
                writeReset(ctx, ex.getMessageID(), ex.getRemoteEndpoint());
            else
                log.warn("Ignore incoming message with malformed header...");
        }

        else if(cause instanceof InvalidOptionException){
            InvalidOptionException ex = (InvalidOptionException) cause;
            MessageType.Name messageType = ex.getMessageType() == MessageType.Name.CON.getNumber() ?
                    MessageType.Name.ACK : MessageType.Name.NON;

            writeBadOptionResponse(ctx, messageType, ex.getMessageID(), ex.getToken(), ex.getRemoteEndpoint(),
                    ex.getMessage());
        }

        else
            log.error("This should never happen (DecodingException with unsupported cause!", cause);

    }


    private void writeReset(ChannelHandlerContext ctx, int messageID, InetSocketAddress remoteEndpoint){
        CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
        Channels.write(ctx, Channels.future(ctx.getChannel()), resetMessage, remoteEndpoint);
    }


    private void writeBadOptionResponse(ChannelHandlerContext ctx, MessageType.Name messageType, int messageID,
                                        Token token, InetSocketAddress remoteEndpoint, String content) {

        CoapResponse errorResponse =
                CoapResponse.createErrorResponse(messageType, MessageCode.Name.BAD_OPTION_402, content);
        errorResponse.setMessageID(messageID);
        errorResponse.setToken(token);

        Channels.write(ctx, Channels.future(ctx.getChannel()), errorResponse, remoteEndpoint);
    }


    private static String toBinaryString(int byteValue){
        StringBuilder buffer = new StringBuilder(8);

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
