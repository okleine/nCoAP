package de.uniluebeck.itm.ncoap.etsi.client;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;

/**
 * Created by olli on 15.09.14.
 */
public class ApplicationFactory{

    private static ApplicationFactory instance = new ApplicationFactory();

    private static CoapClientApplication coapClientApplication = new CoapClientApplication();

    private ApplicationFactory(){}

    public static CoapClientApplication getCoapClientApplication(){
        return coapClientApplication;
    }
}
