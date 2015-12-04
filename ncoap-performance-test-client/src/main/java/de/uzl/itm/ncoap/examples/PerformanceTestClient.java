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

import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.communication.reliability.outbound.MessageIDFactory;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.xml.DOMConfigurator;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by olli on 11.09.15.
 */
public class PerformanceTestClient extends CoapClient {

    public static final int NUMBER_OF_PARALLEL_REQUESTS = 100;
    public static final int INTERVAL_SIZE = 1000;

    public static String SERVER_NAME = "141.83.157.155";
    public static URI TARGET_URI;
    static {
        try {
            TARGET_URI = new URI("coap://" + SERVER_NAME + "/performance");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    public static InetSocketAddress SERVER_SOCKET = new InetSocketAddress(SERVER_NAME, 5683);

    private static String INTERVAL_MESSAGE = "Received Response #%d (Duration (millis): %d (overall), %d (latest %d))";

    private AtomicInteger requests;
    private AtomicInteger openRequests;
    private AtomicInteger responses;
    private AtomicInteger[] retransmissions;
    private AtomicInteger timeouts;

    private AtomicLong overallStart;
    private AtomicLong intervalStart;

    private AtomicBoolean waitForMessageID;

    public PerformanceTestClient() {
        this.requests = new AtomicInteger(0);
        this.openRequests = new AtomicInteger(0);
        this.responses = new AtomicInteger(0);
        this.retransmissions = new AtomicInteger[]{
                new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0)
        };
        this.timeouts = new AtomicInteger(0);
        this.waitForMessageID = new AtomicBoolean(false);

        this.overallStart = new AtomicLong(System.currentTimeMillis());
        this.intervalStart = new AtomicLong(System.currentTimeMillis());

    }


    public void setWaitForMessageID(boolean wait) {
        this.waitForMessageID.set(wait);
    }

    public boolean isWaiting() {
        return this.waitForMessageID.get();
    }


    public void startPerformanceTest() throws Exception {
        for(int i = 0; i < NUMBER_OF_PARALLEL_REQUESTS; i++) {
            sendNextRequest();
        }
    }

    private void increaseResponses(int retransmissions){
        int responseNo = this.responses.incrementAndGet();
        long actualTime = System.currentTimeMillis();
        if(responseNo % INTERVAL_SIZE == 0) {
            long intervalDuration = actualTime - this.intervalStart.get();
            long overallDuration = actualTime - this.overallStart.get();
            System.out.println(String.format(
                    INTERVAL_MESSAGE, responseNo, overallDuration, intervalDuration, INTERVAL_SIZE
            ));
            this.intervalStart.set(actualTime);
        }
        this.retransmissions[retransmissions].incrementAndGet();
        this.openRequests.decrementAndGet();
    }


    private void increaseTimeouts(){
        System.out.println("Request Transmission Timeout #" + this.timeouts.incrementAndGet());
        this.openRequests.decrementAndGet();
    }


    private void sendNextRequest(){
        getExecutor().schedule(new Runnable() {

            @Override
            public void run() {
                setWaitForMessageID(false);
                requests.incrementAndGet();
                openRequests.incrementAndGet();
                CoapRequest coapRequest = new CoapRequest(MessageType.NON, MessageCode.GET, TARGET_URI);
                sendCoapRequest(coapRequest, SERVER_SOCKET, new PerformanceTestCallback());
            }
        }, this.isWaiting() ? MessageIDFactory.EXCHANGE_LIFETIME : 0, TimeUnit.SECONDS);
    }


    private void decreaseRequests(){
        this.requests.decrementAndGet();
    }


    public static void configureDefaultLogging() throws Exception{
        System.out.println("Use default logging configuration, i.e. INFO level...\n");
        URL url = PerformanceTestClient.class.getClassLoader().getResource("log4j.default.xml");
        System.out.println("Use config file " + url);
        DOMConfigurator.configure(url);
    }


    public static void main(String[] args) throws Exception{
        configureDefaultLogging();
        PerformanceTestClient client = new PerformanceTestClient();
        client.startPerformanceTest();
    }

    private class PerformanceTestCallback extends ClientCallback {

        private int retransmissions = 0;

        @Override
        public void processCoapResponse(CoapResponse coapResponse){
            increaseResponses(retransmissions);
            sendNextRequest();
        }

        @Override
        public void processRetransmission() {
            retransmissions++;
        }

        @Override
        public void processTransmissionTimeout() {
            increaseTimeouts();
        }

        @Override
        public void processNoMessageIDAvailable() {
            setWaitForMessageID(true);
            decreaseRequests();
            sendNextRequest();
        }

        @Override
        public void processMiscellaneousError(String description) {
            System.out.println("MISC. ERROR: " + description);
            decreaseRequests();
            sendNextRequest();
        }
    }

}
