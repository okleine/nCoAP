package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapServerDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 30.11.12
 * Time: 17:34
 * To change this template use File | Settings | File Templates.
 */
public class CoapTestServer extends CoapServerApplication {

    private static CoapTestServer instance = new CoapTestServer();

    //if false receivedRequests will not be modified
    boolean receivingEnabled = true;
    public SortedMap<Long, CoapRequest> receivedRequests = new TreeMap<Long, CoapRequest>();

    public List<CoapResponse> responsesToSend = new LinkedList<CoapResponse>();

    public static CoapTestServer getInstance(){
        return instance;
    }

    private CoapTestServer(){}

    //time to block thread in receiveCoapRequest() to force a separate response
    public long waitBeforeSendingResponse = 0;

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

    @Override
    public CoapResponse receiveCoapRequest(CoapRequest coapRequest,
            InetSocketAddress senderAddress) {
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

    @Override
    public void handleRetransmissionTimout() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
