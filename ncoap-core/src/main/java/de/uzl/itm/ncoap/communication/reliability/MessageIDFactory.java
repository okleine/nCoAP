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

import de.uzl.itm.ncoap.communication.events.MessageIDReleasedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An instances of {@link MessageIDFactory} creates and manages message IDs for outgoing messages. On creation of
 * new message IDs the factory ensures that the same message ID is not used twice for different messages to the
 * same remote CoAP endpoints within {@link #EXCHANGE_LIFETIME} seconds.
 *
 * @author Oliver Kleine
*/
public class MessageIDFactory{

    /**
     * The number of seconds (247) a message ID is allocated by the nCoAP framework to avoid duplicate
     * usage of the same message ID in communications with the same remote CoAP endpoints.
     */
    public static final int EXCHANGE_LIFETIME = 247;

    /**
     * The number of different message IDs per remote CoAP endpoint (65536), i.e. there are at most 65536
     * communications with the same endpoints possible within {@link #EXCHANGE_LIFETIME} milliseconds.
     */
    public static final int MODULUS = 65536;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Random random;

    private Map<InetSocketAddress, ArrayDeque<AllocationRetirementTask>> retirementTasks;
    private ReentrantReadWriteLock lock;
    private ScheduledExecutorService executor;
    private Channel channel;

    /**
     * @param executor the {@link ScheduledExecutorService} to provide the thread for operations to
     *                        provide available message IDs
     */
    public MessageIDFactory(ScheduledExecutorService executor){
        this.executor = executor;
        this.retirementTasks = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.random = new Random(System.currentTimeMillis());
    }


    public void setChannel(Channel channel){
        this.channel = channel;
    }
    /**
     * Returns a message ID to be used for outgoing {@link de.uzl.itm.ncoap.message.CoapMessage}s and
     * allocates this message ID for {@link #EXCHANGE_LIFETIME} seconds, i.e. the returned message ID will not
     * be returned again within {@link #EXCHANGE_LIFETIME} seconds.
     *
     * If all message IDs available for the given remote endpoint are in use
     * {@link de.uzl.itm.ncoap.message.CoapMessage#UNDEFINED_MESSAGE_ID} is returned.
     *
     * @param remoteEndpoint the recipient of the message the returned message ID is supposed to be used for
     *
     * @return the message ID to be used for outgoing messages or
     * {@link de.uzl.itm.ncoap.message.CoapMessage#UNDEFINED_MESSAGE_ID} if all IDs are in use.
     */
    public int getNextMessageID(InetSocketAddress remoteEndpoint){

        try{
            lock.readLock().lock();
            ArrayDeque<AllocationRetirementTask> allocations = this.retirementTasks.get(remoteEndpoint);

            if(allocations != null && allocations.size() == MODULUS){
                log.warn("No more message IDs available for remote endpoint {}.", remoteEndpoint);
                return CoapMessage.UNDEFINED_MESSAGE_ID;
            }
        }
        finally{
            lock.readLock().unlock();
        }

        try{
            lock.writeLock().lock();

            ArrayDeque<AllocationRetirementTask> allocations = this.retirementTasks.get(remoteEndpoint);

            if(allocations != null && allocations.size() == MODULUS){
                log.warn("No more message IDs available for remote endpoint {}.", remoteEndpoint);
                return CoapMessage.UNDEFINED_MESSAGE_ID;
            }
            else{
                int nextMessageID = allocations == null ? this.random.nextInt(MODULUS) :
                        (allocations.getFirst().getMessageID() + 1) % MODULUS;

                AllocationRetirementTask retirementTask = new AllocationRetirementTask(remoteEndpoint, nextMessageID);

                if(allocations == null){
                    this.retirementTasks.put(remoteEndpoint, new ArrayDeque<AllocationRetirementTask>());
                }

                allocations = this.retirementTasks.get(remoteEndpoint);
                allocations.push(retirementTask);

                this.executor.schedule(retirementTask, EXCHANGE_LIFETIME, TimeUnit.SECONDS);

                return nextMessageID;
            }
        }
        finally{
            lock.writeLock().unlock();
        }
    }


    public void shutdown(){
        try{
            lock.writeLock().lock();
            this.retirementTasks.clear();
        }
        finally{
            lock.writeLock().lock();
        }
    }


    private class AllocationRetirementTask implements Runnable{

        private InetSocketAddress remoteEndpoint;
        private int messageID;

        private AllocationRetirementTask(InetSocketAddress remoteEndpoint, int messageID) {
            this.remoteEndpoint = remoteEndpoint;
            this.messageID = messageID;
        }

        public int getMessageID() {
            return this.messageID;
        }

        public InetSocketAddress getRemoteEndpoint() {
            return this.remoteEndpoint;
        }

        @Override
        public void run() {
            MessageIDReleasedEvent event = new MessageIDReleasedEvent(this.remoteEndpoint, this.messageID);
            Channels.fireMessageReceived(MessageIDFactory.this.channel, event);

            try{
                lock.writeLock().lock();
                ArrayDeque<AllocationRetirementTask> tasks = retirementTasks.get(remoteEndpoint);
                tasks.remove(this);
                if(tasks.size() == 0){
                    retirementTasks.remove(remoteEndpoint);
                }
            }
            finally {
                lock.writeLock().unlock();
            }
        }
    }
}
