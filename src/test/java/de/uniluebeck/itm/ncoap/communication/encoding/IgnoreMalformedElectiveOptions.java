/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
//package de.uniluebeck.itm.ncoap.communication.encoding;
//
//import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
//import de.uniluebeck.itm.ncoap.application.client.TestResponseProcessor;
//import de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint;
//import de.uniluebeck.itm.ncoap.communication.AbstractCoapCommunicationTest;
//import de.uniluebeck.itm.ncoap.message.CoapRequest;
//import de.uniluebeck.itm.ncoap.message.header.Code;
//import de.uniluebeck.itm.ncoap.message.header.MsgType;
//
//import java.net.URI;
//
///**
// * Created with IntelliJ IDEA.
// * User: olli
// * Date: 12.10.13
// * Time: 13:57
// * To change this template use File | Settings | File Templates.
// */
//public class IgnoreMalformedElectiveOptions extends AbstractCoapCommunicationTest{
//
//    private static CoapTestEndpoint endpoint;
//    private static CoapClientApplication client;
//    private static TestResponseProcessor responseProcessor;
//    private static CoapRequest coapRequest;
//
//    @Override
//    public void setupComponents() throws Exception {
//
//        //Create endpoint
//        endpoint = new CoapTestEndpoint();
//
//        //Create client and response processor
//        client = new CoapClientApplication();
//        responseProcessor = new TestResponseProcessor();
//
//        URI targetUri =  new URI("coap://localhost:" + endpoint.getPort());
//        coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetUri);
//
//    }
//
//    @Override
//    public void createTestScenario() throws Exception {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void shutdownComponents() throws Exception {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void setupLogging() throws Exception {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//}
