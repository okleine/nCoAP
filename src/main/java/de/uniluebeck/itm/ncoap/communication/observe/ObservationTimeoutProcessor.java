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
//package de.uniluebeck.itm.ncoap.communication.observe;
//
//import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
//
//import java.net.InetSocketAddress;
//
///**
//* Interface to be implemented by instances of {@link CoapResponseProcessor} to get informed if an running
//* observation timed out because of a max-age expiry. Observable CoAP resources are supposed to send a new
//* update notification when either it's status changed or max-age of the previous update notification is about
//* to exceed.
//*
//* @author Oliver Kleine
//*/
//public interface ObservationTimeoutProcessor extends CoapResponseProcessor{
//
//    /**
//     * This method is automatically invoked by the nCoap framework if an observerd resource did
//     * not send a follow-up update notification after max-age expiry of the previous update notification
//     *
//     * @param remoteAddress the {@link InetSocketAddress} of the host of the observed webservice
//     */
//    public void processObservationTimeout(InetSocketAddress remoteAddress);
//
//}
