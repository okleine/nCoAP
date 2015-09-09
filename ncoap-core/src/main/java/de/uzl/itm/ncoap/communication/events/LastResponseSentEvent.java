package de.uzl.itm.ncoap.communication.events;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Created by olli on 08.09.15.
 */
public class LastResponseSentEvent extends AbstractMessageExchangeEvent {

    public LastResponseSentEvent(InetSocketAddress remoteEndpoint, Token token) {
        super(remoteEndpoint, token);
    }

    public interface Handler {
        public void handleEvent(LastResponseSentEvent event);
    }
}
