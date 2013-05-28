package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import org.apache.log4j.Logger;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

//import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapExecutorService;


/**
 * A CoapClientApplication for testing purposes.
 * To send a request use the inherited writeCoapRequest() method.
 * Received responses are stored and can be accessed using getReceivedResponses().
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public class CoapTestClient extends CoapClientApplication {

    //private static CoapTestClient instance = new CoapTestClient();

    private Logger log = Logger.getLogger(this.getClass().getName());

    private SortedMap<Long, CoapResponse> receivedResponses = new TreeMap<Long, CoapResponse>();
    private SortedSet<Long> emptyAckNotificationTimes = new TreeSet<Long>();
    private SortedSet<Long> timeoutNotificationTimes = new TreeSet<Long>();

    private boolean receiveEnabled = true;
    
//    public static CoapTestClient getInstance(){
//        return instance;
//    }
//
//    private CoapTestClient(){}

    @Override
    public void receiveResponse(CoapResponse coapResponse) {
        if (receiveEnabled)  {
            receivedResponses.put(System.currentTimeMillis(), coapResponse);
        }
    }

    @Override
    public void receiveEmptyACK(){
        if(receiveEnabled && !emptyAckNotificationTimes.add(System.currentTimeMillis())){
            log.error("Could not add notification time for empty ACK.");
        }
    }

    @Override
    public void handleRetransmissionTimout() {
        if(receiveEnabled && !timeoutNotificationTimes.add(System.currentTimeMillis())){
            log.error("Could not add notification time for retransmission timeout.");
        }
    }

    public SortedSet<Long> getEmptyAckNotificationTimes() {
        return emptyAckNotificationTimes;
    }

    public SortedSet<Long> getTimeoutNotificationTimes() {
        return timeoutNotificationTimes;
    }

    public SortedMap<Long, CoapResponse> getReceivedResponses() {
        return receivedResponses;
    }
    
//    public void reset() {
//        receivedResponses.clear();
//        setReceiveEnabled(true);
//
//        //CoapExecutorService.cancelAll();
//
//        datagramChannel.close().awaitUninterruptibly();
////        datagramChannel.getFactory().releaseExternalResources();
//        CoapClientDatagramChannelFactory.resetInstance();
//        datagramChannel = CoapClientDatagramChannelFactory.getInstance().getChannel();
//
//    }
    
    public synchronized void setReceiveEnabled(boolean receiveEnabled) {
        this.receiveEnabled = receiveEnabled;
    }
}
