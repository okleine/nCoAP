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

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
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

    private LinkedHashMultimap<InetSocketAddress, SettableFuture<Integer>> waitingMessageIDFutures =
            LinkedHashMultimap.create();

    private HashMultimap<InetSocketAddress, Integer> usedMessageIDs;
    private HashMap<InetSocketAddress, Integer> latestMessageIDs;

    private TreeMultimap<Long, Pair<Integer, InetSocketAddress>> messageIDRetirementSchedule;

    /**
     * @param executorService the {@link ScheduledExecutorService} to provide the thread for operations to
     *                        provide available message IDs
     */
    public MessageIDFactory(ScheduledExecutorService executorService){
        this.usedMessageIDs = HashMultimap.create();
        this.latestMessageIDs = new HashMap<>();
        this.messageIDRetirementSchedule = TreeMultimap.create();

        executorService.scheduleAtFixedRate(new MessageIDRetirementTask(), 5, 5, TimeUnit.SECONDS);
    }

    /**
     *
     * @param remoteSocketAddress
     * @return
     */
    public synchronized ListenableFuture<Integer> getNextMessageID(InetSocketAddress remoteSocketAddress){

        final SettableFuture<Integer> messageIDFuture = SettableFuture.create();
        int nextMessageID;

//        messageIDFuture.set(1);

        //there are message IDs in use for this remote endpoint
        if(usedMessageIDs.containsKey(remoteSocketAddress)){
            log.debug("There are message IDs in use for {}.", remoteSocketAddress);

            //The next message ID is (latestMessageID + 1) % MODULUS
            nextMessageID = (latestMessageIDs.get(remoteSocketAddress) + 1) % MODULUS;

            log.debug("Next message ID {} for {} is {}in use.", new Object[]{ nextMessageID, remoteSocketAddress,
                    usedMessageIDs.containsEntry(remoteSocketAddress, nextMessageID) ? "" : "NOT "});

            //If message ID considered next is in use, than all 65536 possible message IDs for this remote
            //endpoint are in use (which is rather unlikely but possible). So, wait for the next available
            //message ID...
            if(usedMessageIDs.containsEntry(remoteSocketAddress, nextMessageID)){
                waitingMessageIDFutures.put(remoteSocketAddress, messageIDFuture);
            }

            //Use the next available message ID and schedule
            else{
                messageIDFuture.set(nextMessageID);
                allocateMessageID(remoteSocketAddress, nextMessageID);
            }
        }
        else{
            log.debug("There are NO message IDs in use for {}.", remoteSocketAddress);
            nextMessageID = 0;
            messageIDFuture.set(nextMessageID);
            allocateMessageID(remoteSocketAddress, nextMessageID);
        }

        return messageIDFuture;
    }

    private void allocateMessageID(InetSocketAddress remoteSocketAddress, int messageID){
        log.debug("Allocate message ID {} for {}", messageID, remoteSocketAddress);

        this.usedMessageIDs.put(remoteSocketAddress, messageID);
        this.latestMessageIDs.put(remoteSocketAddress, messageID);
        this.messageIDRetirementSchedule.put(System.currentTimeMillis() + EXCHANGE_LIFETIME * 1000,
                new Pair<>(messageID, remoteSocketAddress));
    }


    private class MessageIDRetirementTask implements Runnable{

        @Override
        public void run() {
            try{
                System.out.println("Start message ID retirement task!");
                synchronized (MessageIDFactory.this){
                    final long currentTime = System.currentTimeMillis();

                    SetMultimap<Long, Pair<Integer, InetSocketAddress>> filteredMultimap =
                            Multimaps.filterKeys(messageIDRetirementSchedule, new Predicate<Long>() {
                                @Override
                                public boolean apply(Long input) {
                                    return input <= currentTime;
                                }
                            });


                    SortedSet<Pair<Integer, InetSocketAddress>> retiredMessageIDs = new TreeSet<>();
                    retiredMessageIDs.addAll(filteredMultimap.values());

                    for (Pair<Integer, InetSocketAddress> retiredMessageID : retiredMessageIDs) {
                        Integer messageID = retiredMessageID.getFirstElement();
                        InetSocketAddress remoteSocketAddress = retiredMessageID.getSecondElement();

                        //Remove the retired message ID from the list of used message IDs
                        usedMessageIDs.remove(remoteSocketAddress, messageID);

                        //Check if there is message ID future waiting for a retiring message ID
                        Iterator<SettableFuture<Integer>> waitingMessageIDFutures =
                                MessageIDFactory.this.waitingMessageIDFutures.get(remoteSocketAddress).iterator();

                        if (waitingMessageIDFutures.hasNext()) {
                            SettableFuture<Integer> waitingMessageIDFuture = waitingMessageIDFutures.next();

                            MessageIDFactory.this.waitingMessageIDFutures.remove(remoteSocketAddress,
                                    waitingMessageIDFuture);

                            waitingMessageIDFuture.set(messageID);
                            allocateMessageID(remoteSocketAddress, messageID);
                        }
                    }

                    //Remove past message ID retirements
                    Set<Long> pastRetirmentTimesSet = filteredMultimap.keySet();
                    Long[] pastRetirmentTimes = pastRetirmentTimesSet.toArray(new Long[pastRetirmentTimesSet.size()]);

                    for(Long time : pastRetirmentTimes)
                        filteredMultimap.removeAll(time);


                    //Remove latest message IDs if there is no message ID in use anymore
                    Set<InetSocketAddress> addressesSet = latestMessageIDs.keySet();
                    InetSocketAddress[] remoteAddresses = addressesSet.toArray(new InetSocketAddress[addressesSet.size()]);

                    for(InetSocketAddress remoteSocketAddress : remoteAddresses){
                        if(usedMessageIDs.get(remoteSocketAddress).isEmpty()){
                            latestMessageIDs.remove(remoteSocketAddress);

                            if(MessageIDFactory.this.waitingMessageIDFutures.get(remoteSocketAddress).isEmpty())
                                log.error("There are waiting message ID futures but there is no message ID in use!");
                        }
                    }
                }
            }
            catch(Exception e){
                log.error("Exception!", e);
            }
        }
    }


    private class Pair<A extends Comparable<A>, B> implements Comparable<Pair<A, B>>{

        private A firstElement;
        private B secondElement;

        public Pair(A firstElement, B secondElement){
            this.firstElement = firstElement;
            this.secondElement = secondElement;
        }

        public A getFirstElement() {
            return firstElement;
        }

        public B getSecondElement() {
            return secondElement;
        }

        @Override
        public int compareTo(Pair<A, B> other){
            return this.getFirstElement().compareTo(other.getFirstElement());
        }
    }


}
