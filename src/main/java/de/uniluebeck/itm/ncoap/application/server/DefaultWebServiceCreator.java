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
import de.uniluebeck.itm.ncoap.application.server.webservice.AcceptedContentFormatNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 17.11.13
 * Time: 12:44
 * To change this template use File | Settings | File Templates.
 */
public class DefaultWebServiceCreator extends WebServiceCreator {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public DefaultWebServiceCreator(CoapServerApplication serverApplication) {
        super(serverApplication);
    }

    public void handleWebServiceCreationRequest(SettableFuture<CoapResponse> responseFuture,
                                                CoapRequest coapRequest) {
        try {

            long contentFormat = coapRequest.getContentFormat();

            if(contentFormat == ContentFormat.Name.UNDEFINED){
                CoapResponse coapResponse = new CoapResponse(MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415);
                coapResponse.setContent("There is no format defined for the content!".getBytes(CoapMessage.CHARSET));
                responseFuture.set(coapResponse);
                return;
            }

            String webservicePath = coapRequest.getUriPath();
            byte[] content = new byte[coapRequest.getContent().readableBytes()];
            coapRequest.getContent().readBytes(content);

            Webservice webservice = new NotObservableWebservice<byte[]>(webservicePath, content,
                    NotObservableWebservice.SECONDS_PER_YEAR){

                private long contentFormat;

                public void setContentFormat(long contentFormat){
                    this.contentFormat = contentFormat;
                }

                @Override
                public void shutdown() {
                    //Nothing to do here
                }

                @Override
                public boolean allowsDelete() {
                    return true;
                }

                @Override
                public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                               InetSocketAddress remoteAddress) {

                    log.info("Created webservice received request!");

                    try{
                        Set<Long> acceptedContentFormats = coapRequest.getAcceptedContentFormats();

                        if(!(acceptedContentFormats.isEmpty() || acceptedContentFormats.contains(this.contentFormat))){
                            AcceptedContentFormatNotSupportedException ex =
                                    new AcceptedContentFormatNotSupportedException(acceptedContentFormats.iterator().next());

                            responseFuture.setException(ex);
                            return;
                        }

                        CoapResponse coapResponse = new CoapResponse(MessageCode.Name.CONTENT_205);
                        coapResponse.setContent(this.getResourceStatus(), contentFormat);
                        responseFuture.set(coapResponse);
                    }
                    catch(Exception e){
                        log.error("This should never happen!", e);
                        responseFuture.setException(e);
                    }
                }
            };

            this.getServerApplication().registerService(webservice);

            CoapResponse coapResponse = new CoapResponse(MessageCode.Name.CREATED_201);
            coapResponse.setLocationURI(new URI(null, null, null, -1, webservicePath, null, null));

            responseFuture.set(coapResponse);
        }
        catch (Exception e) {
            log.error("This should never happen.", e);
            responseFuture.setException(e);
        }
    }

}
