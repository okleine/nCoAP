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
import java.util.concurrent.ScheduledFuture;

/**
* @author Oliver Kleine
*/
public class InboundReliableMessageTransfer extends InboundMessageTransfer {

    /**
     * Minimum delay in milliseconds (1500) between the reception of a confirmable request and an empty ACK
     */
    public static final int EMPTY_ACK_DELAY = 1500;

    private ScheduledFuture confirmationFuture;
    private boolean confirmed;
    /**
     * Creates a new instance of
     * {@link InboundReliableMessageTransfer}
     *
     * @param remoteEndpoint the sender of the received confirmable {@link de.uniluebeck.itm.ncoap.message.CoapMessage}
     * @param messageID the message ID of the received {@link de.uniluebeck.itm.ncoap.message.CoapMessage}.
     */
    public InboundReliableMessageTransfer(InetSocketAddress remoteEndpoint, int messageID,
                                          ScheduledFuture confirmationFuture) {
        super(remoteEndpoint, messageID);
        this.confirmationFuture = confirmationFuture;
        this.confirmed = false;
    }

    /**
     * Returns <code>true</code> if the framework already sent an empty ACK or <code>false</code> otherwise
     * @return <code>true</code> if the framework already sent an empty ACK or <code>false</code> otherwise
     */
    public boolean isConfirmed(){
        return this.confirmed;
    }

    /**
     * This method is called by the framework once an empty ACK was sent.
     */
    public synchronized void setConfirmed(boolean confirmed){
        this.confirmed = confirmed;
    }


    public void setConfirmationFuture(ScheduledFuture confirmationFuture){
        this.confirmationFuture = confirmationFuture;
    }


    public ScheduledFuture getConfirmationFuture(){
        return this.confirmationFuture;
    }
}
