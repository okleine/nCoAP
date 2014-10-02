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
package de.uniluebeck.itm.ncoap.examples;

import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.UintOptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by olli on 23.09.14.
 */
public class PerformanceTestClientCallback extends ClientCallback {

    private static Logger log = LoggerFactory.getLogger(PerformanceTestClientCallback.class.getName());

    private SortedMap<Long, CoapResponse> updateNotifications;
    private int callbackNo;

    public PerformanceTestClientCallback(int callbackNo){
        updateNotifications = Collections.synchronizedSortedMap(new TreeMap<Long, CoapResponse>());
        this.callbackNo = callbackNo;
    }

    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
        long sequenceNo = coapResponse.getObserve();

        if(sequenceNo == UintOptionValue.UNDEFINED || MessageCode.isErrorMessage(coapResponse.getMessageCode())){
            log.error("Callback #{} received (no update-notification): {}", callbackNo, coapResponse);
        }
        else{
            updateNotifications.put(sequenceNo, coapResponse);
//            log.info("Received #{}", sequenceNo);
        }
    }

    @Override
    public boolean continueObservation(){
        return true;
    }

    public int getCallbackNo(){
        return this.callbackNo;
    }

    public Map<Long, CoapResponse> getUpdateNotifications(){
        return this.updateNotifications;
    }

    public CoapResponse getFirstCoapResponse(){
        return updateNotifications.get(updateNotifications.firstKey());
    }


    public int getFirstStatus(){
        CoapResponse coapResponse = getFirstCoapResponse();

        return coapResponse.getContentFormat() == ContentFormat.TEXT_PLAIN_UTF8 ?
                determineStatusNumberFromPlainText(coapResponse.getContent().toString(CoapMessage.CHARSET)) :
                determineStatusNumberFromXml(coapResponse.getContent().toString(CoapMessage.CHARSET));
    }

    public CoapResponse getLastCoapResponse(){
        return updateNotifications.get(updateNotifications.lastKey());
    }

    public int getLastStatus(){
        CoapResponse coapResponse = getLastCoapResponse();

        return coapResponse.getContentFormat() == ContentFormat.TEXT_PLAIN_UTF8 ?
                determineStatusNumberFromPlainText(coapResponse.getContent().toString(CoapMessage.CHARSET)) :
                determineStatusNumberFromXml(coapResponse.getContent().toString(CoapMessage.CHARSET));
    }

    public Set<Integer> getMissingStates(){
        Set<Integer> result = new TreeSet<>();

        Iterator<Long> iterator = updateNotifications.keySet().iterator();

        CoapResponse previousResponse = updateNotifications.get(iterator.next());
        int previousStatus = previousResponse.getContentFormat() == ContentFormat.TEXT_PLAIN_UTF8 ?
                determineStatusNumberFromPlainText(previousResponse.getContent().toString(CoapMessage.CHARSET)) :
                determineStatusNumberFromXml(previousResponse.getContent().toString(CoapMessage.CHARSET));

        while(iterator.hasNext()){
            CoapResponse actualResponse = updateNotifications.get(iterator.next());
            int actualStatus = actualResponse.getContentFormat() == ContentFormat.TEXT_PLAIN_UTF8 ?
                    determineStatusNumberFromPlainText(actualResponse.getContent().toString(CoapMessage.CHARSET)) :
                    determineStatusNumberFromXml(actualResponse.getContent().toString(CoapMessage.CHARSET));

            if(actualStatus - previousStatus > 1){
                for(int i = previousStatus + 1; i < actualStatus; i++){
                    result.add(i);
                }
            }

            previousStatus = actualStatus;
        }

        return result;
    }

    private int determineStatusNumberFromXml(String payload){
        String tmp = payload.substring(40, payload.indexOf("</status>"));
        return Integer.parseInt(tmp);
    }

    private int determineStatusNumberFromPlainText(String payload){
        return Integer.parseInt(payload.substring(20));
    }
}
