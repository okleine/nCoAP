package de.uniluebeck.itm.ncoap.etsi.client;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import static de.uniluebeck.itm.ncoap.message.MessageCode.Name.CHANGED_204;
import static de.uniluebeck.itm.ncoap.message.MessageCode.Name.CREATED_201;
import static org.junit.Assert.*;

/**
 * Created by olli on 15.09.14.
 */
public class TD_COAP_CORE_04 {

    private static Logger log = Logger.getLogger(TD_COAP_CORE_04.class.getName());

    private static final String SERVER = "coap.me";
    private static final int WAITING_TIME = 2500;

    private static CoapRequest coapRequest;
    private static CoapResponse coapResponse;


    @BeforeClass
    public static void sendRequest() throws Exception{
        LoggingConfiguration.configure();

        CoapClientApplication client = ApplicationFactory.getCoapClientApplication();
        URI targetUri = new URI("coap", null, SERVER, -1, "/test", null, null);
        final InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(SERVER), 5683);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.POST, targetUri);
        coapRequest.setContent("Arbitrary payload...".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        client.sendCoapRequest(coapRequest, new CoapResponseProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                TD_COAP_CORE_04.coapResponse = coapResponse;
                System.out.println("Response (from " + targetAddress + "): " + coapResponse);
            }
        }, targetAddress);

        Thread.sleep(WAITING_TIME);
    }


    @Test
    public void testResponseCode() throws Exception {
        MessageCode.Name actual = coapResponse.getMessageCodeName();
        assertTrue(
                "WRONG RESPONSE CODE (expected:" + CHANGED_204 + " or " + CREATED_201 + ", actual: " + actual + ")",
                actual == CHANGED_204 || actual == CREATED_201
        );
    }

    @Test
    public void testMessageID() throws Exception {
        assertEquals("WRONG MESSAGE ID IN RESPONSE!", coapRequest.getMessageID(), coapResponse.getMessageID());
    }

    @Test
    public void testToken() throws Exception {
        assertEquals("WRONG TOKEN IN RESPONSE!", coapRequest.getToken(), coapResponse.getToken());
    }

    @Test
    public void testPayloadAndContentFormatOption() throws Exception {
        if(coapResponse.getContent().readableBytes() > 0 && coapResponse.getContentFormat() == ContentFormat.UNDEFINED){
            fail("Response contained payload but no content format option!");
        }
    }
}
