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
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.*;

/**
 * Created by olli on 15.09.14.
 */
public class TD_COAP_CORE_09 {

    private static final String SERVER = "coap.me";
    private static final int WAITING_TIME = 8000;

    private static CoapRequest coapRequest;
    private static CoapMessage emptyAck;
    private static CoapResponse coapResponse;


    @BeforeClass
    public static void sendRequest() throws Exception{
        LoggingConfiguration.configure();

        CoapClientApplication client = ApplicationFactory.getCoapClientApplication();
        URI targetUri = new URI("coap", null, SERVER, -1, "/separate", null, null);
        final InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(SERVER), 5683);

        coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);
        client.sendCoapRequest(coapRequest, new EmptyAcknowledgementProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                TD_COAP_CORE_09.coapResponse = coapResponse;
                System.out.println("Response (from " + targetAddress + "): " + coapResponse);
            }

            @Override
            public void processEmptyAcknowledgement(InetSocketAddress remoteEndpoint, int messageID, Token token) {
                TD_COAP_CORE_09.emptyAck = CoapMessage.createEmptyAcknowledgement(messageID);
                System.out.println("Empty ACK (from " + targetAddress + "): " + emptyAck);
            }

        }, targetAddress);

        Thread.sleep(WAITING_TIME);
    }


    @Test
    public void testEmptyAckMessageID(){
        assertTrue("NO EMPTY ACK RECEIVED!", emptyAck != null);
        assertEquals("WRONG MESSAGE ID IN EMPTY ACK!", coapRequest.getMessageID(), emptyAck.getMessageID());
    }


    @Test
    public void testResponseCode() throws Exception {
        assertEquals("WRONG RESPONSE CODE!", MessageCode.Name.CONTENT_205, coapResponse.getMessageCodeName());
    }


    @Test
    public void testToken() throws Exception {
        assertEquals("WRONG TOKEN IN RESPONSE!", coapRequest.getToken(), coapResponse.getToken());
    }


    @Test
    public void testPayload() throws Exception {
        if(coapResponse.getContent().readableBytes() < 1){
            fail("Response did not contain any payload!");
        }
    }


    @Test
    public void testContentFormatOption() throws Exception {
        String message = "NO CONTENT FORMAT OPTION IN RESPONSE!";
        assertNotEquals(message, coapResponse.getContentFormat(), ContentFormat.UNDEFINED);
    }
}
