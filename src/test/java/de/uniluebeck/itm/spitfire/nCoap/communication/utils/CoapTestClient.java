package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.fail;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;


/**
 * A CoapClientApplication for testing purposes.
 * To send a request use the inherited writeCoapRequest() method.
 * Received responses are stored and can be accessed using getReceivedResponses().
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public class CoapTestClient extends CoapClientApplication {

    private static CoapTestClient instance = new CoapTestClient();

    private SortedMap<Long, CoapResponse> receivedResponses = new TreeMap<Long, CoapResponse>();
    private long emptyAckNotificationTime;
    private long timeoutNotificationTime;

    public static CoapTestClient getInstance(){
        return instance;
    }

    private CoapTestClient(){}

    @Override
    public void receiveResponse(CoapResponse coapResponse) {
        receivedResponses.put(System.currentTimeMillis(), coapResponse);
    }

    @Override
    public void receiveEmptyACK() {
        emptyAckNotificationTime = System.currentTimeMillis();
    }

    @Override
    public void handleRetransmissionTimout() {
        timeoutNotificationTime = System.currentTimeMillis();
    }

    public long getEmptyAckNotificationTime() {
        return emptyAckNotificationTime;
    }

    public long getTimeoutNotificationTime() {
        return timeoutNotificationTime;
    }

    public SortedMap<Long, CoapResponse> getReceivedResponses() {
        return receivedResponses;
    }
    
    public void shutdown(){

    }
    public void reset() {
        receivedResponses.clear();
        rebindChannel();
    }
}
