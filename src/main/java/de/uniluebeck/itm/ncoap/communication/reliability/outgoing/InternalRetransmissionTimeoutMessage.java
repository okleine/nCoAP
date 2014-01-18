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
package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import de.uniluebeck.itm.ncoap.application.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;

import java.net.InetSocketAddress;

/**
 * Instances of {@link InternalRetransmissionTimeoutMessage} are sent upstream by the
 * {@link OutgoingMessageReliabilityHandler} if a {@link CoapMessage} of type
 * {@link de.uniluebeck.itm.ncoap.message.MessageType.Name#CON} was not acknowledged despite the maximum number of
 * retransmission attempts.
 *
 * @author Oliver Kleine
 */
public class InternalRetransmissionTimeoutMessage {

    private Token token;
    private InetSocketAddress remoteAddress;

    /**
     * @param token a long value representing the token of the outgoing confirmable {@link CoapMessage} that was not
     *              acknowledged by the recipient
     * @param remoteAddress the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that
     *                      did not acknowledge the reception
     */
    public InternalRetransmissionTimeoutMessage(Token token, InetSocketAddress remoteAddress){
        this.token = token;
        this.remoteAddress = remoteAddress;
    }

    /**
     * Returns the token of the outgoing confirmable {@link CoapMessage} that was not acknowledged by the recipient
     * @return the token of the outgoing confirmable {@link CoapMessage} that was not acknowledged by the recipient
     */
    public Token getToken() {
        return token;
    }

    /**
     * Returns the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that did not
     * acknowledge the reception.
     * @return the address of the intended recipient of the outgoing confirmable {@link CoapMessage} that did not
     * acknowledge the reception.
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public String toString(){
        return "InternalRetransmissionTimeoutMessage: " + remoteAddress + " (remote address), "
                + token + " (token)";
    }
}
