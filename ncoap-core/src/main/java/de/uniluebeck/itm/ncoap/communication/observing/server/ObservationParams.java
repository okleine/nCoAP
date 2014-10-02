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

package de.uniluebeck.itm.ncoap.communication.observing.server;

import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;

import java.net.InetSocketAddress;
import java.util.Set;

/**
* {@link ObservationParams} is a wrapper class. Each instance contains all context information about a running
* observation of an {@link Webservice}.
*
* @author Oliver Kleine
*/
public class ObservationParams {

    private InetSocketAddress remoteEndpoint;
    private Token token;
    private int latestMessageID;
    private String webservicePath;
    private long contentFormat;
    private Set<byte[]> etags;

    /**
     * Creates a new instance of {@link ObservationParams}.
     *
     * @param remoteEndpoint the observing CoAP endpoints
     * @param token the {@link Token} to enable the observing CoAP endpoints to relate update notifications with
     *              the observation request
     * @param webservicePath the path of the {@link Webservice} that is to be observed
     * @param etags the ETAGs contained as {@link OptionValue.Name#ETAG} in the {@link CoapRequest} initiating the
     *              observation
     */
    public ObservationParams(InetSocketAddress remoteEndpoint, Token token, String webservicePath, Set<byte[]> etags){
        this.remoteEndpoint = remoteEndpoint;
        this.token = token;
        this.webservicePath = webservicePath;
        this.etags = etags;
        this.contentFormat = ContentFormat.UNDEFINED;
        this.latestMessageID = CoapMessage.UNDEFINED_MESSAGE_ID;
    }


    /**
     * Returns the socket address of the oberving CoAP endpoints
     *
     * @return the socket address of the oberving CoAP endpoints
     */
    public InetSocketAddress getRemoteEndpoint() {
        return this.remoteEndpoint;
    }


    /**
     * Returns the {@link Token} that in combination with {@link #getRemoteEndpoint()} uniquely identifies this
     * observation.
     *
     * @return the {@link Token} that in combination with {@link #getRemoteEndpoint()} uniquely identifies this
     * observation.
     */
    public Token getToken() {
        return this.token;
    }



    /**
     * Returns the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications. This number
     * represents the content format of the update notifications content. If no content format is set yet the
     * returned value is {@link de.uniluebeck.itm.ncoap.message.options.UintOptionValue#UNDEFINED}
     *
     * @return the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications or
     * {@link de.uniluebeck.itm.ncoap.message.options.UintOptionValue#UNDEFINED} if no content format is defined, yet.
     */
    public long getContentFormat() {
        return this.contentFormat;
    }


    /**
     * Sets the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications. This number
     * represents the content format of the update notifications content.
     *
     * @param contentFormat the number to be set as ({@link OptionValue.Name#CONTENT_FORMAT} for update notifications.
     */
    public void setContentFormat(long contentFormat) {
        this.contentFormat = contentFormat;
    }


    /**
     * Returns the path of the {@link Webservice} to be observed
     *
     * @return the path of the {@link Webservice} to be observed
     */
    public String getWebservicePath() {
        return this.webservicePath;
    }


    /**
     * Returns the {@link Set} of ETAGs that was contained as {@link OptionValue.Name#ETAG} in the {@link CoapRequest}
     * initiating the observation.
     *
     * @return the {@link Set} of ETAGs that was contained as {@link OptionValue.Name#ETAG} in the {@link CoapRequest}
     * initiating the observation.
     */
    public Set<byte[]> getEtags() {
        return this.etags;
    }


    /**
     * Returns the message ID of the latest update notification that was sent to the observing CoAP endpoints
     *
     * @return the message ID of the latest update notification that was sent to the observing CoAP endpoints
     */
    public int getLatestMessageID() {
        return latestMessageID;
    }


    /**
     * Sets the message ID of the latest update notification that was sent to the observing CoAP endpoints
     *
     * @param latestMessageID the message ID of the latest update notification that was sent to the
     *                                          observing CoAP endpoints
     */
    public void setLatestMessageID(int latestMessageID) {
        this.latestMessageID = latestMessageID;
    }

}
