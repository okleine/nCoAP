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
package de.uzl.itm.ncoap.communication.blockwise;

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1Handler;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;


import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.assertEquals;


/**
 * Created by olli on 07.04.16.
 */
public class ClientSendsRequestWithBlock1ToETHZ extends AbstractCoapCommunicationTest {

    private static CoapClient coapClient;
    private static CoapRequest coapRequest;
    private static TestCallback clientCallback;

    @Override
    public void setupComponents() throws Exception {
        coapClient = new CoapClient("Coap Client (BLOCK 1)");
        URI targetURI = new URI("coap://vs0.inf.ethz.ch:5683/large-post");
        coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, targetURI);
        coapRequest.setPreferredBlock1Size(BlockSize.SIZE_16);
        byte[] payload = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(CoapMessage.CHARSET);
        coapRequest.setContent(payload, ContentFormat.TEXT_PLAIN_UTF8);
        clientCallback = new TestCallback();
    }

    @Override
    public void createTestScenario() throws Exception {
        coapClient.sendCoapRequest(coapRequest, new InetSocketAddress("vs0.inf.ethz.ch", 5683), clientCallback);
        Thread.sleep(5000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapClient.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger(ClientBlock1Handler.class.getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void testReceiverReceivedTwoMessages() {
        String message = "Client did not receive 1 response";
        assertEquals(message, 1, clientCallback.getCoapResponses().size());
    }
}
