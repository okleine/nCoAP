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
//package de.uniluebeck.itm.ncoap.message;
//
//import com.google.common.base.Charsets;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertNull;
//import org.junit.Test;
//
//
///**
// * Test of CoapMessage class.
// * @author Stefan Hueske
// */
//public class CoapMessageTest {
//
//    @Test(expected=MessageDoesNotAllowContentException.class)
//    public void testSetPayloadForGET() throws Exception {
//        new CoapMessage(MessageCode.GET) {}.setContent("testpayload".getBytes("UTF8"));
//    }
//
//    @Test(expected=MessageDoesNotAllowContentException.class)
//    public void testSetPayloadForDELETE() throws Exception {
//        new CoapMessage(MessageCode.DELETE) {}.setContent("testpayload".getBytes("UTF8"));
//    }
//
//    @Test
//    public void testgetPayloadAsByteArray() throws MessageDoesNotAllowContentException {
//        CoapMessage message = new CoapMessage(MessageCode.CONTENT_205) {};
//        byte[] payload = "testpayload".getBytes(Charsets.UTF_8);
//        message.setContent(ChannelBuffers.wrappedBuffer(payload));
//        assertArrayEquals(payload, getPayloadAsByteArray(message.getContent()));
//    }
//
//    public static byte[] getPayloadAsByteArray(ChannelBuffer payload){
//        if (payload == null) {
//            return null;
//        }
//        byte[] convertedByteArray = new byte[payload.readableBytes()];
//        for (int i = 0; payload.readable(); i++) {
//            convertedByteArray[i] = payload.readByte();
//        }
//        return convertedByteArray;
//    }
//}
