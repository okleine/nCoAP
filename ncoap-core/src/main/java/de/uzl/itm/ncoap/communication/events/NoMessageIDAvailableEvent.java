package de.uzl.itm.ncoap.communication.events;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Created by olli on 11.09.15.
 */
public class NoMessageIDAvailableEvent extends AbstractMessageExchangeEvent {

    public NoMessageIDAvailableEvent(InetSocketAddress remoteEndpoint, Token token) {
        super(remoteEndpoint, token);
    }

    public interface Handler {
        public void handleEvent(NoMessageIDAvailableEvent event);
    }
}
