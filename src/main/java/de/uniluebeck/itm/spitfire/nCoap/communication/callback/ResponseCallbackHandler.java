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

package de.uniluebeck.itm.spitfire.nCoap.communication.callback;

import com.google.common.collect.HashBasedTable;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalAcknowledgementMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.internal.InternalErrorMessage;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @author Oliver Kleine
 */
public class ResponseCallbackHandler extends SimpleChannelHandler {

    private static Logger log = LoggerFactory.getLogger(ResponseCallbackHandler.class.getName());
    private static ResponseCallbackHandler instance = new ResponseCallbackHandler();

    HashBasedTable<ByteArrayWrapper, InetSocketAddress, ResponseCallback> callbacks = HashBasedTable.create();

    private ResponseCallbackHandler(){}

    public static ResponseCallbackHandler getInstance(){
        return instance;
    }

    /**
     * This method handles downstream message events. It adds a token to outgoing requests to enable the method
     * <code>messageReceived</code> to relate incoming responses to requests.
     *
     * @param ctx The {@link ChannelHandlerContext} to relate this handler to the
     * {@link org.jboss.netty.channel.Channel}
     * @param me The {@link MessageEvent} containing the {@link de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage}
     */
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){

        if(me.getMessage() instanceof CoapRequest){
            log.debug("CoapRequest received on downstream");

            CoapRequest coapRequest = (CoapRequest) me.getMessage();

            if(coapRequest.getResponseCallback() != null){
                try {
                    coapRequest.setToken(TokenFactory.getInstance().getNextToken());
                }
                catch (InvalidOptionException e) {
                    String errorMessage = "Internal CoAP error while setting token: " + e.getCause();
                    log.error(errorMessage);

                    UpstreamMessageEvent ume = new UpstreamMessageEvent(ctx.getChannel(),
                            new InternalErrorMessage(errorMessage, coapRequest.getToken()), me.getRemoteAddress());
                    ctx.sendUpstream(ume);
                    return;
                }
                catch (ToManyOptionsException e) {
                    String errorMessage = "Internal CoAP error while setting token: " + e.getCause();
                    log.error(errorMessage);

                    UpstreamMessageEvent ume = new UpstreamMessageEvent(ctx.getChannel(),
                            new InternalErrorMessage(errorMessage, coapRequest.getToken()), me.getRemoteAddress());
                    ctx.sendUpstream(ume);
                    return;
                }

                callbacks.put(new ByteArrayWrapper(coapRequest.getToken()),
                        (InetSocketAddress) me.getRemoteAddress(),
                        coapRequest.getResponseCallback());

                log.info("New confirmable Request added (Remote Address: " + me.getRemoteAddress() +
                        ", Token: " + Tools.toHexString(coapRequest.getToken()));

                log.debug("Number of registered callbacks: " + callbacks.size());
            }
        }
        ctx.sendDownstream(me);
    }

    /**
     * This method relates incoming responses to open requests and invokes the method <code>reveiveCoapResponse</code>
     * of the client that sent the response. CoaP clients thus should implement the {@link ResponseCallback} interface
     * by extending the abstract class {@link de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication}.
     *
     * @param ctx The {@link ChannelHandlerContext} to relate this handler to the
     * {@link org.jboss.netty.channel.Channel}
     * @param me The {@link MessageEvent} containing the {@link de.uniluebeck.itm.spitfire.nCoap.message.CoapMessage}
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){

        if(me.getMessage() instanceof CoapResponse){
            CoapResponse coapResponse = (CoapResponse) me.getMessage();

            log.debug(" Received message (" + coapResponse.getMessageType() + ", " + coapResponse.getCode() +
                        ") is a response (Remote Address: " + me.getRemoteAddress() +
                        ", Token: " + Tools.toHexString(coapResponse.getToken()));


            ResponseCallback callback = callbacks.remove(new ByteArrayWrapper(coapResponse.getToken()),
                                                         me.getRemoteAddress());

            if(callback != null){
                log.debug(" Received response for request with token " + Tools.toHexString(coapResponse.getToken()));
                callback.receiveResponse(coapResponse);
            }
        }
        else if (me.getMessage() instanceof InternalAcknowledgementMessage){

            ByteArrayWrapper token = ((InternalAcknowledgementMessage) me.getMessage()).getContent();

            ResponseCallback callback = callbacks.get(token, me.getRemoteAddress());

            if(callback != null){
                log.debug("Received empty acknowledgement for request with token " + token.toHexString());
                callback.receiveEmptyACK();
            }
        }
        else if (me.getMessage() instanceof InternalErrorMessage){
            InternalErrorMessage errorMessage = (InternalErrorMessage) me.getMessage();
            ByteArrayWrapper token = new ByteArrayWrapper(errorMessage.getToken());
            ResponseCallback callback = callbacks.get(token, me.getRemoteAddress());

            if(callback != null){
                String error = "Received internal error message for request with token " + token.toHexString() +
                        ":\n" + errorMessage.getContent();
                log.debug(error);
                callback.receiveInternalError(error);
            }

        }
        else{
            ctx.sendUpstream(me);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception{
        log.debug(" Exception caught:", e.getCause());
    }


}
