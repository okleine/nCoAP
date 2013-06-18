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

import de.uniluebeck.itm.spitfire.nCoap.communication.core.callback.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * This is the abstract class to be extended by a CoAP client. By {@link #writeCoapRequest(CoapRequest)} it provides an
 * easy-to-use method to write CoAP requests to a server.
 *
 * @author Oliver Kleine
 */
public abstract class CoapClientApplication implements ResponseCallback{

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private DatagramChannel datagramChannel;

    protected CoapClientApplication(){
        datagramChannel =  CoapClientDatagramChannelFactory.getChannel();
        log.info("New CoAP client on port " + datagramChannel.getLocalAddress().getPort());
    }

    /**
     * This method is supposed be used by the extending client implementation to send a CoAP request to a remote
     * recipient. All necessary information to send the message (like the recipient IP address or port) is
     * automatically extracted from the given {@link CoapRequest} object.
     *
     * @param coapRequest The {@link CoapRequest} object to be sent
     */
    public final void writeCoapRequest(CoapRequest coapRequest){

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
    public void shutdown(){
        //Close the datagram datagramChannel (includes unbind)
        ChannelFuture future = datagramChannel.close();

        //Await the closure and let the factory release its external resource to finalize the shutdown
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                DatagramChannel closedChannel = (DatagramChannel) future.getChannel();
                log.info("Client channel closed (port: " + closedChannel.getLocalAddress().getPort() + ").");
            }
        });

        future.awaitUninterruptibly();
    }
}
