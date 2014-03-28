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
package de.uniluebeck.itm.ncoap.endpoints.client;

import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.ResetProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.TransmissionInformationProcessor;
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
public class CoapResponseTestProcessor implements CoapResponseProcessor, RetransmissionTimeoutProcessor,
        EmptyAcknowledgementProcessor, TransmissionInformationProcessor, ResetProcessor {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //CoAP responses
    private SortedMap<Long, CoapResponse> coapResponses;

    //internal messages
    private SortedSetMultimap<Long, InternalMessageDataWrapper> emptyACKs;
    private SortedSetMultimap<Long, InternalMessageDataWrapper> emptyRSTs;
    private SortedSetMultimap<Long, InternalMessageDataWrapper> transmissions;
    private SortedSetMultimap <Long, InternalMessageDataWrapper> transmissionTimeouts;


    public CoapResponseTestProcessor(){
        this.coapResponses = Collections.synchronizedSortedMap(new TreeMap<Long, CoapResponse>());

        this.emptyACKs = Multimaps.synchronizedSortedSetMultimap(
                TreeMultimap.<Long, InternalMessageDataWrapper>create(Ordering.natural(), Ordering.arbitrary())
        );

        this.emptyRSTs = Multimaps.synchronizedSortedSetMultimap(
                TreeMultimap.<Long, InternalMessageDataWrapper>create(Ordering.natural(), Ordering.arbitrary())
        );

        this.transmissions = Multimaps.synchronizedSortedSetMultimap(
                TreeMultimap.<Long, InternalMessageDataWrapper>create(Ordering.natural(), Ordering.arbitrary())
        );

        this.transmissionTimeouts = Multimaps.synchronizedSortedSetMultimap(
                TreeMultimap.<Long, InternalMessageDataWrapper>create(Ordering.natural(), Ordering.arbitrary())
        );
    }


    @Override
    public void processCoapResponse(CoapResponse coapResponse){
       coapResponses.put(System.currentTimeMillis(), coapResponse);
       log.info("Received #{}: {}", coapResponses.size(), coapResponse);
    }


    @Override
    public void messageTransmitted(InetSocketAddress remoteEndpoint, int messageID,  final Token token,
                                   boolean retransmission) {

        long actualTime = System.currentTimeMillis();
        transmissions.put(actualTime, new InternalMessageDataWrapper(remoteEndpoint, messageID, token));

        log.info("Transmission for message with ID {} to {} (Token: {}) finished.",
                new Object[]{messageID, remoteEndpoint, token});
    }


    @Override
    public void processRetransmissionTimeout(InetSocketAddress remoteEndpoint, int messageID, Token token) {

        long actualTime = System.currentTimeMillis();
        transmissionTimeouts.put(actualTime, new InternalMessageDataWrapper(remoteEndpoint, messageID, token));

        log.info("Transmission TIMED OUT for message with ID {} to {} (Token: {})",
                new Object[]{messageID, remoteEndpoint, token});
    }


    @Override
    public void processEmptyAcknowledgement(InetSocketAddress remoteEndpoint, int messageID, Token token) {

        long actualTime = System.currentTimeMillis();
        emptyACKs.put(actualTime, new InternalMessageDataWrapper(remoteEndpoint, messageID, token));

        log.info("Empty ACK received for message with ID {} to {} (Token: {})",
                new Object[]{messageID, remoteEndpoint, token});
    }

    @Override
    public void processReset(InetSocketAddress remoteEndpoint, int messageID, Token token) {

        long actualTime = System.currentTimeMillis();
        emptyRSTs.put(actualTime, new InternalMessageDataWrapper(remoteEndpoint, messageID, token));

        log.info("RST received for message with ID {} to {} (Token: {})",
                new Object[]{messageID, remoteEndpoint, token});
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


    public SortedSetMultimap<Long, InternalMessageDataWrapper> getEmptyACKs(){
        return this.emptyACKs;
    }


    public SortedSetMultimap<Long, InternalMessageDataWrapper> getEmptyRSTs(){
        return this.emptyRSTs;
    }


    public SortedSetMultimap<Long, InternalMessageDataWrapper> getTransmissions(){
        return this.transmissions;
    }


    public SortedSetMultimap<Long, InternalMessageDataWrapper> getTransmissionTimeouts(){
        return this.transmissionTimeouts;
    }
}
