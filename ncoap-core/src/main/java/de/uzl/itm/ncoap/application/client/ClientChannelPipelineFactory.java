/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.application.client;

import de.uzl.itm.ncoap.application.CoapChannelPipelineFactory;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1Handler;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock2Handler;
import de.uzl.itm.ncoap.communication.dispatching.client.ResponseDispatcher;
import de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory;
import de.uzl.itm.ncoap.communication.identification.ClientIdentificationHandler;
import de.uzl.itm.ncoap.communication.observing.ClientObservationHandler;
import de.uzl.itm.ncoap.communication.reliability.inbound.ClientInboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.outbound.ClientOutboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.outbound.MessageIDFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.DatagramChannel;

import java.util.concurrent.ScheduledExecutorService;


/**
 * Factory to provide the {@link ChannelPipeline} for a newly created {@link DatagramChannel} for a
 * {@link CoapClient}.
 *
 * @author Oliver Kleine
 */
public class ClientChannelPipelineFactory extends CoapChannelPipelineFactory {


    /**
     * Creates a new instance of {@link ClientChannelPipelineFactory}.
     *
     * @param executor The {@link ScheduledExecutorService} to provide the thread(s) for I/O operations
     */
    public ClientChannelPipelineFactory(ScheduledExecutorService executor) {

        super(executor);
        addChannelHandler(new ClientIdentificationHandler(executor));
        addChannelHandler(new ClientOutboundReliabilityHandler(executor, new MessageIDFactory(executor)));
        addChannelHandler(new ClientInboundReliabilityHandler(executor));
        addChannelHandler(new ClientBlock2Handler(executor));
        addChannelHandler(new ClientBlock1Handler(executor));
        addChannelHandler(new ClientObservationHandler(executor));
        addChannelHandler(new ResponseDispatcher(executor, new TokenFactory()));
    }

}
