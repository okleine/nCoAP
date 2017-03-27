/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uzl.itm.ncoap.communication.codec;

import com.google.common.collect.Lists;
import de.uzl.itm.ncoap.AbstractCoapTest;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.codec.tools.CoapTestDecoder;
import de.uzl.itm.ncoap.communication.codec.tools.CoapTestEncoder;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageType;
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
public class DecodeEncodedMessageTest extends AbstractCoapTest {

    @Parameterized.Parameters(name = "Test {index}: {0}")
    public static Collection<Object[]> data() throws Exception {

        initializeLogging();

        return Lists.newArrayList(
                //[0] TKL is 1, but 0 remaining bytes after header
                new Object[]{new CoapRequest(MessageType.CON, MessageCode.GET,
                        new URI("coap://coap.me:5683/test"))},

                //[1] TKL is 8, but only 6 remaining bytes after header
                new Object[]{new CoapRequest(MessageType.NON, MessageCode.POST,
                        new URI("coap://coap.me:5683/p1/p2/p3/p4/p5/p6/p7"))},

                new Object[]{new CoapRequest(MessageType.NON, MessageCode.GET,
                        new URI("coap://example.org/"), true)}
        );
    }

    private CoapMessage coapMessage;
    private ChannelBuffer encodedMessage;

    public DecodeEncodedMessageTest(CoapMessage coapMessage) throws Exception {
        coapMessage.setMessageID(1234);
        coapMessage.setToken(new Token(new byte[]{1,2,3,4}));

        if (coapMessage.getMessageCode() == MessageCode.POST) {
            String payload = "Some arbitrary payload";
            coapMessage.setContent(payload.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        }

        this.coapMessage = coapMessage;
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uzl.itm.ncoap.communication.codec").setLevel(Level.DEBUG);
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    @Before
    public void encodeMessage() throws Exception {
        encodedMessage = new CoapTestEncoder().encode(coapMessage);
    }

    @Test
    public void testDecoding() throws Exception {
        CoapMessage decodedMessage = (CoapMessage) new CoapTestDecoder().decode(encodedMessage);
        assertEquals(coapMessage, decodedMessage);
    }


}
