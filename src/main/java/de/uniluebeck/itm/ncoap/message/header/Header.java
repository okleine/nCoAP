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

import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
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

    private MessageType messageType;
    private MessageCode messageCode;
    private int msgID = MESSAGE_ID_UNDEFINED;

    /**
     * @param messageCode a {@link de.uniluebeck.itm.ncoap.message.MessageCode}
     */
    public Header(MessageCode messageCode){
        setMessageCode(messageCode);
    }

    /**
     * @param messageType a {@link de.uniluebeck.itm.ncoap.message.MessageType}
     * @param messageCode a {@link de.uniluebeck.itm.ncoap.message.MessageCode}
     */
    public Header(MessageType messageType, MessageCode messageCode) {
        this(messageCode);
        setMessageType(messageType);
    }

    /**
     * <b>Note:</b> This constructor is only for internal instance construction and not intended to be used by
     * external applications. However, it is rather unlikely that an application needs to create a new instance of
     * {@link Header} anyway.
     *
     * @param messageType a {@link MessageType}
     * @param messageCode a {@link de.uniluebeck.itm.ncoap.message.MessageCode}
     * @param msgID an integer value
     *
     * @throws InvalidHeaderException
     */
    public Header(MessageType messageType, MessageCode messageCode, int msgID) throws InvalidHeaderException {
        this(messageType, messageCode);
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
     * Sets the {@link de.uniluebeck.itm.ncoap.message.MessageType} of the message this {@link Header} is part of
     * @param messageType a {@link de.uniluebeck.itm.ncoap.message.MessageType}
     */
    public void setMessageType(MessageType messageType){
        this.messageType = messageType;
    }

    /**
     * Returns the {@link de.uniluebeck.itm.ncoap.message.MessageType} set for this {@link Header}
     * @return the {@link de.uniluebeck.itm.ncoap.message.MessageType} set for this {@link Header}
     */
    public MessageType getMessageType(){
        return messageType;
    }

    /**
     * Sets the {@link de.uniluebeck.itm.ncoap.message.MessageCode} of the message this {@link Header} is part of
     * @param messageCode a {@link de.uniluebeck.itm.ncoap.message.MessageCode}
     */
    public void setMessageCode(MessageCode messageCode){
        this.messageCode = messageCode;
    }

    /**
     * Returns the {@link de.uniluebeck.itm.ncoap.message.MessageCode} set for this {@link Header}
     * @return the {@link de.uniluebeck.itm.ncoap.message.MessageCode} set for this {@link Header}
     */
    public MessageCode getMessageCode(){
        return messageCode;
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
     * its components (version, message type, messageCode and message ID) match.
     *
     * @param other any other instance of {@link Object}
     * @return <messageCode>true</messageCode> if and only if the given Object is an instance of {@link Header} and if all all
     * its components (version, message type, messageCode and message ID) match. It returns <messageCode>false</messageCode>
     * otherwise.
     */
    @Override
    public boolean equals(Object other){
        if(!(other instanceof Header)){
            return false;
        }

        Header otherHeader = (Header) other;

        return this.getVersion() == otherHeader.getVersion() &&
               this.messageCode == otherHeader.getMessageCode() &&
               this.msgID == otherHeader.getMsgID() &&
               this.messageType == otherHeader.getMessageType();
    }

    @Override
    public String toString(){
        return "[HEADER] " + getVersion() + " (version), " + messageType + " (type), " + messageCode + " (messageCode), "
                + msgID + " (message ID)";
    }
}
