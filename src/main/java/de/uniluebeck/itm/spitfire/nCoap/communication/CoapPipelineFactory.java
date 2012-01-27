package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.handler.CoapMessageDecoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.handler.CoapMessageEncoder;
import de.uniluebeck.itm.spitfire.nCoap.communication.handler.OutgoingMessageReliabilityHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;


public class CoapPipelineFactory implements ChannelPipelineFactory {

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("CoAP Message Decoder", new CoapMessageDecoder());
        pipeline.addLast("CoAP Message Encoder", new CoapMessageEncoder());
        pipeline.addLast("OutgoingMessageReliabilityHandler", OutgoingMessageReliabilityHandler.getInstance());

        return pipeline;
    }
}
