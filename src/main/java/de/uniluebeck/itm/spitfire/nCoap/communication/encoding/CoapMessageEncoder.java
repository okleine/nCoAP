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

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.options.Option;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Oliver Kleine
 */
public class CoapMessageEncoder extends OneToOneEncoder{

    public static int MAX_OPTION_DELTA = 14;
    private static Logger log = LoggerFactory.getLogger(CoapMessageEncoder.class.getName());

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel ch, Object object) throws Exception {

        if(!(object instanceof CoapMessage)){
            log.debug(" No Message object!");
            return object;
        }

        CoapMessage msg = (CoapMessage) object;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        encodeHeader(buffer, msg.getHeader(), msg.getOptionCount());
        encodeOptions(buffer, msg.getOptionList());

        ChannelBuffer buf = ChannelBuffers.wrappedBuffer(buffer, msg.getPayload());
        log.debug(" Length of encoded message: " + buf.readableBytes());

        return buf;
    }

    private void encodeHeader(ChannelBuffer buffer, Header header, int optionCount){
        buffer.writeInt((header.getVersion() << 30) |
            (header.getMsgType().number << 28) |
            (optionCount << 24) |
            (header.getCode().number << 16) |
            (header.getMsgID()));
    }

    private void encodeOptions(ChannelBuffer buffer, OptionList optionList) throws Exception {

        //Encode options one after the other and append buf option to the buf
        int prevNumber = 0;

        for(OptionName optionName : OptionName.values()){
            for(Option option : optionList.getOption(optionName)){
                encodeOption(buffer, optionName, option, prevNumber);
                prevNumber = optionName.number;

                log.debug(" Encoded option(No: " + optionName.number +
                            ", Value: " + Option.getHexString(option.getValue()) + ")");
            }
        }
    }

    private void encodeOption(ChannelBuffer buffer, OptionName optionName, Option option, int prevNumber)
            throws Exception {

        log.debug(" Start encoding option number " + optionName.number);

        //The previous option number must be smaller or equal to the actual one
        if(prevNumber > optionName.number){
            String msg = "[CoapMessageEncoder] Parameter value prevNumber (" + prevNumber +
                         ") for encoding must not be larger then current option number (" + optionName.number + ")";
            throw new EncodingFailedException(msg);
        }

        //The maximum option delta is 14. For larger deltas use all multiples of 14 between prevNumber and
        //optionName.number as fencepost options
        else if(optionName.number - prevNumber > MAX_OPTION_DELTA){
            //smallest multiple of 14 greater than optionName.number is the first fencepost number
            int nextFencepost = prevNumber + (MAX_OPTION_DELTA - prevNumber % MAX_OPTION_DELTA);

            while(optionName.number - prevNumber > MAX_OPTION_DELTA){

                log.debug(" Option delta for encoding must not be greater than 14 " +
                        "(but is " + (optionName.number - prevNumber) + ")");

                //write an encoded fencepost option to OutputStream
                buffer.writeByte((nextFencepost - prevNumber) << 4);

                log.debug(" Encoded fencepost option added (with option number " +
                            nextFencepost + ")");

                prevNumber = nextFencepost;
                nextFencepost += MAX_OPTION_DELTA;
            }
        }

        //Write option delta and value length
        if(option.getValue().length <= MAX_OPTION_DELTA){
           //4 bits for the 'option delta' and 4 bits for the 'value length'
           buffer.writeByte(((optionName.number - prevNumber) << 4) | option.getValue().length);
        }
        else{
           //4 bits for the 'option delta', 4 bits (1111) to indicate a 'value length'
           //more then 14 and 1 byte for the actual 'value length' - 15
           buffer.writeByte(((optionName.number - prevNumber) << 4) | 15);
           buffer.writeByte(option.getValue().length - 15);
        }

        //Write value
        buffer.writeBytes(option.getValue());

        log.debug("Successfuly encoded option number " + optionName.number);
    }
}
