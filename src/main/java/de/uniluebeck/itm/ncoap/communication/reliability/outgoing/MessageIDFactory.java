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
package de.uniluebeck.itm.ncoap.communication.reliability.outgoing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An instances of {@link MessageIDFactory} creates and manages message IDs for outgoing messages. On creation of
 * new message IDs the factory ensures that the same message ID is not used twice for different messages to the
 * same remote CoAP endpoint within {@link #EXCHANGE_LIFETIME} seconds.
 *
 * @author Oliver Kleine
*/
public class MessageIDFactory extends Observable {

    /**
     * The number of seconds (247) a message ID is allocated by the nCoAP framework to avoid duplicate
     * usage of the same message ID in communications with the same remote CoAP endpoint.
     */
    public static final int EXCHANGE_LIFETIME = 247;

    public static final int NON_LIFETIME = 145;
    /**
     * The number of different message IDs per remote CoAP endpoint (65536), i.e. there are at most 65536
     * communications with the same endpoint possible within {@link #EXCHANGE_LIFETIME} milliseconds.
     */
    public static final int MODULUS = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Map<InetSocketAddress, Deque<AllocatedMessageID>> retiringMessageIDs;

    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the thread for operations to
     *                        provide available message IDs
     */
    public MessageIDFactory(ScheduledExecutorService executorService){
        this.retiringMessageIDs = new HashMap<>();

        executorService.scheduleAtFixedRate(new Runnable(){

            @Override
            public void run() {

                long now = System.currentTimeMillis();
                synchronized (MessageIDFactory.this){
                    try{

                        Iterator<Map.Entry<InetSocketAddress, Deque<AllocatedMessageID>>> retirementsIterator =
                                retiringMessageIDs.entrySet().iterator();

                        while(retirementsIterator.hasNext()){
                            Map.Entry<InetSocketAddress, Deque<AllocatedMessageID>> retirementsForRemoteAddress
                                    = retirementsIterator.next();

                            Deque<AllocatedMessageID> allocatedMessageIDs = retirementsForRemoteAddress.getValue();
                            while(!allocatedMessageIDs.isEmpty()){
                                AllocatedMessageID allocatedMessageID = allocatedMessageIDs.getFirst();

                                if(allocatedMessageID.getRetirementDate() > now)
                                    break;

                                else{
                                    AllocatedMessageID retiredMessageID = allocatedMessageIDs.removeFirst();

                                    setChanged();
                                    notifyObservers(new Object[]{
                                            retirementsForRemoteAddress.getKey(),
                                            retiredMessageID.getMessageID()
                                    });
                                }
                            }

                            if(allocatedMessageIDs.isEmpty())
                                retirementsIterator.remove();


                        }
                    }
                    catch(Exception e){
                        log.error("This should never happen!", e);
                    }
                }
            }

        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns a message ID to be used for outgoing {@link de.uniluebeck.itm.ncoap.message.CoapMessage}s and
     * allocates this message ID for {@link #EXCHANGE_LIFETIME} seconds, i.e. the returned message ID will not
     * be returned again within {@link #EXCHANGE_LIFETIME} seconds.
     *
     * @param remoteEndpoint the recipient of the message the returned message ID is supposed to be used for
     *
     * @return the message ID to be used for outgoing messages
     */
    public synchronized int getNextMessageID(InetSocketAddress remoteEndpoint)
            throws NoMessageIDAvailableException {

        Deque<AllocatedMessageID> allocatedMessageIDsForRemoteAddreses = retiringMessageIDs.get(remoteEndpoint);

        //check if all message IDs are in use for the given endpoint
        if(allocatedMessageIDsForRemoteAddreses != null && allocatedMessageIDsForRemoteAddreses.size() == MODULUS){
            long waitingPeriod =
                    allocatedMessageIDsForRemoteAddreses.getFirst().getRetirementDate() - System.currentTimeMillis();

            throw new NoMessageIDAvailableException(remoteEndpoint, waitingPeriod);
        }

        //there are message IDs available for the given endpoint
        else{
            int messageID = 0;

            if(allocatedMessageIDsForRemoteAddreses != null && allocatedMessageIDsForRemoteAddreses.size() > 0)
                    messageID = (allocatedMessageIDsForRemoteAddreses.getLast().getMessageID() + 1) % MODULUS;

            allocateMessageID(remoteEndpoint, messageID);

            return messageID;
        }
    }


    private synchronized void allocateMessageID(InetSocketAddress remoteEndpoint, int messageID){

        log.debug("Allocate message ID {} for {}", messageID, remoteEndpoint);

        Deque<AllocatedMessageID> allocatedMessageIDsForRemoteAddreses = retiringMessageIDs.get(remoteEndpoint);
        if(allocatedMessageIDsForRemoteAddreses == null){
            allocatedMessageIDsForRemoteAddreses = new ArrayDeque<>();
            retiringMessageIDs.put(remoteEndpoint, allocatedMessageIDsForRemoteAddreses);
        }

        long retirementDate = System.currentTimeMillis() + EXCHANGE_LIFETIME * 1000;

        allocatedMessageIDsForRemoteAddreses.add(new AllocatedMessageID(messageID, retirementDate));
    }


    private class AllocatedMessageID {

        private Integer messageID;
        private Long retirementDate;


        private AllocatedMessageID(Integer messageID, Long retirementDate) {
            this.messageID = messageID;
            this.retirementDate = retirementDate;
        }

        public Integer getMessageID() {
            return messageID;
        }

        public Long getRetirementDate() {
            return retirementDate;
        }

    }
}
