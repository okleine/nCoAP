package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * This observable resource changes its status periodically with a delay given as argument for the constructor.
 * There are 5 possible states. The internal state representation, i.e. the returned value v of
 * {@link #getResourceStatus()} is 0, 1, 2, 3 or 4. The payload contained in a the {@link CoapResponse}
 * returned by {@link #processMessage(CoapRequest, InetSocketAddress)} is "Status 1",
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
        super(path, initialStatus);
        this.artificalDelay = artificalDelay;
        updateInterval = NO_AUTOMATIC_UPDATE;
    }

    /**
     * Schedule automatic status changes every according to the update interval. If there is no update interval
     * defined this method returns <code>false</code>.
     *
     * @return <code>true</code> if automatic updates where scheduled succesfully, <code>false</code> otherwise.
     */
    public boolean scheduleAutomaticStatusChange(){
        if(updateInterval != NO_AUTOMATIC_UPDATE){
            getExecutorService().schedule(new Runnable(){

                @Override
                public void run() {
                    setResourceStatus((getResourceStatus() + 1) % 5);
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
    public CoapResponse processMessage(CoapRequest coapRequest, InetSocketAddress remoteAddress) {
         log.debug("Incoming from {}: {}.", coapRequest, remoteAddress);

        //Simulate a longer processing time using the given delay
        try {
            Thread.sleep(artificalDelay);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //Create response 
        CoapResponse response = new CoapResponse(Code.CONTENT_205);
        try {
            String payload = "Status #" + (getResourceStatus() + 1);
            response.setPayload(ChannelBuffers.wrappedBuffer(payload.getBytes(Charset.forName("UTF-8"))));
        }
        catch (MessageDoesNotAllowPayloadException e) {
            fail(e.getMessage());
        }

        return response;
    }
}
