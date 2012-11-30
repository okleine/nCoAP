package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapException;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 08.01.12
 * Time: 20:08
 * To change this template use File | Settings | File Templates.
 */
public class ToManyOptionsException extends CoapException {

    public ToManyOptionsException(String msg){
        super(msg);
    }
}
