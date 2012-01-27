package de.uniluebeck.itm.spitfire.nCoap.message.header;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author Oliver Kleine
 */
public class Header {

    private static Logger log = Logger.getLogger("nCoap");

    private final byte version = 1;
    private MsgType msgType;
    private Code code;
    private int msgID;;


    public Header(MsgType msgType, Code code) throws InvalidHeaderException{
        this(msgType, code, 0);
    }

    public Header(MsgType msgType, Code code, int msgID) throws InvalidHeaderException {
        setMsgType(msgType);
        setCode(code);
        setMsgID(msgID);

        log.debug("[Header] New Header created (type: " + msgType + ", code: " + code + ", msgID: " + msgID + ")");
    }

    public static Header createHeader (ChannelBuffer buf) throws InvalidHeaderException{
        //Header must have a length of exactly 4 bytes
        if(buf.readableBytes() < 4){
            String msg = "Buffer must contain at least readable 4 bytes (but has " + buf.readableBytes() + ")";
            throw new InvalidHeaderException(msg);
        }

        //Decode the header values (version: 2 bits, msgType: 2 bits, optionCount: 4 bits, code: 4 bits, msgID: 8 bits)
        int header = buf.readInt();
        int msgTypeNumber = ((header << 2) >>> 30);
        int optionCount = ((header << 4) >>> 28);
        int codeNumber = ((header << 8) >>> 24);
        int msgID = ((header << 16) >>> 16);

        Header result = new Header(MsgType.getMsgTypeFromNumber(msgTypeNumber), Code.getCodeFromNumber(codeNumber));
        //result.setOptionCount(optionCount);
        result.setMsgID(msgID);

        log.debug("[Header] New Header created from ChannelBuffer (type: " + result.msgType +
                ", code: " + result.code + ", msgID: " + result.msgID + ")");

        return result;
    }

    public int getVersion(){
        return version;
    }

    public void setMsgType(MsgType msgType){
        this.msgType = msgType;
    }

    public MsgType getMsgType(){
        return msgType;
    }

    public void setCode(Code code){
        this.code = code;
    }

    public Code getCode(){
        return code;
    }

    public void setMsgID(int msgID) throws InvalidHeaderException {
        //Check if msgID is syntactically correct
        if(msgID < 0 || msgID > 65535){
            throw new InvalidHeaderException("Message ID must not be negative or " +
                    "greater than 65535 (but is " + msgID + ")");
        }
        this.msgID = msgID;
    }

    public int getMsgID(){
        return msgID;
    }
}
