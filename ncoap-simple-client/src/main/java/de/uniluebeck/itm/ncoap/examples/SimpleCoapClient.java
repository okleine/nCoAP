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
package de.uniluebeck.itm.ncoap.examples;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.linkformat.LinkFormatDecoder;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LinkAttribute;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.message.*;

import java.net.*;
import java.util.Map;
import java.util.Set;


/**
 * This is a simple example application, to showcase how to use the protocol implementation for clients.
 *
 * <b>Examples</b>:<br>
 * <br>
 * 1. To send a single request to <code>coap://example.org:5683/test</code> one can start the {@link SimpleCoapClient}
 * using e.g. the following command line parameters:<br>
 * <br>
 * <code>--host example.org --port 5683 --path /test -non --duration 20</code>
 * <br>
 * <br>
 * This will cause a non-confirmable {@link CoapRequest} to be sent to the Webservice and either await a single
 * response or 20 seconds to pass (whatever happens first). Then the application is shut down.
 * <br><br>
 * 2. To start the observation of <code>coap://example.org:5683/obs</code> one can start the {@link SimpleCoapClient}
 * using e.g. the following command line parameters:
 * <br><br>
 * <code>--host example.org --port 5683 --path /obs --observing --maxUpdates 5 --duration 60</code>
 * <br><br>
 * This will cause a confirmable {@link CoapRequest} with the observing option set to be sent to the Webservice and
 * either await 5 update notifications or 60 seconds to pass (whatever happens first). If one of this shutdown criteria
 * is satisfied, the application is shut down after another delay of 10 seconds.
 *
 * @author Oliver Kleine
 */
public class SimpleCoapClient extends CoapClientApplication{

    private ClientCmdLineArgumentsWrapper arguments;

    private SimpleCallback responseProcessor;

    /**
     * Creates a new instance of {@link SimpleCoapClient}.
     *
     * @param arguments an instance of {@link ClientCmdLineArgumentsWrapper} containing the given command line
     *                  parameters (use --help as parameter to print the supported parameters)
     */
    public SimpleCoapClient(ClientCmdLineArgumentsWrapper arguments){
        super();
        this.arguments = arguments;
    }

    private void sendRequest() throws URISyntaxException, UnknownHostException {

        //Create CoAP request
        URI webserviceURI = new URI ("coap", null, arguments.getUriHost(), arguments.getUriPort(),
                arguments.getUriPath(), arguments.getUriQuery(), null);

        boolean useProxy = arguments.getProxyAddress() != null;

        MessageType.Name messageType = arguments.isNon() ? MessageType.Name.NON : MessageType.Name.CON;

        CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.Name.GET, webserviceURI, useProxy);

        //Observe or not?
        if(arguments.isObserve())
            coapRequest.setObserve(0);

        //Determine recipient (proxy or webservice host)
        InetSocketAddress recipient;
        if(useProxy)
            recipient = new InetSocketAddress(InetAddress.getByName(arguments.getProxyAddress()),
                    arguments.getProxyPort());
        else
            recipient = new InetSocketAddress(InetAddress.getByName(arguments.getUriHost()),
                    arguments.getProxyPort());

        //Create the response processor
        if(arguments.isObserve())
            responseProcessor = new SimpleObservationCallback(arguments.getMaxUpdates());
        else
            responseProcessor = new SimpleCallback();

        //Send the CoAP request
        this.sendCoapRequest(coapRequest, responseProcessor, recipient);
    }


    private void waitAndShutdown() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while(!isShutdownCriterionSatisfied(startTime)){
            Thread.sleep(100);
        }

        //Wait for another 10 seconds to answer the next update notification with a RST to stop the observation
        if(arguments.isObserve())
            Thread.sleep(10000);

        this.shutdown();
    }


    private boolean isShutdownCriterionSatisfied(long startTime){

        //Check if maximum duration was reached
        if((System.currentTimeMillis() - startTime) / 1000 > arguments.getDuration())
            return true;

        //Check message count or transmission timeout
        int responseCount = responseProcessor.getResponseCount();
        int awaitedResponses = arguments.isObserve() ? arguments.getMaxUpdates() : 1;

        return !(responseCount < awaitedResponses && !responseProcessor.isTimedOut());
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
        if(log4jConfigPath == null)
            LoggingConfiguration.configureDefaultLogging();
        else
            LoggingConfiguration.configureLogging(log4jConfigPath);

        //Start the client
        final SimpleCoapClient client = new SimpleCoapClient(arguments);


//        for(int i = 0; i < 10; i++) {
//            final int finalI = i;
//            client.sendCoapPing(new ClientCallback() {
//                @Override
//                public void processCoapResponse(CoapResponse coapResponse) {
//                    System.out.println("Response " + finalI + "!");
//                }
//
//                @Override
//                public void processReset() {
//                    System.out.println("PONG " + finalI + "!");
//                }
//
//                @Override
//                public void processMiscellaneousError(String description){
//                    System.out.println(description);
//                }
//
//            }, new InetSocketAddress("134.102.218.16", 5683));
//        }

//        Thread.sleep(2000);
//        client.shutdown();

       //Send the request, await the response (or timeout) and shut the client down
//        try{
//            client.sendRequest();
//            client.waitAndShutdown();
//        }
//        catch (Exception ex){
//            client.shutdown();
//            throw ex;
//        }

        URI uri = new URI("coap", null, "coap.me", 5683, "/.well-known/core", null, null);
        CoapRequest request = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, uri);

        client.sendCoapRequest(request, new ClientCallback() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                String result = coapResponse.getContent().toString(CoapMessage.CHARSET);
                System.out.println(result);

                Map<String, Set<LinkAttribute>> attributes = LinkFormatDecoder.decode(result);

                for(String service : attributes.keySet()){
                    System.out.println(service);
                    for(LinkAttribute attribute : attributes.get(service)){
                        System.out.println("   " + attribute);
                    }
                }
            }
        }, new InetSocketAddress("coap.me", 5683));

        Thread.sleep(5000);
        client.shutdown();



    }
}
