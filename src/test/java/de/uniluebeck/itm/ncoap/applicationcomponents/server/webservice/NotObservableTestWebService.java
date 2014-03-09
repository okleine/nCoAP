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
package de.uniluebeck.itm.ncoap.applicationcomponents.server.webservice;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebservice;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.fail;

/**
* Simple implementation of {@link NotObservableWebservice} to handle incoming {@link CoapRequest}s.
*
* @author Oliver Kleine
*/
public class NotObservableTestWebService extends NotObservableWebservice<String> {

    public static final long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;
    private static Logger log = LoggerFactory.getLogger(NotObservableTestWebService.class.getName());

    private long pretendedProcessingTimeForRequests;

    /**
     * @param path the path of this {@link NotObservableWebservice} URI
     * @param initialStatus the initial status of this {@link NotObservableWebservice}
     * @param lifetimeSeconds the lifetime of the initial status in seconds
     * @param pretendedProcessingTimeForRequests the time to delay the processing of incoming {@link CoapRequest}s (to
     *                                           simulate long processing time)
     */
    public NotObservableTestWebService(String path, String initialStatus, long lifetimeSeconds,
                                       long pretendedProcessingTimeForRequests){

        super(path, initialStatus, lifetimeSeconds);
        this.pretendedProcessingTimeForRequests = pretendedProcessingTimeForRequests;
    }


    @Override
    public byte[] getEtag(long contentFormat) {
        return new byte[0];
    }


    @Override
    public void updateEtag(String resourceStatus) {
        //Nothing to do
    }


    @Override
    public void shutdown() {
        //Nothing to do here...
    }


    private void delay(){
        //Simulate a potentially long processing time
        try {
            Thread.sleep(pretendedProcessingTimeForRequests);
        } catch (InterruptedException e) {
            fail("This should never happen.");
        }
    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress)
            throws Exception{

        log.debug("Incoming request for resource " + getPath());

        //Delay the incoming requests
        if(this.pretendedProcessingTimeForRequests > 0)
            delay();

        //Initialize variables
        CoapResponse coapResponse;
        byte[] serializedContent = null;
        long contentFormat = -1;

        //create response content
        Set<Long> acceptedContentFormats = coapRequest.getAcceptedContentFormats();

        if(acceptedContentFormats.size() == 0){
            serializedContent = getSerializedResourceStatus(DEFAULT_CONTENT_FORMAT);
        }
        else{
            Iterator<Long> accepted = acceptedContentFormats.iterator();
            while(serializedContent == null && accepted.hasNext()){
                contentFormat = accepted.next();
                serializedContent = getSerializedResourceStatus(contentFormat);
            }
        }

        //create error response if content could not be created
        if(serializedContent == null){
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.BAD_REQUEST_400);

            String content = "None of accepted content formats is supported by this Webservice.";
            coapResponse.setContent(content.getBytes(CoapMessage.CHARSET));
        }

        //create response with content if available
        else{
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.CONTENT_205);
            coapResponse.setContent(serializedContent, contentFormat);
        }

        //set the future with the created CoAP response
        responseFuture.set(coapResponse);
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormatNumber) {
        if(contentFormatNumber == ContentFormat.TEXT_PLAIN_UTF8)
            return getResourceStatus().getBytes(Charset.forName("UTF-8"));
        else
            return null;
    }
}
