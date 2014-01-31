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
package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.message.MessageCode.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of {@link WebserviceCreator} are supposed to handle incoming {@link CoapRequest}s that intend
 * to create one ore more new {@link Webservice} instances on the server.
 *
 * The framework calls the method {@link #handlePutRequest(SettableFuture, CoapRequest)} for incoming requests with
 * {@link Name#PUT} if the addressed {@link Webservice} does NOT exist (if the addressed {@link Webservice} exists
 * the framework invokes {@link Webservice#processCoapRequest(SettableFuture, CoapRequest, java.net.InetSocketAddress)}
 *
 * @author Oliver Kleine
 */
public abstract class WebserviceCreator {

    private WebserviceManager webserviceManager;

    private static Logger log = LoggerFactory.getLogger(WebserviceCreator.class.getName());

    /**
     * This method is invoked by the framework to set the {@link WebserviceManager} that is supposed to be used to
     * register newly created {@link Webservice} instances.
     *
     * @param webserviceManager the {@link WebserviceManager} that is supposed to be used to register newly created
     * {@link Webservice} instances.
     */
    final void setWebserviceManager(WebserviceManager webserviceManager){
        this.webserviceManager = webserviceManager;
    }


    /**
     * @return the {@link WebserviceManager} for this CoAP server. This {@link WebserviceManager} instance is
     * supposed to be used to register new {@link Webservice} instances using
     * {@link WebserviceManager#registerService(Webservice)}.
     */
    protected WebserviceManager getWebserviceManager(){
        return this.webserviceManager;
    }

    /**
     * This method is invoked by the framework on incoming {@link CoapRequest}s with {@link MessageCode.Name#PUT} if
     * there is no {@link Webservice} registered at the path given as {@link CoapRequest#getUriPath()}.
     *
     * @param responseFuture the {@link SettableFuture} to be set with a proper {@link CoapResponse} to indicate
     *                       whether there was a new {@link Webservice} created or not.
     *
     * @param coapRequest the {@link CoapRequest} to be processed
     */
    public abstract void handlePutRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest);


    /**
     * Returns the default implementation of {@link WebserviceCreator}. The default {@link WebserviceCreator} does not
     * create new instances of {@link Webservice} but sets the given {@link SettableFuture} with a {@link CoapResponse}
     * with code 404 NOT FOUND.
     *
     * @return a new default {@link WebserviceCreator} instance
     */
    public static WebserviceCreator getDefault(){
        return new WebserviceCreator() {
            @Override
            public void handlePutRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest) {
                try {
                    CoapResponse coapResponse = new CoapResponse(MessageCode.Name.NOT_FOUND_404);
                    String content = "Service \"" + coapRequest.getUriPath() + "\" not found"
                            + " and this server does not allow service creation using PUT.";

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
