package de.uniluebeck.itm.ncoap.communication.codec;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import de.uniluebeck.itm.ncoap.communication.codec.tools.CoapTestDecoder;
import de.uniluebeck.itm.ncoap.communication.codec.tools.CoapTestEncoder;
import de.uniluebeck.itm.ncoap.message.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 12.11.13
 * Time: 23:53
 * To change this template use File | Settings | File Templates.
 */

@RunWith(Parameterized.class)
public class DecodeEncodedMessageTest extends AbstractCoapTest{

    @Parameterized.Parameters(name = "{index} test: {0}")
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                //[0] TKL is 1, but 0 remaining bytes after header
                new Object[]{new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, new URI("coap://coap.me:5683/separate"))},

                //[1] TKL is 8, but only 6 remaining bytes after header
                new Object[]{new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, new URI("coap://coap.me:5683/separate"))}
        );
    }

    private CoapMessage coapMessage;
    private ChannelBuffer encodedMessage;

    public DecodeEncodedMessageTest(CoapMessage coapMessage) throws Exception {
        coapMessage.setMessageID(1234);
        this.coapMessage = coapMessage;

    }

    @Before
    public void encodeMessage() throws Exception {
        encodedMessage = (ChannelBuffer) new CoapTestEncoder().encode(coapMessage);
    }

    @Test
    public void testDecoding() throws Exception {
        CoapMessage decodedMessage = (CoapMessage) new CoapTestDecoder().decode(encodedMessage);
        assertEquals(coapMessage, decodedMessage);
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }
}
