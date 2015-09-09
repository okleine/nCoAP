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
package de.uzl.itm.ncoap.communication.events.client;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.AbstractMessageExchangeEvent;

import java.net.InetSocketAddress;

/**
 * Instances of this class are sent downstream if a client decides to stop a running observation. This events is
 * supposed to send an RST messages upon reception of the next update notification.
 *
 * @author Oliver Kleine
 */
public class LazyObservationTerminationEvent extends AbstractMessageExchangeEvent{

//    /**
//     * A collection of possible reasons for an observation to be cancelled
//     */
//    public static enum Reason{
//        /**
//         * The client lazily stopped the observation, i.e. awaits the next update notification and answers with a RST.
//         */
//        LAZY_CANCELLATION_BY_CLIENT("Lazily cancellation by Client (RST)!"),
//
//        /**
//         * The client actively stopped the observation, i.e. sent a GET request with
//         * {@link de.uzl.itm.ncoap.message.options.OptionValue.Name#OBSERVE} set to <code>1</code>.
//         */
//        ACTIVE_CANCELLATION_BY_CLIENT("Active cancellation by Client (GET)!");
//
//        private String reason;
//
//        private Reason(String reason){
//            this.reason = reason;
//        }
//
//        /**
//         * Returns a human readable description of the reason for this observation to be stopped
//         * @return a human readable description of the reason for this observation to be stopped
//         */
//        public String getReason() {
//            return reason;
//        }
//    }

//    private Reason reason;

    /**
     * Creates a new instance of {@link LazyObservationTerminationEvent}
     *
     * @param remoteSocket the {@link java.net.InetSocketAddress} of the remote endpoint providing the observed
     *                       service
     * @param token the {@link de.uzl.itm.ncoap.communication.dispatching.client.Token} to identify the observation to be
     *              stopped
     */
    public LazyObservationTerminationEvent(InetSocketAddress remoteSocket, Token token) {
        super(remoteSocket, token);
//        this.reason = reason;
    }


//    /**
//     * Returns a human readable description of the reason that caused this event
//     * @return a human readable description of the reason that caused this event
//     */
//    public String getReason() {
//        return reason.getReason();
//    }

    @Override
    public String toString(){
        return "LAZY OBSERVATION CANCELLED (remote endpoint: " + this.getRemoteSocket() + ", token: " +
                this.getToken() + ")";
    }

    public interface Handler {
        public void handleEvent(LazyObservationTerminationEvent event);
    }
}
