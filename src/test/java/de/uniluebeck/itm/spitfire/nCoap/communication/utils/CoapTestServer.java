package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class CoapTestServer extends CoapServerApplication {

    private HashBasedTable<InetSocketAddress, ByteArrayWrapper, Long> timeoutMessages = HashBasedTable.create();

    public CoapTestServer(int serverPort){
        super(serverPort);
    }

    @Override
    public void handleRetransmissionTimeout(RetransmissionTimeoutMessage timeoutMessage) {
        timeoutMessages.put(timeoutMessage.getRemoteAddress(), new ByteArrayWrapper(timeoutMessage.getToken()),
                System.currentTimeMillis());
    }
}
