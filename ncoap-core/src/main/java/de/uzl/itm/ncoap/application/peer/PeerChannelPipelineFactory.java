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
package de.uzl.itm.ncoap.application.peer;

import de.uzl.itm.ncoap.application.CoapChannelPipelineFactory;
import de.uzl.itm.ncoap.application.client.ClientChannelPipelineFactory;
import de.uzl.itm.ncoap.application.server.ServerChannelPipelineFactory;
import de.uzl.itm.ncoap.communication.blockwise.Block2OptionHandler;
import de.uzl.itm.ncoap.communication.dispatching.client.ClientCallbackManager;
import de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager;
import de.uzl.itm.ncoap.communication.observing.ClientObservationHandler;
import de.uzl.itm.ncoap.communication.reliability.InboundReliabilityHandler;

import java.util.concurrent.ScheduledExecutorService;

/**
 * The {@link de.uzl.itm.ncoap.application.CoapChannelPipelineFactory} to create the proper
 * {@link org.jboss.netty.channel.ChannelPipeline} for {@link de.uzl.itm.ncoap.application.peer.CoapPeerApplication}s.
 *
 * @author Oliver Kleine
 */
public class PeerChannelPipelineFactory extends CoapChannelPipelineFactory {

    /**
     * Creates a new instance of {@link de.uzl.itm.ncoap.application.peer.PeerChannelPipelineFactory}.
     *
     * @param executor the {@link java.util.concurrent.ScheduledExecutorService} to be used to handle I/O
     *
     * @param tokenFactory the {@link de.uzl.itm.ncoap.communication.dispatching.client.TokenFactory} which is to be
     * used to create {@link de.uzl.itm.ncoap.communication.dispatching.client.Token}s
     *
     * @param notFoundHandler the {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler} to handle
     * incoming requests for unknown {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s.
     */
    public PeerChannelPipelineFactory(ScheduledExecutorService executor, TokenFactory tokenFactory, NotFoundHandler notFoundHandler){

        super(executor);

        // client specific handlers
        addChannelHandler(ClientChannelPipelineFactory.BLOCK2_HANDLER,
                new Block2OptionHandler());
        addChannelHandler(ClientChannelPipelineFactory.CLIENT_OBSERVATION_HANDLER,
                new ClientObservationHandler());
        addChannelHandler(ClientChannelPipelineFactory.CLIENT_CALLBACK_MANAGER,
                new ClientCallbackManager(executor, tokenFactory));

        // server specific handlers
        addChannelHandler(ServerChannelPipelineFactory.INBOUND_RELIABILITY_HANDLER,
                new InboundReliabilityHandler(executor));
        addChannelHandler(ServerChannelPipelineFactory.WEBRESOURCE_MANAGER,
                new WebresourceManager(notFoundHandler, executor));
    }

}
