
package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;

/**
 *
 *
 */
public interface CoapUpdateNotificationProcessor extends CoapResponseProcessor {

    public boolean continueObservation();
}
