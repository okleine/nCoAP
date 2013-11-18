package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 17.11.13
 * Time: 12:44
 * To change this template use File | Settings | File Templates.
 */
public class DefaultWebServiceCreator extends WebServiceCreator {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public DefaultWebServiceCreator(CoapServerApplication serverApplication) {
        super(serverApplication);
    }

    public void handleWebServiceCreationRequest(SettableFuture<CoapResponse> responseFuture,
                                                        CoapRequest coapRequest) {
        try {
            CoapResponse coapResponse = new CoapResponse(MessageCode.Name.BAD_REQUEST_400);
            String message = "This server does not support remote creation of new Websservices via PUT";
            coapResponse.setContent(message.getBytes(CoapMessage.CHARSET));
            responseFuture.set(coapResponse);
        }
        catch (InvalidHeaderException | InvalidMessageException e) {
            log.error("This should never happen.", e);
            responseFuture.setException(e);
        }
    }

}
