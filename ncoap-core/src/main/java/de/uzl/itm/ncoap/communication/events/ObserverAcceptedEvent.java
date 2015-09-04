package de.uzl.itm.ncoap.communication.events;

import de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Created by olli on 04.09.15.
 */
public class ObserverAcceptedEvent extends AbstractMessageTransferEvent{

    private final ObservableWebresource webresource;
    private final long contentFormat;

    /**
     * Creates a new instance of {@link de.uzl.itm.ncoap.communication.events.AbstractMessageTransferEvent}
     *
     * @param remoteEndpoint the remote endpoint of the
     *                       {@link de.uzl.itm.ncoap.communication.reliability.MessageTransfer} that caused this
     *                       event
     * @param messageID      the message ID of the {@link de.uzl.itm.ncoap.communication.reliability.MessageTransfer}
     *                       that caused this event
     * @param token          the {@link de.uzl.itm.ncoap.communication.dispatching.client.Token} of the
     *                       {@link de.uzl.itm.ncoap.communication.reliability.MessageTransfer} that caused this event
     */
    public ObserverAcceptedEvent(InetSocketAddress remoteEndpoint, int messageID, Token token,
            ObservableWebresource webresource, long contentFormat) {

        super(remoteEndpoint, messageID, token);
        this.webresource = webresource;
        this.contentFormat = contentFormat;
    }

    @Override
    public boolean stopsMessageExchange() {
        return false;
    }

    public ObservableWebresource getWebresource() {
        return webresource;
    }

    public long getContentFormat() {
        return contentFormat;
    }

    public interface Handler {
        public void handleObserverAcceptedEvent(ObserverAcceptedEvent event);
    }
}
