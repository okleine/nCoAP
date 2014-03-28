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
//import de.uniluebeck.itm.ncoap.AbstractCoapTest;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//
//import java.net.URI;
//import java.util.Arrays;
//import java.util.Collection;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//
///**
//* Created with IntelliJ IDEA.
//* User: olli
//* Date: 05.11.13
//* Time: 14:10
//* To change this template use File | Settings | File Templates.
//*/
//@RunWith(Parameterized.class)
//public class CoapRequestUriOptionsTest extends AbstractCoapTest{
//
//    @Parameterized.Parameters
//    public static Collection<Object[]> data() throws Exception{
//        return Arrays.asList(new Object[][] {
//                {new URI("coap", null, "[2001:638::1234]", -1, "/path/to/service", "param1=1234&param2=test2", null)}
//        });
//    }
//
//    @Parameterized.Parameter(value = 0)
//    public URI uri;
//
////    @Parameterized.Parameter(value = 1)
////    public String uriHost;
////
////    @Parameterized.Parameter(value = 2)
////    public int uriPort;
////
////    @Parameterized.Parameter(value = 3)
////    public String uriPath;
////
////    @Parameterized.Parameter(value = 4)
////    public String uriQuery;
//
//
//    @Override
//    public void setupLogging() throws Exception {
//
//        Logger.getLogger("de.uniluebeck.itm.ncoap.message").setLevel(Level.DEBUG);
//
//    }
//
//
//    @Test
//    public void testUriHostOption() throws Exception{
//
//        //URI targetURI = new URI(uriScheme, null, uriHost, uriPort, uriPath, uriQuery, null);
//        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.GET, uri);
//        assertEquals(uri.getHost(), coapRequest.getUriHost());
//
//
////        assertEquals(uri.getHost(),
////                ((StringOption) coapRequest.getOptions(OptionName.URI_HOST).iterator().next()).getDecodedValue());
//
////        assertEquals(Option.URI_PORT_DEFAULT, coapRequest.getUriPort());
////        assertEquals(uri.getPath(), coapRequest.getUriPath());
////        assertEquals(uri.getQuery(), coapRequest.getUriQuery());
////
////        assertTrue(coapRequest.getOptions(OptionName.URI_PORT).isEmpty());
//
//    }
//
//
//}
