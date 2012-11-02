package de.uniluebeck.itm.spitfire.nCoap.message;

import org.jboss.netty.buffer.ChannelBuffer;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;

public class CoapNotificationResponse extends CoapResponse {

    public CoapNotificationResponse(Code code){
        super(code);
    }

    public CoapNotificationResponse(MsgType msgType, Code code){
        super(msgType, code);
    }

    public CoapNotificationResponse(MsgType msgType, Code code, int messageID)
            throws ToManyOptionsException, InvalidHeaderException {

        super(msgType, code, messageID);
    }

    public CoapNotificationResponse(Header header, OptionList optionList, ChannelBuffer payload){
        super(header, optionList, payload);
    }

}
