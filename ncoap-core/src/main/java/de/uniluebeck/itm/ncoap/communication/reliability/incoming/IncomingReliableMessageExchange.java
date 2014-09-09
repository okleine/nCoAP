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
package de.uniluebeck.itm.ncoap.communication.reliability.incoming;

import java.net.InetSocketAddress;

/**
 * Instances of {@link de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingReliableMessageExchange}
 * represent a message exchange that was caused by the reception of a confirmable
 * {@link de.uniluebeck.itm.ncoap.message.CoapMessage}.
 *
 * The actions required upon creation of an
 * {@link de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingReliableMessageExchange} depend on the
 * type of the receiving endpoint (client or server), e.g. for a server it is used for duplicate detection to not let the
 * addressed service process the same request twice and to send an empty ACK if there was no response from the
 * addressed service within 2 seconds.
 *
 * @author Oliver Kleine
 */
public class IncomingReliableMessageExchange extends IncomingMessageExchange{

    private boolean acknowledgementSent;

    /**
     * Creates a new instance of
     * {@link de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingReliableMessageExchange}
     *
     * @param remoteEndpoint the sender of the received confirmable {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     * @param messageID the message ID of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}.
     */
    public IncomingReliableMessageExchange(InetSocketAddress remoteEndpoint, int messageID) {
        super(remoteEndpoint, messageID);
        this.acknowledgementSent = false;
    }

    /**
     * Returns <code>true</code> if the framework already sent an empty ACK or <code>false</code> otherwise
     * @return <code>true</code> if the framework already sent an empty ACK or <code>false</code> otherwise
     */
    public boolean isAcknowledgementSent(){
        return this.acknowledgementSent;
    }

    /**
     * This method is called by the framework once an empty ACK was sent.
     */
    public void setAcknowledgementSent(){
        this.acknowledgementSent = true;
    }
}
