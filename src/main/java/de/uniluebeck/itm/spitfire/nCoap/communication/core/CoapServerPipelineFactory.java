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

package de.uniluebeck.itm.spitfire.nCoap.communication.core;

import de.uniluebeck.itm.spitfire.nCoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.blockwise.BlockwiseTransferHandler;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageDecoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.encoding.CoapMessageEncoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.observe.ObservableResourceHandler;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.incoming.IncomingMessageReliabilityHandler;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.MessageIDFactory;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Oliver Kleine
 */
public class CoapServerPipelineFactory implements ChannelPipelineFactory {

    public static final int NUMBER_OF_EXECUTION_THREADS = 50;

    private CoapMessageEncoder encoder = new CoapMessageEncoder();
    private CoapMessageDecoder decoder = new CoapMessageDecoder();

    private ExecutionHandler executionHandler =
            new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(NUMBER_OF_EXECUTION_THREADS, 0, 0));

    private OutgoingMessageReliabilityHandler outgoingMessageReliabilityHandler;
    private IncomingMessageReliabilityHandler incomingMessageReliabilityHandler;

    private BlockwiseTransferHandler blockwiseTransferHandler = new BlockwiseTransferHandler();

    private ChannelHandler serverApp;

    private ObservableResourceHandler observableResourceHandler;
    
    public CoapServerPipelineFactory(CoapServerApplication serverApp, int serverPort,
                                     ScheduledExecutorService executorService){
        this.serverApp = serverApp;
        this.outgoingMessageReliabilityHandler = new OutgoingMessageReliabilityHandler(executorService);
        this.incomingMessageReliabilityHandler = new IncomingMessageReliabilityHandler(executorService);
        this.observableResourceHandler = new ObservableResourceHandler(executorService);

        //MessageIDFactory.setExecutorService(executorService);
    }


    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("CoAP Message Encoder", encoder);
        pipeline.addLast("CoAP Message Decoder", decoder);
        pipeline.addLast("OutgoingMessageReliabilityHandler", outgoingMessageReliabilityHandler);
        pipeline.addLast("IncomingMessageReliabilityHandler", incomingMessageReliabilityHandler);
        //pipeline.addLast("BlockwiseTransferHander", blockwiseTransferHandler);
        //pipeline.addLast("ExecutionHandler", executionHandler);
        pipeline.addLast("ObservableResourceHandler", observableResourceHandler);
        //pipeline.addLast("Execution Handler", executionHandler);
        pipeline.addLast("ServerApplication", serverApp);
        
        return pipeline;
    }

    public ObservableResourceHandler getObservableResourceHandler(){
        return this.observableResourceHandler;
    }

}
