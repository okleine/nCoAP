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
/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;

/**
* The .well-known/core resource is a standard webservice to be provided by every CoAP webserver as defined in
* the CoAP protocol draft. It provides a list of all available services on the server in CoRE Link Format.
*
* @author Oliver Kleine
*/
public final class WellKnownCoreResource extends NotObservableWebservice<Map<String, Webservice>> {

    private static Logger log = LoggerFactory.getLogger(WellKnownCoreResource.class.getName());

    /**
     * Creates the well-known/core resource at path /.well-known/core as defined in the CoAP draft
     * @param initialStatus the Map containing all available path
     */
    public WellKnownCoreResource(Map<String, Webservice> initialStatus) {
        super("/.well-known/core", initialStatus, NotObservableWebservice.SECONDS_PER_YEAR);
    }

    /**
     * The .well-known/core resource only allows requests with {@link MessageCode.Name#GET}. Any other code
     * returns a {@link CoapResponse} with {@link MessageCode.Name#METHOD_NOT_ALLOWED_405}.
     *
     * In case of a request with {@link @link MessageCode.Name#GET} it returns a {@link CoapResponse} with
     * {@link MessageCode.Name#CONTENT_205} and with a payload listing all paths to the available resources
     * (i.e. {@link Webservice} instances}).
     *
     * The payload is always formatted in {@link ContentFormat.Name#APP_LINK_FORMAT}.
     *
     * @param responseFuture The {@link SettableFuture} to be set with a {@link CoapResponse} containing
     *                       the list of available services in CoRE link format.
     * @param request The {@link CoapRequest} to be processed by the {@link Webservice} instance
     * @param remoteAddress The address of the sender of the request
     */
    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest request,
                                   InetSocketAddress remoteAddress) {
        try{
            if(request.getMessageCode() != MessageCode.Name.GET.getNumber()){
                responseFuture.set(new CoapResponse(MessageCode.Name.METHOD_NOT_ALLOWED_405));
                return;
            }

            CoapResponse response = new CoapResponse(MessageCode.Name.CONTENT_205);

            try {
                byte[] payload = getSerializedResourceStatus(ContentFormat.Name.APP_LINK_FORMAT);
                response.setContent(ChannelBuffers.wrappedBuffer(payload), ContentFormat.Name.APP_LINK_FORMAT);

            } catch (Exception e) {
                log.error("This should never happen.", e);
            }

            responseFuture.set(response);
        }
        catch (InvalidHeaderException e) {
            log.error("This should never happen.", e);
            responseFuture.setException(e);
        }
    }

//    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) throws AcceptedContentFormatNotSupportedException {
        StringBuffer buffer = new StringBuffer();

        //TODO make this real CoRE link format
        for(String path : getResourceStatus().keySet()){
            buffer.append("<" + path + ">,\n");
        }
        buffer.deleteCharAt(buffer.length()-2);

        log.debug("Content: \n{}", buffer.toString());

        return buffer.toString().getBytes(CoapMessage.CHARSET);
    }

    @Override
    public void shutdown() {
        //nothing to do here...
    }

    @Override
    public boolean allowsDelete() {
        return false;
    }
}
