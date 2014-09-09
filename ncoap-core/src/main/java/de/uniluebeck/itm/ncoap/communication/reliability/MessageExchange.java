/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.communication.reliability;

import java.net.InetSocketAddress;

/**
 * Abstract base class for message exchanges, i.e. associated request/response pairs.
 *
 * @author Oliver Kleine
*/
public abstract class MessageExchange {

    private final InetSocketAddress remoteEndpoint;
    private final int messageID;


    /**
     * Creates a new instance of {@link de.uniluebeck.itm.ncoap.communication.reliability.MessageExchange}
     *
     * @param remoteEndpoint the sender of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     * @param messageID the message ID of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     */
    public MessageExchange(InetSocketAddress remoteEndpoint, int messageID){
        this.remoteEndpoint = remoteEndpoint;
        this.messageID = messageID;
    }


    /**
     * Returns the sender address of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     * @return the sender address of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     */
    public final InetSocketAddress getRemoteEndpoint(){
        return this.remoteEndpoint;
    }


    /**
     * Returns the message ID of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     * @return the message ID of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     */
    public final int getMessageID(){
        return this.messageID;
    }


    @Override
    public int hashCode(){
        return remoteEndpoint.hashCode() & messageID;
    }


    @Override
    public boolean equals(Object object){

        if(!(object instanceof MessageExchange))
            return false;

        MessageExchange other = (MessageExchange) object;

        return this.getRemoteEndpoint().equals(other.getRemoteEndpoint()) &&
                this.getMessageID() == other.getMessageID();
    }

}
