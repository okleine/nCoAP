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
package de.uniluebeck.itm.ncoap.etsi.client;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import static de.uniluebeck.itm.ncoap.message.MessageCode.Name.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by olli on 15.09.14.
 */
public class TD_COAP_CORE_03 {

    private static final String SERVER = "coap.me";
    private static final int WAITING_TIME = 2500;

    private static CoapRequest coapRequest;
    private static CoapResponse coapResponse;


    @BeforeClass
    public static void sendRequest() throws Exception{
        System.out.println("*** BEGIN OF TD_COAP_CORE_03 ***");
        LoggingConfiguration.configure();

        CoapClientApplication client = ApplicationFactory.getClient();
        URI targetUri = new URI("coap", null, SERVER, -1, "/test", null, null);
        final InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(SERVER), 5683);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.PUT, targetUri);
        coapRequest.setContent("Arbitrary payload...".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        client.sendCoapRequest(coapRequest, new ClientCallback() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                TD_COAP_CORE_03.coapResponse = coapResponse;
                System.out.println("Response (from " + targetAddress + "): " + coapResponse);

            }
        }, targetAddress);

        Thread.sleep(WAITING_TIME);
    }

    @AfterClass
    public static void waitSomeTime() throws Exception{
        Thread.sleep(100);
        System.out.println("*** END OF TD_COAP_CORE_03 ***");
    }

    @Test
    public void testResponseCode() throws Exception {
        MessageCode.Name actual = coapResponse.getMessageCodeName();
        assertTrue(
                "WRONG RESPONSE CODE (expected:" + CHANGED_204 + " or " + CREATED_201 + ", actual: " + actual + ")",
                actual == CHANGED_204 || actual == CREATED_201
        );
    }

    @Test
    public void testMessageID() throws Exception {
        assertEquals("WRONG MESSAGE ID IN RESPONSE!", coapRequest.getMessageID(), coapResponse.getMessageID());
    }

    @Test
    public void testToken() throws Exception {
        assertEquals("WRONG TOKEN IN RESPONSE!", coapRequest.getToken(), coapResponse.getToken());
    }

    @Test
    public void testPayloadAndContentFormatOption() throws Exception {
        if(coapResponse.getContent().readableBytes() > 0 && coapResponse.getContentFormat() == ContentFormat.UNDEFINED){
            fail("Response contained payload but no content format option!");
        }
    }
}
