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

package de.uniluebeck.itm.spitfire.nCoap.application;

import de.uniluebeck.itm.spitfire.nCoap.communication.callback.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapClientDatagramChannelFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannel;

import java.net.InetSocketAddress;

/**
 * This is the abstract class to be extended by a CoAP client.
 * By {@link #writeCoapRequest(de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest)}it provides an
 * easy-to-use method to write CoAP requests to a server.
 *
 * @author Oliver Kleine
 */
public abstract class CoapClientApplication implements ResponseCallback{

    private final DatagramChannel channel = CoapClientDatagramChannelFactory.getInstance().getChannel();

    /**
     * This method should be used by extending client implementation to send a CoAP request to a remote recipient. All
     * necessary information to send the message (like the recipient IP address or port) is automatically extracted
     * from the given {@link CoapRequest} object.
     * @param coapRequest The {@link CoapRequest} object to be sent
     */
    public final void writeCoapRequest(CoapRequest coapRequest){
        InetSocketAddress rcptSocketAddress = new InetSocketAddress(coapRequest.getTargetUri().getHost(),
                coapRequest.getTargetUri().getPort());

        Channels.write(channel, coapRequest, rcptSocketAddress);
    }
}
