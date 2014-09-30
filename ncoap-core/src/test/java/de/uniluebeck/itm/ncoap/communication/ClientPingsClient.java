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

package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.endpoints.client.ClientTestCallback;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertFalse;

/**
* Created by olli on 08.03.14.
*/
public class ClientPingsClient extends AbstractCoapCommunicationTest{

    private static CoapClientApplication coapClientApplication;
    private static CoapServerApplication coapServerApplication;
    private static ClientTestCallback resetProcessor;


    @Override
    public void setupComponents() throws Exception {
        coapClientApplication = new CoapClientApplication();
        resetProcessor = new ClientTestCallback();
        coapServerApplication = new CoapServerApplication();
    }


    @Override
    public void createTestScenario() throws Exception {
        InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", coapServerApplication.getPort());
        coapClientApplication.sendCoapPing(resetProcessor, serverAddress);


        Thread.sleep(1000);
    }


    @Override
    public void shutdownComponents() throws Exception {
        coapClientApplication.shutdown();
        coapServerApplication.shutdown();
    }


    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.communication.reliability.InboundReliabilityHandler")
              .setLevel(Level.INFO);

        Logger.getLogger("de.uniluebeck.itm.ncoap.endpoints")
              .setLevel(Level.INFO);
    }


    @Test
    public void testResetReceived(){
        assertFalse("No RST (Pong) received.", resetProcessor.getEmptyRSTs().isEmpty());
    }

}
