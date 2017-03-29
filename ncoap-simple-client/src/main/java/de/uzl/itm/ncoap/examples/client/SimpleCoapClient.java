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
package de.uzl.itm.ncoap.examples.client;

import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.examples.client.callback.SimpleCallback;
import de.uzl.itm.ncoap.examples.client.callback.SimpleObservationCallback;
import de.uzl.itm.ncoap.examples.client.config.ClientCmdLineArgumentsWrapper;
import de.uzl.itm.ncoap.examples.client.config.LoggingConfiguration;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;

import java.net.*;


/**
 * This is a simple example application, to showcase how to use the protocol implementation for clients.
 *
 * <b>Examples</b>:<br>
 * <br>
 * 1. To send a single request to <code>coap://example.org:5683/test</code> one can start the {@link SimpleCoapClient}
 * using e.g. the following command line parameters:<br>
 * <br>
 * <code>--host example.org --port 5683 --path /test --non --duration 20</code>
 * <br>
 * <br>
 * This will cause a non-confirmable {@link de.uzl.itm.ncoap.message.CoapRequest} to be sent to the Webservice and either await a single
 * response or 20 seconds to pass (whatever happens first). Then the application is shut down.
 * <b>Note:</b> The default value for parameter <code>--duration</code> is 60!
 * <br><br>
 * 2. To start the observation of <code>coap://example.org:5683/obs</code> one can start the {@link SimpleCoapClient}
 * using e.g. the following command line parameters:
 * <br><br>
 * <code>--host example.org --port 5683 --path /obs --observing --maxUpdates 5 --duration 60</code>
 * <br><br>
 * This will cause a confirmable {@link de.uzl.itm.ncoap.message.CoapRequest} with the observing option set to be sent to the Webservice and
 * either await 5 update notifications or 60 seconds to pass (whatever happens first). If one of this shutdown criteria
 * is satisfied, the application is shut down after another delay of 10 seconds.
 *
 * @author Oliver Kleine
 */
public class SimpleCoapClient extends CoapClient {

    private ClientCmdLineArgumentsWrapper arguments;

    private SimpleCallback callback;

    /**
     * Creates a new instance of {@link SimpleCoapClient}.
     *
     * @param arguments an instance of {@link ClientCmdLineArgumentsWrapper} containing the given command line
     *                  parameters (use --help as parameter to print the supported parameters)
     */
    public SimpleCoapClient(ClientCmdLineArgumentsWrapper arguments) {
        super();
        this.arguments = arguments;
    }

    private void sendCoapRequest() throws URISyntaxException, UnknownHostException {

        // determine the URI of the resource to be requested
        String host = arguments.getUriHost();
        int port = arguments.getUriPort();
        String path = arguments.getUriPath();
        String query = arguments.getUriQuery();
        URI resourceURI = new URI ("coap", null, host, port, path, query, null);

        // create the request
        boolean useProxy = arguments.getProxyAddress() != null;
        int messageType = arguments.isNon() ? MessageType.NON : MessageType.CON;
        CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.GET, resourceURI, useProxy);

        // observe resource or not?
        if (arguments.isObserve()) {
            coapRequest.setObserve(0);
        }

        // determine recipient (proxy or webresource host)
        InetSocketAddress remoteSocket;
        if (useProxy) {
            InetAddress proxyAddress = InetAddress.getByName(arguments.getProxyAddress());
            int proxyPort = arguments.getProxyPort();
            remoteSocket = new InetSocketAddress(proxyAddress, proxyPort);
        } else {
            InetAddress serverAddress = InetAddress.getByName(arguments.getUriHost());
            int serverPort = arguments.getUriPort();
            remoteSocket = new InetSocketAddress(serverAddress, serverPort);
        }

        // define the client callback
        if (arguments.isObserve()) {
            callback = new SimpleObservationCallback(arguments.getMaxUpdates());
        } else {
            callback = new SimpleCallback();
        }

        //Send the CoAP request
        this.sendCoapRequest(coapRequest, remoteSocket, callback);
    }


    private void waitAndShutdown() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while(!isShutdownCriterionSatisfied(startTime)) {
            Thread.sleep(100);
        }

        //Wait for another 10 seconds to answer the next update notification with a RST to stop the observation
        if (arguments.isObserve()) {
            Thread.sleep(10000);
        }

        this.shutdown();
    }


    private boolean isShutdownCriterionSatisfied(long startTime) {

        //Check if maximum duration was reached
        if ((System.currentTimeMillis() - startTime) / 1000 > arguments.getDuration()) {
            return true;
        }

        //Check message count or transmission timeout
        int responseCount = callback.getResponseCount();
        int awaitedResponses = arguments.isObserve() ? arguments.getMaxUpdates() : 1;

        return !(responseCount < awaitedResponses && !callback.isTimedOut());
    }


    /**
     * Creates an instance of {@link SimpleCoapClient} and starts to do whatever was defined using the command line
     * parameters. Use <code>--help</code> as command line parameter (program argument) to print a list of available
     * parameters.
     *
     * @param args use <code>--help</code> as command line parameter (program argument) to print a list of available
     * parameters.
     *
     * @throws Exception if something went terribly wrong
     */
    public static void main(String[] args) throws Exception {
        //Read command line arguments
        ClientCmdLineArgumentsWrapper arguments = new ClientCmdLineArgumentsWrapper(args);

        //Configure logging
        String log4jConfigPath = arguments.getLog4jConfigPath();
        if (log4jConfigPath == null) {
            LoggingConfiguration.configureDefaultLogging();
        } else {
            LoggingConfiguration.configureLogging(log4jConfigPath);
        }

        // Start the client
        SimpleCoapClient client = new SimpleCoapClient(arguments);

        // Send the request
        client.sendCoapRequest();

        // wait for shutdown criterion and shutdown...
        client.waitAndShutdown();
    }
}
