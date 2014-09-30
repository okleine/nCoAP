package de.uniluebeck.itm.ncoap.communication.events;

import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Created by olli on 24.09.14.
 */
public class MessageIDAssignedEvent extends MessageExchangeEvent {

    public MessageIDAssignedEvent(InetSocketAddress remoteEndpoint, int messageID, Token token) {
        super(remoteEndpoint, messageID, token);
    }


    @Override
    public boolean stopConversation() {
        return false;
    }
}
