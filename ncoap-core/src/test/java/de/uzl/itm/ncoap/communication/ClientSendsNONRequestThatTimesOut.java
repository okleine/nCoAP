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
package de.uzl.itm.ncoap.communication;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.communication.dispatching.client.ResponseDispatcher;
import de.uzl.itm.ncoap.communication.reliability.outbound.ClientOutboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.outbound.MessageIDFactory;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.message.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static junit.framework.Assert.assertEquals;

/**
* Tests to verify the server functionality related to piggy-backed responses.
*
* @author Stefan Hueske, Oliver Kleine
*/
public class ClientSendsNONRequestThatTimesOut extends AbstractCoapCommunicationTest {

    private static Logger LOG = Logger.getLogger(ClientSendsNONRequestThatTimesOut.class.getName());

    private static CoapClient client;
    private static TestCallback callback;
    private static CoapRequest request;


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(ClientSendsNONRequestThatTimesOut.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ClientOutboundReliabilityHandler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(MessageIDFactory.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(ResponseDispatcher.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(TestCallback.class.getName()).setLevel(Level.DEBUG);

    }

    @Override
    public void setupComponents() throws Exception {
        client = new CoapClient();
        callback = new TestCallback();
        URI targetUri = new URI("coap://localhost/");
        request = new CoapRequest(MessageType.NON, MessageCode.GET, targetUri);
    }

    @Override
    public void shutdownComponents() throws Exception {
        client.shutdown();
    }

    @Override
    public void createTestScenario() throws Exception {

//                Client
//                  |
//              (1) |--------GET (NON)----------->        there is no server ...
//                  |
//                  |
//             timeout (after 247 seconds)


        LOG.warn("*****THIS TEST TAKES MORE THAN 4 MINUTES******");

        //send request to testServer
        client.sendCoapRequest(request, new InetSocketAddress("localhost", 5683), callback);

        //wait some time for response from server
        Thread.sleep(250000);
    }

    @Test
    public void testClientReceivedTimeout() {
        assertEquals("Client received wrong number of timeouts.", 1, callback.getTransmissionTimeouts().size());
    }

}
