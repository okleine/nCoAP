package de.uzl.itm.ncoap.examples;

import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.webresource.NotObservableWebresource;
import de.uzl.itm.ncoap.application.server.webresource.WrappedResourceStatus;
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
        CoapResponse coapResponse = new CoapResponse(MessageType.Name.NON, MessageCode.Name.CONTENT_205);
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
