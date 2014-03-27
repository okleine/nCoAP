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
//package de.uniluebeck.itm.ncoap.examples.test.client;
//
//import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
//import de.uniluebeck.itm.ncoap.examples.test.client.utils.AbstractCoapTest;
//import de.uniluebeck.itm.ncoap.examples.test.client.utils.CoapTestResponseProcessor;
//import de.uniluebeck.itm.ncoap.message.*;
//import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.net.URI;
//import java.util.Iterator;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
//* Created with IntelliJ IDEA.
//* User: olli
//* Date: 19.11.13
//* Time: 13:55
//* To change this template use File | Settings | File Templates.
//*/
//public class TD_COAP_CORE_21 extends AbstractCoapTest {
//
//    private static CoapClientApplication clientApplication;
//    private static CoapTestResponseProcessor testResponseProcessor = new CoapTestResponseProcessor();
//
//    @BeforeClass
//    public static void sendRequest() throws Exception {
//        setupLogging();
//
//        clientApplication = new CoapClientApplication();
//
//        URI targetUri = new URI("coap", null, AbstractCoapTest.TESTSERVER_HOST, AbstractCoapTest.TESTSERVER_PORT,
//                "/validate", null, null);
//
//        CoapRequest coapRequest1 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
//        clientApplication.sendCoapRequest(coapRequest1, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        CoapResponse coapResponse = testResponseProcessor.getCoapResponses().values().iterator().next();
//        byte[] etag = coapResponse.getEtag();
//
//        if(etag == null)
//            return;
//
//        CoapRequest coapRequest2 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
//        coapRequest2.setEtags(etag);
//        clientApplication.sendCoapRequest(coapRequest2, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        CoapRequest coapRequest3 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.PUT, targetUri);
//        coapRequest3.setContent("New Content".getBytes(CoapMessage.CHARSET), ContentFormat.Name.TEXT_PLAIN_UTF8);
//        clientApplication.sendCoapRequest(coapRequest3, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        CoapRequest coapRequest4 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
//        coapRequest4.setEtags(etag);
//        clientApplication.sendCoapRequest(coapRequest4, testResponseProcessor);
//
//        Thread.sleep(3000);
//    }
//
//    @Test
//    public void testResponseCount(){
//        assertEquals("Wrong number of responses, ", 4, testResponseProcessor.getCoapResponses().size());
//    }
//
//    @Test
//    public void testFirstResponseHasEtag(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        CoapResponse coapResponse = iterator.next();
//        assertTrue("First response has no ETAG option!", coapResponse.getEtag() != null);
//    }
//
//    @Test
//    public void testFirstResponseHasContent(){
//        CoapResponse coapResponse = testResponseProcessor.getCoapResponses().values().iterator().next();
//        assertTrue("Expected any content", coapResponse.getContent().readableBytes() > 0);
//
//        System.out.println(coapResponse.getContent().toString(CoapMessage.CHARSET));
//    }
//
//    @Test
//    public void testSecondResponseHasResponseCodeValid(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertEquals("Wrong Message Code", MessageCode.Name.VALID_203, coapResponse.getMessageCodeName());
//    }
//
//
//    @Test
//    public void testSecondResponseHasNoContent(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertTrue("Expected no content", coapResponse.getContent().readableBytes() == 0);
//    }
//
//    @Test
//    public void testThirdResponseHasCodeChanged(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertEquals("Wrong Message Code, ", MessageCode.Name.CHANGED_204, coapResponse.getMessageCodeName());
//    }
//
//    @Test
//    public void testFourthResponseHasCodeContent(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertEquals("Wrong Message Code, ", MessageCode.Name.CONTENT_205, coapResponse.getMessageCodeName());
//    }
//
//    @Test
//    public void testFourthResponseHasContent(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertTrue("Expected some content ", coapResponse.getContent().readableBytes() > 0);
//    }
//
//    @After
//    public void waitSomeTime() throws InterruptedException {
//        Thread.sleep(100);
//    }
//
//    @AfterClass
//    public static void clear(){
//        testResponseProcessor.clear();
//    }
//
//    public static void setupLogging() throws Exception {
//        AbstractCoapTest.initializeLogging();
//        Logger.getRootLogger().setLevel(Level.ERROR);
//        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.codec").setLevel(Level.INFO);
//    }
//}
