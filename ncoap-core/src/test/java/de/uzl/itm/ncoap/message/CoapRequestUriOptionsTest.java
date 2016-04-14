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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;


/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 05.11.13
* Time: 14:10
* To change this template use File | Settings | File Templates.
*/
@RunWith(Parameterized.class)
public class CoapRequestUriOptionsTest extends AbstractCoapTest{

    @Parameterized.Parameters (name = "URI: {0}")
    public static Collection<Object[]> data() throws Exception{
        return Arrays.asList(new Object[][] {
            {
                new URI("coap", null, "[2001:db8::2:1]", -1, null, null, null),
                null, 5683, "/", ""
            },{
                new URI("coap", null, "example.net", 5683, null, null, null),
                "example.net", 5683, "/", ""
            },{
                new URI("coap", null, "example.net", 5683, "/.well-known/core", null, null),
                "example.net", 5683, "/.well-known/core", ""
            },{
                new URI("coap", null, "xn--18j4d.example", 5683, "/%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF", null, null),
                "xn--18j4d.example", 5683, "/こんにちは", ""
            },{
                new URI("coap", null, "example.com", -1, "/~sensors/temp.xml", null, null),
                "example.com", 5683, "/~sensors/temp.xml", ""
            },{
                new URI("coap", null, "example.com", 5683, "/%7Esensors/temp.xml", null, null),
                "example.com", 5683, "/~sensors/temp.xml", ""
            },{
                new URI("coap", null, "example.com", 5683, "/%7esensors/temp.xml", null, null),
                "example.com", 5683, "/~sensors/temp.xml", ""
            },{
                new URI("coap", null, "198.51.100.1", 61616, "//%2F//", "%2F%2F&?%26", null),
                null, 61616,  "//", "//&?&"
            }
        });
    }

    @Parameterized.Parameter(value = 0)
    public URI targetUri;

    @Parameterized.Parameter(value = 1)
    public String expectedUriHost;

    @Parameterized.Parameter(value = 2)
    public int expectedUriPort;

    @Parameterized.Parameter(value = 3)
    public String expectedUriPath;

    @Parameterized.Parameter(value = 4)
    public String expectedUriQuery;

    public CoapRequest coapRequest;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.message").setLevel(Level.DEBUG);
    }

    @Before
    public void createCoapRequest() throws URISyntaxException {
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, targetUri);
    }

    @After
    public void wait1Second() throws InterruptedException {
        //This is due to a bug in IntelliJ in relating log messages to particular tests...
        Thread.sleep(100);
    }

    @Test
    public void testUriHostOption() throws Exception{
        assertEquals(expectedUriHost, coapRequest.getUriHost());
    }

    @Test
    public void testUriPathOptions() {
        assertEquals(expectedUriPath, coapRequest.getUriPath());
    }


    @Test
    public void testUriPortOption() {
        assertEquals(expectedUriPort, coapRequest.getUriPort());
    }


    @Test
    public void testUriQueryOptions() {
        assertEquals(expectedUriQuery, coapRequest.getUriQuery());
    }

}
