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
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.message.options.OptionValue;

import java.util.concurrent.ScheduledExecutorService;

/**
 * This is a simple application to showcase how to use nCoAP for servers
 *
 * @author Oliver Kleine
 */
public class SimpleCoapServer extends CoapServer {

    private void registerSimpleNotObservableWebresource() {
        // create initial status (one string per paragraph)
        String[] status = new String[10];
        for (int i = 0; i < status.length; i++) {
            status[i] = "This is paragraph #" + (i + 1);
        }

        // register resource at server
        this.registerWebresource(new SimpleNotObservableWebresource(
                "/simple", status, OptionValue.MAX_AGE_MAX, this.getExecutor()
        ));
    }

    private void registerSimpleObservableTimeResource() {
        // register resource at server
        this.registerWebresource(new SimpleObservableTimeService("/utc-time", 5, this.getExecutor()));
    }

    /**
     * Creates a new instance of {@link SimpleCoapServer}
     * @param block1Size the preferred, i.e. max. {@link BlockSize} for requests
     * @param block2Size the preferred, i.e. max {@link BlockSize} for responses
     */
    public SimpleCoapServer(BlockSize block1Size, BlockSize block2Size) {
        super(block1Size, block2Size);
    }

    /**
     * Starts a {@link CoapServer} instance with two {@link de.uzl.itm.ncoap.application.server.resource.Webresource}
     * instances where one is observable and the other one is not.
     *
     * @param args no arguments needed (may be empty)
     * @throws Exception if some unexpected error occurred
     */
    public static void main(String[] args) throws Exception {
        // configure logging
        LoggingConfiguration.configureDefaultLogging();

        // create server and register resources
        SimpleCoapServer server = new SimpleCoapServer(BlockSize.SIZE_64, BlockSize.SIZE_64);
        server.registerSimpleNotObservableWebresource();
        server.registerSimpleObservableTimeResource();
    }
}
