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
package de.uniluebeck.itm.ncoap.communication.events;

import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Abstract base class for internal events that are caused within an ongoing or aspired message exchange
 *
 * @author Oliver Kleine
 */
public abstract class AbstractMessageTransferEvent implements MessageTransferEvent{

    private int messageID;
    private Token token;
    private InetSocketAddress remoteEndpoint;

    /**
     * Creates a new instance of {@link de.uniluebeck.itm.ncoap.communication.events.AbstractMessageTransferEvent}
     * @param remoteEndpoint the remote endpoint of the
     *                       {@link de.uniluebeck.itm.ncoap.communication.reliability.MessageTransfer} that caused this
     *                       event
     * @param messageID the message ID of the {@link de.uniluebeck.itm.ncoap.communication.reliability.MessageTransfer}
     *                  that caused this event
     * @param token the {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.Token} of the
     *              {@link de.uniluebeck.itm.ncoap.communication.reliability.MessageTransfer} that caused this event
     */
    protected AbstractMessageTransferEvent(InetSocketAddress remoteEndpoint, int messageID, Token token) {
        this.messageID = messageID;
        this.token = token;
        this.remoteEndpoint = remoteEndpoint;
    }

    /**
     * Returns the remote endpoint of the message exchange (i.e. communication) that caused this events
     * @return the remote endpoint of the message exchange (i.e. communication) that caused this events
     */
    public InetSocketAddress getRemoteEndpoint() {
        return remoteEndpoint;
    }

    /**
     * Returns the token of the message exchange (i.e. communication) that caused this events
     * @return the token of the message exchange (i.e. communication) that caused this events
     */
    public Token getToken() {
        return token;
    }

    /**
     * Returns the message ID of the message that caused this events
     * @return the message ID of the message that caused this events
     */
    public int getMessageID() {
        return messageID;
    }

    /**
     * Returns <code>true</code> if this events causes the related message exchange to stop and <code>false</code>
     * otherwise
     * @return <code>true</code> if this events causes the related message exchange to stop and <code>false</code>
     * otherwise
     */
    public abstract boolean stopsMessageExchange();
}
