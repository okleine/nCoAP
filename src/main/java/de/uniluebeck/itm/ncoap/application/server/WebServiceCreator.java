package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 17.11.13
 * Time: 12:42
 * To change this template use File | Settings | File Templates.
 */
public abstract class WebServiceCreator {

    private CoapServerApplication serverApplication;

    public WebServiceCreator(CoapServerApplication serverApplication){
        this.serverApplication = serverApplication;
    }

    public abstract void handleWebServiceCreationRequest(SettableFuture<CoapResponse> responseFuture,
                                                                 CoapRequest coapRequest);

    public CoapServerApplication getServerApplication() {
        return serverApplication;
    }
}
