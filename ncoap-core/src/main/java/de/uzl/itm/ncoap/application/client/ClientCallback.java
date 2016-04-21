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
package de.uzl.itm.ncoap.application.client;

import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.message.CoapResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import de.uzl.itm.ncoap.message.options.Option;

import java.net.InetSocketAddress;


/**
 * Classes extending {@link ClientCallback} process the
 * feedback related to a {@link de.uzl.itm.ncoap.message.CoapRequest}. Such feedback may be the corresponding
 * {@link de.uzl.itm.ncoap.message.CoapResponse} but also notifications on several events, e.g.
 * the retranmission of a confirmable message.
 *
 * However, to handle inbound {@link de.uzl.itm.ncoap.message.CoapResponse}s the method
 * {@link #processCoapResponse(de.uzl.itm.ncoap.message.CoapResponse)} is to be overridden. To handle
 * the several types of events the corresponding methods are to be overridden (which is optional!).
 *
 * A {@link ClientCallback} is comparable to a tab in a browser, whereas the {@link de.uzl.itm.ncoap.application.client.CoapClient}
 * is comparable to the browser.
 *
 * @author Oliver Kleine
 */
public abstract class ClientCallback {


    /**
     * <p>This method is invoked by the {@link de.uzl.itm.ncoap.application.client.CoapClient} for an inbound response
     * (which is of any type but empty {@link de.uzl.itm.ncoap.message.MessageType#ACK} or
     * {@link de.uzl.itm.ncoap.message.MessageType#RST}).</p>
     *
     * @param coapResponse the response message
     */
    public abstract void processCoapResponse(CoapResponse coapResponse);


    /**
     * <p>This method is called by the framework whenever an update notification was received, i.e. this method is
     * only called if the {@link de.uzl.itm.ncoap.message.CoapRequest} that is associated with this
     * {@link ClientCallback} had the
     * {@link de.uzl.itm.ncoap.message.options.Option#OBSERVE} set to <code>0</code>.</p>
     *
     * @return <code>true</code> if the observation is to be continued, <code>false</code> if the observation
     * is to be canceled (next update notification will cause a RST being send to the remote endpoint). Default,
     * (i.e. if not overridden), is <code>true</code>, i.e. observations stops after first update notification.
     */
    public boolean continueObservation() {
        return false;
    }


    /**
     * <p>This method is called by the framework if the socket address of the remote endpoint changed during an
     * ongoing conversation (e.g. an observation).</p>
     *
     * <b>Note:</b>to somehow handle the change this method is to be overridden.
     */
    public void processRemoteSocketChanged(InetSocketAddress remoteSocket, InetSocketAddress previous) {
        // to be overridden by extending classes
    }


    /**
     * <p>This method is called by the framework if there was no answer (either an empty ACK) or a proper CoAP response
     * from the remote endpoint received within the 247 seconds.</p>
     *
     * <p><b>Note:</b>to somehow handle a conversation timeout this method is to be overridden.</p>
     */
    public void processTransmissionTimeout() {
        //to be overridden by extending classes
    }


    /**
     * <p>This method is called by the framework if the {@link de.uzl.itm.ncoap.message.CoapRequest} corresponding
     * to this {@link ClientCallback} was answered with a RST.</p>
     *
     * <p><b>Note:</b>to somehow handle RST messaged this method is to be overridden.</p>
     */
    public void processReset() {
        //to be overridden by extending classes
    }

    /**
     * <p>This method is called by the framework upon every retransmission of the confirmable
     * {@link de.uzl.itm.ncoap.message.CoapRequest} corresponding to this
     * {@link ClientCallback}, i.e. up to 4 times.</p>
     *
     * <p><b>Note:</b> to somehow handle retransmissions this method is to be overridden.</p>
     */
    public void processRetransmission() {
        //to be overridden by extending classes
    }

    /**
     * <p>This method is called by the framework upon every reception of a response with a BLOCK2 option that
     * indicates that there are more blocks to come.</p>
     *
     * <p><b>Note:</b> Invocation of this method has rather "informal" character. Once all blocks where received, the
     * framework invokes {@link #processCoapResponse(CoapResponse)} with the full payload included in the
     * {@link CoapResponse}.</p>
     *
     * @param receivedLength the number of bytes received so far
     * @param expectedLength the number of bytes expected for the complete representation
     */
    public void processResponseBlockReceived(long receivedLength, long expectedLength) {
        //to be overridden by extending classes
    }

    /**
     * <p>This method is called by the framework upon every reception of a response with
     * {@link de.uzl.itm.ncoap.message.MessageCode#CONTINUE_231} within a blockwise request transfer with
     * {@link de.uzl.itm.ncoap.message.MessageCode#POST} or {@link de.uzl.itm.ncoap.message.MessageCode#PUT}.</p>
     *
     * <p><b>Note:</b> Invocation of this method has rather "informal" character. The callback does not need to
     * do anything and may even ignore it. The next request block is automatically sent by the
     * {@link de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1Handler}.</p>
     *
     * @param block1Size the {@link BlockSize} of the latest successfully transfered request block
     */
    public void processContinueResponseReceived(BlockSize block1Size) {
        // to be overridden by extending classes
    }

    /**
     * This method is called by the framework if the blockwise response transfer failed for any reason, mostly because
     * the representation of the requested resource (i.e. its ETAG) changed during the blockwise transfer.
     *
     * <b>Note:</b> to somehow handle this event this method must be overridden
     */
    public void processBlockwiseResponseTransferFailed() {
        //to be overridden by extending classes
    }

    /**
     * This method is called by the framework if the {@link de.uzl.itm.ncoap.message.CoapRequest} corresponding
     * to this {@link ClientCallback} was confirmed with an empty ACK.
     *
     * <b>Note:</b> to somehow handle the reception of an empty ACK this method is to be overridden.
     */
    public void processEmptyAcknowledgement() {
        //to be overridden by extending classes
    }


    /**
     * This method is called by the framework if the {@link de.uzl.itm.ncoap.message.CoapRequest} associated with
     * caused an error
     *
     * <b>Note:</b> to handle {@link de.uzl.itm.ncoap.communication.events.MiscellaneousErrorEvent}s this method
     * is to be overridden.
     *
     * @param description a description of the error that caused this event
     */
    public void processMiscellaneousError(String description) {
        //to be overridden by extending classes
    }


    /**
     * This method is invoked by the framework if the {@link de.uzl.itm.ncoap.message.CoapRequest} that is
     * associated with this callback is assigned a message ID.
     *
     * @param messageID the message ID that was assigned to the {@link de.uzl.itm.ncoap.message.CoapRequest}
     *                  that is associated with this callback
     */
    public void processMessageIDAssignment(int messageID) {
        //to be overridden by extending classes
    }

    /**
     * This method is invoked by the framework if the {@link de.uzl.itm.ncoap.message.CoapRequest} that is
     * associated with this callback could not be assigned a message ID (due to an exhausted ID pool).
     */
    public void processNoMessageIDAvailable() {
        //to be overridden by extending classes
    }


//    /**
//     * This method is invoked by the framework if the {@link de.uzl.itm.ncoap.message.CoapRequest} that is
//     * associated with this callback is assigned a {@link Token}.
//     *
//     * @param token the {@link Token} that was assigned to the
//     *              {@link de.uzl.itm.ncoap.message.CoapRequest} that is associated with this callback
//     */
//    public void processTokenAssignment(Token token) {
//        //to be overridden by extending classes
//    }

}
