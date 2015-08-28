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
package de.uzl.itm.ncoap.communication.reliability;

import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by olli on 26.09.14.
 */
public class OutboundReliableMessageTransfer extends OutboundMessageTransfer {

    private static Logger log = LoggerFactory.getLogger(OutboundReliableMessageTransfer.class.getName());

    public static final int MAX_RETRANSMISSIONS = 4;

    /**
     * The minimum number of milliseconds (2000) to wait for the first retransmit of an outgoing
     * {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType.Name#CON}
     */
    public static final int ACK_TIMEOUT_MILLIS = 2000;

    /**
     * The factor (1.5) to be multiplied with {@link #ACK_TIMEOUT_MILLIS} to get the maximum number of milliseconds
     * (3000) to wait for the first retransmit of an outgoing {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType.Name#CON}
     */
    public static final double ACK_RANDOM_FACTOR = 1.5;

    private ScheduledFuture retransmissionFuture;
    private int retransmissions;

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    /**
     * Provides a random(!) delay for the given retransmission number according to the CoAP specification
     * @param retransmission the retransmission number (e.g. 2 for the 2nd retransmission)
     * @return a random(!) delay for the given retransmission number according to the CoAP specification
     */
    public static long provideRetransmissionDelay(int retransmission){
        return (long)(Math.pow(2, retransmission - 1) * ACK_TIMEOUT_MILLIS *
                (1 + RANDOM.nextDouble() * (ACK_RANDOM_FACTOR - 1)));
    }

    /**
     * Creates a new instance of
     * {@link OutboundReliableMessageTransfer}
     *
     * @param remoteEndpoint the intended recipient of the {@link de.uzl.itm.ncoap.message.CoapMessage}
     * @param messageID    the message ID of the message to be transmitted
     * @param retransmissionFuture the {@link java.util.concurrent.ScheduledFuture} of the next scheduled
     *                             retransmission.
     */
    public OutboundReliableMessageTransfer(InetSocketAddress remoteEndpoint, int messageID, Token token,
                                           ScheduledFuture retransmissionFuture) {
        super(remoteEndpoint, messageID, token);
        this.retransmissionFuture = retransmissionFuture;
        this.retransmissions = 0;
    }



    /**
     * @return the actual number of retransmission (after increasing)
     */
    public int increaseRetransmissions(){
        return ++this.retransmissions;
    }


    public long getNextRetransmissionDelay(){
        return provideRetransmissionDelay(retransmissions + 1);
    }

    public void setRetransmissionFuture(ScheduledFuture retransmissionFuture){
        this.retransmissionFuture = retransmissionFuture;
    }


    public ScheduledFuture getRetransmissionFuture(){
        return this.retransmissionFuture;
    }

    /**
     * Set this message exchange to be confirmed, i.e. stop further retransmissions.
     */
    public void setConfirmed(){
        if(this.retransmissionFuture.cancel(true)){
            log.info("Retransmission stopped (remote endpoint: {}, message ID: {})", this.getRemoteEndpoint(),
                    this.getMessageID());
        }
        else{
            log.error("Could not stop retransmission (remote endpoint: {}, message ID: {})", this.getRemoteEndpoint(),
                    this.getMessageID());
        }
    }
}
