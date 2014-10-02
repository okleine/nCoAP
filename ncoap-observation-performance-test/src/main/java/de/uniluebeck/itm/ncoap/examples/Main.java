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
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.xml.DOMConfigurator;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by olli on 23.09.14.
 */
public class Main {

    private static final int PARALLELITY = 200;
    private static final int DURATION = 20;

    private CoapClientApplication client;
    private Map<Integer, PerformanceTestClientCallback> callbacks;
    private CoapServerApplication server;

    private static void configureLogging() throws Exception{
        System.out.println("Use default logging configuration, i.e. INFO level...\n");
        URL url = Main.class.getClassLoader().getResource("log4j.default.xml");
        System.out.println("Use config file " + url);
        DOMConfigurator.configure(url);
    }


    public Main(){
        this.callbacks = new HashMap<>();
        this.client = new CoapClientApplication();
        this.server = new CoapServerApplication();
    }


    private void checkNotifications(){
        for(int i = 0; i < PARALLELITY; i++){
            PerformanceTestClientCallback callback = this.callbacks.get(i);
            int notificationCount = callback.getUpdateNotifications().values().size();
            System.out.println("Callback #" + String.format("%03d", i) + " received " +
                    notificationCount + " notifications:");

            System.out.println("First: " + callback.getFirstStatus());
            System.out.println("Last: " + callback.getLastStatus());

            Set<Integer> missing = callback.getMissingStates();
            System.out.print("Missing (" + missing.size() + "): ");
            for(int status : callback.getMissingStates()){
                System.out.print(status + ", ");
            }
            System.out.println("\n");

        }
    }

    public static void main(String[] args) throws Exception{
        configureLogging();

        Main main = new Main();
        InetSocketAddress serverSocket = new InetSocketAddress("localhost", main.server.getPort());


        for(int i = 0; i < PARALLELITY; i++){
            PerformanceTestWebservice service = new PerformanceTestWebservice(i, 0, 50, main.server.getExecutor());
            main.server.registerService(service);
            service.initialize();
        }

        for(int i = 0; i < PARALLELITY; i++){
            URI uri = new URI("coap", null, "localhost", -1, "/service/" + String.format("%03d", i), null, null);
            CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, uri);
            coapRequest.setObserve(0);

            if(i % 2 == 0){
                coapRequest.setAccept(ContentFormat.TEXT_PLAIN_UTF8);
            }
            else{
                coapRequest.setAccept(ContentFormat.APP_XML);
            }

            PerformanceTestClientCallback callback = new PerformanceTestClientCallback(i);
            main.callbacks.put(i, callback);

            main.client.sendCoapRequest(coapRequest, callback, serverSocket);
        }

        Thread.sleep(DURATION * 1000);

        main.client.shutdown();
        main.server.shutdown();

        Thread.sleep(1000);
        main.checkNotifications();
    }

}
