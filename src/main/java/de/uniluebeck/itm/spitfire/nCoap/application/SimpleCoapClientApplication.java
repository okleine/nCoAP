/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uniluebeck.itm.spitfire.nCoap.application;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.URI;
import java.nio.charset.Charset;


/**
 * This is a very simple example application, that sends a confirmable GET request to the URI given as parameter
 * args[0] for the main method. It prints out the reponse payload on the console.
 *
 * @author Oliver Kleine
 */
public class SimpleCoapClientApplication extends CoapClientApplication {

    private static Logger log = LoggerFactory.getLogger(SimpleCoapClientApplication.class.getName());

    private String name;

    public SimpleCoapClientApplication(String name){
        this.name = name;
    }

    /**
     * The method to be called by the ResponseCallbackHandler when there was response reveived (both, piggy-backed
     * or seperated).
     *
     * @param coapResponse the response message
     */
    @Override
    public void receiveCoapResponse(CoapResponse coapResponse) {
        System.out.println("[" + name + "] response received.");
        System.out.println("[" + name + "] Response payload: " +
                new String(coapResponse.getPayload().array(), Charset.forName("UTF-8")));
    }

    /**
     * The main method to start the SimpleApplication
     * @param args args[0] must contain the target URI for the GET request
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        SimpleCoapClientApplication app = new SimpleCoapClientApplication("ClientApp");
        URI uri = new URI(args[0]);
        CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, uri, app);
        app.writeCoapRequest(coapRequest);
    }
}
