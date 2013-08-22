package de.uniluebeck.itm.ncoap.communication.observe;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;

/**
 * Interface to be implemented by instances of {@link CoapResponseProcessor} to get informed if an running
 * observation timed out because of a max-age expiry. Observable CoAP resources are supposed to send a new
 * update notification when either it's status changed or max-age of the previous update notification is about
 * to exceed.
 *
 * @author Oliver Kleine
 */
public interface ObservationTimeoutProcessor {

    /**
     * This method is automatically invoked by the nCoap framework if an observerd resource did
     * not send a follow-up update notification after max-age expiry of the previous update notification
     *
     * @param continueObservationFuture a {@link SettableFuture} to indicate if the observation should be
     *                                  restarted automatically or not. Implementations of this interface, i.e. this
     *                                  method, must set the future with a proper {@link CoapRequest} to
     *                                  restart the observation. If the observation is not supposed to continue, resp.
     *                                  to be restarted, the future must be set with <code>null</code>.
     */
    public void  processObservationTimeout(SettableFuture<CoapRequest> continueObservationFuture);

}
