package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.testtools.Initialization;

import java.util.*;

import static org.junit.Assert.fail;

/**
 * A CoapServerApplication for testing purposes.
 * To add a prepared response use the addResponse() method.
 * In addition a path (or multiple) must be registered on which the resource (response) will be available.
 * (Use the registerDummyService() method.)
 * Received requests can be retrieved using getReceivedRequests().
 * 
 * @author Oliver Kleine, Stefan Hueske
 */
public class CoapTestServer extends CoapServerApplication {

    //if "false" incoming messages are ignored and thrown away
    private boolean receiveEnabled = true;
    private  SortedMap<Long, CoapRequest> receivedRequests = new TreeMap<Long, CoapRequest>();

    //if "false" responses are not actually sent but stored in an outgoing queue
    private boolean writeEnabled = true;
    private List<CoapResponse> responsesToSend = new LinkedList<CoapResponse>();


    //time to block thread in receiveCoapRequest() to force a separate response
    private long waitBeforeSendingResponse = 0;

    private static CoapTestServer instance = new CoapTestServer();

    public static CoapTestServer getInstance(){
        return instance;
    }

    private CoapTestServer(){
        registerService(new ObservableDummyWebService(2000, 0));
        registerService(new NotObservableDummyWebService(0));
    }

    public synchronized void setReceiveEnabled(boolean enabled){
        this.receiveEnabled = enabled;
    }

    public synchronized void setWriteEnabled(boolean enabled){
        this.writeEnabled = enabled;
    }

    public synchronized void reset() {
        receivedRequests.clear();
        responsesToSend.clear();
        waitBeforeSendingResponse = 0;
        setReceiveEnabled(true);
        removeAllServices();
    }

    private CoapResponse receiveCoapRequest(CoapRequest coapRequest) {
        if (receiveEnabled) {
            synchronized(this) {
                receivedRequests.put(System.currentTimeMillis(), coapRequest);
            }
        }
        try {
            Thread.sleep(waitBeforeSendingResponse);
        } catch (InterruptedException ex) {
            fail(ex.toString());
        }
        if(responsesToSend.isEmpty()) {
            fail("responsesToSend is empty. This could be caused by an unexpected request.");
        }
        return responsesToSend.remove(0);
    }

    public void addResponse(CoapResponse... responses) {
        responsesToSend.addAll(Arrays.asList(responses));
    }
    
    public void addResponse(int count, CoapResponse... responses) {
        for (int i = 0; i < count; i++) {
            addResponse(responses);
        }
    }

    public void setWaitBeforeSendingResponse(long waitBeforeSendingResponse) {
        this.waitBeforeSendingResponse = waitBeforeSendingResponse;
    }
    
    @Override
    public void handleRetransmissionTimout() {}

    public SortedMap<Long, CoapRequest> getReceivedRequests() {
        return receivedRequests;
    }

    public static void main(String[] args){
        Initialization.init();
    }
}
