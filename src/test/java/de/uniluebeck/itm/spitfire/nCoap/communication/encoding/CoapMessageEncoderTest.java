package de.uniluebeck.itm.spitfire.nCoap.communication.encoding;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Before;
import org.junit.Test;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.*;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
import static org.junit.Assert.fail;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.StringOption;

/**
 * Test of CoAPMessageEncoder class.
 * @author Stefan Hueske
 */
public class CoapMessageEncoderTest {
    List<EncodeTestMessage> testMessages = new LinkedList<EncodeTestMessage>();   
    CoapMessageEncoder messageEncoder = new CoapMessageEncoder();
    
    /**
     * Fills the testMessages list with test messages.
     */
    @Before
    public void createTestMessages() throws Exception {
        //---------------header tests------------------------------------------------------
        //CoAPMessage to encode
        Header header = new Header(CON, GET, 0);
        OptionList optionList = new OptionList();
        ChannelBuffer payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new EncodeTestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "4"    // 0x4 = 0b0100 => (Ver = 1 and Type = 0 = CON)
                + " 0" // Option Count = 0x0 = 0
                + "01" // Code = 0x01 = 1 = GET
                + "0000")); //Message ID = 0x0000 = 0
        
        //CoAPMessage to encode
        header = new Header(NON, POST, 65535);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new EncodeTestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "5"    // 0x5 = 0b0101 => (Ver = 1 and Type = 1 = NON)
                + " 0" // Option Count = 0x0 = 0
                + "02" // Code = 0x02 = 2 = POST
                + "FFFF")); //Message ID = 0xFFFF = 65535
        
        //CoAPMessage to encode
        header = new Header(ACK, CONTENT_205, 12345);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new EncodeTestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "6"    // 0x6 = 0b0110 => (Ver = 1 and Type = 2 = ACK)
                + " 0" // Option Count = 0x0 = 0
                + "45" // Code = 0x45 = 69 = CONTENT_205
                + "3039")); //Message ID = 0x3039 = 12345
        
        //CoAPMessage to encode
        header = new Header(RST, NOT_FOUND_404, 54321);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new EncodeTestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "7"    // 0x7 = 0b0111 => (Ver = 1 and Type = 3 = RST)
                + " 0" // Option Count = 0x0 = 0
                + "84" // Code = 0x84 = 132 = NOT_FOUND_404
                + "D431")); //Message ID = 0xD431 = 54321
        
        //---------------options and payload tests-----------------------------------------
        //CoAPMessage to encode
        Code code = GET;
        header = new Header(CON, code, 12345);
        optionList = new OptionList();
        optionList.addOption(code, URI_HOST, StringOption.createStringOption(URI_HOST, "testhost"));
        optionList.addOption(code, URI_PORT, UintOption.createUintOption(URI_PORT, 65535));
        payload = ChannelBuffers.wrappedBuffer("testpayload\u00FF".getBytes("UTF8"));
        testMessages.add(new EncodeTestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "42 01 30 39" //Header
                + " 5" //option delta => absolute option number: 5 + 0 = 5 = Uri-Host
                + " 8" //option length in bytes
                + getBytesAsString("testhost".getBytes("UTF8")) //option value
                + " 2" //option delta => absolute option number: 2 + 5 = 7 = Uri-Port
                + " 2" //option length in bytes
                + "ffff" // 65535 = 0xFFFF
                + getBytesAsString("testpayload\u00FF".getBytes("UTF8")))); //payload

        //CoAPMessage to encode

        testMessages.add(new EncodeTestMessage(CoapMessage.createEmptyReset(45)
                , new byte[]{112, 0, 0, 45}));

    }
    
    /**
     * Test of encode method, of class CoapMessageEncoder.
     */
    @Test
    public void testEncode() throws Exception {
        System.out.println("Testing CoapMessageEncoder...");
        for (EncodeTestMessage testMessage : testMessages) {
            Object encodedObject = messageEncoder.encode(null, null, testMessage.messageToEncode);
            if (!(encodedObject instanceof ChannelBuffer)) {
                fail("The object returned by method encode() is not a ChannelBuffer.");
            }
            //run test
            testMessage.test(getByteArrayFromChannelBuffer((ChannelBuffer)encodedObject));
        }
    }
}

/**
 * This class consists of a CoAPMessage object and its encoded form as a byte array.
 */
class EncodeTestMessage {
    //Message which will be encoded in this test
    CoapMessage messageToEncode;
    //The same message, already correct encoded 
    byte[] encodedCoAPMessage;

    public EncodeTestMessage(CoapMessage messageToEncode, byte[] encodedCoAPMessage) {
        this.messageToEncode = messageToEncode;
        this.encodedCoAPMessage = encodedCoAPMessage;
    }
    
    public EncodeTestMessage(CoapMessage messageToEncode, String hexByteArray) {
        this.messageToEncode = messageToEncode;
        this.encodedCoAPMessage = getByteArrayFromString(hexByteArray);
    }    
    
    /**
     * Tests if the passed byte array is correct encoded. JUnit test will fail if not.
     * @param encodedMsgToTest Encoded message to test
     */
    void test(byte[] encodedMsgToTest) {
        if (!Arrays.equals(encodedCoAPMessage, encodedMsgToTest)) {
            fail("Assertion failed. CoAPMessage \"" + messageToEncode + "\" does not match. Differences:\n"
                    + compareBytes(encodedCoAPMessage, encodedMsgToTest));
        }
    }
}
