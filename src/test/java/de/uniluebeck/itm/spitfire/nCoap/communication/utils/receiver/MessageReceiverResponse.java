package de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.04.13
 * Time: 20:32
 * To change this template use File | Settings | File Templates.
 */
public class MessageReceiverResponse extends CoapResponse{
    private boolean receiverSetsMsgID;
    private boolean receiverSetsToken;

    public MessageReceiverResponse(CoapResponse coapResponse, boolean receiverSetsMsgID, boolean receiverSetsToken) throws InvalidHeaderException {
        super(coapResponse);
        this.receiverSetsMsgID = receiverSetsMsgID;
        this.receiverSetsToken = receiverSetsToken;
    }

    public boolean getReceiverSetsMsgID() {
        return receiverSetsMsgID;
    }

    public boolean getReceiverSetsToken() {
        return receiverSetsToken;
    }
}
