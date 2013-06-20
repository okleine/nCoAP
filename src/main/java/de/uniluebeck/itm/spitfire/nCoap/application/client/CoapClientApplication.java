/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uniluebeck.itm.spitfire.nCoap.application.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.uniluebeck.itm.spitfire.nCoap.communication.callback.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.communication.callback.TokenFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This is the abstract class to be extended by a CoAP client. By {@link #writeCoapRequest(CoapRequest)} it provides an
 * easy-to-use method to write CoAP requests to a server.
 *
 * @author Oliver Kleine
 */
public class CoapClientApplication extends SimpleChannelHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private HashBasedTable<ByteArrayWrapper, InetSocketAddress, ResponseCallback> responseCallbacks =
            HashBasedTable.create();

    private DatagramChannel datagramChannel;


    public CoapClientApplication(){
        datagramChannel =  CoapClientDatagramChannelFactory.getChannel();
        log.info("New CoAP client on port {}.", datagramChannel.getLocalAddress().getPort());
    }


    /**
     * This method handles downstream message events. It adds a token to outgoing requests to enable the method
     * <code>messageReceived</code> to relate incoming responses to requests.
     *
     * @param ctx The {@link ChannelHandlerContext} to relate this handler to the
     * {@link Channel}
     * @param me The {@link MessageEvent} containing the {@link de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage}
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me) throws Exception{

        if(me.getMessage() instanceof CoapRequest){
            log.debug("CoapRequest received on downstream");

            CoapRequest coapRequest = (CoapRequest) me.getMessage();

            if(coapRequest.getResponseCallback() != null){

                coapRequest.setToken(TokenFactory.getNextToken());

                addResponseCallback(coapRequest.getToken(), (InetSocketAddress) me.getRemoteAddress(),
                        coapRequest.getResponseCallback());

                log.info("New confirmable Request added (Remote Address: {}" + me.getRemoteAddress() +
                        ", Token: " + Tools.toHexString(coapRequest.getToken()) + ")");

                log.debug("Number of registered responseCallbacks: " + responseCallbacks.size());
            }
        }

        ctx.sendDownstream(me);
    }

    private synchronized void addResponseCallback(byte[] token, InetSocketAddress remoteAddress,
                                                  ResponseCallback responseCallback){
        responseCallbacks.put(new ByteArrayWrapper(token), remoteAddress, responseCallback);
    }

//    private synchronized void removeResponseCallback(ResponseCallback responseCallback){
//        Iterator<Table.Cell<ByteArrayWrapper, InetSocketAddress, ResponseCallback>> iterator =
//                responseCallbacks.cellSet().iterator();
//
//        while(iterator.hasNext()){
//            Table.Cell<ByteArrayWrapper, InetSocketAddress, ResponseCallback> cell = iterator.next();
//            if(cell.getValue().equals(responseCallback)){
//                iterator.remove();
//            }
//        }
//    }

    private synchronized ResponseCallback removeResponseCallback(byte[] token, InetSocketAddress remoteAddress){
        return responseCallbacks.remove(new ByteArrayWrapper(token), remoteAddress);
    }

    /**
     * This method relates incoming responses to open requests and invokes the method <code>reveiveCoapResponse</code>
     * of the client that sent the response. CoaP clients thus should implement the {@link ResponseCallback} interface
     * by extending the abstract class {@link de.uniluebeck.itm.spitfire.nCoap.application.client.CoapClientApplication}.
     *
     * @param ctx The {@link ChannelHandlerContext} to relate this handler to the
     * {@link org.jboss.netty.channel.Channel}
     * @param me The {@link MessageEvent} containing the {@link de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage}
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){

        if(me.getMessage() instanceof EmptyAcknowledgementReceivedMessage){
            EmptyAcknowledgementReceivedMessage emptyAcknowledgementReceivedMessage =
                    (EmptyAcknowledgementReceivedMessage) me.getMessage();

            ResponseCallback callback =
                    responseCallbacks.get(emptyAcknowledgementReceivedMessage.getToken(), me.getRemoteAddress());


            callback.receiveEmptyACK();
            me.getFuture().setSuccess();
        }

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse coapResponse = (CoapResponse) me.getMessage();

            log.debug("Received message (" + coapResponse.getMessageType() + ", " + coapResponse.getCode() +
                    ") is a response (Remote Address: " + me.getRemoteAddress() +
                    ", Token: " + Tools.toHexString(coapResponse.getToken()));


            ResponseCallback callback;

            if(coapResponse.isUpdateNotification()){
                callback = responseCallbacks.get(new ByteArrayWrapper(coapResponse.getToken()),
                        me.getRemoteAddress());
            }
            else{
                callback = removeResponseCallback(coapResponse.getToken(), (InetSocketAddress) me.getRemoteAddress());
            }

            if(callback != null){
                log.debug("Received response for request with token " + Tools.toHexString(coapResponse.getToken()));
                callback.receiveResponse(coapResponse);
            }
            else{
                log.debug("No callback found for request with token " + Tools.toHexString(coapResponse.getToken()));
            }
            me.getFuture().setSuccess();
        }

        else if(me.getMessage() instanceof RetransmissionTimeoutMessage){
            RetransmissionTimeoutMessage timeoutMessage = (RetransmissionTimeoutMessage) me.getMessage();

            //Find proper callback
            ResponseCallback callback =
                    removeResponseCallback(timeoutMessage.getToken(), timeoutMessage.getRemoteAddress());

            //Invoke method of callback instance
            if(callback != null){
                log.debug("Invoke retransmission timeout notification");
                callback.handleRetransmissionTimeout(timeoutMessage);
            }
            else{
                log.debug("No callback found for request with token {}.", Tools.toHexString(timeoutMessage.getToken()));
            }

            me.getFuture().setSuccess();
        }
        else{
            ctx.sendUpstream(me);
        }
    }

    /**
     * This method is supposed be used by the extending client implementation to send a CoAP request to a remote
     * recipient. All necessary information to send the message (like the recipient IP address or port) is
     * automatically extracted from the given {@link CoapRequest} object.
     *
     * @param coapRequest The {@link CoapRequest} object to be sent
     */
    public final void writeCoapRequest(CoapRequest coapRequest, ResponseCallback responseCallback){

        int targetPort = coapRequest.getTargetUri().getPort();
        if(targetPort == -1)
            targetPort = OptionRegistry.COAP_PORT_DEFAULT;


        final InetSocketAddress rcptSocketAddress = new InetSocketAddress(coapRequest.getTargetUri().getHost(),
                targetPort);

        ChannelFuture future = Channels.write(datagramChannel, coapRequest, rcptSocketAddress);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("CoAP Request sent to {}:{}", rcptSocketAddress.getAddress().getHostAddress(),
                        rcptSocketAddress.getPort());
            }
        });
    }



    public int getClientPort() {
        return datagramChannel.getLocalAddress().getPort();
    }

    /**
     * Shuts the client down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     */
    public final void shutdown(){
        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture future = datagramChannel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                DatagramChannel closedChannel = (DatagramChannel) future.getChannel();
                log.info("Client channel closed (port: " + closedChannel.getLocalAddress().getPort() + ").");

                //datagramChannel.getFactory().releaseExternalResources();
                //log.info("External resources released. Shutdown completed.");
            }
        });

        future.awaitUninterruptibly();
    }
}
