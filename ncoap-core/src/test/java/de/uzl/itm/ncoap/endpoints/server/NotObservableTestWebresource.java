/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.endpoints.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.fail;

/**
* Simple implementation of {@link de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource} to handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s.
*
* @author Oliver Kleine
*/
public class NotObservableTestWebresource extends NotObservableWebresource<String> {

    private static Logger log = LoggerFactory.getLogger(NotObservableTestWebresource.class.getName());

    private static final long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;
    private static List<Long> supportedContentFormats = new ArrayList<>(2);
    static{
        supportedContentFormats.add(DEFAULT_CONTENT_FORMAT);
        supportedContentFormats.add(ContentFormat.APP_XML);
    }


    private long pretendedProcessingTimeForRequests;

    /**
     * @param path the path of this {@link de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource} URI
     * @param initialStatus the initial status of this {@link de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource}
     * @param lifetimeSeconds the lifetime of the initial status in seconds
     * @param pretendedProcessingTimeForRequests the time to delay the processing of inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s (to
     *                                           simulate long processing time)
     */
    public NotObservableTestWebresource(String path, String initialStatus, long lifetimeSeconds,
                                        long pretendedProcessingTimeForRequests, ScheduledExecutorService executor) {

        super(path, initialStatus, lifetimeSeconds, executor);
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


    private void delay() {
        //Simulate a potentially long processing time
        try {
            Thread.sleep(pretendedProcessingTimeForRequests);
        } catch (InterruptedException e) {
            fail("This should never happen.");
        }
    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) throws Exception{

        log.info("HANDLE INBOUND REQUEST (Resource: \"" + getUriPath() + "\")");

        //Delay the inbound requests
        if (this.pretendedProcessingTimeForRequests > 0) {
            delay();
        }

        Long contentFormat = determineResponseContentFormat(coapRequest);

        //create error response if content could not be created
        if (contentFormat == null) {
            CoapResponse coapResponse =
                    new CoapResponse(coapRequest.getMessageType(), MessageCode.BAD_REQUEST_400);

            String content = "None of accepted content formats is supported by this Webservice.";
            coapResponse.setContent(content.getBytes(CoapMessage.CHARSET));
            responseFuture.set(coapResponse);
        }

        //create response with content if available
        else{
            byte[] content = getSerializedResourceStatus(contentFormat);
            CoapResponse coapResponse =
                    new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);

            coapResponse.setContent(content, contentFormat);
            responseFuture.set(coapResponse);
        }
    }


    private Long determineResponseContentFormat(CoapRequest coapRequest) {
        Iterator<Long> acceptedContentFormats = coapRequest.getAcceptedContentFormats().iterator();

        Long contentFormat = null;

        if (!acceptedContentFormats.hasNext()) {
            contentFormat  = DEFAULT_CONTENT_FORMAT;
        }

        while(contentFormat == null && acceptedContentFormats.hasNext()) {
            long candidate = acceptedContentFormats.next();
            if (supportedContentFormats.contains(candidate)) {
                contentFormat = candidate;
            }
        }

        return contentFormat;
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        if (contentFormat == ContentFormat.TEXT_PLAIN_UTF8)
            return getResourceStatus().getBytes(Charset.forName("UTF-8"));

        if (contentFormat == ContentFormat.APP_XML)
            return ("<status>" + getResourceStatus() + "</status>").getBytes(Charset.forName("UTF-8"));

        return null;
    }
}
