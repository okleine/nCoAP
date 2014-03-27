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
package de.uniluebeck.itm.ncoap.simpleclient;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;

import java.net.*;


/**
* This is a very simple example application, that sends a confirmable GET request to the URI given as parameter
* args[0] for the main method. It prints out the response on the console and terminates.
*
* @author Oliver Kleine
*/
public class SimpleCoapClient extends CoapClientApplication{

    private CmdLineArgumentsWrapper arguments;

    private SimpleUpdateNotificationProcessor updateNotificationProcessor;

    public SimpleCoapClient(CmdLineArgumentsWrapper arguments){
        super();
        this.arguments = arguments;
    }

    public void sendObservationRequest() throws URISyntaxException, UnknownHostException {

        //Create CoAP request
        URI webserviceURI = new URI ("coap", null, arguments.getUriHost(), arguments.getUriPort(),
                arguments.getUriPath(), arguments.getUriQuery(), null);

        boolean useProxy = arguments.getProxyAddress() != null;

        MessageType.Name messageType = arguments.isNon() ? MessageType.Name.NON : MessageType.Name.CON;

        CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.Name.GET, webserviceURI, useProxy);

        //Determine recipient (proxy or webservice host)
        InetSocketAddress recipient;
        if(useProxy)
            recipient = new InetSocketAddress(InetAddress.getByName(arguments.getProxyAddress()),
                    arguments.getProxyPort());
        else
            recipient = new InetSocketAddress(InetAddress.getByName(arguments.getUriHost()),
                    arguments.getProxyPort());

        //Create the response processor to process the update notifications
        updateNotificationProcessor = new SimpleUpdateNotificationProcessor(arguments.getUpdates());

        //Send the CoAP request
        this.sendCoapRequest(coapRequest, updateNotificationProcessor, recipient);
    }


    public void waitAndShutdown() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        //await the defined number of update notifications
        int responseCount = 0;
        long actualTime = startTime;

        while(responseCount < 1 && (actualTime - startTime) / 1000 < arguments.getDuration()){
            Thread.sleep(100);

            responseCount = updateNotificationProcessor.getUpdateNotificationCount();
            actualTime = System.currentTimeMillis();
        }

        this.shutdown();
    }


    public static void main(String[] args) throws Exception {
        CmdLineArgumentsWrapper arguments = new CmdLineArgumentsWrapper(args);
        LoggingConfiguration.configureLogging(SimpleCoapClient.class, "ncoap-simple-client");

        SimpleCoapClient client = new SimpleCoapClient(arguments);
        try{
            client.sendObservationRequest();
            client.waitAndShutdown();
        }
        catch (Exception ex){
            client.shutdown();
            throw ex;
        }
    }

}
