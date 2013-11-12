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

package de.uniluebeck.itm.ncoap.application.client;

import de.uniluebeck.itm.ncoap.message.*;


/**
* Classes implementing the {@link CoapResponseProcessor} interface handle incoming {@link CoapResponse}s related to
* a particular {@link CoapRequest}.
*
* If one wants an instance of {@link CoapResponseProcessor} to handle other events as well, the implementing class
* must additionally implement one ore more of the following other interfaces, i.e.
* <ul>
*     <li>
*         {@link RetransmissionTimeoutProcessor} to be informed if the maximum number of retransmission attempts
*         was made for a confirmable {@link CoapRequest} and there was no acknowledgement received.
*     </li>
*     <li>
*         {@link EmptyAcknowledgementProcessor} to be informed if a confirmable {@link CoapRequest} was
*         acknowledged by the recipient with an empty acknowledgement.
*     </li>
*     <li>
*         {@link RetransmissionProcessor} to be informed about every transmission attempt for the {@link CoapRequest}.
*     </li>
*     <li>
*         {@link InternalNextBlockReceivedMessage} to be informed about every received block if the server decided to
*         send the response blockwise.
*     </li>
* </ul>
*
* A {@link CoapResponseProcessor} is comparable to a tab in a browser, whereas the {@link CoapClientApplication}
* browser is comparable to the browser application.
*
* @author Oliver Kleine
*/
public interface CoapResponseProcessor {

    /**
     * Method invoked by the {@link CoapClientApplication} for an incoming response (which is of any type but
     * empty {@link de.uniluebeck.itm.ncoap.message.MessageType#ACK} or {@link de.uniluebeck.itm.ncoap.message.MessageType#RST}).
     *
     * @param coapResponse the response message
     */
    public void processCoapResponse(CoapResponse coapResponse);
}
