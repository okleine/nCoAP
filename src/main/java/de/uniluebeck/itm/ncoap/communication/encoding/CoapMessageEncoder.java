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

    public static final int MAX_OPTION_DELTA = 14;
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel ch, Object object) throws Exception{
        if(!(object instanceof CoapMessage)){
            return object instanceof ChannelBuffer ? object : null;
        }

        CoapMessage coapMessage = (CoapMessage) object;
        log.debug("CoapMessage to encode: {}", coapMessage);

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();

        //Encode HEADER
        encodeHeader(buffer, coapMessage);

        //Encode TOKEN
        if(coapMessage.getToken().length > 0)
            buffer.writeBytes(coapMessage.getToken());

        //Encode OPTIONS
        encodeOptions(buffer, coapMessage);

        //Add CONTENT
        ChannelBuffer encodedMessage = ChannelBuffers.wrappedBuffer(buffer, coapMessage.getContent());



        return encodedMessage;
    }

    private void encodeHeader(ChannelBuffer buffer, CoapMessage coapMessage){

        int encodedHeader = (coapMessage.getVersion() << 30) |
                (coapMessage.getMessageType() << 28) |
                (coapMessage.getToken().length << 24) |
                (coapMessage.getMessageCode() << 16) |
                (coapMessage.getMessageID());

        buffer.writeInt(encodedHeader);

        if(log.isDebugEnabled()){
            String binary = Integer.toBinaryString(encodedHeader);
            while(binary.length() < 32){
                binary = "0" + binary;
            }
            log.debug("Encoded Header: {}", binary);
        }

    }

    private void encodeOptions(ChannelBuffer buffer, CoapMessage coapMessage) throws Exception {

        //Encode options one after the other and append buf option to the buf
        int prevNumber = 0;

        for(int optionNumber : coapMessage.getAllOptions().keySet()){
            for(Option option : coapMessage.getOptions(optionNumber))
                encodeOption(buffer, optionNumber, option, prevNumber);
        }

        buffer.writeByte(255);
    }


    private void encodeOption(ChannelBuffer buffer, int optionNumber, Option option, int prevNumber)
            throws Exception {

        //The previous option number must be smaller or equal to the actual one
        if(prevNumber > optionNumber){
            String msg = "Previous option no. (" + prevNumber + ") for encoding must not be larger then current " +
                    "option no (" + optionNumber + ")";
            throw new EncodingFailedException(msg);
        }

        int optionDelta = optionNumber - prevNumber;
        int optionLength = option.getValue().length;

        if(optionLength > 65804)
            throw new EncodingFailedException("Option no. " + optionNumber + " exceeds maximum option length "
                    + "(actual: " + optionLength + ", maximum: " + 65804 + ")");

        if(optionDelta > 65804)
            throw new EncodingFailedException("Option no. " + optionNumber + " exceeds maximum option delta "
                    + "(actual: " + optionDelta + ", maximum: " + 65804 + ")");


        //option delta < 13
        if(optionDelta < 13){
            if(optionLength < 13)
                buffer.writeByte(optionDelta << 4 | optionLength);

            else if (optionLength < 268)
                buffer.writeByte(optionDelta << 4 | 13);

            else
                buffer.writeByte(optionDelta << 4 | 14);
        }

        //13 <= option delta < 269
        else if(optionDelta < 269){

            if(optionLength < 13){
                buffer.writeByte(13 << 4 | optionLength);
                buffer.writeByte(optionDelta - 13);
            }

            else if (optionLength < 269){
                buffer.writeByte(13 << 4 | 13);
                buffer.writeByte(optionDelta - 13);
                buffer.writeByte(optionLength - 13);
            }

            else{
                buffer.writeByte(13 << 4 | 14);
                buffer.writeByte(optionDelta - 13);
                buffer.writeByte((optionLength - 269) >>> 8);
                buffer.writeByte((optionLength - 269)  << 24 >>> 24);
            }
        }

        //269 <= option delta < 65805
        else{

            if(optionLength < 13){
                buffer.writeByte(14 << 4 | optionLength);
                buffer.writeByte((optionDelta - 269) >>> 8);
                buffer.writeByte((optionDelta - 269) << 24 >>> 24);
            }

            else if (optionLength < 269){
                buffer.writeByte(14 << 4 | 13);
                buffer.writeByte((optionDelta - 269) >>> 8);
                buffer.writeByte((optionDelta - 269) << 24 >>> 24);
                buffer.writeByte(optionLength - 13);
            }

            else{
                buffer.writeByte(14 << 4 | 14);
                buffer.writeByte((optionDelta - 269) >>> 8);
                buffer.writeByte((optionLength - 269) >>> 8);
                buffer.writeByte((optionLength - 269)  << 24 >>> 24);
            }
        }

        //Write option value
        buffer.writeBytes(option.getValue());
    }
}
