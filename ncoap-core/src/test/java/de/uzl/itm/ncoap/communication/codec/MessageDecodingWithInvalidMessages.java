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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;



/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 09.11.13
* Time: 16:28
* To change this template use File | Settings | File Templates.
*/
@RunWith(Parameterized.class)
public class MessageDecodingWithInvalidMessages extends AbstractCoapTest{

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                //[0] TKL is 1, but 0 remaining bytes after header
                new Object[]{new byte[]{(byte) (0b01000001 & 0b11111111), 0, 0, 0},
                        HeaderDecodingException.class, "TKL value is 1 but only 0 bytes left!"},

                //[1] TKL is 8, but only 6 remaining bytes after header
                new Object[]{new byte[]{(byte) (0b01001000 & 0b11111111), 1, 1, 0, 1, 1, 1, 1, 1, 1},
                        HeaderDecodingException.class, "TKL value is 8 but only 6 bytes left!"}
        );
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uzl.itm.ncoap.communication.codec").setLevel(Level.DEBUG);
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private ChannelBuffer encodedMessageBuffer;


    public MessageDecodingWithInvalidMessages(byte[] encodedMessage, Class<Exception> expectedExceptionClass,
                                              String expectedMessage) {
        this.encodedMessageBuffer = ChannelBuffers.wrappedBuffer(encodedMessage);
        exception.expect(expectedExceptionClass);
        exception.expectMessage(expectedMessage);
    }


    @Test
    public void testDecoding() throws Exception {
        new CoapTestDecoder().decode(encodedMessageBuffer);
    }
}

