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
package de.uzl.itm.ncoap.communication.codec;

import de.uzl.itm.ncoap.AbstractCoapTest;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.Option;
import de.uzl.itm.ncoap.message.options.OptionValue;
import de.uzl.itm.ncoap.message.options.StringOptionValue;

import de.uzl.itm.ncoap.message.options.UintOptionValue;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URI;

/**
 * Created by olli on 02.08.16.
 */

public class AllowDefaultOptionValues extends AbstractCoapTest {

    private static String HOST = "10.20.30.40";
    private static byte[] HOST_ENCODED = HOST.getBytes(CoapMessage.CHARSET);

    @Override
    public void setupLogging() throws Exception {

    }

    @Test
    public void setIpAddressAsHostOption() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        coapRequest.addOption(Option.URI_HOST, new StringOptionValue(Option.URI_HOST, HOST_ENCODED, true));

        assertEquals("Unexpected value for HOST option: ", HOST, coapRequest.getUriHost());
    }

    @Test (expected = IllegalArgumentException.class)
    public void setIpAddressAsHostOption2() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        coapRequest.addOption(Option.URI_HOST, new StringOptionValue(Option.URI_HOST, HOST_ENCODED));

        // this should not be executed as the previous statement is supposed to throw an exception
        assertTrue(false);
    }

    @Test
    public void setDefaultPortAsPortOption() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        UintOptionValue value = new UintOptionValue(Option.URI_PORT, OptionValue.ENCODED_URI_PORT_DEFAULT, true);
        coapRequest.addOption(Option.URI_PORT, value);

        assertEquals("Unexpected value for PORT option: ", OptionValue.URI_PORT_DEFAULT, coapRequest.getUriPort());
    }

    @Test (expected = IllegalArgumentException.class)
    public void setDefaultPortAsPortOption2() throws Exception{
        URI webserviceURI = new URI("coap://" + HOST + ":" + OptionValue.URI_PORT_DEFAULT + "/registry");
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, webserviceURI, false);
        coapRequest.addOption(Option.URI_PORT, new UintOptionValue(Option.URI_PORT,
                OptionValue.ENCODED_URI_PORT_DEFAULT, false));

        // this should not be executed as the previous statement is supposed to throw an exception
        assertTrue(false);
    }
}
