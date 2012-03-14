package de.uniluebeck.itm.spitfire.nCoap.communication.encoding;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.Code.*;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import static de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionList;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.StringOption;
import de.uniluebeck.itm.spitfire.nCoap.message.options.UintOption;
import static de.uniluebeck.itm.spitfire.nCoap.testtools.ByteTestTools.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * Test of CoAPMessageEncoder class.
 * @author Stefan Hueske
 */
public class CoapMessageEncoderTest {
    List<TestMessage> testMessages = new LinkedList<>();   
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
        testMessages.add(new TestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "4"    // 0x4 = 0b0100 => (Ver = 1 and Type = 0 = CON)
                + " 0" // Option Count = 0x0 = 0
                + "01" // Code = 0x01 = 1 = GET
                + "0000")); //Message ID = 0x0000 = 0
        
        //CoAPMessage to encode
        header = new Header(NON, POST, 65535);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new TestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "5"    // 0x5 = 0b0101 => (Ver = 1 and Type = 1 = NON)
                + " 0" // Option Count = 0x0 = 0
                + "02" // Code = 0x02 = 2 = POST
                + "FFFF")); //Message ID = 0xFFFF = 65535
        
        //CoAPMessage to encode
        header = new Header(ACK, CONTENT_205, 12345);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new TestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "6"    // 0x6 = 0b0110 => (Ver = 1 and Type = 2 = ACK)
                + " 0" // Option Count = 0x0 = 0
                + "45" // Code = 0x45 = 69 = CONTENT_205
                + "3039")); //Message ID = 0x3039 = 12345
        
        //CoAPMessage to encode
        header = new Header(RST, NOT_FOUND_404, 54321);
        optionList = new OptionList();
        payload = ChannelBuffers.EMPTY_BUFFER;
        testMessages.add(new TestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "7"    // 0x7 = 0b0111 => (Ver = 1 and Type = 3 = RST)
                + " 0" // Option Count = 0x0 = 0
                + "84" // Code = 0x84 = 132 = NOT_FOUND_404
                + "D431")); //Message ID = 0xD431 = 54321
        
        //---------------options and payload tests-----------------------------------------
        //CoAPMessage to encode
        Code code = CONTENT_205;
        header = new Header(ACK, code, 12345);
        optionList = new OptionList();
        optionList.addOption(code, CONTENT_TYPE, UintOption.createUintOption(CONTENT_TYPE, 41));
        optionList.addOption(code, URI_HOST, StringOption.createStringOption(URI_HOST, "testhost"));
        optionList.addOption(code, URI_PORT, UintOption.createUintOption(URI_PORT, 65535));
        payload = ChannelBuffers.wrappedBuffer("testpayload\u00FF".getBytes("UTF8"));
        testMessages.add(new TestMessage(new CoapMessage(header, optionList, payload) {},
                //correct encoded CoAP message
                "63 45 30 39" //Header
                + " 1" //option delta => absolute option number: 1 + 0 = 1 = Content-Type
                + " 1" //option length in bytes
                + "29" // 41 = 0x29
                + " 4" //option delta => absolute option number: 4 + 1 = 5 = Uri-Host
                + " 8" //option length in bytes
                + getBytesAsString("testhost".getBytes("UTF8")) //option value
                + " 2" //option delta => absolute option number: 2 + 5 = 7 = Uri-Port
                + " 2" //option length in bytes
                + "ffff" // 65535 = 0xFFFF
                + getBytesAsString("testpayload\u00FF".getBytes("UTF8")))); //payload
    }
    
    /**
     * Test of encode method, of class CoapMessageEncoder.
     */
    @Test
    public void testEncode() throws Exception {
        System.out.println("Testing CoapMessageEncoder...");
        for (TestMessage testMessage : testMessages) {
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
class TestMessage {
    //Message which will be encoded in this test
    CoapMessage messageToEncode;
    //The same message, already correct encoded 
    byte[] encodedCoAPMessage;

    public TestMessage(CoapMessage messageToEncode, byte[] encodedCoAPMessage) {
        this.messageToEncode = messageToEncode;
        this.encodedCoAPMessage = encodedCoAPMessage;
    }
    
    public TestMessage(CoapMessage messageToEncode, String hexByteArray) {
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
