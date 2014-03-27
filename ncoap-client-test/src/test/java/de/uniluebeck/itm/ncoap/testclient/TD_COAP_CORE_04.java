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
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 19.11.13
// * Time: 16:52
// * To change this template use File | Settings | File Templates.
// */
//
//import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
//import de.uniluebeck.itm.ncoap.examples.test.client.utils.AbstractCoapTest;
//import de.uniluebeck.itm.ncoap.examples.test.client.utils.CoapTestResponseProcessor;
//import de.uniluebeck.itm.ncoap.message.*;
//import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
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
//
//public class TD_COAP_CORE_04 extends AbstractCoapTest {
//
//    private static CoapClientApplication clientApplication;
//    private static CoapTestResponseProcessor testResponseProcessor = new CoapTestResponseProcessor();
//
//    private static byte[] content = "Payload for Test 04".getBytes(CoapMessage.CHARSET);
//
//    @BeforeClass
//    public static void sendRequest() throws Exception {
//        setupLogging();
//
//        clientApplication = new CoapClientApplication();
//
//        URI targetUri = new URI("coap", null, AbstractCoapTest.TESTSERVER_HOST, AbstractCoapTest.TESTSERVER_PORT,
//                "/test", null, null);
//
//        CoapRequest coapRequest1 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.POST, targetUri);
//        coapRequest1.setContent(content, ContentFormat.Name.TEXT_PLAIN_UTF8);
//        clientApplication.sendCoapRequest(coapRequest1, testResponseProcessor);
//
//        Thread.sleep(3000);
//
//        if(testResponseProcessor.getCoapResponses().isEmpty())
//            return;
//
//
//        CoapResponse coapResponse = testResponseProcessor.getCoapResponses().values().iterator().next();
//        URI locationURI = coapResponse.getLocationURI();
//
//        URI targetUri2 = new URI("coap", null, AbstractCoapTest.TESTSERVER_HOST, AbstractCoapTest.TESTSERVER_PORT,
//            locationURI.getPath(), locationURI.getQuery(), null);
//
//        CoapRequest coapRequest2 = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri2);
//        clientApplication.sendCoapRequest(coapRequest2, testResponseProcessor);
//
//        Thread.sleep(3000);
//    }
//
//    @Test
//    public void testResponseCount(){
//        assertEquals("Wrong number of responses, ", 2, testResponseProcessor.getCoapResponses().size());
//    }
//
//    @Test
//    public void testFirstResponseHasCorrectCode(){
//        CoapResponse coapResponse = testResponseProcessor.getCoapResponses().values().iterator().next();
//
//        MessageCode.Name messageCodeName = coapResponse.getMessageCodeName();
//        assertTrue("Wrong response code (expected CREATED_201 or CHANGED_204 but was " + messageCodeName + ")",
//                messageCodeName == MessageCode.Name.CREATED_201 || messageCodeName == MessageCode.Name.CHANGED_204);
//    }
//
//    @Test
//    public void testFirstResponseHasContentTypeOptionIfThereIsPayload(){
//        CoapResponse coapResponse = testResponseProcessor.getCoapResponses().values().iterator().next();
//        if(coapResponse.getContent().readableBytes() > 0){
//            System.out.println("Content: " + coapResponse.getContent().toString(CoapMessage.CHARSET));
//            assertTrue("Response has payload but no content format option!",
//                    coapResponse.getContentFormat() != ContentFormat.Name.UNDEFINED);
//        }
//    }
//
//
//    @Test
//    public void testResourceWasActuallyCreatedOrModified(){
//        Iterator<CoapResponse> iterator = testResponseProcessor.getCoapResponses().values().iterator();
//        iterator.next();
//
//        CoapResponse coapResponse = iterator.next();
//
//        assertEquals("Resource was not created or updated (wrong payload), ",
//                new String(content, CoapMessage.CHARSET),
//                new String(coapResponse.getContent().toString(CoapMessage.CHARSET)));
//    }
//
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
//
