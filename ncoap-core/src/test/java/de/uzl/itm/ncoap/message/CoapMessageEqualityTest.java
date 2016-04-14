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
package de.uzl.itm.ncoap.message;

import de.uzl.itm.ncoap.AbstractCoapTest;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

/**
 * Created by olli on 07.03.14.
 */
public class CoapMessageEqualityTest extends AbstractCoapTest {

    @Override
    public void setupLogging() throws Exception {

    }


    @Test
    public void testSameRequests() throws Exception {
        URI targetUri = new URI("coap", null, "localhost", 5683, "/path/to/service", null, null);

        CoapRequest coapRequest1 = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri);
        coapRequest1.setToken(new Token(new byte[0]));
        coapRequest1.setMessageID(12345);

        CoapRequest coapRequest2 = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri);
        coapRequest2.setToken(new Token(new byte[0]));
        coapRequest2.setMessageID(12345);

        Assert.assertEquals(coapRequest1, coapRequest2);
    }


    @Test
    public void testDifferentUriPathOptionsOrder() throws Exception {

        String path1 = "/path/to/service";
        String path2 = "/path/service/to";

        URI targetUri1 = new URI("coap", null, "localhost", 5683, path1, null, null);
        URI targetUri2 = new URI("coap", null, "localhost", 5683, path2, null, null);

        CoapRequest coapRequest1 = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri1);
        coapRequest1.setToken(new Token(new byte[0]));
        coapRequest1.setMessageID(12345);

        CoapRequest coapRequest2 = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri2);
        coapRequest2.setToken(new Token(new byte[0]));
        coapRequest2.setMessageID(12345);

        Assert.assertFalse("Requests with different order of URI path options must not equal!",
                coapRequest1.equals(coapRequest2));
    }


    @Test
    public void testResponsesWitDifferentMessageIDs() throws Exception {

        CoapResponse coapResponse1 = new CoapResponse(MessageType.NON, MessageCode.CONTENT_205);
        coapResponse1.setContent("Test123".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        coapResponse1.setToken(new Token(new byte[0]));
        coapResponse1.setMessageID(12346);

        CoapResponse coapResponse2 = new CoapResponse(MessageType.NON, MessageCode.CONTENT_205);
        coapResponse2.setContent("Test123".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        coapResponse2.setToken(new Token(new byte[0]));
        coapResponse2.setMessageID(12345);

        Assert.assertFalse("Requests with different message IDs must not equal!",
                coapResponse1.equals(coapResponse2));
    }


    @Test
    public void testResponsesWitDifferentTokens() throws Exception {

        CoapResponse coapResponse1 = new CoapResponse(MessageType.NON, MessageCode.CONTENT_205);
        coapResponse1.setContent("Test123".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        coapResponse1.setToken(new Token(new byte[0]));
        coapResponse1.setMessageID(12345);

        CoapResponse coapResponse2 = new CoapResponse(MessageType.NON, MessageCode.CONTENT_205);
        coapResponse2.setContent("Test123".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        coapResponse2.setToken(new Token(new byte[1]));
        coapResponse2.setMessageID(12345);

        Assert.assertFalse("Requests with different tokens must not equal!",
                coapResponse1.equals(coapResponse2));
    }


    @Test
    public void testRequestsWithDifferentOrderOfAcceptOptions() throws Exception {
        URI targetUri = new URI("coap", null, "localhost", 5683, "/path/to/service", null, null);

        CoapRequest coapRequest1 = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri);
        coapRequest1.setToken(new Token(new byte[0]));
        coapRequest1.setMessageID(12345);
        coapRequest1.setAccept(ContentFormat.TEXT_PLAIN_UTF8, ContentFormat.APP_XML);

        CoapRequest coapRequest2 = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri);
        coapRequest2.setToken(new Token(new byte[0]));
        coapRequest2.setMessageID(12345);
        coapRequest1.setAccept(ContentFormat.APP_XML, ContentFormat.TEXT_PLAIN_UTF8);

        Assert.assertFalse("Requests with different order of accept options must not equal!",
                coapRequest1.equals(coapRequest2));
    }
}
