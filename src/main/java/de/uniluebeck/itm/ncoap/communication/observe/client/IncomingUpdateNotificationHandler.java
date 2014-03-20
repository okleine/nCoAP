package de.uniluebeck.itm.ncoap.communication.observe.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by olli on 20.03.14.
 */
public class IncomingUpdateNotificationHandler extends SimpleChannelHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private static final String ERROR = "There is no running observation to cancel for remote endpoint %s and token %s";

    private Multimap<InetSocketAddress, Token> observations;


    public IncomingUpdateNotificationHandler(){
        this.observations = Multimaps.synchronizedMultimap(HashMultimap.<InetSocketAddress, Token>create());
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapRequest)
            handleOutgoingCoapRequest(ctx, me);

        else if(me.getMessage() instanceof InternalStopObservationMessage)
            handleInternalStopObservationMessage(me);

        else
            ctx.sendDownstream(me);
    }

    private void handleInternalStopObservationMessage(MessageEvent me) {
        InternalStopObservationMessage internalMessage = (InternalStopObservationMessage) me.getMessage();

        if(observations.remove(internalMessage.getRemoteEndpoint(), internalMessage.getToken())){
            me.getFuture().setSuccess();

            log.info("Observation stopped! Future update notifications from {} with token {} will cause a RST.",
                    internalMessage.getRemoteEndpoint(), internalMessage.getToken());
        }

        else{
            String errorMessage = String.format(ERROR, internalMessage.getRemoteEndpoint().toString(),
                    internalMessage.getToken().toString());

            me.getFuture().setFailure(new Exception(errorMessage));

            log.error(errorMessage);
        }
    }

    private void handleOutgoingCoapRequest(ChannelHandlerContext ctx, MessageEvent me) {
        CoapRequest coapRequest = (CoapRequest) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        if(coapRequest.isObserveSet() && !observations.containsEntry(remoteEndpoint, coapRequest.getToken()))
            observations.put(remoteEndpoint, coapRequest.getToken());

        ctx.sendDownstream(me);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        if(me.getMessage() instanceof CoapResponse)
            handleIncomingCoapResponse(ctx, me);

        else
            ctx.sendUpstream(me);
    }


    private void handleIncomingCoapResponse(ChannelHandlerContext ctx, MessageEvent me) {
        CoapResponse coapResponse = (CoapResponse) me.getMessage();
        InetSocketAddress remoteEndpoint = (InetSocketAddress) me.getRemoteAddress();

        if(coapResponse.isUpdateNotification() && !observations.containsEntry(remoteEndpoint, coapResponse.getToken()))
            sendResetMessage(ctx, remoteEndpoint, coapResponse.getMessageID());

        else
            ctx.sendUpstream(me);
    }


    private void sendResetMessage(ChannelHandlerContext ctx, InetSocketAddress remoteEndoint, int messageID) {
        final CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);

        ChannelFuture future = Channels.future(ctx.getChannel());
        Channels.write(ctx, future , resetMessage, remoteEndoint);

        if(log.isErrorEnabled()){
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess())
                        log.info("RST message sent ({})", resetMessage);

                    else
                        log.error("Could not send RST message ({})", resetMessage);
                }
            });
        }
    }

}
