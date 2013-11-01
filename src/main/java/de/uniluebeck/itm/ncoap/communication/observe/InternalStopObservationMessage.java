///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.communication.observe;
//
//import java.net.InetSocketAddress;
//import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
//
///**
// * Internal message to be sent downstream, e.g. from the {@link CoapClientApplication}  if an observation timed out,
// * i.e. there was no follow-up update notification from the observed resource within the max-age period of the
// * previous update-notification.
// *
// * @author Oliver Kleine
// */
//public class InternalStopObservationMessage {
//
//    private InetSocketAddress remoteAddress;
//    private byte[] token;
//
//    /**
//     * @param remoteAddress the {@link InetSocketAddress} of the host that hosts the observed service
//     * @param token the token to relate update-notifications with observations
//     */
//    public InternalStopObservationMessage(InetSocketAddress remoteAddress, byte[] token){
//        this.remoteAddress = remoteAddress;
//
//        this.token = token;
//    }
//
//    /**
//     * Returns the {@link InetSocketAddress} of the host that hosts the observed service
//     * @return the {@link InetSocketAddress} of the host that hosts the observed service
//     */
//    public InetSocketAddress getRemoteAddress() {
//        return remoteAddress;
//    }
//
//    /**
//     * Returns the token for the timed-out observation
//     * @return the token for the timed-out observation
//     */
//    public byte[] getToken() {
//        return token;
//    }
//}
