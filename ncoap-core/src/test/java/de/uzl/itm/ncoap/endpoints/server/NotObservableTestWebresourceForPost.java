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
                                   InetSocketAddress remoteEndpoint) throws Exception {

        int messageType = coapRequest.getMessageType();

        if(coapRequest.getMessageCode() == MessageCode.POST) {
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
