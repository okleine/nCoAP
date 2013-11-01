/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static org.junit.Assert.fail;

/**
 * Simple implementation of {@link NotObservableWebService} to handle incoming {@link CoapRequest}s.
 *
 * @author Oliver Kleine
 */
public class NotObservableTestWebService extends NotObservableWebService<String>{

    private static Logger log = LoggerFactory.getLogger(NotObservableTestWebService.class.getName());

    private long pretendedProcessingTimeForRequests;

    public NotObservableTestWebService(String path, String initialStatus, long pretendedProcessingTimeForRequests){
        super(path, initialStatus);
        this.pretendedProcessingTimeForRequests = pretendedProcessingTimeForRequests;
    }

    @Override
    public void shutdown() {
        //Nothing to do here...
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture,
                                   CoapRequest request, InetSocketAddress remoteAddress) {
        log.debug("Incoming request for resource " + getPath());

        //Simulate a potentially long processing time
        try {
            Thread.sleep(pretendedProcessingTimeForRequests);
        } catch (InterruptedException e) {
            fail("This should never happen.");
        }

        //create response
        CoapResponse response = new CoapResponse(MessageCode.CONTENT_205);

        try {
            response.setPayload(ChannelBuffers.wrappedBuffer(getResourceStatus().getBytes(Charset.forName("UTF-8"))));
        } catch (MessageDoesNotAllowPayloadException e) {
            log.error("This should never happen.", e);
            fail("This should never happen.");
        }

        log.debug("Created response for resource {}: {}.", getPath(), response);

        responseFuture.set(response);
    }
}
