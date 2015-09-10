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
package de.uzl.itm.ncoap.communication.observing;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.webresource.WrappedResourceStatus;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.communication.events.server.RemoteClientSocketChangedEvent;
import de.uzl.itm.ncoap.communication.events.server.ObserverAcceptedEvent;
import de.uzl.itm.ncoap.communication.events.ResetReceivedEvent;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by olli on 04.09.15.
 */
public class ServerObservationHandler extends AbstractCoapChannelHandler implements Observer,
        ResetReceivedEvent.Handler, ObserverAcceptedEvent.Handler, RemoteClientSocketChangedEvent.Handler{

    private static Logger LOG = LoggerFactory.getLogger(ServerObservationHandler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, ObservableWebresource> observations1;
    private HashBasedTable<ObservableWebresource, InetSocketAddress, Token> observations2;
    private HashBasedTable<InetSocketAddress, Token, Long> contentFormats;

    private ReentrantReadWriteLock lock;


    public ServerObservationHandler(ScheduledExecutorService executor){
        super(executor);
        this.observations1 = HashBasedTable.create();
        this.observations2 = HashBasedTable.create();
        this.contentFormats = HashBasedTable.create();

        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

        if(coapMessage instanceof CoapRequest) {
            stopObservation(remoteSocket, coapMessage.getToken());
        }

        return true;
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

        if(coapMessage instanceof CoapResponse && !((CoapResponse) coapMessage).isUpdateNotification()){
            stopObservation(remoteSocket, coapMessage.getToken());
        }

        return true;
    }


    @Override
    public void handleEvent(ResetReceivedEvent event) {
        stopObservation(event.getRemoteSocket(), event.getToken());
    }


    @Override
    public void handleEvent(ObserverAcceptedEvent event) {
        startObservation(event.getRemoteSocket(), event.getToken(), event.getWebresource(), event.getContentFormat());
    }

    @Override
    public void handleEvent(RemoteClientSocketChangedEvent event) {
        // TODO
    }


    public void registerWebresource(ObservableWebresource webresource) {
        LOG.debug("ServerObservationHandler is now observing \"{}\".", webresource.getUriPath());
        webresource.addObserver(this);
    }


    private void startObservation(InetSocketAddress remoteAddress, Token token, ObservableWebresource webresource,
            long contentFormat){

        try {
            this.lock.writeLock().lock();
            this.observations1.put(remoteAddress, token, webresource);
            this.observations2.put(webresource, remoteAddress, token);
            this.contentFormats.put(remoteAddress, token, contentFormat);
            LOG.info("Client \"{}\" is now observing \"{}\".", remoteAddress, webresource.getUriPath());

        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void stopObservation(InetSocketAddress remoteSocket, Token token){
        try {
            this.lock.readLock().lock();
            if(!this.observations1.contains(remoteSocket, token)){
                return;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            ObservableWebresource webresource = this.observations1.remove(remoteSocket, token);
            if(webresource == null){
                return;
            }
            this.observations2.remove(webresource, remoteSocket);
            this.contentFormats.remove(remoteSocket, token);
            LOG.info("Client \"{}\" is no longer observing \"{}\" (token was: \"{}\").",
                    new Object[]{remoteSocket, webresource.getUriPath(), token});

        } finally {
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public void update(Observable observable, Object type) {
        ObservableWebresource webresource = (ObservableWebresource) observable;
        if(type.equals(ObservableWebresource.UPDATE)) {
            sendUpdateNotifications(webresource);
        } else if(type.equals(ObservableWebresource.SHUTDOWN)){
            sendShutdownNotifications(webresource);
        }
    }

    private void sendShutdownNotifications(ObservableWebresource webresource){
        try {
            this.lock.writeLock().lock();
            Map<InetSocketAddress, Token> observations = new HashMap<>(this.observations2.row(webresource));

            for (Map.Entry<InetSocketAddress, Token> observation : observations.entrySet()) {
                InetSocketAddress remoteSocket = observation.getKey();
                Token token = observation.getValue();
                stopObservation(remoteSocket, token);

                getExecutor().submit(new ShutdownNotificationTask(remoteSocket, token, webresource.getUriPath()));
            }
        } finally {
            this.lock.writeLock().unlock();
        }

    }


    private void sendUpdateNotifications(ObservableWebresource webresource){
        try {
            this.lock.readLock().lock();
            Map<Long, WrappedResourceStatus> representations = new HashMap<>();
            for(Map.Entry<InetSocketAddress, Token> observation : this.observations2.row(webresource).entrySet()){
                InetSocketAddress remoteSocket = observation.getKey();
                Token token = observation.getValue();
                long contentFormat = this.contentFormats.get(remoteSocket, token);

                WrappedResourceStatus status = representations.get(contentFormat);
                if(status == null) {
                    status = webresource.getWrappedResourceStatus(contentFormat);
                    representations.put(contentFormat, status);
                }

                // schedule update notification (immediately)
                boolean confirmable = webresource.isUpdateNotificationConfirmable(remoteSocket);
                MessageType.Name messageType =  confirmable ? MessageType.Name.CON : MessageType.Name.NON;
                UpdateNotificationTask task = new UpdateNotificationTask(remoteSocket, status, messageType, token);
                getExecutor().schedule(task, 0, TimeUnit.MILLISECONDS);
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private class ShutdownNotificationTask implements Runnable{

        private InetSocketAddress remoteSocket;
        private Token token;
        private String webresourcePath;


        public ShutdownNotificationTask(InetSocketAddress remoteSocket, Token token, String webresourcePath){
            this.remoteSocket = remoteSocket;
            this.token = token;
            this.webresourcePath = webresourcePath;
        }

        public void run() {
            CoapResponse coapResponse = new CoapResponse(MessageType.Name.NON, MessageCode.Name.NOT_FOUND_404);
            coapResponse.setToken(token);
            String content = "Resource \"" + this.webresourcePath + "\" is no longer available.";
            coapResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

            ChannelFuture future = Channels.future(getContext().getChannel());
            Channels.write(getContext(), future, coapResponse, remoteSocket);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(!future.isSuccess()){
                        LOG.error("Shutdown Notification Failure!", future.getCause());
                    }
                    else{
                        LOG.info("Sent NOT_FOUND to \"{}\" (Token: {}).", remoteSocket, token);
                    }
                }
            });
        }
    }

    private class UpdateNotificationTask implements Runnable{

        private InetSocketAddress remoteSocket;
        private MessageType.Name messageType;
        private Token token;
        private WrappedResourceStatus representation;

        public UpdateNotificationTask(InetSocketAddress remoteSocket, WrappedResourceStatus representation,
                    MessageType.Name messageType, Token token){

            this.remoteSocket = remoteSocket;
            this.representation = representation;
            this.messageType = messageType;
            this.token = token;
        }

        public void run() {
            try {
                CoapResponse updateNotification = new CoapResponse(messageType, MessageCode.Name.CONTENT_205);
                updateNotification.setToken(token);
                updateNotification.setEtag(representation.getEtag());
                updateNotification.setContent(representation.getContent(), representation.getContentFormat());
                updateNotification.setObserve();

                ChannelFuture future = Channels.future(getContext().getChannel());
                Channels.write(getContext(), future, updateNotification, remoteSocket);

                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            LOG.error("Update Notification Failure!", future.getCause());
                        } else {
                            LOG.info("Update Notification sent to \"{}\" (Token: {}).", remoteSocket, token);
                        }
                    }
                });
            } catch (Exception ex) {
                LOG.error("Exception!", ex);
            }
        }
    }
}
