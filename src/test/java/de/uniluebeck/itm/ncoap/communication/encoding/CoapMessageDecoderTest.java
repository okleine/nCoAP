/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.communication.encoding;

import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import static de.uniluebeck.itm.ncoap.message.header.Code.*;
import de.uniluebeck.itm.ncoap.message.header.Header;
import static de.uniluebeck.itm.ncoap.message.header.MsgType.*;
import de.uniluebeck.itm.ncoap.message.options.OptionList;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;
import de.uniluebeck.itm.ncoap.message.options.StringOption;
import de.uniluebeck.itm.ncoap.message.options.UintOption;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;

import static de.uniluebeck.itm.ncoap.toolbox.ByteTestTools.getByteArrayFromString;
import static de.uniluebeck.itm.ncoap.toolbox.ByteTestTools.getBytesAsString;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;



/**
 * Test of CoapMessageDecoder class.
 * @author Stefan Hueske
 */
public class CoapMessageDecoderTest extends AbstractCoapTest{

    List<DecodeTestMessage> testMessages = new LinkedList<DecodeTestMessage>();
    CoapMessageDecoder messageDecoder = new CoapMessageDecoder();
    
    InetSocketAddress testRcptAdress = new InetSocketAddress(33210);
    TestChannel testChannel = new TestChannel(testRcptAdress);

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.encoding").setLevel(Level.DEBUG);
    }

    /**
     * Fills the testMessages list with test messages.
     */
    @Before
    public void createTestMessages() throws Exception {
        //---------------header tests------------------------------------------------------
        //CoAPMessage to decode
        Header header = new Header(CON, GET, 0);
        OptionList optionList = new OptionList();
        ChannelBuffer payload = ChannelBuffers.EMPTY_BUFFER;
        CoapMessage coapMessage = new CoapRequest(header, optionList, payload);
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                "4"    // 0x4 = 0b0100 => (Ver = 1 and Type = 0 = CON)
                + " 0" // Option Count = 0x0 = 0
                + "01" // Code = 0x01 = 1 = GET
                + "0000")); //Message ID = 0x0000 = 0
        
        //CoAPMessage to decode
        header = new Header(NON, POST, 65535);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        coapMessage = new CoapResponse(header, optionList, payload);
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                "5"    // 0x5 = 0b0101 => (Ver = 1 and Type = 1 = NON)
                + " 0" // Option Count = 0x0 = 0
                + "02" // Code = 0x02 = 2 = POST
                + "FFFF")); //Message ID = 0xFFFF = 65535
        
        //CoAPMessage to decode
        header = new Header(ACK, CONTENT_205, 12345);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        coapMessage = new CoapResponse(header, optionList, payload) {};
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                "6"    // 0x6 = 0b0110 => (Ver = 1 and Type = 2 = ACK)
                + " 0" // Option Count = 0x0 = 0
                + "45" // Code = 0x45 = 69 = CONTENT_205
                + "3039")); //Message ID = 0x3039 = 12345
        
        //CoAPMessage to decode
        header = new Header(RST, NOT_FOUND_404, 54321);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        coapMessage = new CoapResponse(header, optionList, payload) {};
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                "7"    // 0x7 = 0b0111 => (Ver = 1 and Type = 3 = RST)
                + " 0" // Option Count = 0x0 = 0
                + "84" // Code = 0x84 = 132 = NOT_FOUND_404
                + "D431")); //Message ID = 0xD431 = 54321
        
        //---------------options and payload tests-----------------------------------------
        //CoAPMessage to decode
        Code code = GET;
        header = new Header(CON, code, 12345);
        optionList = new OptionList();
        optionList.addOption(code, URI_HOST, StringOption.createStringOption(URI_HOST, "testhost"));
        optionList.addOption(code, URI_PORT, UintOption.createUintOption(URI_PORT, 65535));
        payload = ChannelBuffers.wrappedBuffer("testpayload\u00FF".getBytes("UTF8"));
        coapMessage = new CoapRequest(header, optionList, payload) {};
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                "42 01 30 39" //Header
                + " 5" //option delta => absolute option number: 5 + 0 = 5 = Uri-Host
                + " 8" //option length in bytes
                + getBytesAsString("testhost".getBytes("UTF8")) //option value
                + " 2" //option delta => absolute option number: 2 + 5 = 7 = Uri-Port
                + " 2" //option length in bytes
                + "ffff" // 65535 = 0xFFFF
                + getBytesAsString("testpayload\u00FF".getBytes("UTF8")))); //payload
        
        //CoAPMessage to decode
        code = CONTENT_205;
        header = new Header(ACK, code, 1);
        optionList = new OptionList();
        optionList.addOption(code, CONTENT_TYPE, UintOption.createUintOption(CONTENT_TYPE, 0));
        optionList.addOption(code, TOKEN, UintOption.createOpaqueOption(TOKEN,
                getByteArrayFromString("6dee9b506332e8fe")));
        payload = ChannelBuffers.wrappedBuffer("testpayload\u00FF".getBytes("UTF8"));
        coapMessage = new CoapResponse(header, optionList, payload) {};
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                //Header
                "6"    // 0x6 = 0b0110 => (Ver = 1 and Type = 2 = ACK)
                + " 2" // Option Count = 0x2 = 2
                + "45" // Code = 0x45 = 69 = CONTENT_205
                + "0001" // Message ID = 0xD431 = 54321
                //Options
                + " 1" //option delta => absolute option number: 1 + 0 = 1 = CONTENT_TYPE
                + " 1" //option length in bytes
                + "00" //CONTENT_TYPE => text/plain; charset=utf-8
                + " a" //option delta => absolute option number: 10 + 1 = 11 = TOKEN
                + " 8" //option length in bytes
                + "6dee9b506332e8fe" // <-- TOKEN
                + getBytesAsString("testpayload\u00FF".getBytes("UTF8")))); //payload
        
        //CoAPMessage to decode
        code = NOT_FOUND_404;
        header = new Header(ACK, code, 0);
        optionList = new OptionList();
        optionList.addOption(code, TOKEN, UintOption.createOpaqueOption(TOKEN,
                getByteArrayFromString("35")));
        payload = ChannelBuffers.wrappedBuffer(new byte[0]);
        coapMessage = new CoapResponse(header, optionList, payload) {};
        coapMessage.setRcptAdress(testRcptAdress.getAddress());
        testMessages.add(new DecodeTestMessage(coapMessage,
                //correct encoded CoAP message
                //Header
                "6"    // 0x6 = 0b0110 => (Ver = 1 and Type = 2 = ACK)
                + " 1" // Option Count = 0x1 = 1
                + "84" // Code = 0x84 = 132 = NOT_FOUND_404
                + "0000" // Message ID = 0x0000 = 0000
                //Options
                + " b" //option delta => absolute option number: 11 + 0 = 11 = TOKEN
                + " 1" //option length in bytes
                + "35")); // <-- TOKEN


        testMessages.add(new DecodeTestMessage(CoapMessage.createEmptyReset(45), new byte[]{112, 0, 0, 45}));
    }
    
    /**
     * Test of decode method, of class CoapMessageEncoder.
     */
    @Test
    public void testDecode() throws Exception {
        System.out.println("Testing CoapMessageDecoder...");
        for (DecodeTestMessage testMessage : testMessages) {
            Object decodedObject = messageDecoder.decode(null, testChannel,
                    ChannelBuffers.wrappedBuffer(testMessage.coAPMessageToDecode));
            if (!(decodedObject instanceof CoapMessage)) {
                fail("The object returned by method decode() is not a CoapMessage.");
            }
            //run test
            testMessage.test((CoapMessage)decodedObject);
        }
    }
}

/**
 * This class consists of a CoAPMessage object and its encoded form as a byte array.
 */
class DecodeTestMessage {
    //Correct decoded message
    CoapMessage correctDecodedCoAPMessage;
    //Message to decode in this test 
    byte[] coAPMessageToDecode;

    public DecodeTestMessage(CoapMessage correctDecodedCoAPMessage, byte[] coapMessageToDecode) {
        this.correctDecodedCoAPMessage = correctDecodedCoAPMessage;
        this.coAPMessageToDecode = coapMessageToDecode;
    }
    
    public DecodeTestMessage(CoapMessage correctDecodedCoAPMessage, String hexByteArray) {
        this.correctDecodedCoAPMessage = correctDecodedCoAPMessage;
        this.coAPMessageToDecode = getByteArrayFromString(hexByteArray);
    }    
    
    /**
     * Tests if the passed byte array is correct encoded. JUnit test will fail if not.
     * @param decodedMsgToTest Encoded message to test
     */
    void test(CoapMessage decodedMsgToTest) {
        if (!decodedMsgToTest.equals(correctDecodedCoAPMessage)) {
            fail("Assertion failed. CoAPMessage \"" + correctDecodedCoAPMessage + "\" does not match with \"" 
                    + decodedMsgToTest + "\"");
        }
    }
}

/**
 * This class fakes a datagramChannel to let CoAPMessageDecoder call the Channel.getLocalAddress()).getAddress()
 * method. (Needed for CoAPMessage.rcptAddress)
 */
class TestChannel implements Channel {
    InetSocketAddress testRcptAdress;
    
    @Override
    public SocketAddress getLocalAddress() {
        return testRcptAdress;
    }
    
    @Override
    public boolean isBound() {
        return false;
    }
    
    public TestChannel(InetSocketAddress testRcptAdress) {
        this.testRcptAdress = testRcptAdress;
    }
    
    //all other methods of this Channel are not supported

    @Override
    public Integer getId() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFactory getFactory() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Channel getParent() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelConfig getConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelPipeline getPipeline() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SocketAddress getRemoteAddress() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture write(Object o) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture write(Object o, SocketAddress sa) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture bind(SocketAddress sa) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture connect(SocketAddress sa) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture disconnect() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture unbind() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture close() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture getCloseFuture() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getInterestOps() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isReadable() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isWritable() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture setInterestOps(int i) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ChannelFuture setReadable(boolean bln) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int compareTo(Channel o) {
        throw new UnsupportedOperationException("Not supported.");
    }
}