package de.uniluebeck.itm.spitfire.nCoap.message.options;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 08.01.12
 * Time: 20:08
 * To change this template use File | Settings | File Templates.
 */
public class ToManyOptionsException extends Exception {

	private static final long serialVersionUID = 1L;

	public ToManyOptionsException(String msg){
        super(msg);
    }
}
