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
import de.uzl.itm.ncoap.communication.codec.tools.CoapTestDecoder;
import de.uzl.itm.ncoap.message.CoapMessage;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;


/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 09.11.13
* Time: 16:28
* To change this template use File | Settings | File Templates.
*/
@RunWith(Parameterized.class)
public class MessageDecodingWithValidMessages extends AbstractCoapTest {

    @Parameterized.Parameters(name = "Test: {1}")
    public static Collection<Object[]> data() throws Exception {

        initializeLogging();

        return Lists.newArrayList(
                new Object[]{new byte[]{(byte) (0x60 & 0xFF), 0, (byte) (0xFF & 0xFF), (byte) (0xFF & 0xFF)},
                        CoapMessage.createEmptyAcknowledgement(65535)},

                new Object[]{new byte[]{(byte) (0x70 & 0xFF), 0, 0, 0},
                        CoapMessage.createEmptyReset(0)}
        );
    }


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uzl.itm.ncoap.communication.codec").setLevel(Level.DEBUG);
        Logger.getRootLogger().setLevel(Level.ERROR);
    }


    private ChannelBuffer encodedMessageBuffer;
    private CoapMessage expected;
    private CoapMessage actual;


    public MessageDecodingWithValidMessages(byte[] encodedMessage, CoapMessage expected) {
        this.expected = expected;
        this.encodedMessageBuffer = ChannelBuffers.wrappedBuffer(encodedMessage);
    }


    @Before
    public void test() throws Exception {
        actual = (CoapMessage) new CoapTestDecoder().decode(encodedMessageBuffer);
    }


    @Test
    public void testProtocolVersion() {
        assertEquals(expected.getProtocolVersion(), actual.getProtocolVersion());
    }

    @Test
    public void testMessageTyoe() {
        assertEquals(expected.getMessageType(), actual.getMessageType());
    }

    @Test
    public void testMessageCode() {
        assertEquals(expected.getMessageCode(), actual.getMessageCode());
    }

    @Test
    public void testMessageID() {
        assertEquals(expected.getMessageID(), actual.getMessageID());
    }

}
