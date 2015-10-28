package de.uzl.itm.ncoap.communication.dispatching.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;

import java.net.InetSocketAddress;

/**
 * Created by olli on 05.10.15.
 */
public interface RequestConsumer {

    /**
     * This method is invoked by the framework on inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s with {@link de.uzl.itm.ncoap.message.MessageCode.Name#PUT} if
     * there is no {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} registered at the path given as {@link de.uzl.itm.ncoap.message.CoapRequest#getUriPath()}.
     *
     * @param responseFuture the {@link com.google.common.util.concurrent.SettableFuture} to be set with a proper {@link de.uzl.itm.ncoap.message.CoapResponse} to indicate
     *                       whether there was a new {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} created or not.
     *
     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be processed
     *
     * @param remoteSocket the {@link java.net.InetSocketAddress} of the {@link de.uzl.itm.ncoap.message.CoapRequest}s origin.
     */
    public abstract void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                            InetSocketAddress remoteSocket) throws Exception;

}
