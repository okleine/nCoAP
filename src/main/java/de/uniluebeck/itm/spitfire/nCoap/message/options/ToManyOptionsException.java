package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapException;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;

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
