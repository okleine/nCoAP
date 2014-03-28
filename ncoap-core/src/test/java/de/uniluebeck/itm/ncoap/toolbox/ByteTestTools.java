/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
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
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.toolbox;
//
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.junit.Test;
//
//import java.io.UnsupportedEncodingException;
//
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.fail;
//
///**
// * This class offers tools to create, compare and print byte arrays.
// * @author Stefan Hueske
// */
//public class ByteTestTools {
//    /**
//     * Compares two byte arrays and shows differences very "human friendly".
//     * @param correctByteArray The correct byte array
//     * @param byteArrayToTest The byte array to test
//     * @return Formatted comparison as String
//     */
//    public static String compareBytes(byte[] correctByteArray, byte[] byteArrayToTest) {
//        //TODO comment this
//        if (correctByteArray.length != byteArrayToTest.length) {
//            return "Byte array length differs. Should be: "
//                    + correctByteArray.length + ". Actually is: " + byteArrayToTest.length + "."
//                    + "\nCorrect bytearray:" + getBytesAsString(correctByteArray)
//                    + "\nBytearray to test:" + getBytesAsString(byteArrayToTest);
//        }
//        byte[] c1 = copyArray(correctByteArray);
//        byte[] c2 = copyArray(byteArrayToTest);
//
//        StringBuilder r1 = new StringBuilder("Correct bytearray:");
//        StringBuilder r2 = new StringBuilder("\n\nBytearray to test:");
//
//        for (int i = 0; i < correctByteArray.length; i++) {
//            if (i % 8 == 0) {
//                r1.append(" ");
//                r2.append(" ");
//            }
//            if (i % 16 == 0) {
//                r1.append("\n");
//                r2.append("\n");
//            }
//            if (c1[i] == c2[i]) {
//                r1.append("-- ");
//                r2.append("-- ");
//            } else {
//                byte b = c1[i];
//                int i1 = (b >> 4) & 0x0F;
//                int i2 = b & 0x0F;
//                r1.append(Integer.toString(i1, 16));
//                r1.append(Integer.toString(i2, 16));
//                r1.append(" ");
//                b = c2[i];
//                i1 = (b >> 4) & 0x0F;
//                i2 = b & 0x0F;
//                r2.append(Integer.toString(i1, 16));
//                r2.append(Integer.toString(i2, 16));
//                r2.append(" ");
//            }
//
//        }
//        r1.append(r2);
//        return r1.toString();
//    }
//
//    /**
//    * Returns a copy of the passed byte array.
//    * @param b Byte array to copy
//    * @return Copy of b
//    */
//    public static byte[] copyArray(byte[] b) {
//        byte[] res = new byte[b.length];
//        System.arraycopy(b, 0, res, 0, b.length);
//        return res;
//    }
//
//    /**
//     * Returns a hex interpretation of a String as byte array.
//     * @param hexByteArray String to convert
//     * @return Converted String as byte array
//     */
//    public static byte[] getByteArrayFromString(String hexByteArray) {
//        //TODO comment this
//        hexByteArray = hexByteArray.replace(" ", "");
//        hexByteArray = hexByteArray.replace("\n", "");
//        if (hexByteArray.length() % 2 != 0) {
//            hexByteArray = "0" + hexByteArray;
//        }
//        byte[] res = new byte[hexByteArray.length() / 2];
//        for (int i = 0; i < hexByteArray.length() / 2; i++) {
//            String c = hexByteArray.substring(i * 2, i * 2 + 1);
//            byte b1 = Byte.parseByte(c, 16);
//            c = hexByteArray.substring(i * 2 + 1, i * 2 + 2);
//            byte b2 = Byte.parseByte(c, 16);
//            res[i] = (byte) ((b1 << 4) + b2);
//        }
//        return res;
//    }
//
//    /**
//     * Converts a ChannelBuffer to a byte array.
//     * @param channelBuffer ChannelBuffer to convert
//     * @return Converted byte array
//     */
//    public static byte[] getByteArrayFromChannelBuffer(ChannelBuffer channelBuffer) {
//        byte[] convertedByteArray = new byte[channelBuffer.readableBytes()];
//        for (int i = 0; channelBuffer.readable(); i++) {
//            convertedByteArray[i] = channelBuffer.readByte();
//        }
//        return convertedByteArray;
//    }
//
//    /**
//     * Returns human friendly hex string representation of a passed byte array.
//     * (The same formatting wireshark uses)
//     * @param data Data as byte array
//     * @param n Only the first n bytes from data will be read
//     * @return Formatted hex string
//     */
//    public static String getBytesAsString(byte[] data, int n) {
//        StringBuilder res = new StringBuilder();
//
//        for (int i = 0; i < n; i++) {
//            if (i % 8 == 0) {
//                res.append(" ");
//            }
//            if (i % 16 == 0) {
//                res.append("\n");
//            }
//            byte b = data[i];
//            int b1 = (b >> 4) & 0x0F;
//            int b2 = b & 0x0F;
//            res.append(Integer.toString(b1, 16));
//            res.append(Integer.toString(b2, 16));
//            res.append(" ");
//        }
//
//        return res.toString();
//    }
//
//    /**
//     * Returns human friendly hex string representation of a passed byte array.
//     * (The same formatting wireshark uses)
//     * @param data Data as byte array
//     * @return Formatted hex string
//     */
//    public static String getBytesAsString(byte[] data) {
//        return getBytesAsString(data, data.length);
//    }
//
//    @Test
//    public void testByteTestTools() {
//        byte[] byteArray = new byte[3];
//        byteArray[0] = 0x12;
//        byteArray[1] = 0x34;
//        byteArray[2] = 0x56;
//        assertArrayEquals(byteArray, getByteArrayFromString("123456"));
//
//        byteArray = new byte[3];
//        byteArray[0] = 0x00;
//        byteArray[1] = 0x00;
//        byteArray[2] = 0x00;
//        assertArrayEquals(byteArray, getByteArrayFromString("000000"));
//
//        byteArray = new byte[3];
//        byteArray[0] = (byte) 0xFF;
//        byteArray[1] = (byte) 0xFF;
//        byteArray[2] = (byte) 0xFF;
//        assertArrayEquals(byteArray, getByteArrayFromString("FFFFFF"));
//
//        byteArray = new byte[3];
//        byteArray[0] = 0x05;
//        byteArray[1] = 0x43;
//        byteArray[2] = 0x21;
//        assertArrayEquals(byteArray, getByteArrayFromString("54321"));
//    }
//
//    public static void assertEquals(String message, ChannelBuffer expected, ChannelBuffer actual) {
//        boolean equals = expected == null ? actual == null : expected.equals(actual);
//        if (!equals) {
//            try {
//                String expectedAsString = expected == null ? "null" :
//                        new String(getByteArrayFromChannelBuffer(expected), "UTF-8");
//                String actualAsString = actual == null ? "null" :
//                        new String(getByteArrayFromChannelBuffer(actual), "UTF-8");
//                fail(String.format("'%s', UTF-8 decoded Channelbuffer: expected: '%s', actual: '%s'",
//                        message, expectedAsString, actualAsString));
//            } catch (UnsupportedEncodingException ex) {
//                throw new InternalError("This should never happen, UTF-8 encoding was not recognized");
//            }
//        }
//    }
//}
