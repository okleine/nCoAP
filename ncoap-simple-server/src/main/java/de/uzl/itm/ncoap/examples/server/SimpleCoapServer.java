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
package de.uzl.itm.ncoap.examples.server;


import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.message.options.OptionValue;

/**
 * This is a simple application to showcase ho to use nCoAP for servers
 *
 * @author Oliver Kleine
 */
public class SimpleCoapServer extends CoapServer {

    /**
     * Starts a {@link CoapServer} instance with two {@link de.uzl.itm.ncoap.application.server.resource.Webresource}
     * instances where one is observable and the other one is not.
     *
     * @param args no arguments needed (may be empty)
     * @throws Exception if some unexpected error occurred
     */
    public static void main(String[] args) throws Exception {
        LoggingConfiguration.configureDefaultLogging();

        SimpleCoapServer server = new SimpleCoapServer();

        String status = "This is the status of a simple not observable Web Resource running on a CoAP Server...";
        SimpleNotObservableWebresource simpleWebresource =
                new SimpleNotObservableWebresource("/simple", status, OptionValue.MAX_AGE_MAX, server.getExecutor());
        server.registerWebresource(simpleWebresource);

        SimpleObservableTimeService timeResource = new SimpleObservableTimeService("/utc-time", 5,
                server.getExecutor());
        server.registerWebresource(timeResource);
    }
}
