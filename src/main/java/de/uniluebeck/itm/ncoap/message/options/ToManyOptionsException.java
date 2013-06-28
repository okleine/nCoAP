package de.uniluebeck.itm.ncoap.message.options;

import de.uniluebeck.itm.ncoap.communication.core.CoapException;
import de.uniluebeck.itm.ncoap.message.CoapMessage;

/**
 * Exception thrown by the nCoAP framework when there are to many options to be included in a
 * {@link CoapMessage}.
 */
public class ToManyOptionsException extends CoapException {

    /**
     * @param message A string representation of the reason that caused the excpetion
     */
    public ToManyOptionsException(String message){
        super(message);
    }
}
