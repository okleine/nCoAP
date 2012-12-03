package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 27.08.12
 * Time: 18:22
 * To change this template use File | Settings | File Templates.
 */
public class InternalAcknowledgementMessage implements InternalMessage {

    private ByteArrayWrapper token;

    public InternalAcknowledgementMessage(ByteArrayWrapper token){
        this.token = token;
    }

    @Override
    public ByteArrayWrapper getContent(){
        return token;
    }
}
