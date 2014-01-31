package de.uniluebeck.itm.ncoap.application.server;

import de.uniluebeck.itm.ncoap.communication.codec.CoapMessageDecoder;
import de.uniluebeck.itm.ncoap.communication.codec.CoapMessageEncoder;
import de.uniluebeck.itm.ncoap.communication.observe.ObservableResourceHandler;
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
        handler.put(OUTGOING_MESSAGE_RELIABILITY_HANDLER, new OutgoingMessageReliabilityHandler(executorService));
        handler.put(INCOMING_MESSAGE_RELIABILITY_HANDLER, new IncomingMessageReliabilityHandler(executorService));
        handler.put(OBSERVATION_HANDLER, new ObservableResourceHandler(executorService));
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
