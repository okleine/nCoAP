package de.uniluebeck.itm.ncoap.communication.encoding;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 05.11.13
 * Time: 18:01
 * To change this template use File | Settings | File Templates.
 */
public class DecodingFailedException extends Exception{

    public DecodingFailedException(String message){
        super(message);
    }
}
