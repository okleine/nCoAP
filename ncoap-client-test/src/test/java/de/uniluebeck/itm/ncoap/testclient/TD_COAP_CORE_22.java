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
//import java.util.Arrays;
//import java.util.Iterator;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//
///**
//* Created with IntelliJ IDEA.
//* User: olli
//* Date: 19.11.13
//* Time: 13:55
//* To change this template use File | Settings | File Templates.
//*/
//public class TD_COAP_CORE_22 extends AbstractCoapTest {
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
//        //Get ETAG of the second response
//        CoapResponse coapResponse1 = testResponseProcessor.getCoapResponses().values().iterator().next();
//        byte[] etag1 = coapResponse1.getEtag();
//
//        if(etag1 == null)
//            return;
//
//        //Write PUT to update the resource
//        CoapRequest coapRequest2 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.PUT, targetUri);
//        coapRequest2.setIfMatch(etag1);
//        coapRequest2.setContent("Some arbitrary new content...".getBytes(CoapMessage.CHARSET),
//                ContentFormat.Name.TEXT_PLAIN_UTF8);
//        clientApplication.sendCoapRequest(coapRequest2, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        //Send GET to retrieve the new ETAG
//        CoapRequest coapRequest3 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
//        clientApplication.sendCoapRequest(coapRequest3, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        //Get ETAG from third response
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse3 = iterator.next();
//        byte[] etag2 = coapResponse3.getEtag();
//
//
//        //Update the resource to make it have a new ETAG
//        CoapRequest coapRequest4 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.PUT, targetUri);
//        coapRequest4.setContent("Some even newer arbitrary content...".getBytes(CoapMessage.CHARSET),
//                ContentFormat.Name.TEXT_PLAIN_UTF8);
//        clientApplication.sendCoapRequest(coapRequest4, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        CoapRequest coapRequest5 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.PUT, targetUri);
//        coapRequest5.setIfMatch(etag2);
//        coapRequest5.setContent("The absolutely latest arbitrary content...".getBytes(CoapMessage.CHARSET),
//                ContentFormat.Name.TEXT_PLAIN_UTF8);
//        clientApplication.sendCoapRequest(coapRequest5, testResponseProcessor);
//
//        Thread.sleep(3000);
//    }
//
//    @Test
//    public void testResponseCount(){
//        assertEquals("Wrong number of responses, ", 5, testResponseProcessor.getCoapResponses().size());
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
//    public void testFirstResponseHasResponseCodeContent(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//
//        CoapResponse coapResponse = iterator.next();
//        assertEquals("Wrong Message Code", MessageCode.Name.CONTENT_205, coapResponse.getMessageCodeName());
//
//        System.out.println("Content: " + coapResponse.getContent().toString(CoapMessage.CHARSET));
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
//
//    @Test
//    public void testSecondResponseHasResponseCodeChanged(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertEquals("Wrong Message Code", MessageCode.Name.CHANGED_204, coapResponse.getMessageCodeName());
//    }
//
//    @Test
//    public void testSecondResponseHasContentFormatOptionIfThereIsContent(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        if(coapResponse.getContent().readableBytes() > 0)
//            assertTrue("Content but no Content Format Option",
//                    coapResponse.getContentFormat() != ContentFormat.Name.UNDEFINED);
//    }
//
//
//    @Test
//    public void testThirdResponseHasCodeContent(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//        assertEquals("Wrong Message Code, ", MessageCode.Name.CONTENT_205, coapResponse.getMessageCodeName());
//    }
//
//    @Test
//    public void testThirdResponseHasOtherEtagThanFirstResponse(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        CoapResponse coapRespsonse1 = iterator.next();
//
//        iterator.next();
//
//        CoapResponse coapResponse3 = iterator.next();
//        assertFalse("ETAG did not change!, ", Arrays.equals(coapRespsonse1.getEtag(), coapResponse3.getEtag()));
//    }
//
//    @Test
//    public void testThirdResponseHasContentThatWasSentWithSecondRequest(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse3 = iterator.next();
//
//        assertEquals("Wrong content, ",
//                "Some arbitrary new content...", coapResponse3.getContent().toString(CoapMessage.CHARSET));
//    }
//
//    @Test
//    public void testFifthResponseHasCodePreconditionFailed(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//        iterator.next();
//        iterator.next();
//        iterator.next();
//
//        CoapResponse coapResponse5 = iterator.next();
//        assertEquals("Wrong Message Code, ",
//                MessageCode.Name.PRECONDITION_FAILED_412, coapResponse5.getMessageCodeName());
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
