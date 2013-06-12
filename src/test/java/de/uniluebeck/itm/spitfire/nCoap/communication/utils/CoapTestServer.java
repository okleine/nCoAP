package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest;

public class CoapTestServer extends CoapServerApplication {

    public CoapTestServer(int serverPort){
        super(serverPort);
    }

    @Override
    public void handleRetransmissionTimout() {
        //Do nothing...
    }


//    public static void main(String[] args){
//        AbstractCoapCommunicationTest.initializeLogging();
//        CoapTestServer server = new CoapTestServer(5683);
//
//        server.registerService(new ObservableTestWebService("/obs", 0, 0, 2000));
//
//    }
}
