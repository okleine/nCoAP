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
package de.uniluebeck.itm.ncoap.applications;

import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.communication.events.MessageRetransmittedEvent;
import de.uniluebeck.itm.ncoap.communication.events.TransmissionTimeoutEvent;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by olli on 20.03.14.
 */
public class SimpleCoapClientCallback extends ClientCallback {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //private AtomicInteger responseCounter;
    private AtomicBoolean responseReceived;
    private AtomicInteger transmissionCounter;
    private AtomicBoolean timedOut;


    public SimpleCoapClientCallback(){
        this.responseReceived = new AtomicBoolean(false);
        this.transmissionCounter = new AtomicInteger(0);
        this.timedOut = new AtomicBoolean(false);
    }

    /**
     * Increases the reponse counter by 1, i.e. {@link #getResponseCount()} will return a higher value after
     * invocation of this method.
     *
     * @param coapResponse the response message
     */
    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
        responseReceived.set(true);
        log.info("Received: {}", coapResponse);

    }

    /**
     * Returns the number of responses received
     * @return the number of responses received
     */
    public int getResponseCount(){
        return this.responseReceived.get() ? 1 : 0;
    }


    @Override
    public void processRetransmission(MessageRetransmittedEvent event) {
        int value = transmissionCounter.incrementAndGet();
        log.info("#{}: {}", value, event);
    }


    @Override
    public void processTransmissionTimeout(TransmissionTimeoutEvent event) {
        log.info("{}", event);
        timedOut.set(true);
    }


    public boolean isTimedOut(){
        return timedOut.get();
    }
}
