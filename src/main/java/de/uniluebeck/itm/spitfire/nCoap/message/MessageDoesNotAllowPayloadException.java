package de.uniluebeck.itm.spitfire.nCoap.message;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 08.01.12
 * Time: 21:57
 * To change this template use File | Settings | File Templates.
 */
public class MessageDoesNotAllowPayloadException extends Exception{

    public MessageDoesNotAllowPayloadException(String msg){
        super(msg);
    }
}
