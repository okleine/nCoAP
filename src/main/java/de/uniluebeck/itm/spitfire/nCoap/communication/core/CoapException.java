package de.uniluebeck.itm.spitfire.nCoap.communication.core;

/**
 * @author Oliver Kleine
 */
public abstract class CoapException extends Exception{

    public CoapException(String msg) {
            super(msg);
    }
}
