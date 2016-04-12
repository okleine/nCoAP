package de.uzl.itm.ncoap.communication.events.client;

import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.AbstractMessageExchangeEvent;

import java.net.InetSocketAddress;

/**
 * Created by olli on 12.04.16.
 */
public class BlockwiseResponseTransferFailedEvent extends AbstractMessageExchangeEvent {

    public BlockwiseResponseTransferFailedEvent(InetSocketAddress remoteEndpoint, Token token) {
        super(remoteEndpoint, token);
    }

    public interface Handler {
        void handleEvent(BlockwiseResponseTransferFailedEvent event);
    }
}
