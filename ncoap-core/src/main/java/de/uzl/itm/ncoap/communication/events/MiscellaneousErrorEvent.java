/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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

import de.uzl.itm.ncoap.communication.dispatching.Token;

import java.net.InetSocketAddress;

/**
 * Instances of {@link MiscellaneousErrorEvent} are sent upstream if some error occurred during message transfer.
 *
 * @author Oliver Kleine
 */
public class MiscellaneousErrorEvent extends AbstractMessageTransferEvent {

    private final String description;

    /**
     * Creates a new instance of {@link MiscellaneousErrorEvent}
     *
     * @param remoteSocket the remote socket of the transfer that caused this event
     * @param messageID the message ID of the message that caused this event
     * @param token the {@link Token} of the message that caused this event
     * @param description a human readable description of the error that caused this event
     */
    public MiscellaneousErrorEvent(InetSocketAddress remoteSocket, int messageID, Token token, String description) {
        super(remoteSocket, messageID, token);
        this.description = description;
    }

    /**
     * Returns a human readable description of the error that caused this event
     * @return a human readable description of the error that caused this event
     */
    public String getDescription() {
        return this.description;
    }

//    @Override
//    public boolean stopsMessageExchange() {
//        return true;
//    }

    @Override
    public String toString() {
        return "MISCELLANEOUS MESSAGE EXCHANGE ERROR (remote endpoint: " + this.getRemoteSocket() +
                ", message ID: " + this.getMessageID() + ", token: " + this.getToken() + ")";
    }

    public interface Handler {
        public void handleEvent(MiscellaneousErrorEvent event);
    }
}
