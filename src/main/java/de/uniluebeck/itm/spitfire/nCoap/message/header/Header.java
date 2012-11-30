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

    public static int MESSAGE_ID_UNDEFINED = -1;

    private MsgType msgType;
    private Code code;
    private int msgID = MESSAGE_ID_UNDEFINED;

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

    @Override
    public boolean equals(Object other){
        if(!(other instanceof Header)){
            return false;
        }

        Header otherHeader = (Header) other;

        return this.getVersion() == otherHeader.getVersion() &&
               this.code == otherHeader.getCode() &&
               this.msgID == otherHeader.getMsgID() &&
               this.msgType == otherHeader.getMsgType();
    }

    @Override
    public String toString(){
        return "{[" + this.getClass().getName() + "] " + msgType + ", " + code + ", " + msgID + "}";
    }
}
