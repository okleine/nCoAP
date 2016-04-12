package de.uzl.itm.ncoap.communication.blockwise;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1Handler;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock2Handler;
import de.uzl.itm.ncoap.communication.blockwise.server.ServerBlock1Handler;
import de.uzl.itm.ncoap.communication.blockwise.server.ServerBlock2Handler;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
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
public class ClientSends100PostRequestWithBlock1AndBlock2 extends AbstractCoapCommunicationTest {

    private static CoapServer coapServer;
    private static NotObservableTestWebresourceForPost webresource;
    private static CoapClient coapClient;
    private static CoapRequest[] coapRequests = new CoapRequest[100];
    private static TestCallback[] clientCallbacks = new TestCallback[100];

    @Override
    public void setupComponents() throws Exception {
        // setup server
        InetSocketAddress serverSocket = new InetSocketAddress(CoapServer.DEFAULT_COAP_SERVER_PORT);
        coapServer = new CoapServer(NotFoundHandler.getDefault(), serverSocket, BlockSize.SIZE_16, BlockSize.SIZE_128);
        webresource = new NotObservableTestWebresourceForPost("/test", "", 0, coapServer.getExecutor());
        coapServer.registerWebresource(webresource);

        // setup client
        coapClient = new CoapClient("CoAP Client");

        // setup request
        URI targetURI = new URI("coap://localhost:5683/test");

        for(int i = 0; i < 100; i++) {
            coapRequests[i] = new CoapRequest(MessageType.CON, MessageCode.POST, targetURI);
            coapRequests[i].setPreferedBlock1Size(BlockSize.SIZE_64);
            coapRequests[i].setPreferedBlock2Size(BlockSize.SIZE_32);
            byte[] payload = ("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz").getBytes(CoapMessage.CHARSET);
            coapRequests[i].setContent(payload, ContentFormat.TEXT_PLAIN_UTF8);

            // setup callback
            clientCallbacks[i] = new TestCallback();
        }
    }

    @Override
    public void createTestScenario() throws Exception {
        for(int i = 0; i < 100; i++) {
            coapClient.sendCoapRequest(coapRequests[i], new InetSocketAddress("localhost", 5683), clientCallbacks[i]);
        }
        Thread.sleep(5000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapClient.shutdown();
        coapServer.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(ClientBlock1Handler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ClientBlock2Handler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ServerBlock1Handler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ServerBlock2Handler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(TestCallback.class.getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void allCallbacksReceivedOneResponse() {
        for(int i = 0; i < 100; i++) {
            String message = "Client " + i + " did not receive 1 response";
            assertEquals(message, 1, clientCallbacks[i].getCoapResponses().size());
        }
    }
}
