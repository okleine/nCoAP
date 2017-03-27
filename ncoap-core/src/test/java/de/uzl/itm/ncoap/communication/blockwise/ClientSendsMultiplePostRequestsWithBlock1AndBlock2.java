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

import com.google.common.base.Strings;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.communication.AbstractCoapCommunicationTest;
import de.uzl.itm.ncoap.endpoints.client.TestCallback;
import de.uzl.itm.ncoap.endpoints.server.NotObservableTestWebresourceForPost;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.assertEquals;


/**
 * Created by olli on 07.04.16.
 */
public class ClientSendsMultiplePostRequestsWithBlock1AndBlock2 extends AbstractCoapCommunicationTest {

    private static final int NUMBER_OF_PARALLEL_REQUESTS = 10;

    // server components
    private static CoapServer coapServer;
    private static NotObservableTestWebresourceForPost webresource;
    private static BlockSize serverMaxBlock1Size = BlockSize.SIZE_16;
    private static BlockSize serverMaxBlock2Size = BlockSize.SIZE_128;

    // client components
    private static CoapClient coapClient;
    private static TestCallback[] clientCallbacks = new TestCallback[NUMBER_OF_PARALLEL_REQUESTS];

    // request components
    private static CoapRequest[] coapRequests = new CoapRequest[NUMBER_OF_PARALLEL_REQUESTS];
    private static BlockSize initialRequestBlock1Size = BlockSize.SIZE_64;
    private static BlockSize initialRequestBlock2Size = BlockSize.SIZE_32;

    //private static byte[] capitals = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(CoapMessage.CHARSET);

    @Override
    public void setupComponents() throws Exception {
        // setup server
        coapServer = new CoapServer(serverMaxBlock1Size, serverMaxBlock2Size);
        webresource = new NotObservableTestWebresourceForPost("/test", "", 0, coapServer.getExecutor());
        coapServer.registerWebresource(webresource);

        // setup client
        coapClient = new CoapClient();

        // setup request
        URI targetURI = new URI("coap://localhost:5683/test");

        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            coapRequests[i] = new CoapRequest(MessageType.CON, MessageCode.POST, targetURI);
            coapRequests[i].setPreferredBlock1Size(initialRequestBlock1Size);
            coapRequests[i].setPreferredBlock2Size(initialRequestBlock2Size);

            // payload length = 5 x 26 = 130
            String p = Strings.padStart("" + (i + 1), 4, '0');
            for(int j = 0; j < 26; j++) {
                p = p + '|' + Strings.padStart("" + (i + 1), 4, '0');;
            }
            ChannelBuffer payload = ChannelBuffers.wrappedBuffer(p.getBytes(CoapMessage.CHARSET));
            coapRequests[i].setContent(payload, ContentFormat.TEXT_PLAIN_UTF8);

            // setup callback
            clientCallbacks[i] = new TestCallback(i + 1);
        }
    }

    @Override
    public void createTestScenario() throws Exception {
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            coapClient.sendCoapRequest(coapRequests[i], new InetSocketAddress("localhost", 5683), clientCallbacks[i]);
        }
        Thread.sleep(10000);
    }

    @Override
    public void shutdownComponents() throws Exception {
        coapClient.shutdown();
        coapServer.shutdown();
    }

    @Override
    public void setupLogging() throws Exception {
//        Logger.getLogger(ClientBlock1Handler.class.getName()).setLevel(Level.DEBUG);
//        Logger.getLogger(ClientBlock2Handler.class.getName()).setLevel(Level.DEBUG);
//        Logger.getLogger(ServerBlock1Handler.class.getName()).setLevel(Level.DEBUG);
//        Logger.getLogger(ServerBlock2Handler.class.getName()).setLevel(Level.DEBUG);
        Logger.getLogger(TestCallback.class.getName()).setLevel(Level.DEBUG);
    }

    @Test
    public void allCallbacksReceivedOneResponse() {
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            String message = "Client " + i + " did not receive 1 response";
            assertEquals(message, 1, clientCallbacks[i].getCoapResponses().size());
        }
    }

    @Test
    public void allResponseHaveCodeChanged() {
        String expectedCode = MessageCode.asString(MessageCode.CHANGED_204);
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            String message = "Client " + i + " received wrong response code";
            CoapResponse coapResponse = clientCallbacks[i].getCoapResponses().values().iterator().next();
            assertEquals(message, expectedCode, coapResponse.getMessageCodeName());
        }
    }

    @Test
    public void allCallbacksReceivedResponseBlockReceviedEvents() {
        // total payload length: 130 bytes => 5 blocks (32 byte) => 4 events (last block does not cause this event)
        for(int i= 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            String message = "Client " + i + " received wrong number of events";
            assertEquals(message, 4, clientCallbacks[i].getResponseBlockReceptions().size());
        }
    }

    @Test
    public void allCallbacksReceivedContinueResponseEvents() {
        // total payload length: 130 bytes
        // payload blocks (late negotiation): 64 + 16 + 16 + 16 + 16 + 2 = 130 (i.e. 6 blocks => 5 events)

        for(int i= 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            String message = "Client " + i + " received wrong number of events";
            assertEquals(message, 5, clientCallbacks[i].getRequestBlockDeliveryConfirmations().size());
        }
    }
}
