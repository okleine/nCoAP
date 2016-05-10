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
package de.uzl.itm.ncoap.examples.server;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.CT;
import static de.uzl.itm.ncoap.message.options.ContentFormat.APP_XML;
import static de.uzl.itm.ncoap.message.options.ContentFormat.TEXT_PLAIN_UTF8;

/**
 * A simple not observable {@link de.uzl.itm.ncoap.application.server.resource.Webresource} implementation
 *
 * @author Oliver Kleine
 */
public class SimpleNotObservableWebresource extends NotObservableWebresource<String[]> {

    private static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private int weakEtag;

    protected SimpleNotObservableWebresource(String servicePath, String[] initialStatus, long lifetimeSeconds,
                                             ScheduledExecutorService executor) {

        super(servicePath, initialStatus, lifetimeSeconds, executor);

        //Sets the link attributes for supported content types ('ct')
        String ctValue = "\"" + TEXT_PLAIN_UTF8 + " " + APP_XML + "\"";
        this.setLinkParam(LinkParam.createLinkParam(CT, ctValue));
    }


    @Override
    public byte[] getEtag(long contentFormat) {
        return Ints.toByteArray(weakEtag & Longs.hashCode(contentFormat));
    }


    @Override
    public void updateEtag(String[] resourceStatus) {
        weakEtag = Arrays.hashCode(resourceStatus);
    }


    @Override
    public void shutdown() {
        //nothing to to
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception {

        if (coapRequest.getMessageCode() != MessageCode.GET) {
            setMethodNotAllowedResponse(responseFuture, coapRequest);
        } else {
            processGet(responseFuture, coapRequest);
        }
    }


    private void setMethodNotAllowedResponse(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest)
            throws Exception{

        CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.METHOD_NOT_ALLOWED_405);

        byte[] content = "Only method GET is allowed!".getBytes(CoapMessage.CHARSET);
        coapResponse.setContent(content, ContentFormat.TEXT_PLAIN_UTF8);

        responseFuture.set(coapResponse);
    }


    private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest) {
        //create resource status
        WrappedResourceStatus resourceStatus;
        if (coapRequest.getAcceptedContentFormats().isEmpty()) {
            resourceStatus = getWrappedResourceStatus(DEFAULT_CONTENT_FORMAT);
        } else {
            resourceStatus = getWrappedResourceStatus(coapRequest.getAcceptedContentFormats());
        }

        //create CoAP response
        CoapResponse coapResponse;
        if (resourceStatus == null) {
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);
            coapResponse.setContent("None of the accepted content formats is supported!".getBytes(CoapMessage.CHARSET),
                    ContentFormat.TEXT_PLAIN_UTF8);
        } else {
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), resourceStatus.getContentFormat());
            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());
        }

        responseFuture.set(coapResponse);
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        StringBuffer buffer = new StringBuffer();
        if (contentFormat == ContentFormat.TEXT_PLAIN_UTF8) {
            for (String paragraph : this.getResourceStatus()) {
                buffer.append(paragraph).append("\n");
            }
        } else if (contentFormat == ContentFormat.APP_XML) {
            for (String paragraph : this.getResourceStatus()) {
                buffer.append("<paragraph>\n  ").append(paragraph).append("\n</paragraph>\n");
            }
        }

        if (buffer.length() == 0) {
            return null;
        } else {
            buffer.deleteCharAt(buffer.length() - 1);
            return buffer.toString().getBytes(CoapMessage.CHARSET);
        }
    }
}
