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
public class MessageIDFactory{

    /**
     * The number of seconds (247) a message ID is allocated by the nCoAP framework to avoid duplicate
     * usage of the same message ID in communications with the same remote CoAP endpoint.
     */
    public static final int EXCHANGE_LIFETIME = 247;

    /**
     * The number of different message IDs per remote CoAP endpoint (65536), i.e. there are at most 65536
     * communications with the same endpoint possible within {@link #EXCHANGE_LIFETIME} milliseconds.
     */
    public static final int MODULUS = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Map<InetSocketAddress, Deque<RetiringMessageID>> retiringMessageIDs;

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

                        Iterator<Map.Entry<InetSocketAddress, Deque<RetiringMessageID>>> retirementsIterator =
                                retiringMessageIDs.entrySet().iterator();

                        while(retirementsIterator.hasNext()){
                            Map.Entry<InetSocketAddress, Deque<RetiringMessageID>> retirementsForRemoteAddress
                                    = retirementsIterator.next();

                            Deque<RetiringMessageID> retiringMessageIDs = retirementsForRemoteAddress.getValue();
                            while(!retiringMessageIDs.isEmpty()){
                                RetiringMessageID retiringMessageID = retiringMessageIDs.getFirst();

                                if(retiringMessageID.getRetirementDate() > now)
                                    break;

                                else
                                    retiringMessageIDs.removeFirst();
                            }

                            if(retiringMessageIDs.isEmpty())
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
     *
     * @param remoteSocketAddress
     * @return
     */
    public synchronized int getNextMessageID(InetSocketAddress remoteSocketAddress)
            throws NoMessageIDAvailableException {

        Deque<RetiringMessageID> retiringMessageIDsForRemoteAddress = retiringMessageIDs.get(remoteSocketAddress);

        //all message IDs are in use for the given endpoint
        if(retiringMessageIDsForRemoteAddress != null && retiringMessageIDsForRemoteAddress.size() == MODULUS){
            long waitingPeriod =
                    retiringMessageIDsForRemoteAddress.getFirst().getRetirementDate() - System.currentTimeMillis();

            throw new NoMessageIDAvailableException(remoteSocketAddress, waitingPeriod);
        }

        //there are message IDs available for the given endpoint
        else{
            int messageID = 0;

            if(retiringMessageIDsForRemoteAddress != null && retiringMessageIDsForRemoteAddress.size() > 0)
                    messageID = (retiringMessageIDsForRemoteAddress.getLast().getMessageID() + 1) % MODULUS;

//            System.out.println("Next message ID:" + messageID);
            allocateMessageID(remoteSocketAddress, messageID);

            return messageID;
        }
    }


    private void allocateMessageID(final InetSocketAddress remoteSocketAddress, final int messageID){
        log.debug("Allocate message ID {} for {}", messageID, remoteSocketAddress);

        Deque<RetiringMessageID> retiringMessageIDsForRemoteAddress = retiringMessageIDs.get(remoteSocketAddress);
        if(retiringMessageIDsForRemoteAddress == null){
            retiringMessageIDsForRemoteAddress = new ArrayDeque<>();
            retiringMessageIDs.put(remoteSocketAddress, retiringMessageIDsForRemoteAddress);
        }

        long retirementDate = System.currentTimeMillis() + EXCHANGE_LIFETIME * 1000;

        retiringMessageIDsForRemoteAddress.add(new RetiringMessageID(messageID, retirementDate));
    }




//    private synchronized void deallocateMessageID(InetSocketAddress remoteSocketAddress, int messageID){
//        usedMessageIDs.remove(remoteSocketAddress, messageID);
//
//        if(latestMessageIDs.get(remoteSocketAddress) == messageID)
//            latestMessageIDs.remove(remoteSocketAddress);
//    }
//
//
//    private synchronized SettableFuture<Integer> getNextMessageIDFuture(InetSocketAddress remoteSocketAddress){
//        //Check if there is message ID future waiting...
//        Iterator<SettableFuture<Integer>> futureIterator =
//                waitingMessageIDFutures.get(remoteSocketAddress).iterator();
//
//
//        if(futureIterator.hasNext()){
//            SettableFuture<Integer> messageIDFuture = futureIterator.next();
//            waitingMessageIDFutures.remove(remoteSocketAddress, messageIDFuture);
//            return messageIDFuture;
//        }
//
//        return null;
//    }


    private class RetiringMessageID{

        private Integer messageID;
        private Long retirementDate;


        private RetiringMessageID(Integer messageID, Long retirementDate) {
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

//    private class Deallocation{
//
//        private Long deallocationDate;
//        private InetSocketAddress remoteSocketAddress;
//        private Integer messageID;
//
//
//        private Deallocation(Long deallocationDate, InetSocketAddress remoteSocketAddress, Integer messageID) {
//            this.deallocationDate = deallocationDate;
//            this.remoteSocketAddress = remoteSocketAddress;
//            this.messageID = messageID;
//        }
//
//        public Long getDeallocationDate() {
//            return deallocationDate;
//        }
//
//        public InetSocketAddress getRemoteSocketAddress() {
//            return remoteSocketAddress;
//        }
//
//        public Integer getMessageID() {
//            return messageID;
//        }
//    }
}
