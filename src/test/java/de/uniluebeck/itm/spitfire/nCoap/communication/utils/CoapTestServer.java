package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest;

import java.util.LinkedList;
import java.util.List;

public class CoapTestServer extends CoapServerApplication {

    public CoapTestServer(int serverPort){
        super(serverPort);
    }
}
