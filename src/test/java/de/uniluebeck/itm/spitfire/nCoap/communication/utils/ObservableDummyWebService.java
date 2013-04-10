package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.webservice.ObservableWebService;
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

import static org.junit.Assert.fail;

/**
 * This observable resource changes its status periodically with a delay given as argument for the constructor.
 */
public class ObservableDummyWebService extends ObservableWebService<Boolean>{

    private static Logger log = LoggerFactory.getLogger(ObservableDummyWebService.class.getName());

    private long pretendedProcessingTimeForRequests;

    public ObservableDummyWebService(final long updateIntervalMillis, long pretendedProcessingTimeForRequests){
        super("/observable", false);
        this.pretendedProcessingTimeForRequests = pretendedProcessingTimeForRequests;

        if(updateIntervalMillis > 0){
            //Start regular status update
            new Thread(new Runnable(){

                @Override
                public void run() {
                    while(true){
                        try {
                            Thread.sleep(updateIntervalMillis);
                            //switch status between true and false
                            setResourceStatus(!getResourceStatus());
                            log.debug("New resource status: " + (getResourceStatus() ? "testpayload1" : "testpayload2"));
                        } catch (InterruptedException e) {
                            log.error("This should never happen.", e);
                        }
                    }
                }
            }).start();
        }
    }


    @Override
    public CoapResponse processMessage(CoapRequest request) {
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
}
