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
package de.uniluebeck.itm.ncoap.application.server;

import de.uniluebeck.itm.ncoap.communication.codec.CoapMessageDecoder;
import de.uniluebeck.itm.ncoap.communication.codec.CoapMessageEncoder;
import de.uniluebeck.itm.ncoap.communication.observe.WebserviceObservationHandler;
import de.uniluebeck.itm.ncoap.communication.reliability.incoming.IncomingMessageReliabilityHandler;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
* Factory to provide the {@link ChannelPipeline} for newly created {@link DatagramChannel}s.
*
* @author Oliver Kleine
*/
public class ServerChannelPipelineFactory implements ChannelPipelineFactory {

    public static final String EXECUTION_HANDLER = "ExecutionHandler";
    public static final String ENCODER = "Encoder";
    public static final String DECODER = "Decoder";
    public static final String INCOMING_MESSAGE_RELIABILITY_HANDLER = "IncomingMessageReliabilityHandler";
    public static final String OUTGOING_MESSAGE_RELIABILITY_HANDLER = "OutgoingMessageReliabilityHandler";
    public static final String OBSERVATION_HANDLER = "ObservationHandler";
    public static final String WEBSERVICE_MANAGER = "WebserviceManager";

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Map<String, ChannelHandler> handler;

    /**
     * @param executorService The {@link ScheduledExecutorService} to provide the thread(s) for I/O operations
     */
    public ServerChannelPipelineFactory(ScheduledExecutorService executorService,
                                        WebserviceCreator webserviceCreator){

        this.handler = new LinkedHashMap<String, ChannelHandler>(7);

        handler.put(EXECUTION_HANDLER, new ExecutionHandler(executorService));
        handler.put(ENCODER, new CoapMessageEncoder());
        handler.put(DECODER, new CoapMessageDecoder());
        handler.put(OBSERVATION_HANDLER, new WebserviceObservationHandler(executorService));
        handler.put(OUTGOING_MESSAGE_RELIABILITY_HANDLER, new OutgoingMessageReliabilityHandler(executorService));
        handler.put(INCOMING_MESSAGE_RELIABILITY_HANDLER, new IncomingMessageReliabilityHandler(executorService));
        handler.put(WEBSERVICE_MANAGER, new WebserviceManager(webserviceCreator, executorService));
    }


    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        for(String handlerName : handler.keySet()){
            pipeline.addLast(handlerName, handler.get(handlerName));
            log.debug("Added Handler to Pipeline: {}.", handlerName);
        }

//        pipeline.addLast(EXECUTION_HANDLER, handler.get(EXECUTION_HANDLER));
//        pipeline.addLast(ENCODER, handler.get(ENCODER));
//        pipeline.addLast(DECODER, handler.get(DECODER));
//        pipeline.addLast(OUTGOING_MESSAGE_RELIABILITY_HANDLER, handler.get(OUTGOING_MESSAGE_RELIABILITY_HANDLER));
//        pipeline.addLast(INCOMING_MESSAGE_RELIABILITY_HANDLER, handler.get(INCOMING_MESSAGE_RELIABILITY_HANDLER));
//        pipeline.addLast(OBSERVATION_HANDLER, handler.get(OBSERVATION_HANDLER));
//        pipeline.addLast(WEBSERVICE_MANAGER, handler.get(WEBSERVICE_MANAGER));

        return pipeline;
    }

    /**
     * Returns the {@link org.jboss.netty.channel.ChannelHandler} instance which is part of each pipeline created
     * using this factory (or <code>null</code> if no such handler exists). See static constants for the
     * available names.
     *
     * @param name the name of the {@link org.jboss.netty.channel.ChannelHandler instance to be returned}
     *
     * @return the {@link org.jboss.netty.channel.ChannelHandler} instance which is part of each pipeline created
     * using this factory (or <code>null</code> if no such handler exists).
     */
    public ChannelHandler getChannelHandler(String name){
        return handler.get(name);
    }

}
