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

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.EmptyLinkAttribute;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LinkAttribute;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LongLinkAttribute;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.StringLinkAttribute;
import de.uniluebeck.itm.ncoap.message.options.OptionValue;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;

/**
* The .well-known/core resource is a standard webservice to be provided by every CoAP webserver as defined in
* the CoAP protocol draft. It provides a list of all available services on the server in CoRE Link Format.
*
* @author Oliver Kleine
*/
public final class WellKnownCoreResource extends NotObservableWebservice<Map<String, Webservice>> {

    private static Logger log = LoggerFactory.getLogger(WellKnownCoreResource.class.getName());

    private byte[] etag;

    /**
     * Creates the well-known/core resource at path /.well-known/core as defined in the CoAP draft
     * @param initialStatus the Map containing all available path
     */
    public WellKnownCoreResource(Map<String, Webservice> initialStatus) {
        super("/.well-known/core", initialStatus, 0);
    }

    /**
     * The .well-known/core resource only allows requests with {@link MessageCode.Name#GET}. Any other code
     * returns a {@link CoapResponse} with {@link MessageCode.Name#METHOD_NOT_ALLOWED_405}.
     *
     * In case of a request with {@link @link MessageCode.Name#GET} it returns a {@link CoapResponse} with
     * {@link MessageCode.Name#CONTENT_205} and with a payload listing all paths to the available resources
     * (i.e. {@link Webservice} instances}).
     *
     * <b>Note:</b> The payload is always formatted in {@link ContentFormat#APP_LINK_FORMAT}, possibly contained
     * {@link OptionValue.Name#ACCEPT} options in incoming {@link CoapRequest}s are ignored!
     *
     * @param responseFuture The {@link SettableFuture} to be set with a {@link CoapResponse} containing
     *                       the list of available services in CoRE link format.
     * @param coapRequest The {@link CoapRequest} to be processed by the {@link Webservice} instance
     * @param remoteEndpoint The address of the sender of the request
     *
     * @throws Exception Implementing classes may throw any {@link Exception}. Thrown {@link Exception}s cause the
     * framework to send a {@link CoapResponse} with {@link MessageCode.Name#INTERNAL_SERVER_ERROR_500} to the
     * client.
     */
    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception{

        CoapResponse coapResponse;

        if(!(coapRequest.getMessageCodeName() == MessageCode.Name.GET)){
            coapResponse = CoapResponse.createErrorResponse(coapRequest.getMessageTypeName(),
                    MessageCode.Name.METHOD_NOT_ALLOWED_405, "GET is the only allowed method!");
        }

        else{
            coapResponse = processCoapGetRequest(coapRequest);
        }

        responseFuture.set(coapResponse);
    }


    private CoapResponse processCoapGetRequest(CoapRequest coapRequest){
        try{
            LinkAttribute filterAttribute = createLinkAttributeFromQuery(coapRequest.getUriQuery());

            CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageTypeName(),
                    MessageCode.Name.CONTENT_205);

            byte[] content = getSerializedResourceStatus(filterAttribute);

            coapResponse.setContent(content, ContentFormat.APP_LINK_FORMAT);
            coapResponse.setEtag(this.etag);

            return coapResponse;
        }

        catch(IllegalArgumentException ex){

            return CoapResponse.createErrorResponse(coapRequest.getMessageTypeName(),
                    MessageCode.Name.BAD_REQUEST_400, ex.getMessage());
        }
    }


    private LinkAttribute createLinkAttributeFromQuery(String queryParameter) throws IllegalArgumentException{

        if(!queryParameter.equals("")){
            String[] param = queryParameter.split("=");

            if(param.length != 2)
                throw new IllegalArgumentException("Could not parse query " + queryParameter);

            LinkAttribute linkAttribute;
            int attributeType = LinkAttribute.getAttributeType(param[0]);

            if(attributeType == LinkAttribute.STRING_ATTRIBUTE)
                linkAttribute = new StringLinkAttribute(param[0], param[1]);

            else if(attributeType == LinkAttribute.LONG_ATTRIBUTE)
                linkAttribute = new LongLinkAttribute(param[0], Long.parseLong(param[1]));

            else if(attributeType == LinkAttribute.EMPTY_ATTRIBUTE)
                linkAttribute = new EmptyLinkAttribute(param[0], null);

            else
                throw new IllegalArgumentException("This should never happen!");

            return linkAttribute;
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    public byte[] getSerializedResourceStatus(LinkAttribute attribute){
        StringBuilder buffer = new StringBuilder();

        for(Webservice webservice : getResourceStatus().values()){

            if(attribute != null && !webservice.hasLinkAttribute(attribute))
                continue;

            buffer.append("<").append(webservice.getPath()).append(">");

            String previousKey = null;
            for (LinkAttribute linkAttribute : (Iterable<LinkAttribute>) webservice.getLinkAttributes()) {
                buffer.append(linkAttribute.getKey().equals(previousKey) ? " " : ";" + linkAttribute.getKey());

                if(!(linkAttribute instanceof EmptyLinkAttribute))
                    buffer.append("=").append(linkAttribute.getValue());

                previousKey = linkAttribute.getKey();
            }

            buffer.append(",\n");
        }

        if(buffer.length() > 3)
            buffer.deleteCharAt(buffer.length() - 2);

        log.debug("Content: \n{}", buffer.toString());

        return buffer.toString().getBytes(CoapMessage.CHARSET);

    }


    @Override
    @SuppressWarnings("unchecked")
    public byte[] getSerializedResourceStatus(long contentFormat){
        StringBuilder buffer = new StringBuilder();

        for(Webservice webservice : getResourceStatus().values()){
            buffer.append("<").append(webservice.getPath()).append(">");

            String previousKey = null;
            for (LinkAttribute linkAttribute : (Iterable<LinkAttribute>) webservice.getLinkAttributes()) {
                buffer.append(linkAttribute.getKey().equals(previousKey) ? " " : ";")
                        .append(linkAttribute.getValue());

                previousKey = linkAttribute.getKey();
            }
        }

        if(buffer.length() > 3)
            buffer.deleteCharAt(buffer.length() - 2);

        log.debug("Content: \n{}", buffer.toString());

        return buffer.toString().getBytes(CoapMessage.CHARSET);
    }



    @Override
    public void shutdown() {
        //nothing to do here...
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return this.etag;
    }

    @Override
    public void updateEtag(Map<String, Webservice> resourceStatus) {
        this.etag = Ints.toByteArray(Arrays.hashCode(getSerializedResourceStatus(ContentFormat.APP_LINK_FORMAT)));
    }
}
