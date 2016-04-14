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
package de.uzl.itm.ncoap.message;

import com.google.common.collect.Lists;
import de.uzl.itm.ncoap.AbstractCoapTest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 10.11.13
 * Time: 23:34
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Parameterized.class)
public class CoapRequestCreationTest extends AbstractCoapTest{

    @Parameterized.Parameters(name = "[{index}] Message Type: {0}, Message Code: {1}")
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                new Object[]{MessageType.CON,
                             MessageCode.GET,
                             new URI("coap", null, "www.example.org", -1, "/path/to/service", null, null),
                             null,
                             12345,
                             new byte[]{1, 2, 3, 4, 5, 6, 7, 8}},

                new Object[]{MessageType.CON,
                             MessageCode.GET,
                             new URI("coap", null, "www.example.org", -1, "/path/to/service", null, null),
                             InetAddress.getByName("2001:1:2:3:4:5:6:7"),
                             65535,
                             new byte[0]}
        );
    }

    private Logger log = Logger.getLogger(this.getClass().getName());

    private final int messageType;
    private final int messageCode;
    private final URI targetUri;
    private final InetAddress proxyAddress;
    private final int messageID;
    private final byte[] token;
    private CoapRequest coapRequest;

    public CoapRequestCreationTest(int messageType, int messageCode, URI targetUri,
                                   InetAddress proxyAddress, int messageID, byte[] token) throws Exception {

        this.messageType = messageType;
        this.messageCode = messageCode;
        this.targetUri = targetUri;
        this.proxyAddress = proxyAddress;
        this.messageID = messageID;
        this.token = token;

        log.debug("Create CoAP Request: (Type) " + messageType + ", (Code) " + messageCode);
        log.debug("Create CoAP Request: (Type) " + messageType + ", (Code) " + messageCode);
        this.coapRequest = new CoapRequest(messageType, messageCode, targetUri);


    }

    @Test
    public void testMessageTypeName() {
        assertEquals("Message types do not match, ", coapRequest.getMessageType(), messageType);
    }

    @Test
    public void testMessageType() {
        assertEquals("Message type numbers do not match, ", coapRequest.getMessageType(), messageType);
    }

    @After
    public void justWaitSomeTimeToCompleteLogging() throws InterruptedException {
        Thread.sleep(100);
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.message").setLevel(Level.DEBUG);
    }
}
