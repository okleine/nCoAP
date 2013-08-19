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

package de.uniluebeck.itm.ncoap.message.header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class represent the header of a CoAP message.
 *
 * @author Oliver Kleine
 */
public class Header {

    private static Logger log = LoggerFactory.getLogger(Header.class.getName());

    /**
     * A constant used by the nCoAP framework to identify that the message ID was not set yet
     */
    public static int MESSAGE_ID_UNDEFINED = -1;

    private MsgType msgType;
    private Code code;
    private int msgID = MESSAGE_ID_UNDEFINED;

    /**
     * @param code a {@link Code}
     */
    public Header(Code code){
        setCode(code);
    }

    /**
     * @param msgType a {@link MsgType}
     * @param code a {@link Code}
     */
    public Header(MsgType msgType, Code code) {
        this(code);
        setMsgType(msgType);
    }

    /**
     * <b>Note:</b> This constructor is only for internal instance construction and not intended to be used by
     * external applications. However, it is rather unlikely that an application needs to create a new instance of
     * {@link Header} anyway.
     *
     * @param msgType a {@link MsgType}
     * @param code a {@link Code}
     * @param msgID an integer value
     *
     * @throws InvalidHeaderException
     */
    public Header(MsgType msgType, Code code, int msgID) throws InvalidHeaderException {
        this(msgType, code);
        setMsgID(msgID);
    }

    /**
     * Returns the version of the CoAP message. According to the CoAP draft this is always 1.
     * @return the version of the CoAP message. According to the CoAP draft this is always 1.
     */
    public int getVersion(){
        return 1;
    }

    /**
     * Sets the {@link MsgType} of the message this {@link Header} is part of
     * @param msgType a {@link MsgType}
     */
    public void setMsgType(MsgType msgType){
        this.msgType = msgType;
    }

    /**
     * Returns the {@link MsgType} set for this {@link Header}
     * @return the {@link MsgType} set for this {@link Header}
     */
    public MsgType getMsgType(){
        return msgType;
    }

    /**
     * Sets the {@link Code} of the message this {@link Header} is part of
     * @param code a {@link Code}
     */
    public void setCode(Code code){
        this.code = code;
    }

    /**
     * Returns the {@link Code} set for this {@link Header}
     * @return the {@link Code} set for this {@link Header}
     */
    public Code getCode(){
        return code;
    }

    /**
     * Sets the message ID of this {@link Header}
     *
     * @param msgID the message ID
     *
     * @throws InvalidHeaderException if the given message ID is not valid.
     */
    public void setMsgID(int msgID) throws InvalidHeaderException {
        //Check if msgID is syntactically correct
        if(msgID < -1 || msgID > 65535){
            throw new InvalidHeaderException("Message ID must not be negative or " +
                    "greater than 65535 (but is " + msgID + ")");
        }
        this.msgID = msgID;

        log.debug("Message ID " + this.msgID + " successfully set.");
    }

    /**
     * Returns the message ID set for this {@link Header}
     * @return the message ID set for this {@link Header}
     */
    public int getMsgID(){
        return msgID;
    }


    /**
     * Returns true if and only if the given Object is an instance of {@link Header} and if all all
     * its components (version, message type, code and message ID) match.
     *
     * @param other any other instance of {@link Object}
     * @return <code>true</code> if and only if the given Object is an instance of {@link Header} and if all all
     * its components (version, message type, code and message ID) match. It returns <code>false</code>
     * otherwise.
     */
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
        return "[HEADER] " + getVersion() + " (version), " + msgType + " (type), " + code + " (code), "
                + msgID + " (message ID)";
    }
}
