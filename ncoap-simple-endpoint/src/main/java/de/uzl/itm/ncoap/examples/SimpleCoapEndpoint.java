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
package de.uzl.itm.ncoap.examples;

import de.uzl.itm.ncoap.application.endpoint.CoapEndpoint;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Created by olli on 27.08.15.
 */
public class SimpleCoapEndpoint extends CoapEndpoint {

    public SimpleCoapEndpoint(NotFoundHandler resourceNotFoundHandler, InetSocketAddress localSocket) {
        super(resourceNotFoundHandler, localSocket);
    }


    public void sendRequest(ClientCallback callback) throws Exception {
        URI remoteResource = new URI("coap://coap.me:5683/test");
        CoapRequest request = new CoapRequest(MessageType.CON, MessageCode.GET, remoteResource);

        sendCoapRequest(request, callback, new InetSocketAddress(InetAddress.getByName("coap.me"), 5683));
    }


    public static void main(String[] args) throws Exception{
        LoggingConfiguration.configureDefaultLogging();
        SimpleCoapEndpoint coapPeer = new SimpleCoapEndpoint(NotFoundHandler.getDefault(), new InetSocketAddress(5683));

        // register services
        coapPeer.registerWebresource(new SimpleNotObservableWebresource("/simple", "Some aribtrary content...",
                60, coapPeer.getExecutor()));

        coapPeer.registerWebresource(new SimpleObservableTimeService("/utc-time", 2000, coapPeer.getExecutor()));

        // send some requests...
        ClientCallback callback = new SimpleCallback();

        for(int i = 1; i < 10; i++) {
            coapPeer.sendRequest(callback);
            Thread.sleep(2000);
        }

        coapPeer.shutdown();
    }
}
