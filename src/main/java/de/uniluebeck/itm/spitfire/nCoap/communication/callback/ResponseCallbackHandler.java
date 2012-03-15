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
import de.uniluebeck.itm.spitfire.nCoap.helper.Helper;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * @author Oliver Kleine
 */
public class ResponseCallbackHandler extends SimpleChannelHandler {

    private static Logger log = Logger.getLogger(ResponseCallbackHandler.class.getName());
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

            log.debug("[ResponseCallbackHandler] Handling downstream event!");
            CoapRequest coapRequest = (CoapRequest) me.getMessage();

            if(coapRequest.getCallback() != null){
                try {
                    coapRequest.setToken(TokenFactory.getInstance().getNextToken());
                }
                catch (InvalidOptionException e) {
                    log.debug("[ResponseCallbackHandler] Error while setting token.\n", e);

                    //TODO tell the application an error occured.
                    return;
                }
                catch (ToManyOptionsException e) {
                    log.debug("[ResponseCallbackHandler] Error while setting token.\n", e);

                    //TODO tell the application an error occured.
                    return;
                }

                log.debug("[ResponseCallbackHandler] New Confirmable Request added: \n" +
                            "\tRemote Address: " + me.getRemoteAddress() + "\n" +
                            "\tToken: " + Helper.toHexString(coapRequest.getToken()));

                callbacks.put(new ByteArrayWrapper(coapRequest.getToken()),
                        (InetSocketAddress) me.getRemoteAddress(),
                        coapRequest.getCallback());

                if(log.isDebugEnabled()){
                    log.debug("[ResponseCallbackHandler] Number of registered callbacks: " + callbacks.size());
                }
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

        if(!(me.getMessage() instanceof CoapResponse)){
            ctx.sendUpstream(me);
            return;
        }

        CoapResponse coapResponse = (CoapResponse) me.getMessage();

        if(log.isDebugEnabled()){
           log.debug("[ResponseCallbackHandler] Received message is a response: \n" +
                    "\tRemote Address: " + me.getRemoteAddress() + "\n" +
                    "\tToken: " + Helper.toHexString(coapResponse.getToken()));

            ResponseCallback callback = callbacks.remove(new ByteArrayWrapper(coapResponse.getToken()),
                                                         me.getRemoteAddress());

            if(callback != null){
                if(log.isDebugEnabled()){
                    log.debug("[ResponseCallbackHandler] Response callback found. " +
                            "Invoking method receiveCoapResponse");
                }

                callback.receiveCoapResponse(coapResponse);
            }
        }
    }

    //This wrapper is necessary since two raw byte arrays don't equal even if they have the same content!
    private class ByteArrayWrapper{
        private final byte[] data;

            public ByteArrayWrapper(byte[] data)
            {
                if (data == null)
                {
                    throw new NullPointerException();
                }
                this.data = data;
            }

            @Override
            public boolean equals(Object other)
            {
                if (!(other instanceof ByteArrayWrapper))
                {
                    return false;
                }
                return Arrays.equals(data, ((ByteArrayWrapper) other).data);
            }

            @Override
            public int hashCode()
            {
                return Arrays.hashCode(data);
            }

    }
}
