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
package de.uniluebeck.itm.ncoap.communication.observe;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.endpoints.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.endpoints.server.ObservableTestWebservice;
import de.uniluebeck.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Created by olli on 18.03.14.
 */
public class TestResourceStatusChangeDuringRetransmission extends AbstractCoapCommunicationTest{

    private static CoapServerApplication coapServerApplication;
    private static CoapTestEndpoint client;

    private static CoapRequest coapRequest;


    @Override
    public void setupComponents() throws Exception {
        coapServerApplication = new CoapServerApplication();
        coapServerApplication.registerService(new ObservableTestWebservice("/observable", 0, 0, 5000));

        client = new CoapTestEndpoint();

        URI targetUri = new URI("coap", null, "localhost", 5683, "/observable", null, null);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        coapRequest.setObserve(true);

    }

    @Override
    public void createTestScenario() throws Exception {
        client.writeMessage(coapRequest, new InetSocketAddress("localhost", 5683));

        Thread.sleep(70000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapServerApplication.shutdown();
        client.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.outgoing").setLevel(Level.DEBUG);
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.observe").setLevel(Level.DEBUG);
    }

    @Test
    public void test(){

    }
}
