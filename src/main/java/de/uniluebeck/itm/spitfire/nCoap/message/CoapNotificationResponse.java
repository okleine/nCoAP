package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication.ObservableResourceManager;
import org.jboss.netty.buffer.ChannelBuffer;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;

public class CoapNotificationResponse extends CoapResponse {
    
    CoapServerApplication.ObservableResourceManager observableResourceManager;
    
    public CoapNotificationResponse(Code code){
        super(code);
    }

    public CoapNotificationResponse(MsgType msgType, Code code){
        super(msgType, code);
    }

    public CoapNotificationResponse(MsgType msgType, Code code, int messageID)
            throws ToManyOptionsException, InvalidHeaderException {

        super(msgType, code, messageID);
    }

    public CoapNotificationResponse(Header header, OptionList optionList, ChannelBuffer payload){
        super(header, optionList, payload);
    }

    public void setObservableResourceManager(ObservableResourceManager observableResourceManager) {
        this.observableResourceManager = observableResourceManager;
    }

    public ObservableResourceManager getObservableResourceManager() {
        return observableResourceManager;
    }
}
