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
package de.uzl.itm.ncoap.communication.identification;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.ConversationFinishedEvent;
import de.uzl.itm.ncoap.communication.events.RemoteSocketChangedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 31.08.15.
 */
public class IdentificationHandler extends SimpleChannelHandler{

    private static Logger LOG = LoggerFactory.getLogger(IdentificationHandler.class.getName());

    // the endpoint IDs assigned to (!) other endpoints
    private HashBasedTable<EndpointID, Token, InetSocketAddress> assignedByMe1;
    private HashBasedTable<InetSocketAddress, Token, EndpointID> assignedByMe2;

    // the endpoint IDs assigned by (!) other endpoints
    private HashBasedTable<InetSocketAddress, Token, byte[]> assignedToMe;

    private EndpointIDFactory factory;
    private ReentrantReadWriteLock lock;


    public IdentificationHandler(){
        this.assignedByMe1 = HashBasedTable.create();
        this.assignedByMe2 = HashBasedTable.create();

        this.assignedToMe = HashBasedTable.create();

        this.factory = new EndpointIDFactory();
        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapMessage){
            handleOutboundCoapMessage(ctx, me);
        }

        else if(me.getMessage() instanceof ConversationFinishedEvent){
            ConversationFinishedEvent event = (ConversationFinishedEvent) me.getMessage();
            EndpointID endpointID = event.getEndpointID();
            if(endpointID != null) {
                removeFromAssignedByMe(endpointID, event.getToken(), true);
            }
            removeFromAssignedToMe(event.getRemoteEndpoint(), event.getToken());
        }

        else {
            LOG.warn("Downstream Message: {}", me.getMessage());
            me.getFuture().setSuccess();
        }
    }


    private void handleOutboundCoapMessage(ChannelHandlerContext ctx, MessageEvent me){
        CoapMessage coapMessage = (CoapMessage) me.getMessage();

        // set the endpoint ID 2 value (if there was one assigned by the remote endpoint)
        InetSocketAddress remoteSocket = (InetSocketAddress) me.getRemoteAddress();
        byte[] endpointID2 = getFromAssignedToMe(remoteSocket, coapMessage.getToken());
        if(endpointID2 != null){
            coapMessage.setEndpointID2(endpointID2);
        }


        // continue with endpoint ID 1 (if necessary)
        EndpointID endpointID1 = coapMessage.getEndpointID1() == null ? null :
                    new EndpointID(coapMessage.getEndpointID1());

        // nothing to do if no EID1 option present...
        if(endpointID1 == null){
            ctx.sendDownstream(me);
            return;
        }

        // set the endpoint ID 1 option with a valid value
        endpointID1 = getFromAssignedByMe(remoteSocket, coapMessage.getToken());
        if(endpointID1 == null) {
            endpointID1 = factory.getNextEndpointID();
            addToAssignedByMe(remoteSocket, coapMessage.getToken(), endpointID1);
        }

        coapMessage.setEndpointID1(endpointID1.getBytes());
        ctx.sendDownstream(me);
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){

        if(me.getMessage() instanceof CoapMessage) {
            handleInboundCoapMessage(ctx, me);
        }
        else {
            ctx.sendUpstream(me);
        }
    }


    private void handleInboundCoapMessage(ChannelHandlerContext ctx, MessageEvent me){
        CoapMessage coapMessage = (CoapMessage) me.getMessage();
        InetSocketAddress remoteSocket = (InetSocketAddress) me.getRemoteAddress();
        Token token = coapMessage.getToken();

        // handle endpoint ID 2 if any (assigned by me)
        byte[] value = coapMessage.getEndpointID2();
        if(value != null){
            EndpointID endpointID = new EndpointID(value);
            InetSocketAddress previousRemoteSocket = getFromAssignedByMe(endpointID, token);

            if(updateAssignedByMe(endpointID, token, remoteSocket)){
                RemoteSocketChangedEvent event = new RemoteSocketChangedEvent(remoteSocket, previousRemoteSocket,
                        coapMessage.getMessageID(), coapMessage.getToken(), endpointID);

                Channels.fireMessageReceived(ctx, event);
            }
        }


        byte[] endpointID1 = coapMessage.getEndpointID1();
        if(endpointID1 != null){
            // update "assignedToMe"
            addToAssignedToMe(remoteSocket, token, endpointID1);
        }

        ctx.sendUpstream(me);
    }


    private void addToAssignedToMe(InetSocketAddress remoteSocket, Token token, byte[] endpointID){
        try{
            lock.writeLock().lock();
            this.assignedToMe.put(remoteSocket, token,  endpointID);
            LOG.info("New ID to identify myself at remote endpoint {}: {}", remoteSocket, new EndpointID(endpointID));
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private byte[] getFromAssignedToMe(InetSocketAddress remoteSocket, Token token){
        try{
            lock.readLock().lock();
            return this.assignedToMe.get(remoteSocket, token);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    private void removeFromAssignedToMe(InetSocketAddress remoteSocket, Token token){
        try{
            lock.readLock().lock();
            if(!this.assignedToMe.contains(remoteSocket, token)){
                return;
            }
        }

        finally {
            lock.readLock().unlock();
        }
        try{
            lock.writeLock().lock();
            byte[] endpointID = this.assignedToMe.remove(remoteSocket, token);
            LOG.info("Removed ID to identify myself at remote endpoint {}: {}", remoteSocket, new EndpointID(endpointID));
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private InetSocketAddress getFromAssignedByMe(EndpointID endpointID, Token token){
        try{
            lock.readLock().lock();
            return this.assignedByMe1.get(endpointID, token);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    private EndpointID getFromAssignedByMe(InetSocketAddress remoteSocket, Token token){
        try{
            lock.readLock().lock();
            return this.assignedByMe2.get(remoteSocket, token);
        }
        finally {
            lock.readLock().unlock();
        }
    }


//    private void removeFromAssignedByMe(InetSocketAddress remoteSocket, Token token, EndpointID endpointID,
//            boolean releaseEndpointID){
//        try {
//            lock.readLock().lock();
//            if(!endpointID.equals(this.assignedByMe2.get(remoteSocket, token))){
//                return;
//            }
//        }
//
//        finally {
//            lock.readLock().unlock();
//        }
//        try{
//            lock.writeLock().lock();
//            if(!endpointID.equals(this.assignedByMe2.get(remoteSocket, token))){
//                return;
//            }
//
//            this.assignedByMe2.remove(remoteSocket, token);
//            this.assignedByMe1.remove(endpointID, token);
//            if(releaseEndpointID) {
//                this.factory.passBackEndpointID(endpointID);
//            }
//            LOG.info("Removed ID to identify remote host {}: {}", remoteSocket, endpointID);
//        }
//        finally {
//            lock.writeLock().unlock();
//        }
//    }

    private void removeFromAssignedByMe(EndpointID endpointID, Token token, boolean releaseEndpointID){
        try {
            lock.readLock().lock();
            if(!this.assignedByMe1.contains(endpointID, token)){
                return;
            }
        }

        finally {
            lock.readLock().unlock();
        }
        try{
            lock.writeLock().lock();
            if(!this.assignedByMe1.contains(endpointID, token)){
                return;
            }
            InetSocketAddress remoteSocket = this.assignedByMe1.remove(endpointID, token);
            this.assignedByMe2.remove(remoteSocket, token);
            if(releaseEndpointID) {
                this.factory.passBackEndpointID(endpointID);
            }
            LOG.info("Removed ID to identify remote host {}: {}", remoteSocket, endpointID);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private void addToAssignedByMe(InetSocketAddress remoteSocket, Token token, EndpointID endpointID){
        try {
            this.lock.writeLock().lock();
            this.assignedByMe1.put(endpointID, token, remoteSocket);
            this.assignedByMe2.put(remoteSocket, token, endpointID);
        }
        finally {
            this.lock.writeLock().unlock();
        }
    }

    private boolean updateAssignedByMe(EndpointID endpointID, Token token, InetSocketAddress remoteSocket){

        try {
            this.lock.readLock().lock();
            InetSocketAddress previousRemoteSocket = getFromAssignedByMe(endpointID, token);
            if(remoteSocket.equals(previousRemoteSocket)){
                return false;
            }
        }
        finally {
            this.lock.readLock().unlock();
        }

        try {
            lock.writeLock().lock();
            InetSocketAddress previousRemoteSocket = getFromAssignedByMe(endpointID, token);
            if(remoteSocket.equals(previousRemoteSocket)){
                return false;
            }

            removeFromAssignedByMe(endpointID, token, false);
            addToAssignedByMe(remoteSocket, token, endpointID);

            LOG.info("Updated Socket for remote Endpoint (ID: {}): {}", endpointID, remoteSocket);

            return true;
        }
        finally {
            lock.writeLock().unlock();
        }
    }



}
