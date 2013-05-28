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

package de.uniluebeck.itm.spitfire.nCoap.communication.callback;

import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutHandler;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;

/**
 * Interface to be implemented by client applications to handle responses
 *
 * @author Oliver Kleine
 */
public interface ResponseCallback extends RetransmissionTimeoutHandler {

    /**
     * Method to be called by the {@link ResponseCallbackHandler} for an incoming response (which is of any type but
     * empty acknowledgement). For empty ACK messages the {@link ResponseCallbackHandler} invokes the receiveEmptyACK()
     * method.
     *
     * @param coapResponse the response message
     */
    public void receiveResponse (CoapResponse coapResponse);

    /**
     * This method is invoked by the {@link ResponseCallbackHandler} (automatically) for an incoming empty
     * acknowledgement. If the client application is e.g. a browser, one could e.g. display a message in the
     * browser windows telling the user that the server has received the request but needs some time to
     * process it.
     */
    public void receiveEmptyACK();
//
//    /**
//     * Returns true is this {@link ResponseCallback} instance observes a resource, false otherwise
//     * @return true is this {@link ResponseCallback} instance observes a resource, false otherwise
//     */
//    public boolean isObserver();
}
