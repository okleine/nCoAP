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
import com.google.common.primitives.Ints;
import de.uniluebeck.itm.spitfire.nCoap.application.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.message.Message;
import de.uniluebeck.itm.spitfire.nCoap.message.Request;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * @author Oliver Kleine
 */
public class ResponseCallbackHandler extends SimpleChannelHandler {

    private static Logger log = Logger.getLogger("nCoap");
    private static ResponseCallbackHandler instance = new ResponseCallbackHandler();

    HashBasedTable<Integer, Integer, ResponseCallback> callbacks = HashBasedTable.create();

    private ResponseCallbackHandler(){}

    public static ResponseCallbackHandler getInstance(){
        return instance;
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof Request){
            Request request = (Request) me.getMessage();
            int messageID = request.getHeader().getMsgID();
            byte[] token = request.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue();

            ResponseCallback callback = request.getCallback();

            if(log.isDebugEnabled()){
                log.debug("[ResponseCallbackHandler] Number of registered callbacks before: " + callbacks.size());
            }

            callbacks.put(messageID, Ints.fromByteArray(token), callback);
            if(log.isDebugEnabled()){
                log.debug("[ResponseCallbackHandler] Number of registered callbacks after: " + callbacks.size());
            }
        }

        ctx.sendDownstream(me);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof Message){
            Message message = (Message) me.getMessage();
            if(log.isDebugEnabled()){
                log.debug("[ResponseCallbackHandler] Received message!");
            }
            if(!message.getHeader().getCode().isRequest()){
                log.debug("[ResponseCallbackHandler] Received message is a response!");
                int messageID = message.getHeader().getMsgID();
                byte[] token = message.getOption(OptionRegistry.OptionName.TOKEN).get(0).getValue();
                ResponseCallback callback = callbacks.remove(messageID, Ints.fromByteArray(token));
                if(callback != null){
                    if(log.isDebugEnabled()){
                        log.debug("[ResponseCallbackHandler] Response callback found!");
                    }
                    callback.responseReceived(message);
                }
            }
        }
        else{
            ctx.sendUpstream(me);
        }
    }
}
