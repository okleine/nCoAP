package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * This observable resource changes its status periodically with a delay given as argument for the constructor.
 * If the actual status (return value of method <code>getResourceStatus()</code>) is <code>true</code>, the payload
 * of a response is <code>testpayload1</code>, otherwise the payload is <code>testpayload2</code>.
 */
public class ObservableDummyWebService extends ObservableWebService<Boolean>{

    private static Logger log = LoggerFactory.getLogger(ObservableDummyWebService.class.getName());

    private long pretendedProcessingTimeForRequests;
    private Thread statusUpdateThread;

    private List<CoapResponse> responsesToSend = new LinkedList<CoapResponse>();
    
    public ObservableDummyWebService(String path, Boolean initialStatus, long pretendedProcessingTimeForRequests,
                                     final long updateIntervalMillis){

        super(path, initialStatus);
        this.pretendedProcessingTimeForRequests = pretendedProcessingTimeForRequests;

        if(updateIntervalMillis > 0){
            //Start regular status update
            statusUpdateThread = new Thread() {

                @Override
                public void run() {
                    while(!isInterrupted()){
                        try {
                            Thread.sleep(updateIntervalMillis);
                            //switch status between true and false
                            setResourceStatus(!getResourceStatus());
                            log.debug("New resource status: " + (getResourceStatus() ? "testpayload1" : "testpayload2"));
                        } catch (InterruptedException e) {
                            interrupt();
                            log.debug("StatusUpdateThread was interrupted, shutting down...");
                        }
                    }
                }
            };
            statusUpdateThread.start();
        }
    }
    
    public void shutdownStatusUpdateThread() {
        if (statusUpdateThread != null) {
            statusUpdateThread.interrupt();
        }
    }


    @Override
    public CoapResponse processMessage(CoapRequest request) {
        if (!responsesToSend.isEmpty()) {
            return responsesToSend.remove(0);
        }
        
        //Simulate a potentially long processing time
        try {
            Thread.sleep(pretendedProcessingTimeForRequests);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //Create response 
        CoapResponse response = new CoapResponse(Code.CONTENT_205);
        try {
            String payload = getResourceStatus() ? "testpayload1" : "testpayload2";
            response.setPayload(ChannelBuffers.wrappedBuffer(payload.getBytes(Charset.forName("UTF-8"))));

            response.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);

        } catch (InvalidOptionException e) {
            fail(e.getMessage());
        } catch (ToManyOptionsException e) {
            fail(e.getMessage());
        } catch (MessageDoesNotAllowPayloadException e) {
            fail(e.getMessage());
        }

        return response;
    }
    
    public void addPreparedResponses(CoapResponse... responses) {
        responsesToSend.addAll(Arrays.asList(responses));
    }

    public void addPreparedResponses(int multiplier, CoapResponse response) {
        for (int i = 0; i < multiplier; i++) {
            addPreparedResponses(response);
        }
    }
}
