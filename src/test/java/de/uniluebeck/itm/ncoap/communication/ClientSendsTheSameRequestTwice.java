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
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.communication;
//
//import de.uniluebeck.itm.ncoap.plugtest.endpoint.CoapTestEndpoint;
//import de.uniluebeck.itm.ncoap.plugtest.server.CoapTestServer;
//import de.uniluebeck.itm.ncoap.plugtest.server.webservice.NotObservableTestWebService;
//import de.uniluebeck.itm.ncoap.message.CoapRequest;
//import de.uniluebeck.itm.ncoap.message.MessageCode;
//import de.uniluebeck.itm.ncoap.message.MessageType;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//
//import java.net.InetSocketAddress;
//import java.net.URI;
//
//import static junit.framework.Assert.assertEquals;
//
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 19.08.13
// * Time: 12:37
// * To change this template use File | Settings | File Templates.
// */
//public class ClientSendsTheSameRequestTwice extends AbstractCoapCommunicationTest{
//
//    private static CoapTestServer server;
//    private static CoapTestEndpoint endpoint;
//    private static CoapRequest coapRequest1;
//    private static CoapRequest coapRequest2;
//
//    @Override
//    public void setupComponents() throws Exception {
//        server = new CoapTestServer(0);
//        server.registerService(new NotObservableTestWebService("/path", "Status 1", 1000));
//
//        endpoint = new CoapTestEndpoint();
//
//        URI serviceURI = new URI("coap", null, "localhost", server.getServerPort(), "/path", null, null);
//        coapRequest1 = new CoapRequest(MessageType.NON, MessageCode.GET, serviceURI);
//        coapRequest1.setMessageID(1);
//        coapRequest1.setToken(new byte[]{1, 2, 3, 4});
//
//        coapRequest2 = new CoapRequest(MessageType.NON, MessageCode.GET, serviceURI);
//        coapRequest2.setMessageID(1);
//        coapRequest2.setToken(new byte[]{1, 2, 3, 4});
//    }
//
//    @Override
//    public void shutdownComponents() throws Exception {
//        //server.shutdown();
//        //endpoint.shutdown();
//    }
//
//    @Override
//    public void createTestScenario() throws Exception {
//        endpoint.writeMessage(coapRequest1, new InetSocketAddress("localhost", server.getServerPort()));
//        Thread.sleep(200);
//        endpoint.writeMessage(coapRequest2, new InetSocketAddress("localhost", server.getServerPort()));
//        Thread.sleep(2000);
//    }
//
//    @Override
//    public void setupLogging() throws Exception {
//        //Logger.getRootLogger().setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.plugtest.endpoint").setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.plugtest.server").setLevel(Level.DEBUG);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability").setLevel(Level.DEBUG);
//    }
//
//    @Test
//    public void testMessagesForEquality(){
//        assertEquals(coapRequest1, coapRequest2);
//        assertEquals(coapRequest1.hashCode(), coapRequest2.hashCode());
//    }
//
//    @Test
//    public void testEndpointReceivedOnlyOneResponse(){
//        assertEquals(1, endpoint.getReceivedCoapMessages().size());
//    }
//}
