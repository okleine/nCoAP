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
package de.uzl.itm.ncoap.application.endpoint;

import de.uzl.itm.ncoap.application.CoapChannelPipelineFactory;
//import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock2Handler;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock1Handler;
import de.uzl.itm.ncoap.communication.blockwise.client.ClientBlock2Handler;
import de.uzl.itm.ncoap.communication.blockwise.server.ServerBlock1Handler;
import de.uzl.itm.ncoap.communication.blockwise.server.ServerBlock2Handler;
import de.uzl.itm.ncoap.communication.dispatching.Token;
import de.uzl.itm.ncoap.communication.dispatching.client.ResponseDispatcher;
import de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.RequestDispatcher;
import de.uzl.itm.ncoap.communication.identification.ClientIdentificationHandler;
import de.uzl.itm.ncoap.communication.identification.ServerIdentificationHandler;
import de.uzl.itm.ncoap.communication.observing.ClientObservationHandler;
import de.uzl.itm.ncoap.communication.observing.ServerObservationHandler;
import de.uzl.itm.ncoap.communication.reliability.inbound.ClientInboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.inbound.ServerInboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.outbound.ClientOutboundReliabilityHandler;
import de.uzl.itm.ncoap.communication.reliability.outbound.MessageIDFactory;
import de.uzl.itm.ncoap.communication.reliability.outbound.ServerOutboundReliabilityHandler;

import java.util.concurrent.ScheduledExecutorService;

/**
 * The {@link de.uzl.itm.ncoap.application.CoapChannelPipelineFactory} to create the proper
 * {@link org.jboss.netty.channel.ChannelPipeline} for {@link CoapEndpoint}s.
 *
 * @author Oliver Kleine
 */
public class CoapEndpointChannelPipelineFactory extends CoapChannelPipelineFactory {

    /**
     * Creates a new instance of {@link CoapEndpointChannelPipelineFactory}.
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to be used to handle I/O
     *
     * @param tokenFactory the {@link de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory} which is to be
     * used to create {@link Token}s
     *
     * @param notFoundHandler the {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler} to handle
     * incoming requests for unknown {@link de.uzl.itm.ncoap.application.server.resource.Webresource}s.
     */
    public CoapEndpointChannelPipelineFactory(ScheduledExecutorService executor, TokenFactory tokenFactory,
                             NotFoundHandler notFoundHandler, BlockSize maxBlock1Size, BlockSize maxBlock2Size) {

        super(executor);
        MessageIDFactory factory = new MessageIDFactory(executor);

        // identification
        addChannelHandler(new ClientIdentificationHandler(executor));
        addChannelHandler(new ServerIdentificationHandler(executor));

        // client specific handlers
        addChannelHandler(new ClientOutboundReliabilityHandler(executor, factory));
        addChannelHandler(new ClientInboundReliabilityHandler(executor));
        addChannelHandler(new ClientBlock2Handler(executor));
        addChannelHandler(new ClientBlock1Handler(executor));
        addChannelHandler(new ClientObservationHandler(executor));
        addChannelHandler(new ResponseDispatcher(executor, tokenFactory));

        // server specific handlers
        addChannelHandler(new ServerOutboundReliabilityHandler(executor, factory));
        addChannelHandler(new ServerInboundReliabilityHandler(executor));
        addChannelHandler(new ServerBlock1Handler(executor, maxBlock1Size));
        addChannelHandler(new ServerBlock2Handler(executor, maxBlock2Size));
        addChannelHandler(new ServerObservationHandler(executor));
        addChannelHandler(new RequestDispatcher(notFoundHandler, executor));
    }

}
