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
package de.uniluebeck.itm.ncoap.applicationcomponents.client;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.*;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;


/**
* Instance of {@link CoapResponseProcessor} additionally implementing {@link RetransmissionTimeoutProcessor},
* {@link EmptyAcknowledgementProcessor}, and {@link RetransmissionTimeoutProcessor} to handle all possible
* events related to a sent {@link CoapRequest}.
*/
public class TestResponseProcessor implements CoapResponseProcessor, RetransmissionTimeoutProcessor,
        EmptyAcknowledgementProcessor, TransmissionInformationProcessor, ResetProcessor {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private SortedMap<Long, CoapResponse> coapResponses =
            Collections.synchronizedSortedMap(new TreeMap<Long, CoapResponse>());

    private final TreeMultiset<Long> emptyAcknowledgementReceptionTimes = TreeMultiset.create();
    private final TreeMultiset<Long> resetReceptionTimes = TreeMultiset.create();

    private final TreeMultiset<Long> requestTransmissionTimes = TreeMultiset.create();
    private final TreeMultiset<Long> requestTransmissionTimeoutTimes = TreeMultiset.create();


    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
       coapResponses.put(System.currentTimeMillis(), coapResponse);
       log.info("Received Response #" + coapResponses.size() + ": " + coapResponse);
    }


    @Override
    public void messageTransmitted(InetSocketAddress remoteEndpoint, int messageID,  final Token token,
                                   boolean retransmission) {

        synchronized (requestTransmissionTimes){
            if(!retransmission && requestTransmissionTimes.size() > 0)
                log.error("Initial transmission happened after first retransmission.");

            long actualTransmissionTime = System.currentTimeMillis();
            log.info("Attempt #" + (requestTransmissionTimes.size() + 1) + " to sent request finished.");

            if(!requestTransmissionTimes.isEmpty()){
                long previousTransmissionTime = requestTransmissionTimes.lastEntry().getElement();
                log.info("Delay to attempt #{}: {} ms", requestTransmissionTimes.size(),
                        actualTransmissionTime - previousTransmissionTime);
            }

            requestTransmissionTimes.add(System.currentTimeMillis());

        }
    }


    @Override
    public void processRetransmissionTimeout(InetSocketAddress remoteEndpoint, int messageID, Token token) {
        synchronized (requestTransmissionTimeoutTimes){
            requestTransmissionTimeoutTimes.add(System.currentTimeMillis());
            log.info("Retransmission timed out!");
        }
    }


    @Override
    public void processEmptyAcknowledgement(InetSocketAddress remoteEndpoint, int messageID, Token token) {
        synchronized (emptyAcknowledgementReceptionTimes){
            emptyAcknowledgementReceptionTimes.add(System.currentTimeMillis());
            log.info("Received empty ACK.");
        }
    }

    @Override
    public void processReset(InetSocketAddress remoteEndpoint, int messageID, Token token) {
        synchronized (resetReceptionTimes){
            log.info("Received RST");
            resetReceptionTimes.add(System.currentTimeMillis());
        }
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


    public SortedMultiset<Long> getEmptyAcknowledgemenReceptionTimes(){
        return this.emptyAcknowledgementReceptionTimes;
    }


    public SortedMultiset<Long> getResetReceptionTimes(){
        return this.resetReceptionTimes;
    }


    public SortedMultiset<Long> getRequestTransmissionTimes(){
        return this.requestTransmissionTimes;
    }


    public SortedMultiset<Long> getRequestTransmissionTimeoutTimes(){
        return this.requestTransmissionTimeoutTimes;
    }


}
