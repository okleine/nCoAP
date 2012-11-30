package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import java.net.InetSocketAddress;

/**
 * @author Oliver Kleine
 */
public abstract class CoapException extends Exception{

    public CoapException(String msg) {
            super(msg);
    }
}
