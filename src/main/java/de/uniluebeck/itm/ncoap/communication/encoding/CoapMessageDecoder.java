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

import com.google.common.primitives.UnsignedBytes;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.Option;
import de.uniluebeck.itm.ncoap.message.OptionName;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;


/**
* A {@link CoapMessageDecoder} de-serializes incoming messages.
*
* @author Oliver Kleine
*/
public class CoapMessageDecoder extends OneToOneDecoder{

    private static Logger log = LoggerFactory.getLogger(CoapMessageDecoder.class.getName());

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object object) throws DecodingFailedException {

        //Do nothing but return the given object if it's not an instance of ChannelBuffer
        if(!(object instanceof ChannelBuffer)){
            return object;
        }

        ChannelBuffer buffer = (ChannelBuffer) object;

        //Decode the Message Header which must have a length of exactly 4 bytes
        if(buffer.readableBytes() < 4){
            String msg = "Buffer must contain at least readable 4 bytes (but has " + buffer.readableBytes() + ")";
            throw new DecodingFailedException(msg);
        }

        //Decode the header values
        int encodedHeader = buffer.readInt();
        int version = encodedHeader >>> 30;
        if(version != 1)
            throw new DecodingFailedException("Unsupported CoAP protocol version: " + version);

        int messageType = encodedHeader << 2 >>> 30;
        int tokenLength = encodedHeader << 4 >>> 28;
        int messageCode = encodedHeader << 8 >>> 24;
        int messageID = encodedHeader << 16 >>> 16;

        log.debug("Decoded Header: (T) {}, (TKL) {}, (C) {}, (ID) {}",
                new Object[]{messageType, tokenLength, messageCode, messageID});


        //Decode the token
        byte[] token = new byte[tokenLength];
        buffer.readBytes(token);

        //Decode the options
        byte firstByte = buffer.readByte();
        while(firstByte != -128){
            int optionDelta = firstByte <<
        }




        //The remaining bytes (if any) are the messages payload. If there is no payload, reader and writer index are
        //at the same position (buf.readableBytes() == 0).
        buffer.discardReadBytes();

        CoapMessage result;

        if(header.getMessageCode().isRequest()){
            result = new CoapRequest(header, optionList, buffer);
            log.debug("Decoded CoapRequest.");
        }
        else if (header.getMessageCode().isResponse()){
            result = new CoapResponse(header, optionList, buffer);
            log.debug("Decoded CoapResponse.");
        }
        else if(header.getMessageCode() == MessageCode.EMPTY){
            if(header.getMessageType() == MessageType.ACK)
                result = CoapMessage.createEmptyAcknowledgement(header.getMsgID());
            else if(header.getMessageType() == MessageType.RST)
                result = CoapMessage.createEmptyReset(header.getMsgID());

            else
                throw new EncodingFailedException("Not decodable header: " + header);
        }
        else{
            throw new EncodingFailedException("Not decodable header: " + header);
        }

        //TODO Set IP address of local socket (currently [0::] for wildcard address)
        InetAddress rcptAddress = ((InetSocketAddress) channel.getLocalAddress()).getAddress();
        result.setRcptAdress(rcptAddress);

        log.debug("Set receipient address to: " + rcptAddress);
        log.info("Decoded payload: {}", result.getContent().toString(Charset.forName("UTF-8")));
        log.info("Decoded payload size: {}", result.getContent().readableBytes());
        log.info("Decoded: " + result);
        return result;
    }

    /**
     * This method method creates an OptionList containing the specified number of options. It does
     * not matter whether there are more options or payload contained in the ChannelBuffer. The
     * creation process stops right after the specified number of options. This method assumes
     * the first option to begin at the current reader position of the ChannelBuffer.
     *
     * After the creation process the reader index of the ChannelBuffer points to the position
     * right after the last byte used to create the last option in the OptionList. In most
     * cases this will be the starting position of the payload (if there is any).
     *
     * Note, that eventually contained malformed but elective options will not be added to the list but will be
     * silently ignored. Malformed critical options cause an InvalidOptionException. No list will be created in the
     * latter case.
     *
     * @param buffer the {@link ChannelBuffer} containing the options to be decoded
     * @param optionCount the number of options to be decoded
     * @param messageCode the {@link de.uniluebeck.itm.ncoap.message.MessageCode} of the message that is intended to include the new OptionList
     * @param header the {@link Header} of the message to be decoded
     *
     * @return An {@link OptionList} instance containing the decoded options
     *
     * @throws InvalidOptionException if a critical option is malformed, e.g. size is out of defined bounds
     * @throws ToManyOptionsException if there are too many options contained in the list
     */
    private OptionList decodeOptionList(ChannelBuffer buffer, int optionCount, MessageCode messageCode, Header header)
            throws InvalidOptionException, ToManyOptionsException {

        if(optionCount > 15){
            throw new ToManyOptionsException("Option count of " + optionCount +
                    " exceeds the number of allowed options");
        }

        OptionList result = new OptionList();
        int prevOptionNumber = 0;
        for(int i = 0; i < optionCount; i++){
            //Create the next readable option from the ChannelBuffer and move the buffers read-index to
            //the starting position of the next option (resp. of the payload if existing)
            try{
                Option newOption = decodeOption(buffer, prevOptionNumber, header);
                 //Add new Option to the list
                log.debug("Option with number 0x{} to be created.", newOption.getOptionNumber());
                OptionName optionName = OptionName.getByNumber(newOption.getOptionNumber());
                log.debug("Option " + optionName + " to be created.");
                result.addOption(messageCode, optionName, newOption);
                prevOptionNumber = Math.abs(newOption.getOptionNumber()); //double datatype for observe option hack
            }
            catch(InvalidOptionException e){
                if(e.isCritical()){
                    log.error("Malformed " + e.getOptionName() + " option is critical.");
                    throw e;
                }
                log.debug("Malformed " + e.getOptionName() + " option silently ignored.", e);
            }
        }

        return result;
    }

    /**
     * This method creates reads and decodes the {@link Option} starting at the current reader index of
     * the given {@link ChannelBuffer}. Thus, there must be an encoded option starting at the current reader index.
     * Otherwise an {@link InvalidOptionException} is thrown.
     *
     * @param buf a {@link ChannelBuffer} with its reader index at an options starting position
     * @param prevOptionNumber the option number of the previous option in the {@link ChannelBuffer}
     *                         (or ZERO if there is no)
     * @param header the {@link Header} of the message to be decoded
     *
     * @return The decoded {@link de.uniluebeck.itm.ncoap.message.Option}
     *
     * @throws InvalidOptionException if the option to be decoded is invalid
     */
    private Option decodeOption(ChannelBuffer buf, int prevOptionNumber, Header header) throws InvalidOptionException {
        byte firstByte = buf.readByte();

        //Exclude option delta and add to previous option optionNumber
        int optionNumber = (UnsignedBytes.toInt(firstByte) >>> 4) +  prevOptionNumber;

        // Small hack, due to two types of the observe option
        if(!header.getMessageCode().isRequest() && optionNumber == OBSERVE_REQUEST.getNumber()) {
            optionNumber = OBSERVE_RESPONSE.getNumber();
        }

        OptionName optionName = OptionName.getByNumber(optionNumber);
        log.debug("Option name of number {} is {}", optionNumber, optionName);

        if(optionName == OptionName.UNKNOWN)
            throw new InvalidOptionException(optionNumber, "Unknown option number " + optionNumber);

        //Option optionNumber 21 is "If-none-match" and must not contain any value. This is e.g. useful for
        //PUT requests not being supposed to overwrite existing resources
        if(optionName.equals(OptionRegistry.OptionName.IF_NONE_MATCH)){
            return Option.createEmptyOption(optionName);
        }

        //Exclude options valueLength. If all of the last 4 digits of the first byte are 1,
        //the valueLength must be calculated by 15 + the second bytes value treated as unsigned.
        int valueLength = firstByte & 0x0f;
        if(valueLength  == 15){
            valueLength = UnsignedBytes.toInt(buf.readByte()) + 15;
        }

        //Determine option specific valueLength constraints
        int minLength = OptionRegistry.getMinLength(optionName);
        int maxLength = OptionRegistry.getMaxLength(optionName);

        if(valueLength < minLength || valueLength > maxLength){
            throw new InvalidOptionException(optionNumber, optionName + " option must have a value length"
                    + " between " + minLength + " and " + maxLength + " (both including) but has " +  valueLength);
        }

        //Read encoded value from buffer
        byte[] encodedValue = new byte[valueLength];
        buf.readBytes(encodedValue);

        //Create appropriate Option
        Option result = Option.createOption(optionName, encodedValue);

        log.debug("Decoded {} {}", optionName, result.getDecodedValue());
        return result;
    }

}
