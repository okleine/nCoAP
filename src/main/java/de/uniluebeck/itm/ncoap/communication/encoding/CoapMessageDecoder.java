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
/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.ncoap.communication.encoding;

import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.UnknownOptionException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;


/**
* A {@link CoapMessageDecoder} de-serializes incoming messages.
*
* @author Oliver Kleine
*/
public class CoapMessageDecoder extends OneToOneDecoder{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object object) throws DecodingFailedException {

        //Do nothing but return the given object if it's not an instance of ChannelBuffer
        if(!(object instanceof ChannelBuffer)){
            return object;
        }

        ChannelBuffer buffer = (ChannelBuffer) object;
        log.debug("Incoming message to be decoded (length: {})", buffer.readableBytes());

        //Decode the Message Header which must have a length of exactly 4 bytes
        if(buffer.readableBytes() < 4)
            throw new DecodingFailedException(MessageType.Name.UNKNOWN.getNumber(), CoapMessage.MESSAGE_ID_UNDEFINED,
                    new InvalidHeaderException("Buffer must contain at least readable 4 " +
                            "bytes (but has " + buffer.readableBytes() + ")"));


        //Decode the header values
        int encodedHeader = buffer.readInt();
        int version = (encodedHeader >>> 30) & 0b11;
        int messageType = (encodedHeader >>> 28) & 0b11;
        int tokenLength = (encodedHeader >>> 24) & 0b1111;
        int messageCode = (encodedHeader >>> 16) & 0b11111111;
        int messageID = encodedHeader & 0b1111111111111111;

        log.debug("Decoded Header: (T) {}, (TKL) {}, (C) {}, (ID) {}",
                new Object[]{messageType, tokenLength, messageCode, messageID});

        if(version != CoapMessage.PROTOCOL_VERSION)
            throw new DecodingFailedException(messageType, messageID,
                    new InvalidHeaderException("Unsupported CoAP protocol version: " + version));


        //Check whether message code indicates empty message and either TKL is greater than 0 or there are unread bytes
        //following the header
        if(messageCode == MessageCode.Name.EMPTY.getNumber()){
            if(tokenLength > 0){
                String message = "Invalid TKL header value for empty message: " + tokenLength;
                log.warn(message);
                throw new DecodingFailedException(messageType, messageID,
                        new InvalidHeaderException(message));
            }

            if(buffer.readableBytes() > 0){
                String message = "Empty messages MUST NOT contain anything but the 4 bytes header "
                        + " (actual remaining bytes after header: " + buffer.readableBytes() + ")";
                log.warn(message);
                throw new DecodingFailedException(messageType, messageID,
                        new InvalidMessageException(message));
            }
        }

        //Check whether TKL indicates a not allowed token length
        if(tokenLength > CoapMessage.MAX_TOKEN_LENGTH)
            throw new DecodingFailedException(messageType, messageID,
                    new InvalidMessageException("TKL value out of bounds (max: " + CoapMessage.MAX_TOKEN_LENGTH
                        + ", actual: " + tokenLength + ")."));


        //Check whether there are enough unread bytes left to read the token
        if(buffer.readableBytes() < tokenLength)
            throw new DecodingFailedException(messageType, messageID,
                    new InvalidHeaderException("TKL header value: " + tokenLength + ", Readable bytes: " +
                            buffer.readableBytes() + "."));

        //Decode the token
        byte[] token = new byte[tokenLength];
        buffer.readBytes(token);

        //Create CoAP message object
        CoapMessage coapMessage = null;
        try {
            coapMessage = CoapMessage.createCoapMessage(messageType, messageCode, messageID, token);
        }
        catch (InvalidHeaderException e) {
            throw new DecodingFailedException(messageType, messageID, e);
        }

        //If message code indicates empty message, follow-up bytes (i.e. token, options, and content) are ignored.
        if(coapMessage.getMessageCode() == MessageCode.Name.EMPTY.getNumber() && buffer.readableBytes() > 0){
            if(buffer.readableBytes() > 0)
                log.warn("Ignore remaining {} bytes after header that indicates empty message.", buffer.readableBytes());

            return coapMessage;
        }

        //Decode and set the options
        try{
            if(buffer.readableBytes() > 0)
                setOptions(coapMessage, buffer);
        }
        catch (InvalidOptionException e){
            throw new DecodingFailedException(messageType, messageID, e);
        }

        //The remaining bytes (if any) are the messages payload. If there is no payload, reader and writer index are
        //at the same position (buf.readableBytes() == 0).
        buffer.discardReadBytes();
        try {
            coapMessage.setContent(buffer);
        }
        catch (InvalidMessageException e) {
            throw new DecodingFailedException(messageType, messageID, e);
        }

        //TODO Set IP address of local socket (currently [0::] for wildcard address)
        if(channel != null){
            InetAddress rcptAddress = ((InetSocketAddress) channel.getLocalAddress()).getAddress();
            coapMessage.setRecipientAddress(rcptAddress);
            log.debug("Set receipient address to: " + rcptAddress);
        }

        log.info("Decoded Message: {}", coapMessage);

        return coapMessage;
    }


    private void setOptions(CoapMessage coapMessage, ChannelBuffer buffer) throws InvalidOptionException {

        //Decode the options
        int previousOption = 0;
        int firstByte = buffer.readByte() & 0xFF;

        while(firstByte != 255 && buffer.readableBytes() > 0){
            log.debug("First byte: {}", firstByte);
            int optionDelta = (firstByte & 0xFF) >>> 4;
            int optionLength = firstByte & 0x0F;

            if(optionDelta == 13)
                optionDelta += buffer.readByte() & 0xFF;

            else if(optionDelta == 14){
                optionDelta += (buffer.readByte() << 8) & 0xFF00;
                optionDelta += buffer.readByte() & 0xFF;
            }

            if(optionLength == 13)
                optionLength += buffer.readByte() & 0xFF;

            else if(optionLength == 14){
                optionLength += (buffer.readByte() << 8) & 0xFF00;
                optionLength += buffer.readByte() & 0xFF;
            }

            int optionNumber = previousOption + optionDelta;
            log.debug("Decode option no. {} with length of {} bytes.", optionNumber, optionLength);

            try {
                byte[] optionValue = new byte[optionLength];
                buffer.readBytes(optionValue);
                coapMessage.addOption(optionNumber, Option.createOption(optionNumber, optionValue));
            }
            catch (UnknownOptionException e) {
                if(Option.isCritical(optionNumber)){
                    String message = "Could not decode unsupported critical option no. " + optionNumber;
                    log.warn(message);
                    throw new InvalidOptionException(optionNumber, message);
                }

                log.info("Silently ignored unsupported elective option no. {}", optionNumber);

            }
            catch (InvalidOptionException e) {
                if(Option.isCritical(optionNumber)){
                    String message = "Could not decode malformed option no. " + optionNumber;
                    log.warn(message);
                    throw new InvalidOptionException(optionNumber, message);
                }

                log.info("Silently ignored malformed elective option no. {}", optionNumber);
            }

            previousOption = optionNumber;
            if(buffer.readableBytes() > 0)
                firstByte = buffer.readByte() & 0xFF;

            log.debug("{} readable bytes remaining.", buffer.readableBytes());
        }
    }

//    /**
//     * This method method creates an OptionList containing the specified number of options. It does
//     * not matter whether there are more options or payload contained in the ChannelBuffer. The
//     * creation process stops right after the specified number of options. This method assumes
//     * the first option to begin at the current reader position of the ChannelBuffer.
//     *
//     * After the creation process the reader index of the ChannelBuffer points to the position
//     * right after the last byte used to create the last option in the OptionList. In most
//     * cases this will be the starting position of the payload (if there is any).
//     *
//     * Note, that eventually contained malformed but elective options will not be added to the list but will be
//     * silently ignored. Malformed critical options cause an InvalidOptionException. No list will be created in the
//     * latter case.
//     *
//     * @param buffer the {@link ChannelBuffer} containing the options to be decoded
//     * @param optionCount the number of options to be decoded
//     * @param messageCode the {@link de.uniluebeck.itm.ncoap.message.MessageCode} of the message that is intended to include the new OptionList
//     * @param header the {@link Header} of the message to be decoded
//     *
//     * @return An {@link OptionList} instance containing the decoded options
//     *
//     * @throws InvalidOptionException if a critical option is malformed, e.g. size is out of defined bounds
//     * @throws ToManyOptionsException if there are too many options contained in the list
//     */
//    private OptionList decodeOptionList(ChannelBuffer buffer, int optionCount, MessageCode messageCode, Header header)
//            throws InvalidOptionException, ToManyOptionsException {
//
//        if(optionCount > 15){
//            throw new ToManyOptionsException("Option count of " + optionCount +
//                    " exceeds the number of allowed options");
//        }
//
//        OptionList result = new OptionList();
//        int prevOptionNumber = 0;
//        for(int i = 0; i < optionCount; i++){
//            //Create the next readable option from the ChannelBuffer and move the buffers read-index to
//            //the starting position of the next option (resp. of the payload if existing)
//            try{
//                Option newOption = decodeOption(buffer, prevOptionNumber, header);
//                 //Add new Option to the list
//                log.debug("Option with number 0x{} to be created.", newOption.getOptionNumber());
//                OptionName optionName = OptionName.getByNumber(newOption.getOptionNumber());
//                log.debug("Option " + optionName + " to be created.");
//                result.addOption(messageCode, optionName, newOption);
//                prevOptionNumber = Math.abs(newOption.getOptionNumber()); //double datatype for observe option hack
//            }
//            catch(InvalidOptionException e){
//                if(e.isCritical()){
//                    log.error("Malformed " + e.getOptionName() + " option is critical.");
//                    throw e;
//                }
//                log.debug("Malformed " + e.getOptionName() + " option silently ignored.", e);
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * This method creates reads and decodes the {@link Option} starting at the current reader index of
//     * the given {@link ChannelBuffer}. Thus, there must be an encoded option starting at the current reader index.
//     * Otherwise an {@link InvalidOptionException} is thrown.
//     *
//     * @param buf a {@link ChannelBuffer} with its reader index at an options starting position
//     * @param prevOptionNumber the option number of the previous option in the {@link ChannelBuffer}
//     *                         (or ZERO if there is no)
//     * @param header the {@link Header} of the message to be decoded
//     *
//     * @return The decoded {@link de.uniluebeck.itm.ncoap.message.options.Option}
//     *
//     * @throws InvalidOptionException if the option to be decoded is invalid
//     */
//    private Option decodeOption(ChannelBuffer buf, int prevOptionNumber, Header header) throws InvalidOptionException {
//        byte firstByte = buf.readByte();
//
//        //Exclude option delta and add to previous option optionNumber
//        int optionNumber = (UnsignedBytes.toInt(firstByte) >>> 4) +  prevOptionNumber;
//
//        // Small hack, due to two types of the observe option
//        if(!header.getMessageCode().isRequest() && optionNumber == OBSERVE_REQUEST.getNumber()) {
//            optionNumber = OBSERVE_RESPONSE.getNumber();
//        }
//
//        OptionName optionName = OptionName.getByNumber(optionNumber);
//        log.debug("Option name of number {} is {}", optionNumber, optionName);
//
//        if(optionName == OptionName.UNKNOWN)
//            throw new InvalidOptionException(optionNumber, "Unknown option number " + optionNumber);
//
//        //Option optionNumber 21 is "If-none-match" and must not contain any value. This is e.g. useful for
//        //PUT requests not being supposed to overwrite existing resources
//        if(optionName.equals(OptionRegistry.OptionName.IF_NONE_MATCH)){
//            return Option.createEmptyOption(optionName);
//        }
//
//        //Exclude options valueLength. If all of the last 4 digits of the first byte are 1,
//        //the valueLength must be calculated by 15 + the second bytes value treated as unsigned.
//        int valueLength = firstByte & 0x0f;
//        if(valueLength  == 15){
//            valueLength = UnsignedBytes.toInt(buf.readByte()) + 15;
//        }
//
//        //Determine option specific valueLength constraints
//        int minLength = OptionRegistry.getMinLength(optionName);
//        int maxLength = OptionRegistry.getMaxLength(optionName);
//
//        if(valueLength < minLength || valueLength > maxLength){
//            throw new InvalidOptionException(optionNumber, optionName + " option must have a value length"
//                    + " between " + minLength + " and " + maxLength + " (both including) but has " +  valueLength);
//        }
//
//        //Read encoded value from buffer
//        byte[] encodedValue = new byte[valueLength];
//        buf.readBytes(encodedValue);
//
//        //Create appropriate Option
//        Option result = Option.createOption(optionName, encodedValue);
//
//        log.debug("Decoded {} {}", optionName, result.getDecodedValue());
//        return result;
//    }

}
