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
//package de.uniluebeck.itm.ncoap.examples.test.client.utils;
//
//import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
//import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
//import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
//import de.uniluebeck.itm.ncoap.message.CoapResponse;
//
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 19.11.13
// * Time: 16:23
// * To change this template use File | Settings | File Templates.
// */
//public class CoapTestResponseProcessor implements CoapResponseProcessor, EmptyAcknowledgementProcessor{
//
//    private Map<Long, CoapResponse> coapResponses = new LinkedHashMap<>();
//    private List<Long> emptyAcknowledgements = new ArrayList<>();
//
//
//    @Override
//    public void processCoapResponse(CoapResponse coapResponse) {
//        coapResponses.put(System.currentTimeMillis(), coapResponse);
//    }
//
//    @Override
//    public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) {
//        emptyAcknowledgements.add(System.currentTimeMillis());
//    }
//
//    public Map<Long, CoapResponse> getCoapResponses(){
//        return this.coapResponses;
//    }
//
//    public List<Long> getEmptyAcknowledgementTimes(){
//        return this.emptyAcknowledgements;
//    }
//
//    public void clear(){
//        this.emptyAcknowledgements.clear();
//        this.getCoapResponses().clear();
//    }
//}
