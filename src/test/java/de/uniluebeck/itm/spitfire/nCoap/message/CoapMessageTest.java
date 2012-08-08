package de.uniluebeck.itm.spitfire.nCoap.message;

import com.google.common.base.Charsets;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import java.io.UnsupportedEncodingException;
import org.apache.commons.lang.CharSet;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * Test of CoapMessage class.
 * @author Stefan Hueske
 */
public class CoapMessageTest {
    
    @Test(expected=MessageDoesNotAllowPayloadException.class)
    public void testSetPayloadForGET() throws Exception {
        new CoapMessage(Code.GET) {}.setPayload("testpayload".getBytes("UTF8"));
    }
    
    @Test(expected=MessageDoesNotAllowPayloadException.class)
    public void testSetPayloadForDELETE() throws Exception {
        new CoapMessage(Code.DELETE) {}.setPayload("testpayload".getBytes("UTF8"));
    }
    
    @Test
    public void testgetPayloadAsByteArray() throws MessageDoesNotAllowPayloadException {
        CoapMessage message = new CoapMessage(Code.CONTENT_205) {};
        byte[] payload = "testpayload".getBytes(Charsets.UTF_8);
        message.setPayload(ChannelBuffers.wrappedBuffer(payload));
        assertArrayEquals(payload, message.getPayloadAsByteArray());
        
        message = new CoapMessage(Code.CONTENT_205) {};
        assertNull(message.getPayloadAsByteArray());
    }
}
