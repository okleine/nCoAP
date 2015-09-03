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

package de.uzl.itm.ncoap.application.server;

import de.uzl.itm.ncoap.application.CoapChannelPipelineFactory;
import de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler;
import de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager;
import de.uzl.itm.ncoap.communication.reliability.InboundReliabilityHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.DatagramChannel;

import java.util.concurrent.ScheduledExecutorService;

/**
* Factory to provide the {@link ChannelPipeline} for newly created {@link DatagramChannel}s.
*
* @author Oliver Kleine
*/
public class ServerChannelPipelineFactory extends CoapChannelPipelineFactory {

    /**
     * The name of the {@link de.uzl.itm.ncoap.communication.reliability.InboundReliabilityHandler}
     * instance of a CoAP server
     */
    public static final String INBOUND_RELIABILITY_HANDLER = "INBOUND-RELIABILITY";


    /**
     * The name of the {@link de.uzl.itm.ncoap.communication.dispatching.server.WebresourceManager}
     * instance of a CoAP server
     */
    public static final String WEBRESOURCE_MANAGER = "WEBRESOURCE-MANAGER";


    /**
     * Creates a new instance of {@link ServerChannelPipelineFactory}.
     *
     * @param executor The {@link ScheduledExecutorService} to provide the thread(s) for I/O operations
     * @param notFoundHandler the {@link de.uzl.itm.ncoap.communication.dispatching.server.NotFoundHandler}
     *                        to handle inbound {@link de.uzl.itm.ncoap.message.CoapRequest}s targeting
     *                        unknown {@link de.uzl.itm.ncoap.application.server.webresource.Webresource}s.
     */
    public ServerChannelPipelineFactory(ScheduledExecutorService executor, NotFoundHandler notFoundHandler){

        super(executor);

        addChannelHandler(INBOUND_RELIABILITY_HANDLER, new InboundReliabilityHandler(executor));
        addChannelHandler(WEBRESOURCE_MANAGER, new WebresourceManager(notFoundHandler, executor));
    }
}
