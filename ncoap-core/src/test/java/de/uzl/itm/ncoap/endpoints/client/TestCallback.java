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
package de.uzl.itm.ncoap.endpoints.client;

import com.google.common.collect.Ordering;
import de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uzl.itm.ncoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class TestCallback extends ClientCallback {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //CoAP responses
    private SortedMap<Long, CoapResponse> coapResponses;

    //internal messages
    private Set<Long> emptyACKs;
    private Set<Long> emptyRSTs;
    private Set<Long> transmissions;
    private Set<Long> transmissionTimeouts;


    public TestCallback(){
        this.coapResponses = Collections.synchronizedSortedMap(
                new TreeMap<Long, CoapResponse>(Ordering.natural())
        );
        this.emptyACKs = Collections.synchronizedSet(new TreeSet<Long>((Ordering.natural())));
        this.emptyRSTs = Collections.synchronizedSet(new TreeSet<Long>((Ordering.natural())));
        this.transmissions = Collections.synchronizedSet(new TreeSet<Long>((Ordering.natural())));
        this.transmissionTimeouts = Collections.synchronizedSet(new TreeSet<Long>((Ordering.natural())));
    }


    @Override
    public void processCoapResponse(CoapResponse coapResponse){
       coapResponses.put(System.currentTimeMillis(), coapResponse);
       log.info("Received #{}: {}", coapResponses.size(), coapResponse);
    }


    @Override
    public void processRetransmission() {
        long actualTime = System.currentTimeMillis();
        transmissions.add(actualTime);
        log.info("Finished Retransmission #{}.", transmissions.size());
    }


    @Override
    public void processTransmissionTimeout() {
        long actualTime = System.currentTimeMillis();
        transmissionTimeouts.add(actualTime);
        log.info("Transmission Timeout!");
    }


    @Override
    public void processEmptyAcknowledgement() {
        long actualTime = System.currentTimeMillis();
        emptyACKs.add(actualTime);
        log.info("Received empty ACK!");
    }

    @Override
    public void processReset() {
        long actualTime = System.currentTimeMillis();
        emptyRSTs.add(actualTime);
        log.info("Received RST!");
    }

    /**
     * Returns a {@link SortedMap} containing all received {@link CoapResponse} instances as values and their reception
     * timestamps as key.
     *
     * @return a {@link SortedMap} containing all received {@link CoapResponse} instances as values and their reception
     * timestamps as key.
     */
    public SortedMap<Long, CoapResponse> getCoapResponses(){
        return this.coapResponses;
    }


    public Set<Long> getEmptyACKs(){
        return this.emptyACKs;
    }


    public Set<Long> getEmptyRSTs(){
        return this.emptyRSTs;
    }


    public Set<Long> getTransmissions(){
        return this.transmissions;
    }


    public Set<Long> getTransmissionTimeouts(){
        return this.transmissionTimeouts;
    }
}
