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
package de.uzl.itm.ncoap.communication.observing;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.application.server.CoapServer;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.ResetReceivedEvent;
import de.uzl.itm.ncoap.communication.events.TransmissionTimeoutEvent;
import de.uzl.itm.ncoap.communication.events.server.ObserverAcceptedEvent;
import de.uzl.itm.ncoap.communication.events.server.RemoteClientSocketChangedEvent;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * The {@link ServerObservationHandler} is responsible to maintain the list of registered clients observing any
 * of the {@link ObservableWebresource}s available on this {@link CoapServer} instance.
 *
 * @author Oliver Kleine
 */
public class ServerObservationHandler extends AbstractCoapChannelHandler implements Observer,
        ResetReceivedEvent.Handler, ObserverAcceptedEvent.Handler, RemoteClientSocketChangedEvent.Handler,
        TransmissionTimeoutEvent.Handler {

    private static Logger LOG = LoggerFactory.getLogger(ServerObservationHandler.class.getName());

    private HashBasedTable<InetSocketAddress, Token, ObservationParams> observations1;
    private HashBasedTable<ObservableWebresource, InetSocketAddress, Token> observations2;

    private ReentrantReadWriteLock lock;

    /**
     * Creates a new instance of {@link ServerObservationHandler}
     *
     * @param executor the {@link ScheduledExecutorService} to handle internal tasks such as sending update
     *                 notifications
     */
    public ServerObservationHandler(ScheduledExecutorService executor) {
        super(executor);
        this.observations1 = HashBasedTable.create();
        this.observations2 = HashBasedTable.create();

        this.lock = new ReentrantReadWriteLock();
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

        if (coapMessage instanceof CoapRequest) {
            stopObservation(remoteSocket, coapMessage.getToken());
        }

        return true;
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {

        if (coapMessage instanceof CoapResponse && !((CoapResponse) coapMessage).isUpdateNotification()) {
            if (stopObservation(remoteSocket, coapMessage.getToken()) != null) {
                LOG.info("Observation stopped due to non-update-notification response.");
            }
        }

        return true;
    }


    @Override
    public void handleEvent(ResetReceivedEvent event) {
        if (stopObservation(event.getRemoteSocket(), event.getToken()) != null) {
            LOG.info("Observation stopped due to RST message from \"{}\" (Token: {})!", event.getRemoteSocket(),
                    event.getToken());
        }
    }


    @Override
    public void handleEvent(ObserverAcceptedEvent event) {
        startObservation(event.getRemoteSocket(), event.getToken(), event.getWebresource(), event.getContentFormat(),
                event.getBlock2Size());
    }

    @Override
    public void handleEvent(TransmissionTimeoutEvent event) {
       if (stopObservation(event.getRemoteSocket(), event.getToken()) != null) {
        LOG.info("Observation stopped due to transmission timeout of latest update notification!");
       }
    }

    @Override
    public void handleEvent(RemoteClientSocketChangedEvent event) {
        if (this.updateObserverSocket(event.getPreviousRemoteSocket(), event.getRemoteSocket(), event.getToken())) {
            LOG.info("Updated observer socket (Old: \"{}\", New: \"{}\", Token: {})", new Object[]{
               event.getPreviousRemoteSocket(), event.getRemoteSocket(), event.getToken()
            });
        }
    }


    public void registerWebresource(ObservableWebresource webresource) {
        LOG.debug("ServerObservationHandler is now observing \"{}\".", webresource.getUriPath());
        webresource.addObserver(this);
    }


    private void startObservation(InetSocketAddress remoteSocket, Token token, ObservableWebresource webresource,
            long contentFormat, BlockSize block2Size) {

        try {
            this.lock.writeLock().lock();
            ObservationParams params =
                    new ObservationParams(webresource, remoteSocket, token, contentFormat, block2Size);
            this.observations1.put(remoteSocket, token, params);
            this.observations2.put(webresource, remoteSocket, token);
            LOG.info("Client \"{}\" is now observing \"{}\".", remoteSocket, webresource.getUriPath());
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private ObservationParams stopObservation(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            if (!this.observations1.contains(remoteSocket, token)) {
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            ObservationParams params = this.observations1.remove(remoteSocket, token);
            if (params == null) {
                return null;
            }
            this.observations2.remove(params.getWebresource(), remoteSocket);
            LOG.info("Client \"{}\" is no longer observing \"{}\" (token was: \"{}\").",
                    new Object[]{remoteSocket, params.getWebresource().getUriPath(), token});
            return params;

        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private boolean updateObserverSocket(InetSocketAddress previousRemoteSocket, InetSocketAddress newRemoteSocket,
                                      Token token) {

        try {
            this.lock.writeLock().lock();
            ObservationParams params = this.observations1.remove(previousRemoteSocket, token);
            if (params == null) {
                return false;
            } else {
                this.observations2.remove(params.getWebresource(), previousRemoteSocket);
                this.startObservation(newRemoteSocket, token, params.getWebresource(), params.getContentFormat(),
                        params.getBlock2Size());
                return true;
            }
        } finally {
            this.lock.writeLock().unlock();
        }

    }

    @Override
    public void update(Observable observable, Object type) {
        ObservableWebresource webresource = (ObservableWebresource) observable;
        LOG.info("Webresource {} was updated. Starting to send update notifications to observers.");
        if (type.equals(ObservableWebresource.UPDATE)) {
            sendUpdateNotifications(webresource);
        } else if (type.equals(ObservableWebresource.SHUTDOWN)) {
            sendShutdownNotifications(webresource);
        }
    }

    private void sendShutdownNotifications(ObservableWebresource webresource) {
        try {
            this.lock.writeLock().lock();
            Map<InetSocketAddress, Token> observations = new HashMap<>(this.observations2.row(webresource));
            for (Map.Entry<InetSocketAddress, Token> observation : observations.entrySet()) {
                InetSocketAddress remoteSocket = observation.getKey();
                Token token = observation.getValue();
                ObservationParams params = stopObservation(remoteSocket, token);
                if(params != null) {
                    BlockSize block2Size = params.getBlock2Size();
                    String uriPath = webresource.getUriPath();
                    getExecutor().submit(new ShutdownNotificationTask(remoteSocket, token, uriPath, block2Size));
                } else {
                    LOG.error("This should never happen!");
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }

    }


    private void sendUpdateNotifications(ObservableWebresource webresource) {
        try {
            this.lock.readLock().lock();
            Map<Long, WrappedResourceStatus> representations = new HashMap<>();
            for(Map.Entry<InetSocketAddress, Token> observation : this.observations2.row(webresource).entrySet()) {
                // determine observation specific data
                InetSocketAddress remoteSocket = observation.getKey();
                Token token = observation.getValue();
                ObservationParams params = this.observations1.get(remoteSocket, token);
                long contentFormat = params.getContentFormat();
                BlockSize block2Size = params.getBlock2Size();

                // get the actual resource status
                WrappedResourceStatus status = representations.get(contentFormat);
                if (status == null) {
                    status = webresource.getWrappedResourceStatus(contentFormat);
                    representations.put(contentFormat, status);
                }

                // schedule update notification (immediately)
                boolean confirmable = webresource.isUpdateNotificationConfirmable(remoteSocket);
                int messageType =  confirmable ? MessageType.CON : MessageType.NON;
                getExecutor().submit(new UpdateNotificationTask(remoteSocket, status, messageType, token, block2Size));
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private class ObservationParams {

        private ObservableWebresource webresource;
        private InetSocketAddress remoteSocket;
        private Token token;
        private long contentFormat;
        private BlockSize block2Size;

        public ObservationParams(ObservableWebresource webresource, InetSocketAddress remoteSocket, Token token,
                                 long contentFormat, BlockSize block2Size) {

            this.webresource = webresource;
            this.remoteSocket = remoteSocket;
            this.token = token;
            this.contentFormat = contentFormat;
            this.block2Size = block2Size;
        }


        public InetSocketAddress getRemoteSocket() {
            return remoteSocket;
        }

        public void setRemoteSocket(InetSocketAddress remoteSocket) {
            this.remoteSocket = remoteSocket;
        }

        public Token getToken() {
            return token;
        }

        public long getContentFormat() {
            return contentFormat;
        }

        public BlockSize getBlock2Size() {
            return block2Size;
        }

        public ObservableWebresource getWebresource() {
            return webresource;
        }
    }



    private class ShutdownNotificationTask implements Runnable{

        private InetSocketAddress remoteSocket;
        private Token token;
        private String webresourcePath;
        private BlockSize block2Size;


        public ShutdownNotificationTask(InetSocketAddress remoteSocket, Token token, String webresourcePath,
                                        BlockSize block2Size) {
            this.remoteSocket = remoteSocket;
            this.token = token;
            this.webresourcePath = webresourcePath;
            this.block2Size = block2Size;
        }

        public void run() {
            //prepare CoAP response
            CoapResponse coapResponse = new CoapResponse(MessageType.NON, MessageCode.NOT_FOUND_404);
            coapResponse.setToken(token);
            String content = "Resource \"" + this.webresourcePath + "\" is no longer available.";
            coapResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
            coapResponse.setPreferredBlock2Size(block2Size);

            ChannelFuture future = sendCoapMessage(coapResponse, this.remoteSocket);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        LOG.error("Shutdown Notification Failure!", future.cause());
                    } else {
                        LOG.info("Sent NOT_FOUND to \"{}\" (Token: {}).", remoteSocket, token);
                    }
                }
            });
        }
    }

    private class UpdateNotificationTask implements Runnable{

        private InetSocketAddress remoteSocket;
        private int messageType;
        private Token token;
        private BlockSize block2Size;
        private WrappedResourceStatus representation;

        public UpdateNotificationTask(InetSocketAddress remoteSocket, WrappedResourceStatus representation,
                    int messageType, Token token, BlockSize block2Size) {

            this.remoteSocket = remoteSocket;
            this.representation = representation;
            this.messageType = messageType;
            this.token = token;
            this.block2Size = block2Size;
        }

        public void run() {
            try {
                CoapResponse updateNotification = new CoapResponse(messageType, MessageCode.CONTENT_205);
                updateNotification.setToken(token);
                updateNotification.setEtag(representation.getEtag());
                updateNotification.setContent(representation.getContent(), representation.getContentFormat());
                updateNotification.setMaxAge(representation.getMaxAge());
                updateNotification.setObserve();
                updateNotification.setPreferredBlock2Size(block2Size);

                ChannelFuture future = sendCoapMessage(updateNotification, remoteSocket);

                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            LOG.error("Update Notification Failure!", future.cause());
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
