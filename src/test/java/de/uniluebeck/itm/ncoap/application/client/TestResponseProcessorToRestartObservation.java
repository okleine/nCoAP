package de.uniluebeck.itm.ncoap.application.client;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 22.08.13
 * Time: 17:00
 * To change this template use File | Settings | File Templates.
 */
public class TestResponseProcessorToRestartObservation extends TestResponseProcessor {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private CoapRequest coapRequestToRestartObservation;

    public TestResponseProcessorToRestartObservation(CoapRequest coapRequestToRestartObservation){
        this.coapRequestToRestartObservation = coapRequestToRestartObservation;
    }

    @Override
    public void processObservationTimeout(SettableFuture<CoapRequest> continueObservation) {
        log.info("Observation timed out! Restart");
        resetRetransmissionCounter();
        setObservationTimedOut(true);
        continueObservation.set(coapRequestToRestartObservation);
    }
}
