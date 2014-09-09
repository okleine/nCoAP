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
package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import de.uniluebeck.itm.ncoap.application.client.Token;

import java.net.InetSocketAddress;

/**
 * This exception is thrown in case of the (very unlikely) event that no more message IDs are available for the
 * given remote endpoint, i.e. there were 65535 messages sent to that remote endpoint within the last ~4 minutes.
 *
 * @author Oliver Kleine
 */
public class NoMessageIDAvailableException extends Exception{

    private InetSocketAddress remoteEndpoint;
    private long waitingPeriod;
    private Token token;

    /**
     * Creates a new instance of
     * {@link de.uniluebeck.itm.ncoap.communication.reliability.outgoing.NoMessageIDAvailableException}
     * @param remoteEndpoint the address of the remote endpoint where there is currently no more message ID available
     *                       for
     * @param waitingPeriod the time to wait (in milliseconds) until there is at least one message ID available
     */
    public NoMessageIDAvailableException(InetSocketAddress remoteEndpoint, long waitingPeriod){
        super("All message IDs for " + remoteEndpoint + " are in use. Wait for " +
                waitingPeriod + " milliseconds");
        this.remoteEndpoint = remoteEndpoint;
        this.waitingPeriod = waitingPeriod;
    }


    /**
     * This method is called by the framework to set the token of the outgoing message that caused this exception
     * @param token the token of the outgoing message that caused this exception
     */
    void setToken(Token token){
        this.token = token;
    }

    /**
     * Returns the address of the CoAP endpoint that was supposed to receive the outgoing message that caused this
     * exception
     * @return the address of the CoAP endpoint that was supposed to receive the outgoing message that caused this
     * exception
     */
    public InetSocketAddress getRemoteEndpoint() {
        return remoteEndpoint;
    }


    /**
     * Returns the time to wait (in milliseconds) until there is at least one message ID available
     * @return the time to wait (in milliseconds) until there is at least one message ID available
     */
    public long getWaitingPeriod() {
        return waitingPeriod;
    }


    /**
     * Returns the {@link de.uniluebeck.itm.ncoap.application.client.Token} of the outgoing message that caused this
     * exception
     * @return the {@link de.uniluebeck.itm.ncoap.application.client.Token} of the outgoing message that caused this
     * exception
     */
    public Token getToken() {
        return token;
    }
}
