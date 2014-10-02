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

package de.uniluebeck.itm.ncoap.communication.dispatching.client;

import de.uniluebeck.itm.ncoap.communication.events.MessageTransferEvent;
import de.uniluebeck.itm.ncoap.communication.events.*;
import de.uniluebeck.itm.ncoap.communication.events.TransmissionTimeoutEvent;
import de.uniluebeck.itm.ncoap.message.*;


/**
 * Classes extending {@link ClientCallback} process the
 * feedback related to a {@link CoapRequest}. Such feedback may be the corresponding
 * {@link de.uniluebeck.itm.ncoap.message.CoapResponse} but also notifications on several events, e.g.
 * the retranmission of a confirmable message.
 *
 * However, to handle inbound {@link de.uniluebeck.itm.ncoap.message.CoapResponse}s the method
 * {@link #processCoapResponse(de.uniluebeck.itm.ncoap.message.CoapResponse)} is to be overridden. To handle
 * the several types of events the corresponding methods are to be overridden (which is optional!).
 *
 * A {@link ClientCallback} is comparable to a tab in a browser, whereas the {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication}
 * is comparable to the browser.
 *
 * @author Oliver Kleine
 */
public abstract class ClientCallback {

    private boolean observing = false;

    /**
     * Method invoked by the {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} for an inbound response (which is of any type but
     * empty {@link de.uniluebeck.itm.ncoap.message.MessageType.Name#ACK} or
     * {@link de.uniluebeck.itm.ncoap.message.MessageType.Name#RST}).
     *
     * @param coapResponse the response message
     */
    public abstract void processCoapResponse(CoapResponse coapResponse);


    void setObserving(){
        this.observing = true;
    }

    boolean isObserving(){
        return this.observing;
    }
    /**
     * This method is called by the framework whenever an update notification was received, i.e. this method is
     * only called if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} that is associated with this
     * {@link ClientCallback} had the
     * {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#OBSERVE} set to <code>0</code>.
     *
     * @return <code>true</code> if the observation is to be continued, <code>false</code> if the observation
     * is to be canceled (next update notification will cause a RST being send to the remote endpoint). Default,
     * (i.e. if not overridden), is <code>false</code>, i.e. observations stops after first update notification.
     */
    public boolean continueObservation(){
        return false;
    }


    final void processMessageExchangeEvent(MessageTransferEvent event){

        if(event instanceof EmptyAckReceivedEvent){
            processEmptyAcknowledgement();
        }

        else if(event instanceof ResetReceivedEvent){
            processReset();
        }

        else if(event instanceof MessageRetransmittedEvent){
            processRetransmission();
        }

        else if(event instanceof TransmissionTimeoutEvent){
            processTransmissionTimeout();
        }

        else if(event instanceof MessageIDAssignedEvent){
            processMessageIDAssignment(event.getMessageID());
        }

        else if(event instanceof MiscellaneousErrorEvent){
            processMiscellaneousError(((MiscellaneousErrorEvent) event).getDescription());
        }
    }


    /**
     * This method is called by the framework if there was no answer (either an empty ACK) or a proper CoAP response
     * from the remote endpoint received within the 247 seconds.
     *
     * <b>Note:</b>to somehow handle a conversation timeout this method is to be overridden.
     */
    public void processTransmissionTimeout() {
        //to be overridden by extending classes
    }


    /**
     * This method is called by the framework if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} corresponding
     * to this {@link ClientCallback} was answered with a RST.
     *
     * <b>Note:</b>to somehow handle RST messaged this method is to be overridden.
     */
    public void processReset() {
        //to be overridden by extending classes
    }

    /**
     * This method is called by the framework upon every retransmission of the confirmable
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} corresponding to this
     * {@link ClientCallback}, i.e. up to 4 times.
     *
     * <b>Note:</b> to somehow handle re-tranmissions this method is to be overridden.
     */
    public void processRetransmission() {
        //to be overridden by extending classes
    }

    /**
     * This method is called by the framework if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} corresponding
     * to this {@link ClientCallback} was confirmed with an empty ACK.
     *
     * <b>Note:</b> to somehow handle the reception of an empty ACK this method is to be overridden.
     */
    public void processEmptyAcknowledgement(){
        //to be overridden by extending classes
    }


    /**
     * This method is called by the framework if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} associated with
     * caused an error
     *
     * <b>Note:</b> to handle {@link de.uniluebeck.itm.ncoap.communication.events.MiscellaneousErrorEvent}s this method
     * is to be overridden.
     *
     * @param description a description of the error that caused this event
     */
    public void processMiscellaneousError(String description){
        //to be overridden by extending classes
    }


    /**
     * This method is invoked by the framework if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} that is
     * associated with this callback is assigned a message ID.
     *
     * @param messageID the message ID that was assigned to the {@link de.uniluebeck.itm.ncoap.message.CoapRequest}
     *                  that is associated with this callback
     */
    public void processMessageIDAssignment(int messageID){
        //to be overridden by extending classes
    }

    /**
     * This method is invoked by the framework if the {@link de.uniluebeck.itm.ncoap.message.CoapRequest} that is
     * associated with this callback is assigned a {@link Token}.
     *
     * @param token the {@link Token} that was assigned to the
     *              {@link de.uniluebeck.itm.ncoap.message.CoapRequest} that is associated with this callback
     */
    public void processTokenAssignment(Token token){
        //to be overridden by extending classes
    }

}
