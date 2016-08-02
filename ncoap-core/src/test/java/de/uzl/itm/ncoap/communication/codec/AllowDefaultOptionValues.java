package de.uzl.itm.ncoap.communication.codec;

import de.uzl.itm.ncoap.AbstractCoapTest;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.OptionValue;
import de.uzl.itm.ncoap.message.options.StringOptionValue;

import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URI;

/**
 * Created by olli on 02.08.16.
 */

public class AllowDefaultOptionValues extends AbstractCoapTest {

    private static String HOST = "10.20.30.40";
    private static byte[] HOST_ENCODED = HOST.getBytes(CoapMessage.CHARSET);

    @Override
    public void setupLogging() throws Exception {

    }

    @Test
    public void setIpAddressAsHostOption() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        coapRequest.addOption(Option.URI_HOST, new StringOptionValue(Option.URI_HOST, HOST_ENCODED, true));

        assertEquals("Unexpected value for HOST option: ", HOST, coapRequest.getUriHost());
    }

    @Test (expected = IllegalArgumentException.class)
    public void setIpAddressAsHostOption2() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        coapRequest.addOption(Option.URI_HOST, new StringOptionValue(Option.URI_HOST, HOST_ENCODED));

        // this should not be executed as the previous statement is supposed to throw an exception
        assertTrue(false);
    }

    @Test
    public void setDefaultPortAsPortOption() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        UintOptionValue value = new UintOptionValue(Option.URI_PORT, OptionValue.ENCODED_URI_PORT_DEFAULT, true);
        coapRequest.addOption(Option.URI_PORT, value);

        assertEquals("Unexpected value for PORT option: ", OptionValue.URI_PORT_DEFAULT, coapRequest.getUriPort());
    }

    @Test (expected = IllegalArgumentException.class)
    public void setDefaultPortAsPortOption2() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        coapRequest.addOption(Option.URI_PORT, new UintOptionValue(Option.URI_PORT,
                OptionValue.ENCODED_URI_PORT_DEFAULT, false));

        // this should not be executed as the previous statement is supposed to throw an exception
        assertTrue(false);
    }
}
