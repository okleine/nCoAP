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

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.options.OptionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by olli on 11.04.16.
 */
public class NotObservableTestWebresourceForPost extends NotObservableWebresource<String>{

    private static Logger LOG = LoggerFactory.getLogger(NotObservableTestWebresourceForPost.class.getName());

    private byte[] weakEtag;

    public NotObservableTestWebresourceForPost(String servicePath, String initialStatus, long lifetimeSeconds,
                                               ScheduledExecutorService executor) {
        super(servicePath, initialStatus, lifetimeSeconds, executor);
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return this.weakEtag;
    }

    @Override
    public void updateEtag(String resourceStatus) {
       this.weakEtag = Ints.toByteArray(resourceStatus.hashCode());
    }

    @Override
    public void shutdown() {
        // nothing to do...
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteSocket) throws Exception {

        int messageType = coapRequest.getMessageType();

        if (coapRequest.getMessageCode() == MessageCode.POST) {
            this.setResourceStatus(coapRequest.getContent().toString(CoapMessage.CHARSET), 0);
            WrappedResourceStatus status = getWrappedResourceStatus(ContentFormat.TEXT_PLAIN_UTF8);

            CoapResponse coapResponse = new CoapResponse(messageType, MessageCode.CHANGED_204);
            coapResponse.setContent(status.getContent(), status.getContentFormat());
            coapResponse.setEtag(status.getEtag());

            responseFuture.set(coapResponse);
        } else {
            CoapResponse coapResponse = new CoapResponse(messageType, MessageCode.METHOD_NOT_ALLOWED_405);
            coapResponse.setContent("POST ONLY".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

            responseFuture.set(coapResponse);
        }
    }

    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        return getResourceStatus().getBytes(CoapMessage.CHARSET);
    }
}
