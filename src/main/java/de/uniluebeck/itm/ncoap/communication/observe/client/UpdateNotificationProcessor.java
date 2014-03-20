
package de.uniluebeck.itm.ncoap.communication.observe.client;

import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageType;

/**
 * An {@link UpdateNotificationProcessor} is a {@link CoapResponseProcessor} for observations, i.e. there is more
 * than one {@link CoapResponse}, i.e. update notification expected.
 *
 * @author Oliver Kleine
 */
public interface UpdateNotificationProcessor extends CoapResponseProcessor {

    /**
     * This method is called by the framework for each update notification that was received while an observation
     * is running. If the implementing instance returns <code>false</code> the observation is canceled via
     * {@link MessageType.Name#RST} message.
     *
     * @return <code>true</code> if the observation is to be continued or <code>false</code> otherwise
     */
    public boolean continueObservation();

}
