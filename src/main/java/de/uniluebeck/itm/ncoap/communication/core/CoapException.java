package de.uniluebeck.itm.ncoap.communication.core;

/**
 * Abstract class for
 * @author Oliver Kleine
 */
public abstract class CoapException extends Exception{

    public CoapException(String msg) {
            super(msg);
    }
}
