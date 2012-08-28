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

import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface to be implemented by client applications to handle responses
 *
 * @author Oliver Kleine
 */
public abstract class ResponseCallback {

    private Logger log = LoggerFactory.getLogger(ResponseCallback.class.getName());

    /**
     * Method to be called by the ReponseCallbackHandler for an incoming response (which is of any type but
     * empty acknowledgement)
     * @param coapResponse the response message
     */
    public void receiveResponse (CoapResponse coapResponse){
        log.info("Received response with code " + coapResponse.getCode() + ".");
    };

    /**
     * Method to be called by the ReponseCallbackHandler for an incoming empty acknowledgement
     */
    public void receiveEmptyACK(){
        log.info("Received empty acknowledgement.");
    };

    public void receiveInternalError(String errorMessage){
        log.info("Internal error message received:\n" + errorMessage);
    };

}
