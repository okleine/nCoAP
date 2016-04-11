package de.uzl.itm.ncoap.communication.blockwise;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1Handler;
import de.uzl.itm.ncoap.communication.blockwise.server.ServerBlock1Handler;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.RequestDispatcher;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.endpoints.server.NotObservableTestWebresourceForPost;
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
public class ClientSendsPostRequestWithBlock1 extends AbstractCoapCommunicationTest {

    private static CoapServer coapServer;
    private static NotObservableTestWebresourceForPost webresource;
    private static CoapClient coapClient;
    private static CoapRequest coapRequest;
    private static TestCallback clientCallback;

    @Override
    public void setupComponents() throws Exception {

        InetSocketAddress serverSocket = new InetSocketAddress(CoapServer.DEFAULT_COAP_SERVER_PORT);
        coapServer = new CoapServer(NotFoundHandler.getDefault(), serverSocket, BlockSize.SIZE_16);
        webresource = new NotObservableTestWebresourceForPost("/test", "XXX", 0, coapServer.getExecutor());
        coapServer.registerWebresource(webresource);

        coapClient = new CoapClient("Coap Client (BLOCK 1)");
        URI targetURI = new URI("coap://localhost:5683/test");
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, targetURI);
        coapRequest.setPreferedBlock1Size(BlockSize.SIZE_32);
        byte[] payload = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(CoapMessage.CHARSET);
        coapRequest.setContent(payload, ContentFormat.TEXT_PLAIN_UTF8);

        clientCallback = new TestCallback();
    }

    @Override
    public void createTestScenario() throws Exception {
        coapClient.sendCoapRequest(coapRequest, new InetSocketAddress("localhost", 5683), clientCallback);
        Thread.sleep(10000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapClient.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(ClientBlock1Handler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ServerBlock1Handler.class.getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Client did not receive 1 response";
        assertEquals(message, 1, clientCallback.getCoapResponses().size());
    }
}
