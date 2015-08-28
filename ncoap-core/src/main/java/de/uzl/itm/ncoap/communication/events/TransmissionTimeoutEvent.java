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
package de.uzl.itm.ncoap.communication.events;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Instances of {@link TransmissionTimeoutEvent} are sent upstream if
 * a {@link MessageIDReleasedEvent} occurs which is related to an open
 * {@link de.uzl.itm.ncoap.communication.reliability.OutboundReliableMessageTransfer}, i.e. there was an
 * outbound reliable message transfer (CON message) which was neither acknowledged nor resetted by the remote endpoint
 * within {@link de.uzl.itm.ncoap.communication.reliability.MessageIDFactory#EXCHANGE_LIFETIME} seconds.
 */
public class TransmissionTimeoutEvent extends MessageIDReleasedEvent implements MessageTransferEvent {

    private final Token token;

    /**
     * Creates a new instance of {@link TransmissionTimeoutEvent}
     *
     * @param remoteEndpoint the remote endpoint that did not confirm the reception of a reliable message
     * @param messageID the message ID of the timed out
     *                  {@link de.uzl.itm.ncoap.communication.reliability.OutboundReliableMessageTransfer}
     * @param token the {@link de.uzl.itm.ncoap.communication.dispatching.client.Token} of the timed out
     *              {@link de.uzl.itm.ncoap.communication.reliability.OutboundReliableMessageTransfer}
     */
    public TransmissionTimeoutEvent(InetSocketAddress remoteEndpoint, int messageID, Token token) {
        super(remoteEndpoint, messageID);
        this.token = token;
    }

    @Override
    public Token getToken() {
        return this.token;
    }

    @Override
    public boolean stopsMessageExchange() {
        return true;
    }
}
