package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.*;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * This observable resource changes its status periodically with a delay given as argument for the constructor.
 * There are 5 possible states. The internal state representation, i.e. the returned value v of
 * {@link #getResourceStatus()} is 1, 2, 3, 4 or 5. The payload contained in a the {@link CoapResponse}
 * produced by {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} is "Status 1",
 * "Status 2", ..., "Status 5".
 */
public class ObservableTestWebService extends ObservableWebService<Integer>{

    private static Logger log = LoggerFactory.getLogger(ObservableTestWebService.class.getName());

    private long artificalDelay;
    private long updateInterval;

    public static int NO_AUTOMATIC_UPDATE = -1;

    /**
     * @param path the absolute path of the service
     * @param initialStatus the initial internal status
     * @param artificalDelay the artificial delay in milliseconds to simulate a longer processing time for incoming
     *                       requests
     * @param updateInterval the time passing between two status updates
     */
    public ObservableTestWebService(String path, int initialStatus, long artificalDelay, final long updateInterval){
        super(path, initialStatus);
        this.artificalDelay = artificalDelay;
        this.updateInterval = updateInterval;
    }

    /**
     * @param path the absolute path of the service
     * @param initialStatus the initial internal status
     * @param artificalDelay the artificial delay in milliseconds to simulate a longer processing time for incoming
     *                       requests
     */
    public ObservableTestWebService(String path, int initialStatus, long artificalDelay){
        this(path, initialStatus, artificalDelay, NO_AUTOMATIC_UPDATE);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService executorService){
        super.setScheduledExecutorService(executorService);
        scheduleAutomaticStatusChange();
    }

    /**
     * Schedule automatic status changes every according to the update interval. If there is no update interval
     * defined this method returns <code>false</code>.
     *
     * @return <code>true</code> if automatic updates where scheduled succesfully, <code>false</code> otherwise.
     */
    private boolean scheduleAutomaticStatusChange(){
        if(updateInterval != NO_AUTOMATIC_UPDATE){
            getScheduledExecutorService().schedule(new Runnable(){

                @Override
                public void run() {
                    int newStatus = (getResourceStatus() + 1) % 6;
                    if(newStatus == 0)
                        newStatus = 1;
                    setResourceStatus(newStatus);
                    scheduleAutomaticStatusChange();
                }
            }, updateInterval, TimeUnit.MILLISECONDS);

            return true;
        }
        else{
            log.error("No update interval defined for service " + getPath() + ". Could not schedule automatic" +
                    "updates");
            return false;
        }
    }

    @Override
    public void shutdown() {
        //Nothing to do here...
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) {

        log.debug("Incoming from {}: {}.", coapRequest, remoteAddress);

        //Simulate a longer processing time using the given delay
        try {
            Thread.sleep(artificalDelay);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //Create response 
        CoapResponse response = new CoapResponse(Code.CONTENT_205);

        byte[] payload = null;
        if(coapRequest.getAcceptedMediaTypes().isEmpty()){
            try {
                payload = getSerializedResourceStatus(MediaType.TEXT_PLAIN_UTF8);
            }
            catch (MediaTypeNotSupportedException e) {
                log.error("Unsupported media type {}.", e.getMediaType());
            }
        }
        else{
            for(MediaType mediaType : coapRequest.getAcceptedMediaTypes()){
                try{
                    payload = getSerializedResourceStatus(mediaType);
                }
                catch (MediaTypeNotSupportedException e) {
                    log.debug("Unsupported media type {}.", e.getMediaType());
                }
                if(payload != null)
                    break;
            }
        }

        try {
            response.setPayload(ChannelBuffers.wrappedBuffer(payload));
        }
        catch (MessageDoesNotAllowPayloadException e) {
            fail(e.getMessage());
        }

        responseFuture.set(response);
    }

    @Override
    public byte[] getSerializedResourceStatus(OptionRegistry.MediaType mediaType) throws MediaTypeNotSupportedException{
        return ("Status #" + getResourceStatus()).getBytes(Charset.forName("UTF-8"));
    }
}
