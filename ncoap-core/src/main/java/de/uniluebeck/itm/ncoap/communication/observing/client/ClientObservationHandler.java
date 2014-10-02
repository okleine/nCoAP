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
package de.uniluebeck.itm.ncoap.communication.observing.client;

import com.google.common.collect.*;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.communication.events.client.ObservationCancelledEvent;
import de.uniluebeck.itm.ncoap.communication.events.ResetReceivedEvent;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.UintOptionValue;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * The {@link ClientObservationHandler} deals with
 * running observations. It e.g. ensures that inbound update notifications answered with a RST if the
 * observation was canceled by the {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication}.
 *
 * @author Oliver Kleine
 */
public class ClientObservationHandler extends SimpleChannelHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Table<InetSocketAddress, Token, ResourceStatusAge> observations;

    /**
     * Creates a new instance of
     * {@link ClientObservationHandler}
     */
    public ClientObservationHandler(){
        this.observations = HashBasedTable.create();
    }


    private synchronized void startObservation(InetSocketAddress remoteEndpoint, Token token){
        if(this.observations.put(remoteEndpoint, token, new ResourceStatusAge(0, 0)) != null){
            log.error("Running observation (remote endpoint: {}, token: {}) overridden!", remoteEndpoint, token);
        }
        else{
            log.info("New observation added (remote endpoint: {}, token: {})", remoteEndpoint, token);
        }
    }

    private synchronized void updateStatusAge(InetSocketAddress remoteEndpoint, Token token, ResourceStatusAge age){
        this.observations.put(remoteEndpoint, token, age);
    }

    private synchronized ResourceStatusAge stopObservation(InetSocketAddress remoteEndpoint, Token token){
        ResourceStatusAge result = this.observations.remove(remoteEndpoint, token);
        if(result == null){
            log.error("No observation found to be stopped (remote endpoint: {}, token: {})", remoteEndpoint, token);
        }
        else{
            log.info("Observation of {} with token {} stopped! Next update notifications (if any) will cause a RST.",
                    remoteEndpoint, token);
        }
        return result;
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){

        if(me.getMessage() instanceof CoapRequest){
            handleOutgoingCoapRequest(ctx, me);
        }
        else if(me.getMessage() instanceof ObservationCancelledEvent){
            handleObservationCancelledEvent((ObservationCancelledEvent) me.getMessage());
        }
        else if(me.getMessage() instanceof CoapMessage){
            ctx.sendDownstream(me);
        }
        else{
            log.warn("Event: {}", me.getMessage());
            me.getFuture().setSuccess();
        }
    }


    private void handleObservationCancelledEvent(ObservationCancelledEvent event) {
        InetSocketAddress remoteEndpoint = event.getRemoteEndpoint();
        Token token = event.getToken();
        stopObservation(remoteEndpoint, token);
    }


    private void handleOutgoingCoapRequest(ChannelHandlerContext ctx, MessageEvent me) {
        CoapRequest coapRequest = (CoapRequest) me.getMessage();

        long observe = coapRequest.getObserve();
        if(observe != UintOptionValue.UNDEFINED){
            InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
            Token token = coapRequest.getToken();

            if(observe == 0){
                log.debug("Add observation (remote endpoint: {}, token: {})", remoteEndpoint, token);
                startObservation(remoteEndpoint, token);
            }

            else{
                log.debug("Stop observation due to \"observe != 0\" (remote endpoint: {}, token: {})",
                        remoteEndpoint, token);
                stopObservation(remoteEndpoint, token);
            }
        }

        ctx.sendDownstream(me);
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){

        //any kind of (non-empty) response
        if(me.getMessage() instanceof CoapResponse){
            handleIncomingCoapResponse(ctx, me);
        }

        //received RST from remote endpoint
        else if(me.getMessage() instanceof ResetReceivedEvent) {
            handleResetReceivedEvent(ctx, me);
        }

        //something else...
        else{
            ctx.sendUpstream(me);
        }
    }


    private void handleResetReceivedEvent(ChannelHandlerContext ctx, MessageEvent me) {
        ResetReceivedEvent event = (ResetReceivedEvent) me.getMessage();
        InetSocketAddress remoteEndpoint = event.getRemoteEndpoint();
        Token token = event.getToken();

        if(this.observations.contains(remoteEndpoint, token)){
            stopObservation(remoteEndpoint, token);
        }

        ctx.sendUpstream(me);
    }


    private void handleIncomingCoapResponse(ChannelHandlerContext ctx, MessageEvent me) {

        CoapResponse coapResponse = (CoapResponse) me.getMessage();

        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();
        Token token = coapResponse.getToken();

        //Current response is NO update notification or is an error response (which SHOULD implicate the first)
        if(!coapResponse.isUpdateNotification() || MessageCode.isErrorMessage(coapResponse.getMessageCode())){
            if(observations.contains(remoteEndpoint, token)){
                log.info("Stop observation (remote address: {}, token: {}) due to received response: {}",
                        new Object[]{remoteEndpoint, token, coapResponse});

                stopObservation(remoteEndpoint, token);
            }
        }

        else if(coapResponse.isUpdateNotification()){
            //current response is update notification but there is no suitable observation
            if(!observations.contains(remoteEndpoint, token)){
                log.warn("No observation found for update notification (remote endpoint: {}, token: {}).",
                        remoteEndpoint, token);
            }

            //Current response is (non-error) update notification and there is a suitable observation
            else if(coapResponse.isUpdateNotification() && !MessageCode.isErrorMessage(coapResponse.getMessageCode())){
                //Lookup status age of latest update notification
                ResourceStatusAge latestStatusAge = observations.get(remoteEndpoint, token);

                //Get status age from newly received update notification
                long receivedSequenceNo = coapResponse.getObserve();
                ResourceStatusAge receivedStatusAge = new ResourceStatusAge(receivedSequenceNo, System.currentTimeMillis());

                if(ResourceStatusAge.isReceivedStatusNewer(latestStatusAge, receivedStatusAge)){
                    updateStatusAge(remoteEndpoint, token, receivedStatusAge);
                }

                else{
                    log.warn("Received update notification ({}) is older than latest ({}). IGNORE!",
                            receivedStatusAge, latestStatusAge);
                    return;
                }
            }
        }

        ctx.sendUpstream(me);
    }
}
