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
package de.uzl.itm.ncoap.application.server.webresource;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.message.CoapMessage;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by olli on 06.10.14.
 */
public class Observation {

    private InetSocketAddress remoteEndpoint;
    private ScheduledFuture heartbeatFuture;
    private Token token;
    private byte[] endpointID2ForUpdateNotifications;
    private long contentFormat;
    private int messageID;
    private Set<byte[]> etags;

    public Observation(InetSocketAddress remoteEndpoint, Token token, long contentFormat,
            byte[] endpointID2ForUpdateNotifications) {

        this(remoteEndpoint, token, contentFormat, new HashSet<byte[]>(0), endpointID2ForUpdateNotifications);
    }

    public Observation(InetSocketAddress remoteEndpoint, Token token, long contentFormat, Set<byte[]> etags,
            byte[] endpointID2ForUpdateNotifications){

        this.remoteEndpoint = remoteEndpoint;
        this.token = token;
        this.contentFormat = contentFormat;
        this.endpointID2ForUpdateNotifications = endpointID2ForUpdateNotifications;
        this.messageID = CoapMessage.UNDEFINED_MESSAGE_ID;
        this.etags = etags;
    }

    public long getContentFormat() {
        return this.contentFormat;
    }


    /**
     * Sets the message ID to be used for the next update notification
     * @param messageID the message ID to be used for the next update notification
     */
    public void setMessageID(int messageID){
        this.messageID = messageID;
    }


    /**
     * Returns the message ID to be used for the next update notification. This method will return a value
     * other than {@link de.uzl.itm.ncoap.message.CoapMessage#UNDEFINED_MESSAGE_ID} if and only if there
     * was a not yet acknowledged or timed out confirmable update notification.
     *
     * @return the message ID to be used for the next update notification
     */
    public int getMessageID() {
        return this.messageID;
    }

    /**
     * Returns the set of ETAGs which are supposed to cause an update notification with code
     * {@link de.uzl.itm.ncoap.message.MessageCode.Name#VALID_203}
     * instead of {@link de.uzl.itm.ncoap.message.MessageCode.Name#CONTENT_205}.
     *
     * @return the set of ETAGs which are supposed to cause an update notification with code
     * {@link de.uzl.itm.ncoap.message.MessageCode.Name#VALID_203}
     * instead of {@link de.uzl.itm.ncoap.message.MessageCode.Name#CONTENT_205}.
     */
    public Set<byte[]> getEtags() {
        return etags;
    }

    public InetSocketAddress getRemoteEndpoint() {
        return remoteEndpoint;
    }

    public Token getToken() {
        return token;
    }

    public ScheduledFuture getHeartbeatFuture() {
        return heartbeatFuture;
    }

    public void setHeartbeatFuture(ScheduledFuture heartbeatFuture) {
        this.heartbeatFuture = heartbeatFuture;
    }

    public byte[] getEndpointID2ForUpdateNotifications() {
        return endpointID2ForUpdateNotifications;
    }
}
