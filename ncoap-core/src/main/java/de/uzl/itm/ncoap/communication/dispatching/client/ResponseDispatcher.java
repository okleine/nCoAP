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
package de.uzl.itm.ncoap.communication.dispatching.client;

import com.google.common.collect.HashBasedTable;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.communication.AbstractCoapChannelHandler;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.events.client.*;
import de.uzl.itm.ncoap.communication.events.*;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>The {@link ResponseDispatcher} is responsible for
 * processing inbound {@link de.uzl.itm.ncoap.message.CoapResponse}s. That is why each
 * {@link de.uzl.itm.ncoap.message.CoapRequest} needs an associated instance of
 * {@link de.uzl.itm.ncoap.application.client.ClientCallback} to be called upon reception
 * of a related {@link de.uzl.itm.ncoap.message.CoapResponse}.</p>
 * <p/>
 * <p>Besides the response dispatching the
 * {@link ResponseDispatcher} also deals with
 * the reliability of inbound {@link de.uzl.itm.ncoap.message.CoapResponse}s, i.e. sends RST or ACK
 * messages if necessary.</p>
 *
 * @author Oliver Kleine
 */
public class ResponseDispatcher extends AbstractCoapChannelHandler implements RemoteServerSocketChangedEvent.Handler,
        EmptyAckReceivedEvent.Handler, ResetReceivedEvent.Handler, MessageIDAssignedEvent.Handler,
        MessageRetransmittedEvent.Handler, TransmissionTimeoutEvent.Handler, NoMessageIDAvailableEvent.Handler,
        MiscellaneousErrorEvent.Handler, TokenReleasedEvent.Handler, ResponseBlockReceivedEvent.Handler,
        BlockwiseResponseTransferFailedEvent.Handler, ContinueResponseReceivedEvent.Handler,
        MessageIDReleasedEvent.Handler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private TokenFactory tokenFactory;

    private HashBasedTable<InetSocketAddress, Token, ClientCallback> clientCallbacks;
    private ReentrantReadWriteLock lock;


    /**
     * Creates a new instance of {@link ResponseDispatcher}
     *
     * @param executor     the {@link java.util.concurrent.ScheduledExecutorService} to execute the tasks, e.g. send,
     *                     receive and process {@link de.uzl.itm.ncoap.message.CoapMessage}s.
     * @param tokenFactory the {@link TokenFactory} to
     *                     provide {@link Token}
     *                     instances for outbound {@link de.uzl.itm.ncoap.message.CoapRequest}s
     */
    public ResponseDispatcher(ScheduledExecutorService executor, TokenFactory tokenFactory) {
        super(executor);
        this.clientCallbacks = HashBasedTable.create();
        this.lock = new ReentrantReadWriteLock();
        this.tokenFactory = tokenFactory;
    }


    @Override
    public boolean handleInboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        if (coapMessage instanceof CoapResponse) {
            handleInboundCoapResponse((CoapResponse) coapMessage, remoteSocket);
            return false;
        } else {
            return true;
        }
    }


    @Override
    public boolean handleOutboundCoapMessage(CoapMessage coapMessage, InetSocketAddress remoteSocket) {
        return true;
    }


    @Override
    public void handleEvent(RemoteServerSocketChangedEvent event) {
        InetSocketAddress previousSocket = event.getPreviousRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = updateCallback(event.getRemoteSocket(), previousSocket, token);
        if (callback != null) {
            callback.processRemoteSocketChanged(event.getRemoteSocket(), previousSocket);
        } else {
            log.warn("No callback found for socket change (previous: \"{}\", token: {})", previousSocket, token);
        }
    }


    @Override
    public void handleEvent(EmptyAckReceivedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = getCallback(remoteSocket, token);
        if (callback != null) {
            callback.processEmptyAcknowledgement();
        } else {
            log.warn("No callback found for empty ACK (remote socket: \"{}\", token: {})", remoteSocket, token);
        }
    }

    @Override
    public void handleEvent(TokenReleasedEvent event) {
        Token token = event.getToken();
        this.tokenFactory.releaseToken(token);
    }


    @Override
    public void handleEvent(ResetReceivedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = removeCallback(remoteSocket, token);
        if (callback != null) {
            callback.processReset();
        } else {
            log.warn("No callback found for RESET (remote socket: \"{}\", token: {}", remoteSocket, token);
        }
    }


//    @Override
//    public void handleEvent(PartialContentReceivedEvent event) {
//        InetSocketAddress remoteSocket = event.getRemoteSocket();
//        Token token = event.getToken();
//        ClientCallback callback = getCallback(remoteSocket, token);
//        if (callback != null) {
//            callback.processReset();
//        } else {
//            log.warn("No callback found for block reception (remote socket: \"{}\", token: {}", remoteSocket, token);
//        }
//    }


    @Override
    public void handleEvent(MessageIDAssignedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = getCallback(remoteSocket, token);
        if (callback != null) {
            callback.processMessageIDAssignment(event.getMessageID());
        } else {
            log.warn("No callback found for MsgID assignment (remote socket: \"{}\", token: {}", remoteSocket, token);
        }
    }

    @Override
    public void handleEvent(NoMessageIDAvailableEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = removeCallback(remoteSocket, token);
        if (callback != null) {
            callback.processNoMessageIDAvailable();
        } else {
            log.warn("No callback found for \"no MsgID available\" (remote socket: \"{}\", token: {}", remoteSocket, token);
        }
    }


    @Override
    public void handleEvent(MessageRetransmittedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = getCallback(remoteSocket, token);
        if (callback != null) {
            callback.processRetransmission();
        } else {
            log.warn("No callback found for retransmission (remote socket: \"{}\", token: {}", remoteSocket, token);
        }
    }


    @Override
    public void handleEvent(TransmissionTimeoutEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = removeCallback(remoteSocket, token);
        if (callback != null) {
            callback.processTransmissionTimeout();
        } else {
            log.warn("No callback found for timeout (remote socket: \"{}\", token: {}, message ID: {})",
                new Object[]{remoteSocket, token, event.getMessageID()});
        }
    }

    @Override
    public void handleEvent(ResponseBlockReceivedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = getCallback(remoteSocket, token);
        if (callback != null) {
            callback.processResponseBlockReceived(event.getReceivedLength(), event.getExpectedLength());
        } else {
            log.warn("No callback found for partial response (remote socket: \"{}\", token: {})", remoteSocket, token);
        }
    }


    @Override
    public void handleEvent(ContinueResponseReceivedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = getCallback(remoteSocket, token);
        if (callback != null) {
            callback.processContinueResponseReceived(event.getBlock1Size());
        } else {
            log.warn("No callback found for CONTINUE (remote socket: \"{}\", token: {})", remoteSocket, token);
        }
    }
    
    @Override
    public void handleEvent(BlockwiseResponseTransferFailedEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = removeCallback(remoteSocket, token);
        if (callback != null) {
            callback.processBlockwiseResponseTransferFailed();
        } else {
            log.warn("No callback found for blockwise response transfer failure (remote socket: \"{}\", token: \"{}\")",
                remoteSocket, token);
        }
    }

    @Override
    public void handleEvent(MiscellaneousErrorEvent event) {
        InetSocketAddress remoteSocket = event.getRemoteSocket();
        Token token = event.getToken();
        ClientCallback callback = removeCallback(remoteSocket, token);
        if (callback != null) {
            callback.processMiscellaneousError(event.getDescription());
        } else {
            log.warn("No callback found for misc. error (remote socket: \"{}\", token: {}", remoteSocket, token);
        }
    }

    /**
     * This method is called by the {@link de.uzl.itm.ncoap.application.client.CoapClient} or by the
     * {@link de.uzl.itm.ncoap.application.endpoint.CoapEndpoint} to send a request to a remote endpoint (server).
     *
     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be sent
     * @param remoteSocket the {@link java.net.InetSocketAddress} of the recipient
     * @param callback the {@link de.uzl.itm.ncoap.application.client.ClientCallback} to be
     * called upon reception of a response or any kind of
     * {@link de.uzl.itm.ncoap.communication.events.AbstractMessageExchangeEvent}.
     */
    public void sendCoapRequest(CoapRequest coapRequest, InetSocketAddress remoteSocket, ClientCallback callback) {
        getExecutor().submit(new WriteCoapMessageTask(coapRequest, remoteSocket, callback));
    }

//    /**
//     * This method is called by the {@link de.uzl.itm.ncoap.application.client.CoapClient} or by the
//     * {@link de.uzl.itm.ncoap.application.endpoint.CoapEndpoint} to send a request to a remote endpoint (server).
//     *
//     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be sent
//     * @param remoteSocket the {@link java.net.InetSocketAddress} of the recipient
//     * @param callback the {@link de.uzl.itm.ncoap.application.client.ClientCallback} to be
//     * called upon reception of a response or any kind of
//     * {@link de.uzl.itm.ncoap.communication.events.AbstractMessageExchangeEvent}.
//     */
//    public void sendCoapRequest(CoapRequest coapRequest, InetSocketAddress remoteSocket, ClientCallback callback,
//                                BlockSize block1Size, BlockSize block2Size) {
//        getExecutor().submit(
//                new WriteCoapMessageTask(coapRequest, remoteSocket, callback, block1Size, block2Size)
//        );
//    }

    public void sendCoapPing(InetSocketAddress remoteSocket, ClientCallback callback) {
        final CoapMessage coapPing = CoapMessage.createPing(CoapMessage.UNDEFINED_MESSAGE_ID);
        getExecutor().submit(
                new WriteCoapMessageTask(coapPing, remoteSocket, callback)
        );
    }


    private ClientCallback updateCallback(InetSocketAddress remoteSocket, InetSocketAddress previous, Token token) {
        try {
            this.lock.readLock().lock();
            if (getCallback(previous, token) == null) {
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            ClientCallback callback = this.clientCallbacks.remove(previous, token);
            if (callback != null) {
                this.clientCallbacks.put(remoteSocket, token, callback);
                log.info("Updated remote socket (old: \"{}\", new: \"{}\")", previous, remoteSocket);
            }
            return callback;
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private void addCallback(InetSocketAddress remoteSocket, Token token, ClientCallback clientCallback) {
        try {
            this.lock.readLock().lock();
            if (this.clientCallbacks.contains(remoteSocket, token)) {
                log.error("Tried to use token twice (remote endpoint: {}, token: {})", remoteSocket, token);
                return;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            if (this.clientCallbacks.contains(remoteSocket, token)) {
                log.error("Tried to use token twice (remote endpoint: {}, token: {})", remoteSocket, token);
            } else {
                clientCallbacks.put(remoteSocket, token, clientCallback);
                log.info("Added callback (remote endpoint: {}, token: {})", remoteSocket, token);
                if (this.clientCallbacks.size() > 1000) {
                    log.error("More than 1000 callbacks!");
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    private ClientCallback removeCallback(InetSocketAddress remoteSocket, Token token) {
        try {
            this.lock.readLock().lock();
            if (!this.clientCallbacks.contains(remoteSocket, token)) {
                log.info("No callback found to be removed (remote endpoint: {}, token: {})", remoteSocket, token);
                return null;
            }
        } finally {
            this.lock.readLock().unlock();
        }

        try {
            this.lock.writeLock().lock();
            ClientCallback callback = clientCallbacks.remove(remoteSocket, token);
            if (callback == null) {
                log.info("No callback found to be removed (remote endpoint: {}, token: {})", remoteSocket, token);
            } else {
                log.info("Removed callback (remote endpoint: {}, token: {}). Remaining: {}",
                        new Object[]{remoteSocket, token, this.clientCallbacks.size()});
                triggerEvent(new TokenReleasedEvent(remoteSocket, token), true);
            }
            return callback;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private ClientCallback getCallback(InetSocketAddress remoteAddress, Token token) {
        try {
            this.lock.readLock().lock();
            return this.clientCallbacks.get(remoteAddress, token);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private void handleInboundCoapResponse(CoapResponse coapResponse, InetSocketAddress remoteSocket) {
        Token token = coapResponse.getToken();
        ClientCallback callback = getCallback(remoteSocket, token);

        if (callback == null) {
            log.warn("No callback found for CoAP response (from {}): {}", remoteSocket, coapResponse);
        } else if (!coapResponse.isLastBlock2()) {
            callback.processCoapResponse(coapResponse);
            log.debug("Callback found for token {} from {}.", token, remoteSocket);
        } else {
            if (coapResponse.isErrorResponse() || !coapResponse.isUpdateNotification()) {
                removeCallback(remoteSocket, token);
                log.debug("Callback removed because inbound response was no update notification!");
            }
            callback.processCoapResponse(coapResponse);

            if (coapResponse.isUpdateNotification() && !callback.continueObservation()) {
                removeCallback(remoteSocket, token);
            }
            log.debug("Callback found for token {} from {}.", token, remoteSocket);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee) {
        log.error("Exception: ", ee.getCause());
    }

    @Override
    public void handleEvent(MessageIDReleasedEvent event) {

    }


    private class WriteCoapMessageTask implements Runnable {

        private final CoapMessage coapMessage;
        private final InetSocketAddress remoteSocket;
        private final ClientCallback callback;

        public WriteCoapMessageTask(CoapMessage coapMessage, InetSocketAddress remoteSocket, ClientCallback callback) {

            this.coapMessage = coapMessage;
            this.remoteSocket = remoteSocket;
            this.callback = callback;
        }

        @Override
        public void run() {
            if (this.coapMessage.isPing()) {
                //CoAP ping
                Token emptyToken = new Token(new byte[0]);
                if (getCallback(remoteSocket, emptyToken) != null) {
                    String description = "There is another ongoing PING for \"" + remoteSocket + "\".";
                    callback.processMiscellaneousError(description);
                    return;
                } else {
                    // no other PING for the same remote socket...
                    this.coapMessage.setToken(emptyToken);
                }
            } else if (this.coapMessage.getMessageCode() == MessageCode.GET && this.coapMessage.getObserve() == 1) {
                // request to stop an ongoing observation
                Token token = this.coapMessage.getToken();
                if (getCallback(this.remoteSocket, token) == null) {
                    String description = "No ongoing observation on remote endpoint " + remoteSocket
                            + " and token " + token + "!";
                    this.callback.processMiscellaneousError(description);
                    return;
                }
            } else {
                //Prepare CoAP request, the response reception and then send the CoAP request
                Token token = tokenFactory.getNextToken();
                if (token == null) {
                    String description = "No token available for remote endpoint " + remoteSocket + ".";
                    this.callback.processMiscellaneousError(description);
                    return;
                } else {
                    this.coapMessage.setToken(token);
                }
            }

            //Add the response callback to wait for the inbound response
            addCallback(this.remoteSocket, this.coapMessage.getToken(), this.callback);
            sendRequest();
        }

        private void sendRequest() {
            ChannelFuture future = Channels.future(getContext().getChannel());
            Channels.write(getContext(), future, coapMessage, this.remoteSocket);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        ClientCallback callback = removeCallback(remoteSocket, coapMessage.getToken());
                        log.error("Could not write CoAP Request!", future.getCause());
                        if (callback != null) {
                            callback.processMiscellaneousError("Message could not be sent (Reason: \"" +
                                    future.getCause().getMessage() + ")\"");
                        }
                    }
                }
            });
        }
    }
}
