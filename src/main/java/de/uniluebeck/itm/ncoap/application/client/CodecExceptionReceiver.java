package de.uniluebeck.itm.ncoap.application.client;

import de.uniluebeck.itm.ncoap.communication.codec.CodecException;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 18.11.13
 * Time: 12:15
 * To change this template use File | Settings | File Templates.
 */
public interface CodecExceptionReceiver {

    public void handleCodecException(Throwable excpetion);

}
