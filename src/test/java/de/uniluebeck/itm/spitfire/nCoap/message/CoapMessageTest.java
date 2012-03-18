package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
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
   
}
