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
import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.OptionList;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.OBSERVE_REQUEST;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.OBSERVE_RESPONSE;

/**
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
        encodeHeader(buffer, coapMessage.getHeader(), coapMessage.getOptionCount());
        encodeOptions(buffer, coapMessage.getOptionList());

        ChannelBuffer buf = ChannelBuffers.wrappedBuffer(buffer, coapMessage.getPayload());



        return buf;
    }

    private void encodeHeader(ChannelBuffer buffer, Header header, int optionCount){
        int encodedHeader = (header.getVersion() << 30) |
                (header.getMsgType().number << 28) |
                (optionCount << 24) |
                (header.getCode().number << 16) |
                (header.getMsgID());

        buffer.writeInt(encodedHeader);

        if(log.isDebugEnabled()){
            String binary = Integer.toBinaryString(encodedHeader);
            while(binary.length() < 32){
                binary = "0" + binary;
            }
            log.debug("Encoded Header: {}", binary);
        }
    }

    private void encodeOptions(ChannelBuffer buffer, OptionList optionList) throws Exception {

        //Encode options one after the other and append buf option to the buf
        int prevNumber = 0;

        for(OptionName optionName : OptionName.values()){
            for(Option option : optionList.getOption(optionName)){

                // Small hack, due to two types of the observe option
                if(optionName == OBSERVE_RESPONSE) {
                    encodeOption(buffer, OBSERVE_REQUEST, option, prevNumber);
                    prevNumber = OBSERVE_REQUEST.getNumber();

                } else {
                    encodeOption(buffer, optionName, option, prevNumber);
                    prevNumber = optionName.getNumber();
                }

                log.debug("Encoded {}: {}", optionName, new ByteArrayWrapper(option.getValue()));
            }
        }
    }

    private void encodeOption(ChannelBuffer buffer, OptionName optionName, Option option, int prevNumber)
            throws Exception {

        //The previous option number must be smaller or equal to the actual one
        if(prevNumber > optionName.getNumber()){
            String msg = "Parameter value prevNumber (" + prevNumber + ") for encoding must not be larger then current " +
                    "option number (" + optionName.getNumber() + ")";
            throw new EncodingFailedException(msg);
        }

        //The maximum option delta is 14. For larger deltas use all multiples of 14 between prevNumber and
        //optionName.number as fencepost options
        else if(optionName.getNumber() - prevNumber > MAX_OPTION_DELTA){
            //smallest multiple of 14 greater than optionName.number is the first fencepost number
            int nextFencepost = prevNumber + (MAX_OPTION_DELTA - prevNumber % MAX_OPTION_DELTA);

            while(optionName.getNumber() - prevNumber > MAX_OPTION_DELTA){

                //write an encoded fencepost option to OutputStream
                buffer.writeByte((nextFencepost - prevNumber) << 4);

                log.debug("Encoded fencepost option added (no {}).", nextFencepost);

                prevNumber = nextFencepost;
                nextFencepost += MAX_OPTION_DELTA;
            }
        }

        //Write option delta and value length
        if(option.getValue().length <= MAX_OPTION_DELTA){
           //4 bits for the 'option delta' and 4 bits for the 'value length'
           buffer.writeByte(((optionName.getNumber() - prevNumber) << 4) | option.getValue().length);
        }
        else{
           //4 bits for the 'option delta', 4 bits (1111) to indicate a 'value length'
           //more then 14 and 1 byte for the actual 'value length' - 15
           buffer.writeByte(((optionName.getNumber() - prevNumber) << 4) | 15);
           buffer.writeByte(option.getValue().length - 15);
        }

        //Write value
        buffer.writeBytes(option.getValue());
    }
}
