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

package de.uniluebeck.itm.ncoap.communication.dispatching.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * <p>Instances of {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler} are invoked to handle
 * inbound {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s that targets a not (yet?) existing
 * {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice}. Instances may e.g. create and
 * register or update {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice}s upon reception
 * of a POST or PUT request.</p>
 *
 * <p>The framework calls the method {@link #processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} for
 * inbound {@link de.uniluebeck.itm.ncoap.message.CoapRequest}s if the addressed
 * {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} does NOT exist.</p>
 *
 * @author Oliver Kleine
 */
public abstract class NotFoundHandler {

    private WebserviceManager webserviceManager;

    private static Logger log = LoggerFactory.getLogger(NotFoundHandler.class.getName());

    /**
     * This method is invoked by the framework to set the {@link WebserviceManager} that is supposed to be used to
     * register newly created {@link Webservice} instances.
     *
     * @param webserviceManager the {@link WebserviceManager} that is supposed to be used to register newly created
     * {@link Webservice} instances.
     */
    public final void setWebserviceManager(WebserviceManager webserviceManager){
        this.webserviceManager = webserviceManager;
    }


    /**
     * Returns the {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.WebserviceManager} for this CoAP
     * server. The {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.WebserviceManager} instance can be
     * e.g. used to register new {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice}s
     * using {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.WebserviceManager
     * #registerService(Webservice)}.
     *
     * @return the {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.WebserviceManager} for this CoAP
     * server.
     */
    protected WebserviceManager getWebserviceManager(){
        return this.webserviceManager;
    }

    /**
     * This method is invoked by the framework on inbound {@link CoapRequest}s with {@link MessageCode.Name#PUT} if
     * there is no {@link Webservice} registered at the path given as {@link CoapRequest#getUriPath()}.
     *
     * @param responseFuture the {@link SettableFuture} to be set with a proper {@link CoapResponse} to indicate
     *                       whether there was a new {@link Webservice} created or not.
     *
     * @param coapRequest the {@link CoapRequest} to be processed
     *
     * @param remoteEndpoint the {@link InetSocketAddress} of the {@link CoapRequest}s origin.
     */
    public abstract void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                            InetSocketAddress remoteEndpoint);


    /**
     * Returns the default implementation of
     * {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler}. The default
     * {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler} does not create new instances
     * or updates or deletes existing instances of
     * {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} but sets the given
     * {@link com.google.common.util.concurrent.SettableFuture} with a
     * {@link de.uniluebeck.itm.ncoap.message.CoapResponse} with
     * {@link de.uniluebeck.itm.ncoap.message.MessageCode.Name#NOT_FOUND_404}.
     *
     * @return a new default {@link de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler} instance
     */
    public static NotFoundHandler getDefault(){

        return new NotFoundHandler() {

            private String message = "Webservice \"%s\" not found.";

            @Override
            public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                           InetSocketAddress remoteEndpoint) {
                try {
                    CoapResponse coapResponse =
                            new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.NOT_FOUND_404);

                    String content = String.format(message, coapRequest.getUriPath());

                    coapResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
                    responseFuture.set(coapResponse);
                }
                catch (Exception e) {
                    log.error("This should never happen.", e);
                    responseFuture.setException(e);
                }
            }
        };
    }
}
