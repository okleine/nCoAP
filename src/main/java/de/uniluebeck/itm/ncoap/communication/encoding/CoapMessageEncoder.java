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

import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* A {@link CoapMessageEncoder} serializes outgoing {@link CoapMessage}s.
*
* @author Oliver Kleine
*/
public class CoapMessageEncoder extends OneToOneEncoder {

    public static final int MAX_OPTION_DELTA = 65804;
    public static final int MAX_OPTION_LENGTH = 65804;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel ch, Object object) throws EncodingFailedException{
        if(!(object instanceof CoapMessage)){
            return object instanceof ChannelBuffer ? object : null;
        }

        CoapMessage coapMessage = (CoapMessage) object;
        log.debug("CoapMessage to encode: {}", coapMessage);

        if(coapMessage.getMessageID() == CoapMessage.MESSAGE_ID_UNDEFINED)
            throw new EncodingFailedException(coapMessage.getMessageType(), CoapMessage.MESSAGE_ID_UNDEFINED,
                    new InvalidHeaderException("Message ID is not defined."));

        if(coapMessage.getToken().length > CoapMessage.MAX_TOKEN_LENGTH)
            throw new EncodingFailedException(coapMessage.getMessageType(), coapMessage.getMessageID(),
                    new InvalidHeaderException("Maximum token length is " + CoapMessage.MAX_TOKEN_LENGTH + " (actual: "
                        + coapMessage.getToken().length + ")"));

        //Start encoding
        ChannelBuffer encodedMessage = ChannelBuffers.dynamicBuffer();

        //Encode HEADER
        encodeHeader(encodedMessage, coapMessage);
        log.debug("Encoded length of message (after HEADER): {}", encodedMessage.readableBytes());

        //Encode TOKEN
        if(coapMessage.getToken().length > 0)
            encodedMessage.writeBytes(coapMessage.getToken());

        log.debug("Encoded length of message (after TOKEN): {}", encodedMessage.readableBytes());

        if(coapMessage.getAllOptions().size() == 0 && coapMessage.getContent().readableBytes() == 0)
            return encodedMessage;


        //Encode OPTIONS
        try{
            encodeOptions(encodedMessage, coapMessage);
        }
        catch(InvalidOptionException e){
            throw new EncodingFailedException(coapMessage.getMessageType(), coapMessage.getMessageID(), e);
        }
        log.debug("Encoded length of message (after OPTIONS): {}", encodedMessage.readableBytes());

        //Add CONTENT
        encodedMessage = ChannelBuffers.wrappedBuffer(encodedMessage, coapMessage.getContent());
        log.debug("Encoded length of message (after CONTENT): {}", encodedMessage.readableBytes());

        return encodedMessage;
    }

    protected void encodeHeader(ChannelBuffer buffer, CoapMessage coapMessage){

        int encodedHeader = ((coapMessage.getProtocolVersion()  & 0b11)     << 30)
                          | ((coapMessage.getMessageType()      & 0b11)     << 28)
                          | ((coapMessage.getToken().length     & 0b1111)   << 24)
                          | ((coapMessage.getMessageCode()      & 0xFF)     << 16)
                          | ((coapMessage.getMessageID()        & 0xFFFF));

        buffer.writeInt(encodedHeader);

        if(log.isDebugEnabled()){
            String binary = Integer.toBinaryString(encodedHeader);
            while(binary.length() < 32){
                binary = "0" + binary;
            }
            log.debug("Encoded Header: {}", binary);
        }

    }

    protected void encodeOptions(ChannelBuffer buffer, CoapMessage coapMessage) throws InvalidOptionException {

        //Encode options one after the other and append buf option to the buf
        int prevNumber = 0;

        for(int optionNumber : coapMessage.getAllOptions().keySet()){
            for(Option option : coapMessage.getOptions(optionNumber))
                encodeOption(buffer, optionNumber, option, prevNumber);
        }

        buffer.writeByte(255);
    }


    protected void encodeOption(ChannelBuffer buffer, int optionNumber, Option option, int prevNumber)
            throws InvalidOptionException {

        //The previous option number must be smaller or equal to the actual one
        if(prevNumber > optionNumber)
            throw new InvalidOptionException(optionNumber, "Previous option no. (" + prevNumber +
                    ") for encoding must not be larger then current option no (" + optionNumber + ")");


        int optionDelta = optionNumber - prevNumber;
        int optionLength = option.getValue().length;

        if(optionLength > MAX_OPTION_LENGTH)
            throw new InvalidOptionException(optionNumber, "Option no. " + optionNumber +
                    " exceeds maximum option length (actual: " + optionLength + ", maximum: " +
                    MAX_OPTION_LENGTH + ")");

        if(optionDelta > MAX_OPTION_DELTA)
            throw new InvalidOptionException(optionNumber, "Option no. " + optionNumber +
                    " exceeds maximum option delta (actual: " + optionDelta + ", maximum: " + MAX_OPTION_DELTA + ")");


        //option delta < 13
        if(optionDelta < 13){

            if(optionLength < 13){
                buffer.writeByte(((optionDelta << 4) & 0xFF) | (optionLength & 0xFF));
            }

            else if (optionLength < 269){
                buffer.writeByte(((optionDelta << 4) & 0xFF) | (13 & 0xFF));
                buffer.writeByte((optionLength - 13) & 0xFF);
            }

            else{
                buffer.writeByte(((optionDelta << 4) & 0xFF) | (14 & 0xFF));
                buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionLength - 269) & 0xFF);
            }
        }

        //13 <= option delta < 269
        else if(optionDelta < 269){

            if(optionLength < 13){
                buffer.writeByte(((13 & 0xFF) << 4) | (optionLength & 0xFF));
                buffer.writeByte((optionDelta - 13) & 0xFF);
            }

            else if (optionLength < 269){
                buffer.writeByte(((13 & 0xFF) << 4) | (13 & 0xFF));
                buffer.writeByte((optionDelta - 13) & 0xFF);
                buffer.writeByte((optionLength - 13) & 0xFF);
            }

            else{
                buffer.writeByte((13 & 0xFF) << 4 | (14 & 0xFF));
                buffer.writeByte((optionDelta - 13) & 0xFF);
                buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionLength - 269) & 0xFF);
            }
        }

        //269 <= option delta < 65805
        else{

            if(optionLength < 13){
                buffer.writeByte(((14 & 0xFF) << 4) | (optionLength & 0xFF));
                buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionDelta - 269) & 0xFF);
            }

            else if (optionLength < 269){
                buffer.writeByte(((14 & 0xFF) << 4) | (13 & 0xFF));
                buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionDelta - 269) & 0xFF);
                buffer.writeByte((optionLength - 13) & 0xFF);
            }

            else{
                buffer.writeByte(((14 & 0xFF) << 4) | (14 & 0xFF));
                buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionDelta - 269) & 0xFF);
                buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
                buffer.writeByte((optionLength - 269) & 0xFF);
            }
        }

        //Write option value
        buffer.writeBytes(option.getValue());
    }
}
