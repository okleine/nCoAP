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

import de.uzl.itm.ncoap.application.AbstractCoapApplication;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.communication.dispatching.client.ResponseDispatcher;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * An instance of {@link CoapClient} is the entry point to send {@link CoapMessage}s to a (remote)
 * server or proxy.
 * 
 * With {@link #sendCoapRequest(CoapRequest, InetSocketAddress, ClientCallback)} it e.g. provides an
 * easy-to-use method to write CoAP requests to a server.
 * 
 * Furthermore, with {@link #sendCoapPing(java.net.InetSocketAddress, ClientCallback)} it provides a method to test
 * if a remote CoAP endpoint (i.e. the CoAP application and not only the host(!)) is alive.
 * 
 * @author Oliver Kleine
*/
public class CoapClient extends AbstractCoapApplication {

//    public static final int RECEIVE_BUFFER_SIZE = 65536;

    public static final String DEFAULT_NAME = "nCoAP Client";

    private ResponseDispatcher responseDispatcher;
    private static Logger LOG = LoggerFactory.getLogger(CoapClient.class.getName());


    /**
     * Creates a new instance of {@link CoapClient} with default parameters, i.e. a random port number and
     * <code>Runtime.getRuntime().availableProcessors() * 2</code> as number of I/O-Threads.
     */
    public CoapClient() {
        this(DEFAULT_NAME, new InetSocketAddress(0));
    }

    /**
     * Creates a new instance of {@link CoapClient} with default parameters, i.e. a random port number and
     * <code>Runtime.getRuntime().availableProcessors() * 2</code> as number of I/O-Threads.
     */
    public CoapClient(String name) {
        this(name, new InetSocketAddress(0));
    }

    /**
     * Creates a new instance of {@link CoapClient}.
     *
     * @param name the name of the application (used for logging purposes)
     * @param portNumber the number of the port to send , this {@link CoapClient} should be bound to (use <code>0</code> for
     *             arbitrary port)
     */
    public CoapClient(String name, int portNumber) {
        this(name, new InetSocketAddress(portNumber));
    }

    /**
     * Creates a new instance of {@link CoapClient}.
     *
     * @param name the name of the application (used for logging purposes)
     * @param clientSocket the socket to send {@link CoapMessage}s
     */
    public CoapClient(String name, InetSocketAddress clientSocket) {
        super(name);

        ClientChannelPipelineFactory factory = new ClientChannelPipelineFactory(this.getExecutor());
        startApplication(factory, clientSocket);

        this.responseDispatcher = getChannel().getPipeline().get(ResponseDispatcher.class);
    }



    /**
     * Sends a {@link de.uzl.itm.ncoap.message.CoapRequest} to the given remote endpoints, i.e. CoAP server or
     * proxy, and registers the given {@link ClientCallback}
     * to be called upon reception of a {@link de.uzl.itm.ncoap.message.CoapResponse}.
     *
     * <b>Note:</b> Override {@link ClientCallback
     * #continueObservation(InetSocketAddress, Token)} on the given callback for observations!
     *
     * @param coapRequest the {@link de.uzl.itm.ncoap.message.CoapRequest} to be sent
     *
     * @param callback the {@link ClientCallback} to process the corresponding response, resp.
     *                              update notification (which are also instances of {@link CoapResponse}.
     *
     * @param remoteSocket the desired recipient of the given {@link de.uzl.itm.ncoap.message.CoapRequest}
     */
    public void sendCoapRequest(CoapRequest coapRequest, InetSocketAddress remoteSocket, ClientCallback callback) {
        this.responseDispatcher.sendCoapRequest(coapRequest, remoteSocket, callback);
    }


    /**
     * Sends a CoAP PING, i.e. a {@link de.uzl.itm.ncoap.message.CoapMessage} with
     * {@link de.uzl.itm.ncoap.message.MessageType#CON} and
     * {@link de.uzl.itm.ncoap.message.MessageCode#EMPTY} to the given CoAP endpoints and registers the
     * given {@link ClientCallback}
     * to be called upon reception of the corresponding {@link de.uzl.itm.ncoap.message.MessageType#RST}
     * message (CoAP PONG).
     *
     * Make sure to override {@link ClientCallback
     * #processReset()} to handle the CoAP PONG!
     *
     * @param callback the {@link ClientCallback} to be
     *                       called upon reception of the corresponding
     *                       {@link de.uzl.itm.ncoap.message.MessageType#RST} message.
     *                       <br><br>
     *                       <b>Note:</b> To handle the CoAP PONG, i.e. the empty RST, the method
     *                       {@link ClientCallback
     *                       #processReset()} MUST be overridden
     * @param remoteSocket the desired recipient of the CoAP PING message
     */
    public void sendCoapPing(InetSocketAddress remoteSocket, ClientCallback callback) {
        this.responseDispatcher.sendCoapPing(remoteSocket, callback);
    }


    /**
     * Shuts this {@link CoapClient} down by closing its
     * {@link org.jboss.netty.channel.socket.DatagramChannel} which includes to unbind
     * this {@link org.jboss.netty.channel.socket.DatagramChannel} from the listening port and by this means free the
     * port.
     */
    public final void shutdown() {
        LOG.warn("Start to shutdown " + this.getApplicationName() + " (Port : " + this.getPort() + ")");

        getChannel().close().awaitUninterruptibly().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                LOG.warn("Channel closed ({}).", CoapClient.this.getApplicationName());
                getChannel().getFactory().releaseExternalResources();
                LOG.warn("External resources released ({}).", CoapClient.this.getApplicationName());
                LOG.warn("Shutdown of " + getApplicationName() + " completed.");
            }
        });
    }
}
