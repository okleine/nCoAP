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

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by olli on 23.09.14.
 */
public class PerformanceTestWebservice extends ObservableWebservice<Integer> {

    private static Logger log = LoggerFactory.getLogger(PerformanceTestWebservice.class.getName());

    private static final long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private final int serviceNo;
    private final int updateInterval;


    public PerformanceTestWebservice(int serviceNo, int initialStatus, int updateInterval,
                                     ScheduledExecutorService executor){
        super("/service/" + String.format("%03d", serviceNo), initialStatus, executor);
        this.serviceNo = serviceNo;
        this.updateInterval = updateInterval;
    }


    public void initialize(){
        this.getExecutor().scheduleAtFixedRate(new Runnable(){

            @Override
            public void run() {
                try{
                    setResourceStatus(getResourceStatus() + 1, 0);
                    log.info("New status of service {}: {}", String.format("%03d", serviceNo), getResourceStatus());
                }
                catch(Exception ex){
                    log.error("Unexpected Error!", ex);
                }
            }

        }, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
    }


    @Override
    public MessageType.Name getMessageTypeForUpdateNotification(InetSocketAddress remoteEndpoint, Token token) {
        return MessageType.Name.CON;
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return new byte[0];
    }

    @Override
    public void updateEtag(Integer resourceStatus) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        String result = null;

        if(contentFormat == ContentFormat.TEXT_PLAIN_UTF8){
            result = "Service " + String.format("%03d", serviceNo) + ": Status " + getResourceStatus();
        }

        else if(contentFormat == ContentFormat.APP_XML){
            result = "<service>\n" +
                     "\t<number>" + String.format("%03d", serviceNo) + "<number>\n" +
                     "\t<status>" + getResourceStatus() + "</status>\n" +
                     "</service>";
        }

        if(result != null){
            return result.getBytes(CoapMessage.CHARSET);
        }

        else {
            return null;
        }
    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception {

        log.info("Service #{} received request!", String.format("%03d", serviceNo));

        CoapResponse coapResponse;

        if(coapRequest.getMessageCodeName() != MessageCode.Name.GET){
            coapResponse = CoapResponse.createErrorResponse(MessageType.Name.CON,
                    MessageCode.Name.METHOD_NOT_ALLOWED_405, "GET is the only supported method!");
        }
        else{

            long contentFormat = determineResponseContentFormat(coapRequest);

            if(contentFormat == ContentFormat.UNDEFINED){
                coapResponse = CoapResponse.createErrorResponse(MessageType.Name.CON,
                        MessageCode.Name.BAD_REQUEST_400, "Accepted content format(s) not supported!");
            }

            else{
                coapResponse = new CoapResponse(MessageType.Name.CON, MessageCode.Name.CONTENT_205);
                coapResponse.setContent(getSerializedResourceStatus(contentFormat), contentFormat);
                coapResponse.setObserve();
            }
        }

        responseFuture.set(coapResponse);
    }


    private long determineResponseContentFormat(CoapRequest coapRequest){

        Set<Long> accepted = coapRequest.getAcceptedContentFormats();

        if(accepted.isEmpty()){
            return DEFAULT_CONTENT_FORMAT;
        }

        else if(coapRequest.getAcceptedContentFormats().contains(ContentFormat.TEXT_PLAIN_UTF8)){
            return ContentFormat.TEXT_PLAIN_UTF8;
        }

        else if(coapRequest.getAcceptedContentFormats().contains(ContentFormat.APP_XML)){
            return ContentFormat.APP_XML;
        }

        else{
            return ContentFormat.UNDEFINED;
        }
    }
}
