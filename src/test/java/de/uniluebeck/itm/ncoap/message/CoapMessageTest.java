package de.uniluebeck.itm.ncoap.message;

import com.google.common.base.Charsets;
import de.uniluebeck.itm.ncoap.message.header.Code;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;


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
        assertArrayEquals(payload, getPayloadAsByteArray(message.getPayload()));
    }
    
    public static byte[] getPayloadAsByteArray(ChannelBuffer payload){
        if (payload == null) {
            return null;
        }
        byte[] convertedByteArray = new byte[payload.readableBytes()];
        for (int i = 0; payload.readable(); i++) {
            convertedByteArray[i] = payload.readByte();
        }        
        return convertedByteArray;
    }
}
