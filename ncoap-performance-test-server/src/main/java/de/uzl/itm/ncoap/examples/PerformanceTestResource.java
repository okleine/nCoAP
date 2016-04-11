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
package de.uzl.itm.ncoap.examples;

import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.resource.NotObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by olli on 11.09.15.
 */
public class PerformanceTestResource extends NotObservableWebresource<String> {

    private AtomicInteger requestCount;

    private byte[] status = "PERFORMANCE!".getBytes(CoapMessage.CHARSET);

    public PerformanceTestResource(String servicePath, String initialStatus, long lifetimeSeconds,
            ScheduledExecutorService executor) {
        super(servicePath, initialStatus, lifetimeSeconds, executor);
        this.requestCount = new AtomicInteger(0);
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception {

        int count = requestCount.incrementAndGet();
        if(count % 1000 == 0) {
            System.out.println("Process Request No. " + count);
        }
        WrappedResourceStatus status = getWrappedResourceStatus(0);
        CoapResponse coapResponse = new CoapResponse(MessageType.NON, MessageCode.CONTENT_205);
        coapResponse.setContent(status.getContent(), ContentFormat.TEXT_PLAIN_UTF8);
        responseFuture.set(coapResponse);
    }

    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        return this.status;
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return new byte[1];
    }

    @Override
    public void updateEtag(String resourceStatus) {

    }

    @Override
    public void shutdown() {

    }


}
