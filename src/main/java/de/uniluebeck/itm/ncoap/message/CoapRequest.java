/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uniluebeck.itm.ncoap.message;

import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.message.options.*;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;

import de.uniluebeck.itm.ncoap.toolbox.Tools;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;

/**
 * @author Oliver Kleine
 */
public class CoapRequest extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapRequest.class.getName());

    //private CoapResponseProcessor callback;

    /**
     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     *
     * @param msgType  A {@link MsgType}
     * @param code A {@link Code}
     * @param targetUri the recipients URI
     *
     * @throws {@link InvalidOptionException} if one of the target URI options to be created is not valid
     * @throws {@link URISyntaxException} if the URI is not appropriate for a CoAP message.
     * @throws {@link ToManyOptionsException} if the target URI needs more than the maximum number of options per message
     * @throws {@link InvalidMessageException} if the given code is not suitable for a request
     */
    public CoapRequest(MsgType msgType, Code code, URI targetUri)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {

        super(msgType, code);

        if(!code.isRequest()){
            throw new InvalidMessageException("Code " + code + " is no request code!");
        }

        setTargetURI(targetUri);

        log.debug("New request created: {}.", this);

    }

//    /**
//     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
//     * and initial option list with target URI-related options set.
//     *
//     * @param msgType  a {@link MsgType}
//     * @param code a {@link Code}
//     * @param targetUri the recipients URI
//     * @param callback a {@link de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor} instance, most likely the {@link CoapClientApplication} instance that
//     *                 created this request
//     *
//     * @throws {@link InvalidOptionException} if one of the target URI options to be created is not valid
//     * @throws {@link URISyntaxException} if the URI is not appropriate for a CoAP message
//     * @throws {@link ToManyOptionsException} if the target URI needs more than the maximum number of options per message
//     * @throws {@link InvalidMessageException} if the given code is not suitable for a request
//     *
//     */
//    public CoapRequest(MsgType msgType, Code code, URI targetUri, CoapResponseProcessor callback)
//            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {
//
//        this(msgType, code, targetUri);
//        this.callback = callback;
//    }

    /**
     * Creates a new {@link CoapRequest} instance using the given parameters
     *
     * @param header a {@link Header}
     * @param optionList an {@link OptionList}
     * @param payload a {@link ChannelBuffer} containing the payload
     */
    public CoapRequest(Header header, OptionList optionList, ChannelBuffer payload){
        super(header, optionList, payload);
    }

//    /**
//     * Returns the {@link de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor} instance to be called upon reception of a {@link CoapResponse}
//     * @return the {@link de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor} instance to be called upon reception of a {@link CoapResponse}
//     */
//    public CoapResponseProcessor getResponseCallback() {
//        return callback;
//    }
//
//    /**
//     * Set the {@link de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor} instance to be called upon reception of a {@link CoapResponse}
//     *
//     * @param coapResponseProcessor the {@link de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor} instance to be called upon reception of a
//     * {@link CoapResponse}
//     */
//    public void setResponseCallback(CoapResponseProcessor coapResponseProcessor){
//        this.callback = coapResponseProcessor;
//    }

     /**
     * Returns the target URI of this {@link CoapRequest}.
     * @return the target URI of this {@link CoapRequest}.
     */
    public URI getTargetUri() {

        try {
            String uri = "coap://";

            //add host
            uri += getOption(URI_HOST).get(0).getDecodedValue();

            //add port
            long port = (Long) getOption(URI_PORT).get(0).getDecodedValue();
            if(port != OptionRegistry.COAP_PORT_DEFAULT)
                uri += ":" + port;

            //add path
            for(Option option : getOption(URI_PATH)){
                 uri += "/" + option.getDecodedValue();
            }

            //add query
            List<Option> list = getOption(URI_QUERY);
            if(!list.isEmpty()){
                uri += "?";
                for(Option option : list){
                    uri = uri + option.getDecodedValue() + "&";
                }
                //remove the last "&"
                uri = uri.substring(0, uri.length() - 1);
            }

            return new URI(uri);
        }
        catch (URISyntaxException e) {
            log.error("This should never happen!", e);
            return null;
        }
    }

    /**
     * This method sets all necessary target URI related options. This causes eventually already contained
     * target URI related options to be removed from the list even in case of an exception.
     *
     * The URI host option will only be added if its not given
     * as IP Address (IPv4 or IPv6). The URI port will only added if its not the CoAP standard port 5683. Nevertheless
     * the methods {@link #getTargetUri()} and {@link #getOption(OptionName o)} both return the given target
     * IP address, respective the target port using the reconstructing strategy defined in the CoAP draft. Not to add
     * default values as actual options avoids unnecessary traffic on the network and enables more useful options
     * to be set in the option list which has a maximum size of 15.
     *
     * @param targetUri The absolute {@link URI} of the recipients service
     *
     * @throws {@link URISyntaxException} if the given URI is not valid (e.g. not absolute)
     * @throws {@link InvalidOptionException} if at least one of the options to be set is not valid.
     * @throws {@link ToManyOptionsException} if adding all target URI options would exceed the maximum number
     * of options per message.
     */
    public void setTargetURI(URI targetUri) throws URISyntaxException, InvalidOptionException, ToManyOptionsException {
        optionList.removeTargetURI();
        try{
            //Create collection of target URI related options
            Collection<Option> targetUriOptions = Option.createTargetURIOptions(targetUri);

            //Add options to the list
            for(Option option : targetUriOptions){

                log.debug("Add {} option with value {}.", OptionName.getByNumber(option.getOptionNumber()),
                        Tools.toHexString(option.getValue()));


                OptionRegistry.OptionName optionName = OptionName.getByNumber(option.getOptionNumber());
                optionList.addOption(header.getCode(), optionName, option);
            }

            //Try to determine the receipients IP address if there was no URI host option set
            if(optionList.getOption(URI_HOST).isEmpty()){
                try{
                    rcptAddress = InetAddress.getByName(targetUri.getHost());
                } catch (UnknownHostException e) {
                    log.debug("The target hostname {} could not be resolved.", targetUri.getHost());
                }
            }
        }
        catch(InvalidOptionException e){
            optionList.removeTargetURI();

            log.debug("Critical option for target URI could not be added.", e);

            throw e;
        }
        catch(ToManyOptionsException e){
            optionList.removeTargetURI();
            log.debug("Critical option for target URI could not be added.", e);
            throw e;
        }
    }

    /**
     * Set one option for each media type to be accepted as response payload. This causes eventually already contained
     * accept options to be removed from the list even in case of an exception.
     *
     * @param mediaTypes the set of media types accepted as response payload
     * @throws InvalidOptionException if at least one of the options to be added is not valid
     * @throws ToManyOptionsException if adding all accept options would exceed the maximum number of
     * options per message.
     * @return <code>true</code> if accept options were succesfully set, <code>false</code> if accept option is not
     * meaningful with the message code and thus silently ignored
     */
    public boolean setAccept(OptionRegistry.MediaType... mediaTypes) {
        optionList.removeAllOptions(ACCEPT);
        try{
            for(OptionRegistry.MediaType mediaType : mediaTypes){
                Option option = Option.createUintOption(ACCEPT, mediaType.number);
                optionList.addOption(header.getCode(), ACCEPT, option);
            }
            return true;
        }
        catch (InvalidOptionException e) {
            optionList.removeAllOptions(ACCEPT);
            log.debug("Elective option (" + ACCEPT + ") could not be added.", e);
            return false;
        }
        catch (ToManyOptionsException e) {
            optionList.removeAllOptions(ACCEPT);
            log.debug("Elective option (" + ACCEPT + ") could not be added.", e);
            return false;
        }
    }

    /**
     * Returns a {@link Set<MediaType>} reconstructed from the contained {@link OptionName#ACCEPT} options
     * @return a {@link Set<MediaType>} reconstructed from the contained {@link OptionName#ACCEPT} options
     */
    public Set<MediaType> getAcceptedMediaTypes(){
        EnumSet<MediaType> result = EnumSet.noneOf(MediaType.class);

        for(Option option : optionList.getOption(ACCEPT)){
            result.add(MediaType.getByNumber((Long) option.getDecodedValue()));
        }

        return result;
    }

    /**
     * Returns the proxy URI constructed from the proxy URI related options contained in this {@link CoapRequest}.
     *
     * @return  the messages proxy URI (if any) or null otherwise
     * @throws {@link URISyntaxException} if the reconstruction of the URI from the options contained in this
     * {@link CoapRequest} fails
     */
    public URI getProxyURI() throws URISyntaxException {
        Collection<Option> options = optionList.getOption(PROXY_URI);

        if(options.isEmpty()){
            return null;
        }

        String result = "";
        for(Option option : options){
            result += option.getDecodedValue();
        }
        return new URI(result);

    }

     /**
     * Adds an appropriate number of proxy URI options to the list. This causes eventually already contained
     * proxy URI options to be removed from the list even in case of an exception.
     *
     * @param proxyURI The proxy URI to be added as options
     * @throws InvalidOptionException if at least one of the options to be added is invalid
     * @throws java.net.URISyntaxException if the given URI is not valid
     * @throws ToManyOptionsException if adding all proxy URI options would exceed the maximum number of options per
     * message.
     */
    public void setProxyURI(URI proxyURI) throws InvalidOptionException, URISyntaxException, ToManyOptionsException {
        optionList.removeAllOptions(PROXY_URI);
        try{
            Collection<Option> options = Option.createProxyUriOptions(proxyURI);
            for(Option option : options){
                optionList.addOption(header.getCode(), PROXY_URI, option);
            }
        }
        catch(InvalidOptionException e){
            optionList.removeAllOptions(PROXY_URI);
            log.debug("Critical option (" + PROXY_URI + ") could not be added.", e);

            throw e;
        }
        catch(ToManyOptionsException e){
            optionList.removeAllOptions(PROXY_URI);
            log.debug("Critical option (" + PROXY_URI + ") could not be added.", e);

            throw e;
        }
    }

    /**
     * Set one option for each ETAG enabling the computing of this requests payload on the server. This causes
     * eventually already contained if-match options to be removed from the list even in case of an exception.
     *
     * @param etags the set of ETAGs enabling the computing of this messages payload
     *
     * @throws {@link InvalidOptionException} if at least one of the options to be added is not valid
     * @throws {@link ToManyOptionsException} if adding all if-match options would exceed the maximum number of
     * options per message.
     */
    public void setIfMatch(byte[]... etags) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(IF_MATCH);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(IF_MATCH, etag);
                optionList.addOption(header.getCode(), IF_MATCH, option);
            }
        }
        catch (InvalidOptionException e) {
            optionList.removeAllOptions(IF_MATCH);
            log.debug("Critical option (" + IF_MATCH + ") could not be added.", e);
            throw e;
        }
        catch (ToManyOptionsException e) {
            optionList.removeAllOptions(IF_MATCH);
            log.debug("Critical option (" + IF_MATCH + ") could not be added.", e);
            throw e;
        }
    }

    /**
     * Set the if-non-match option. This causes eventually already contained if-non-match options to be removed from
     * the list even in case of an exception.
     *
     * @throws {@link ToManyOptionsException} if adding an if-non-match options would exceed the maximum number of
     * options per message.
     */
    public void setIfNoneMatch() throws ToManyOptionsException {
        optionList.removeAllOptions(IF_NONE_MATCH);
        try{
            Option option = Option.createEmptyOption(IF_NONE_MATCH);
            optionList.addOption(header.getCode(), IF_NONE_MATCH, option);
        } catch (InvalidOptionException e) {
            optionList.removeAllOptions(IF_NONE_MATCH);
            log.error("This should never happen!", e);
        } catch (ToManyOptionsException e) {
            optionList.removeAllOptions(IF_NONE_MATCH);
            log.debug("Critical option (" + IF_NONE_MATCH + ") could not be added.", e);
            throw e;
        }
    }

    /**
     * Set the observe option. This causes eventually already contained observe options to be removed from
     * the list even in case of an exception.
     *
     * @throws {@link ToManyOptionsException} if adding an observe options would exceed the maximum number of
     * options per message.
     */
    public void setObserveOptionRequest() throws ToManyOptionsException {
        optionList.removeAllOptions(OBSERVE_REQUEST);
        try{
            Option option = Option.createEmptyOption(OBSERVE_REQUEST);
            optionList.addOption(header.getCode(), OBSERVE_REQUEST, option);
        } catch (InvalidOptionException e) {
            optionList.removeAllOptions(OBSERVE_REQUEST);
            log.error("This should never happen!", e);
        } catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OBSERVE_REQUEST);
            log.debug("Critical option (" + OBSERVE_REQUEST + ") could not be added.", e);
            throw e;
        }
    }
}
