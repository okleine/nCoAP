package de.uzl.itm.ncoap.endpoints.client;

import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.linkformat.LinkValueList;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapResponse;

/**
 * Created by olli on 09.05.16.
 */
//public class WkcCallback extends ClientCallback {
//
//    private int numberOfResources = 0;
//
//    @Override
//    public void processCoapResponse(CoapResponse coapResponse) {
//        LinkValueList linkValueList = LinkValueList.decode(
//                new String(coapResponse.getContentAsByteArray(), CoapMessage.CHARSET)
//        );
//        this.numberOfResources = linkValueList.getUriReferences().size();
//    }
//
//    public int getNumberOfResources() {
//        return this.numberOfResources;
//    }
//}
