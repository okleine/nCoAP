package de.uzl.itm.ncoap.communication.blockwise;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1OptionHandler;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock2OptionHandler;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.assertEquals;


/**
 * Created by olli on 07.04.16.
 */
public class ClientSendsRequestWithBlock1AndBlock2 extends AbstractCoapCommunicationTest {

    private static CoapClient coapClient;
    private static CoapRequest coapRequest;
    private static TestCallback clientCallback;

    @Override
    public void setupComponents() throws Exception {
        coapClient = new CoapClient("Coap Client (BLOCK 1)");
        URI targetURI = new URI("coap://vs0.inf.ethz.ch:5683/large-post");
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, targetURI);
        coapRequest.setPreferedBlock1Size(BlockSize.SIZE_16);
        coapRequest.setPreferedBlock2Size(BlockSize.SIZE_16);
        byte[] payload = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(CoapMessage.CHARSET);
        coapRequest.setContent(payload, ContentFormat.TEXT_PLAIN_UTF8);
        clientCallback = new TestCallback();
    }

    @Override
    public void createTestScenario() throws Exception {
        coapClient.sendCoapRequest(coapRequest, new InetSocketAddress("vs0.inf.ethz.ch", 5683), clientCallback);
        Thread.sleep(5000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapClient.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(ClientBlock1OptionHandler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ClientBlock2OptionHandler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(TestCallback.class.getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Client did not receive 1 response";
        assertEquals(message, 1, clientCallback.getCoapResponses().size());
    }
}
