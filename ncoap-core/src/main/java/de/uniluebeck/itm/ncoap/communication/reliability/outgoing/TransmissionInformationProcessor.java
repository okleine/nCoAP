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
import de.uniluebeck.itm.ncoap.message.*;

import java.net.InetSocketAddress;

/**
 * Interface to be implemented by instances of {@link de.uniluebeck.itm.ncoap.application.client.CoapClientCallback}
 * that want to somehow know about each transmission attempt of the related CoAP request, e.g. in order to print
 * it on screen or log it.
 *
 * @author Oliver Kleine
 */

public interface TransmissionInformationProcessor {

    /**
     * Method invoked by the framework for each attempt to send the CoAP request related
     * to this {@link de.uniluebeck.itm.ncoap.application.client.CoapClientCallback}.
     *
     * This happens 5 times at maximum, once for the original request and 4 times for retransmission attempts if the
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} was confirmable.
     *
     * @param remoteEndpoint the address of the remote endpoint to receive the transmitted message
     * @param messageID the message ID of the transmitted message
     * @param token the {@link de.uniluebeck.itm.ncoap.application.client.Token} of the transmitted message
     * @param retransmission <code>false</code> if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} was
     *                       transmitted for the first time (for both, {@link MessageType.Name#CON} and
     *                       {@link MessageType.Name#NON) and <code>true</code> for retransmissions
     *                       (for {@link MessageType.Name#CON} only).
     */
    public void messageTransmitted(InetSocketAddress remoteEndpoint, int messageID, Token token, boolean retransmission);
}
