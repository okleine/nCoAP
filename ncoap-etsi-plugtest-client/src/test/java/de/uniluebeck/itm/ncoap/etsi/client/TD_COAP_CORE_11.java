package de.uniluebeck.itm.ncoap.etsi.client;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.communication.observe.client.UpdateNotificationProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.*;

/**
 * Created by olli on 15.09.14.
 */
public class TD_COAP_CORE_11 {

    private static final String SERVER = "coap.me";
    private static final int WAITING_TIME = 8000;

    private static CoapRequest coapRequest;
    private static CoapMessage emptyAck;
    private static CoapResponse coapResponse;


    @BeforeClass
    public static void sendRequest() throws Exception{
        LoggingConfiguration.configure();

        CoapClientApplication client = ApplicationFactory.getCoapClientApplication();

        //This is a dirty hack to force the client to use a non-empty token for the second request
        wasteEmptyToken(client);
        Thread.sleep(2);
        //---------------------

        URI targetUri = new URI("coap", null, SERVER, -1, "/separate", null, null);
        final InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(SERVER), 5683);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        client.sendCoapRequest(coapRequest, new EmptyAcknowledgementProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                TD_COAP_CORE_11.coapResponse = coapResponse;
                System.out.println("Response (from " + targetAddress + "): " + coapResponse);
            }

            @Override
            public void processEmptyAcknowledgement(InetSocketAddress remoteEndpoint, int messageID, Token token) {
                TD_COAP_CORE_11.emptyAck = CoapMessage.createEmptyAcknowledgement(messageID);
                System.out.println("Empty ACK (from " + targetAddress + "): " + emptyAck);
            }

        }, targetAddress);

        Thread.sleep(WAITING_TIME);
    }


    private static void wasteEmptyToken(CoapClientApplication client) throws Exception{
        URI fakeURI = new URI("coap", null, SERVER, -1, "/does/probably/not/exist", null, null);
        final InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(fakeURI.getHost()), 5683);
        CoapRequest fakeRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, fakeURI);

        client.sendCoapRequest(fakeRequest, new CoapResponseProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                //Nothing to do...
            }
        }, targetAddress);
    }


    @Test
    public void testResponseCode() throws Exception {
        assertEquals("WRONG RESPONSE CODE!", MessageCode.Name.CONTENT_205, coapResponse.getMessageCodeName());
    }

    @Test
    public void testEmptyAckMessageID(){
        assertTrue("NO EMPTY ACK RECEIVED!", emptyAck != null);
        assertEquals("WRONG MESSAGE ID IN EMPTY ACK!", coapRequest.getMessageID(), emptyAck.getMessageID());
    }

    @Test
    public void testTokenLength(){
        assertTrue("TOKEN MUST NOT BE EMPTY!", coapResponse.getToken().getBytes().length > 0);
    }

    @Test
    public void testToken() throws Exception {
        assertEquals("WRONG TOKEN IN RESPONSE!", coapRequest.getToken(), coapResponse.getToken());
    }

    @Test
    public void testPayload() throws Exception {
        if(coapResponse.getContent().readableBytes() < 1){
            fail("Response did not contain any payload!");
        }
    }

    @Test
    public void testContentFormatOption() throws Exception {
        String message = "NO CONTENT FORMAT OPTION IN RESPONSE!";
        assertNotEquals(message, coapResponse.getContentFormat(), ContentFormat.UNDEFINED);
    }
}
