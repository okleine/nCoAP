package de.uzl.itm.ncoap.communication.events;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;

import java.net.InetSocketAddress;

/**
 * Created by olli on 07.09.15.
 */
public class PartialContentReceivedEvent extends AbstractMessageExchangeEvent {

    private long block2Number;
    private final long blockSize;

    public PartialContentReceivedEvent(InetSocketAddress remoteEndpoint, Token token, long block2Number,
            long blockSize) {

        super(remoteEndpoint, token);
        this.block2Number = block2Number;
        this.blockSize = blockSize;
    }

    public long getBlock2Number() {
        return block2Number;
    }


    public long getBlockSize() {
        return blockSize;
    }

    public interface Handler {
        public void handleEvent(PartialContentReceivedEvent event);
    }
}
