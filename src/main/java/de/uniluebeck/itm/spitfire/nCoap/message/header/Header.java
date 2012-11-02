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

package de.uniluebeck.itm.spitfire.nCoap.message.header;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Oliver Kleine
 */
public class Header {

    private static Logger log = LoggerFactory.getLogger(Header.class.getName());

    public static int MESSAGE_ID_NOT_SET = -1;

    private MsgType msgType;
    private Code code;
    private int msgID = MESSAGE_ID_NOT_SET;

    public Header(Code code){
        setCode(code);
    }

    public Header(Header header) throws InvalidHeaderException {
        this(header.getMsgType(), header.getCode(), header.msgID);
    }

    public Header(MsgType msgType, Code code) {
        this(code);
        setMsgType(msgType);
    }

    public Header(MsgType msgType, Code code, int msgID) throws InvalidHeaderException {
        this(msgType, code);
        setMsgID(msgID);
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
        //int optionCount = ((header << 4) >>> 28);
        int codeNumber = ((header << 8) >>> 24);
        int msgID = ((header << 16) >>> 16);

        Header result = new Header(MsgType.getMsgTypeFromNumber(msgTypeNumber), Code.getCodeFromNumber(codeNumber));
        //result.setOptionCount(optionCount);
        result.setMsgID(msgID);

        log.debug("New Header created from ChannelBuffer (type: " + result.msgType +
                ", code: " + result.code + ", msgID: " + result.msgID + ")");

        return result;
    }

    public int getVersion(){
        return 1;
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
        if(msgID < -1 || msgID > 65535){
            throw new InvalidHeaderException("Message ID must not be negative or " +
                    "greater than 65535 (but is " + msgID + ")");
        }
        this.msgID = msgID;

        log.debug("Message ID " + this.msgID + " successfully set.");
    }

    public int getMsgID(){
        return msgID;
    }
}
