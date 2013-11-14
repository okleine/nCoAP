package de.uniluebeck.itm.ncoap.communication.codec;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.11.13
 * Time: 14:01
 * To change this template use File | Settings | File Templates.
 */
public abstract class CodecException extends Exception{

    public CodecException(Throwable cause){
        super(cause);
    }

}
