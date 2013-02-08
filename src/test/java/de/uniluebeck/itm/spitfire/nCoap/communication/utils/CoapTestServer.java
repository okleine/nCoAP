package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.Service;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.fail;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;

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

    private static CoapTestServer instance = new CoapTestServer();

    //if false receivedRequests will not be modified
    private boolean receivingEnabled = true;
    private  SortedMap<Long, CoapRequest> receivedRequests = new TreeMap<Long, CoapRequest>();

    private List<CoapResponse> responsesToSend = new LinkedList<CoapResponse>();

    private DummyService dummyService;
    
    //time to block thread in receiveCoapRequest() to force a separate response
    private long waitBeforeSendingResponse = 0;
    
    public static CoapTestServer getInstance(){
        return instance;
    }

    private CoapTestServer(){
        dummyService = new DummyService();
    }
    
    private class DummyService extends Service {
        @Override
        public CoapResponse getStatus(CoapRequest request) {
            return receiveRequest(request);
        }
        
        public void notifyObservers1() {
            notifyCoapObservers();
        }
        
    }

    public void registerDummyService(String path) {
        registerService(path, dummyService);
    }

    public void notifyCoapObservers() {
        dummyService.notifyObservers1();
    }

    public synchronized void enableReceiving() {
        receivingEnabled = true;
    }

    public synchronized void disableReceiving() {
        receivingEnabled = false;
    }

    public synchronized void reset() {
        receivedRequests.clear();
        responsesToSend.clear();
        waitBeforeSendingResponse = 0;
        enableReceiving();
        removeAllServices();
        rebindChannel();
    }

    public void blockUntilMessagesReceivedOrTimeout(long timeout, int messagesCount)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - startTime < timeout) {
            synchronized(this) {
                if (receivedRequests.size() >= messagesCount) {
                    return;
                }
            }
            Thread.sleep(50);
        }
    }
    
    private CoapResponse receiveRequest(CoapRequest coapRequest) {
        if (receivingEnabled) {
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
    
}
