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

package de.uniluebeck.itm.spitfire.nCoap.communication.encoding;

import com.google.common.primitives.UnsignedBytes;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import java.net.InetSocketAddress;

/**
 *
 * @author Oliver Kleine
 */
public class CoapMessageDecoder extends OneToOneDecoder{

    private static Logger log = Logger.getLogger(CoapMessageDecoder.class.getName());

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object obj) throws Exception {
        //Do nothing but return the given object if it's not an instance of ChannelBuffer
        if(!(obj instanceof ChannelBuffer)){
            return obj;
        }

        ChannelBuffer buffer = (ChannelBuffer) obj;

        //Decode incoming message
        if(log.isDebugEnabled()){
            log.debug("[Message] Create new message object from ChannelBuffer");
        }

        //Decode the Message Header which must have a length of exactly 4 bytes
        if(buffer.readableBytes() < 4){
            String msg = "Buffer must contain at least readable 4 bytes (but has " + buffer.readableBytes() + ")";
            throw new InvalidHeaderException(msg);
        }

        //Decode the header values (version: 2 bits, msgType: 2 bits, optionCount: 4 bits, code: 4 bits, msgID: 8 bits)
        int encHeader = buffer.readInt();
        int msgTypeNumber = ((encHeader << 2) >>> 30);
        int optionCount = ((encHeader << 4) >>> 28);
        int codeNumber = ((encHeader << 8) >>> 24);
        int msgID = ((encHeader << 16) >>> 16);

        Header header =
                new Header(MsgType.getMsgTypeFromNumber(msgTypeNumber), Code.getCodeFromNumber(codeNumber), msgID);

        if(log.isDebugEnabled()){
            log.debug("[CoAPMessageDecoder] New Header created from ChannelBuffer (type: " + header.getMsgType() +
                    ", code: " + header.getCode() + ", msgID: " + header.getMsgID() + ")");
        }

        //Create OptionList
        try{
            OptionList optionList = decodeOptionList(buffer, optionCount, Code.getCodeFromNumber(codeNumber));

            //The remaining bytes (if any) are the messages payload. If there is no payload, reader and writer index are
            //at the same position (buf.readableBytes() == 0).
            buffer.discardReadBytes();

            CoapMessage result;

            if(header.getCode().isRequest()){
                result = new CoapRequest(header, optionList, buffer);
            }
            else{
                result = new CoapResponse(header, optionList, buffer);
            }

            result.setRcptAdress(((InetSocketAddress)channel.getLocalAddress()).getAddress());

            return result;
        }
        catch(InvalidOptionException e){
            //TODO send RST
            return null;
        }
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
     * @param buffer The ChannelBuffer containing the options to be decoded
     * @param optionCount The number of options to be decoded
     * @param code The message code of the message that is intended to include the new OptionList
     * @return An OptionList object containing the decoded options
     * @throws InvalidOptionException if a critical option is malformed, e.g. size is out of defined bounds
     * @throws ToManyOptionsException if there are too many options contained in the list
     */
    private OptionList decodeOptionList(ChannelBuffer buffer, int optionCount, Code code)
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
                Option newOption = decodeOption(buffer, prevOptionNumber);
                 //Add new Option to the list
                OptionName optionName = OptionRegistry.getOptionName(newOption.getOptionNumber());
                result.addOption(code, optionName, newOption);
                prevOptionNumber = newOption.getOptionNumber();
            }
            catch(InvalidOptionException e){
                if(e.isCritical()){
                    log.error("[CoapMessageDecoder] Malformed " + e.getOptionName() + " option is critical. Send RST!");
                    throw e;
                }
                if(log.isDebugEnabled()){
                    log.debug("[CoapMessageDecoder] Malformed " + e.getOptionName() + " option silently ignored.");
                }
            }
        }

        return result;
    }

    /**
     * This static methodes creates reads and decodes the Option starting at the current reader index of
     * the given ChannelBuffer. Thus, there must be an encoded option starting at the current reader index.
     * Otherwise an InvalidOptionException is thrown
     * @param buf A ChannelBuffer with its reader index at an options starting position
     * @param prevOptionNumber The option number of the previous option in the ChannelBuffer (or ZERO if there is no)
     * @return The decoded Option
     * @throws InvalidOptionException if the option to be decoded is invalid
     */
    private Option decodeOption(ChannelBuffer buf, int prevOptionNumber) throws InvalidOptionException {
        byte firstByte = buf.readByte();

        //Exclude option delta and add to previous option optionNumber
        OptionName optionName = OptionRegistry
                .getOptionName((UnsignedBytes.toInt(firstByte) >>> 4) +  prevOptionNumber);

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
            throw new InvalidOptionException(optionName, "[Option] " + optionName + " options must have a value length"
                    + " between " + minLength + " and " + maxLength + " (both including) but has " +  valueLength);
        }

        if(log.isDebugEnabled()){
            log.debug("[Option] Creating " + optionName + " option with encoded value length of " + valueLength + " bytes");
        }

        //Read encoded value from buffer
        byte[] encodedValue = new byte[valueLength];
        buf.readBytes(encodedValue);

        //Create appropriate Option
        return Option.createOption(optionName, encodedValue);
    }

}
