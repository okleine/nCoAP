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

import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import java.net.InetSocketAddress;

/**
 * Interface to be implemented by {@link CoapResponseProcessor} instances to be informed when a {@link CoapRequest}
 * could not be sent to a remote CoAP endpoints because all message IDs for that remote endpoints are currently
 * allocated.
 *
 * @author Oliver Kleine
 */
public interface NoMessageIDAvailableProcessor extends CoapResponseProcessor{

    /**
     * This method is invoked by the framework if a {@link CoapRequest} could not be sent to a remote
     * CoAP endpoints because all 65536 message IDs (the number of available message IDs per communication partner)
     * have been used for previous {@link CoapRequest}s and none of them has retired, yet.
     *
     * A used message ID retires, i.e. is usable for a new {@link CoapRequest}
     * {@link MessageIDFactory#EXCHANGE_LIFETIME} seconds (247) after it was allocated.
     *
     * @param remoteEndpoint the desired recipient of the {@link CoapRequest} that could not be transmitted.
     * @param waitingPeriod the number of milliseconds to wait until the next message ID for the remote CoAP endpoints
     *                      retires, e.g. is ready to be used for a new request.
     */
    public void handleNoMessageIDAvailable(InetSocketAddress remoteEndpoint, long waitingPeriod);

}
