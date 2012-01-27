/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.communication.handler;

import de.uniluebeck.itm.spitfire.nCoap.communication.EncodingFailedException;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.options.Option;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 *
 * @author Oliver Kleine
 */
public class CoapMessageEncoder extends OneToOneEncoder{

    public static int MAX_OPTION_DELTA = 14;
    private static Logger log = Logger.getLogger("nCoap");

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel ch, Object object) throws Exception {

        log.debug("[CoapMessageEncoder] Encode!");
        if(!(object instanceof Message)){
            log.debug("[CoapMessageEncoder] No Message object!");
            return object;
        }

        Message msg = (Message) object;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        encodeHeader(buffer, msg.getHeader(), msg.getOptionCount());
        encodeOptions(buffer, msg.getOptionList());

        ChannelBuffer buf = ChannelBuffers.wrappedBuffer(buffer, msg.getPayload());
        log.debug("[CoapMessageEncoder] Length of encoded message: " + buf.array().length);
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

                log.debug("[CoapMessageEncoder] Encoded option(No: " + optionName.number +
                        ", Value: " + Option.getHexString(option.getValue()) + ")");
            }
        }
    }

    private void encodeOption(ChannelBuffer buffer, OptionName optionName, Option option, int prevNumber)
            throws Exception {

        log.debug("[CoapMessageEncoder] Start encoding option number " + optionName.number);

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

                log.debug("[CoapMessageEncoder] Option delta for encoding must not be greater than 14 " +
                        "(but is " + (optionName.number - prevNumber) + ")");

                //write an encoded fencepost option to OutputStream
                buffer.writeByte((nextFencepost - prevNumber) << 4);
                log.debug("[CoapMessageEncoder] Encoded fencepost option added (with option number " + nextFencepost + ")");

                prevNumber = nextFencepost;
                nextFencepost += MAX_OPTION_DELTA;
            }
        }

        //Write option delta and value length
        if(option.getValue().length <= 14){
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

        log.debug("[CoapMessageEncoder] Successfuly encoded option number " + optionName.number);
    }
}
