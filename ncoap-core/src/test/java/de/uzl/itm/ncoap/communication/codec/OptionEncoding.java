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
import de.uzl.itm.ncoap.communication.codec.tools.CoapTestEncoder;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.OptionValue;
import de.uzl.itm.ncoap.message.options.StringOptionValue;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
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
 * Time: 21:07
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Parameterized.class)
public class OptionEncoding extends AbstractCoapTest {


    @Parameterized.Parameters(name = "Test: [Prev. No. {0}, Option No. {1}, value: {2}]")
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                new Object[]{0, Option.URI_HOST, "www.example.org"},
                new Object[]{0, Option.URI_HOST, "WWW.EXAMPLE.ORG"},
                new Object[]{0, Option.URI_HOST, "WwW.eXaMpLe.OrG"},
                new Object[]{0, Option.URI_HOST, "WwW.this-is-a-long-uri-host-part.OrG"},

                //Short Proxy URI
                new Object[]{0, Option.PROXY_URI, "coap://www.example.org/path/to/service"},

                //Very long Proxy URI
                new Object[]{30, Option.PROXY_URI, "coap://www.example.org/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service/this/is/a/very/long/path/to/service/" +
                        "this/is/a/very/long/path/to/service"}


        );
    }

    private Logger log = Logger.getLogger(this.getClass().getName());

    private CoapTestEncoder coapTestEncoder;

    private int previousOptionNumber;
    private int optionNumber;
    private OptionValue optionValue;

    private ChannelBuffer encodedOption;


    private OptionEncoding(int previousOptionNumber, int optionNumber, OptionValue optionValue) {
        this.coapTestEncoder = new CoapTestEncoder();
        this.previousOptionNumber = previousOptionNumber;
        this.optionNumber = optionNumber;
        this.optionValue = optionValue;
    }


    public OptionEncoding(int previousOptionNumber, int optionNumber, String value) throws Exception{
        this(previousOptionNumber, optionNumber, new StringOptionValue(optionNumber, value));
    }


    @Before
    public void encodeOption() throws Exception {
        log.info("Start Tests with Option " + optionValue);
        encodedOption = ChannelBuffers.dynamicBuffer();
        coapTestEncoder.encodeOption(encodedOption, optionNumber, optionValue, previousOptionNumber);
    }

    @Test
    public void testDeltaPartOfFirstByte() {
        int expectedDelta = this.optionNumber - this.previousOptionNumber;

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(encodedOption);

        int firstByte = buffer.readByte() & 0xFF;

        if (expectedDelta < 13) {
            assertEquals("Delta part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(expectedDelta), 4),
                    getZeroPaddedBinaryString(Integer.toBinaryString(firstByte >>> 4), 4)
            );
        }

        if (expectedDelta >= 13 && expectedDelta < 269) {
            assertEquals("Delta part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(13), 4),
                    getZeroPaddedBinaryString(Integer.toBinaryString(firstByte >>> 4), 4)
            );
        }

        if (expectedDelta >= 269 && expectedDelta < 65804) {
            assertEquals("Delta part of first byte does is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(14), 4),
                    getZeroPaddedBinaryString(Integer.toBinaryString(firstByte >>> 4), 4)
            );
        }
    }

    @Test
    public void testLengthPartOfFirstByte() {
        int expectedLength = this.optionValue.getValue().length;

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(encodedOption);

        int firstByte = buffer.readByte() & 0xFF;

        if (expectedLength < 13) {
            assertEquals("Length part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(expectedLength), 4),
                    getZeroPaddedBinaryString(Integer.toBinaryString(firstByte & 0x0F), 4)
            );

            assertEquals("Encoded length and readable bytes do not match, ",
                    (firstByte & 0x0F) + 269, buffer.readableBytes());
        }

        if (expectedLength >= 13 && expectedLength < 269) {
            assertEquals("Length part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(13), 4),
                    getZeroPaddedBinaryString(Integer.toBinaryString(firstByte & 0x0F), 4)
            );
        }

        if (expectedLength >= 269 && expectedLength < 65804) {
            assertEquals("Length part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(14), 4),
                    getZeroPaddedBinaryString(Integer.toBinaryString(firstByte & 0x0F), 4)
            );
        }
    }

    @Test
    public void testExtendedDelta() {

        int expectedDelta = this.optionNumber - this.previousOptionNumber;

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(encodedOption);
        buffer.readByte();

        if (expectedDelta >= 13 && expectedDelta < 269) {
            int extendedDelta = (buffer.readByte() & 0xFF);
            assertEquals("Extended delta part is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(expectedDelta - 13), 8),
                    getZeroPaddedBinaryString(Integer.toBinaryString(extendedDelta), 8));
        }

        if (expectedDelta >= 269 && expectedDelta < 65804) {
            int extendedDelta = ((buffer.readByte() & 0xFF) << 8 | (buffer.readByte() & 0xFF));
            assertEquals("Length part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(expectedDelta - 269), 16),
                    getZeroPaddedBinaryString(Integer.toBinaryString(extendedDelta), 16)
            );
        }
    }

    @Test
    public void testExtendedLength() {

        int expectedLength = optionValue.getValue().length;

        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(encodedOption);
        int firstByte = buffer.readByte();
        int deltaPart = (firstByte & 0xFF) >>> 4 ;

        //read and ignore next byte (extended option delta)
        if (deltaPart == 13) {
            log.debug("Skip next byte (extended delta)");
            buffer.readByte();
        }

        //read and ignore next 2 bytes (extended option delta)
        if (deltaPart == 14) {
            log.debug("Skip next 2 bytes (extended delta)");
            buffer.readBytes(new byte[2]);
        }

        if (expectedLength >= 13 && expectedLength < 269) {
            log.info("Expected extended length value: " + (expectedLength - 13));
            int extendedLength = (buffer.readByte() & 0xFF);

            assertEquals("Extended Length part is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(expectedLength - 13), 8),
                    getZeroPaddedBinaryString(Integer.toBinaryString(extendedLength), 8));

            assertEquals("Encoded length and readable bytes do not match, ",
                    extendedLength + 13, buffer.readableBytes());
        }

        if (expectedLength >= 269 && expectedLength < 65804) {
            log.info("Expected extended length value: " + (expectedLength - 269));
            int extendedLength = ((buffer.readByte() & 0xFF) << 8 | (buffer.readByte() & 0xFF));

            assertEquals("Length part of first byte is incorrect, ",
                    getZeroPaddedBinaryString(Integer.toBinaryString(expectedLength - 269), 16),
                    getZeroPaddedBinaryString(Integer.toBinaryString(extendedLength), 16)
            );

            assertEquals("Encoded length and readable bytes do not match, ",
                    extendedLength + 269, buffer.readableBytes());
        }
    }



    private static String getZeroPaddedBinaryString(String original, int expectedLength) {
        String result = original;
        while(result.length() < expectedLength)
            result = 0 + result;

        return result;
    }



    @After
    public void justWaitASecond() throws InterruptedException {
        Thread.sleep(100);
    }


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uzl.itm.ncoap.communication.codec").setLevel(Level.DEBUG);
        Logger.getRootLogger().setLevel(Level.ERROR);
    }
}
