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

package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.communication.callback.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.*;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;

/**
 * @author Oliver Kleine
 */
public class CoapRequest extends CoapMessage {

    private static Logger log = Logger.getLogger(CoapRequest.class.getName());

    private ResponseCallback callback;

    /**
     * This method creates a new Message object and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     * @param msgType  The message type
     * @param code The message code
     * @param targetUri the recipients URI
     * @throws de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException if one of the target URI options to be created is not valid
     * @throws java.net.URISyntaxException if the URI is not appropriate for a CoAP message
     * @throws de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException if the target URI needs more than the maximum number of options per message
     * @throws InvalidMessageException if the given code is not suitable for a request
     * @return a new Message instance
     */
    public CoapRequest(MsgType msgType, Code code, URI targetUri)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {

        super(msgType, code);

        if(!code.isRequest()){
            throw new InvalidMessageException("[CoapDefaultRequest] Code " + code + " is no request code!");
        }

        setTargetURI(targetUri);

        if(log.isDebugEnabled()){
            log.debug("[CoapDefaultRequest] Created new request instance " +
                "(MsgType: " + msgType + ", Code: " + code + ", TargetURI: " + getTargetUri() + ")");
        }
    }

    /**
     * This method creates a new Message object and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     * @param msgType  The message type
     * @param code The message code
     * @param targetUri the recipients URI
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws java.net.URISyntaxException if the URI is not appropriate for a CoAP message
     * @throws ToManyOptionsException if the target URI needs more than the maximum number of options per message
     * @throws InvalidMessageException if the given code is not suitable for a request
     * @return a new Message instance
     */
    public CoapRequest(MsgType msgType, Code code, URI targetUri, ResponseCallback callback)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {

        this(msgType, code, targetUri);
        this.callback = callback;
    }

    public CoapRequest(Header header, OptionList optionList, ChannelBuffer payload){
        super(header, optionList, payload);
    }

    public ResponseCallback getResponseCallback() {
        return callback;
    }

    public void setResponseCallback(ResponseCallback responseCallback){
        this.callback = responseCallback;
    }

     /**
     * Returns the messages target URI
     * @return the messages target URI if the message is a request or null if its a response
     */
    public URI getTargetUri() {

        try {
            String uri = "coap://";

            //add host
            List<Option> list = getOption(OptionRegistry.OptionName.URI_HOST);

            StringOption uriHost = (StringOption) list.toArray()[0];
            uri = uri + uriHost.getDecodedValue();

            //add port
            list = getOption(OptionRegistry.OptionName.URI_PORT);
            UintOption uriPort = (UintOption) list.toArray()[0];
            uri = uri + ":" + uriPort.getDecodedValue();

            //add path
            list = getOption(OptionRegistry.OptionName.URI_PATH);
            for(Option option : list){
                StringOption uriPath = (StringOption) option;
                uri = uri + "/" + uriPath.getDecodedValue();
            }

            //add query
            list = getOption(OptionRegistry.OptionName.URI_QUERY);
            if(!list.isEmpty()){
                uri = uri + "?";
                for(Option option : list){
                    StringOption uriQuery = (StringOption) option;
                    uri = uri + uriQuery.getDecodedValue() + "&";
                }
                //remove the last "&"
                uri = uri.substring(0, uri.length() - 1);
            }

            return new URI(uri);
        }
        catch (URISyntaxException e) {
            log.fatal("[Message] This should never happen!", e);
            return null;
        }
    }

    /**
     * This method sets all necessary target URI related options. This causes eventually already contained
     * target URI related options to be removed from the list even in case of an exception.
     *
     * The URI host option will only be added if its not given
     * as IP Address (IPv4 or IPv6). The URI port will only added if its not the CoAP standard port 5683. Nevertheless
     * the methods <code>getTargetURI</code> and <code>getOption(OptionName o)</code> both return the given target
     * IP address, respective the target port using the reconstructing strategy defined in the CoAP draft. Not to add
     * default values as actual options avoids unnecessary traffic on the network and enables more useful options
     * to be set in the option list which has a maximum size of 15.
     *
     * @param targetUri The absolute URI of the recipients service
     * @throws URISyntaxException if the given URI is not valid (e.g. not absolute)
     * @throws InvalidOptionException if at least one of the options to be set is not valid.
     * @throws ToManyOptionsException if adding all target URI options would exceed the maximum number of options per
     * message.
     */
    public void setTargetURI(URI targetUri) throws URISyntaxException, InvalidOptionException, ToManyOptionsException {
        optionList.removeTargetURI();
        try{
            //Create collection of target URI related options
            Collection<Option> targetUriOptions = Option.createTargetURIOptions(targetUri);

            //Add options to the list
            for(Option option : targetUriOptions){
                if(log.isDebugEnabled()){
                    log.debug("[Message] Add " + OptionRegistry.getOptionName(option.getOptionNumber()) +
                            " option with value: " + Option.getHexString(option.getValue()));
                }

                OptionRegistry.OptionName optionName = OptionRegistry.getOptionName(option.getOptionNumber());
                optionList.addOption(header.getCode(), optionName, option);
            }

            //Try to determine the receipients IP address if there was no URI host option set
            if(optionList.getOption(OptionRegistry.OptionName.URI_HOST).isEmpty()){
                try{
                    rcptAddress = InetAddress.getByName(targetUri.getHost());
                } catch (UnknownHostException e) {
                    if(log.isDebugEnabled()){
                        log.debug("[Message] The target hostname " + targetUri.getHost() + " could not be resolved.");
                    }
                }
            }
        }
        catch(InvalidOptionException e){
            optionList.removeTargetURI();
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option for target URI could not be added.", e);
            }
            throw e;
        }
        catch(ToManyOptionsException e){
            optionList.removeTargetURI();
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option for target URI could not be added.", e);
            }
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
    public boolean setAccept(OptionRegistry.MediaType... mediaTypes) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.ACCEPT);
        try{
            for(OptionRegistry.MediaType mediaType : mediaTypes){
                Option option = Option.createUintOption(OptionRegistry.OptionName.ACCEPT, mediaType.number);
                optionList.addOption(header.getCode(), OptionRegistry.OptionName.ACCEPT, option);
            }
            return true;
        }
        catch (InvalidOptionException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.ACCEPT);
            if(log.isDebugEnabled()){
                log.debug("[Message] Elective option (" + OptionRegistry.OptionName.ACCEPT + ") could not be added.", e);
            }
            return false;
        }
        catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.ACCEPT);
            if(log.isDebugEnabled()){
                log.debug("[Message] Elective option (" + OptionRegistry.OptionName.ACCEPT + ") could not be added.", e);
            }
            return false;
        }
    }

    /**
     * Returns the contained Proxy URI
     * @return  the messages proxy URI (if any) or null otherwise
     * @throws java.net.URISyntaxException
     */
    public URI getProxyURI() throws URISyntaxException {
        Collection<Option> options = optionList.getOption(OptionRegistry.OptionName.PROXY_URI);

        if(options.isEmpty()){
            return null;
        }

        String result = "";
        for(Option option : options){
            result += ((StringOption)option).getDecodedValue();
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
        optionList.removeAllOptions(OptionRegistry.OptionName.PROXY_URI);
        try{
            Collection<Option> options = Option.createProxyUriOptions(proxyURI);
            for(Option option : options){
                optionList.addOption(header.getCode(), OptionRegistry.OptionName.PROXY_URI, option);
            }
        }
        catch(InvalidOptionException e){
            optionList.removeAllOptions(OptionRegistry.OptionName.PROXY_URI);

            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.PROXY_URI + ") could not be added.", e);
            }

            throw e;
        }
        catch(ToManyOptionsException e){
            optionList.removeAllOptions(OptionRegistry.OptionName.PROXY_URI);

            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.PROXY_URI + ") could not be added.", e);
            }

            throw e;
        }
    }

    /**
     * Set one option for each ETAG enabling the computing of this requests payload on the server. This causes
     * eventually already contained if-match options to be removed from the list even in case of an exception.
     *
     * @param etags the set of ETAGs enabling the computing of this messages payload
     * @throws InvalidOptionException if at least one of the options to be added is not valid
     * @throws ToManyOptionsException if adding all if-match options would exceed the maximum number of
     * options per message.
     */
    public void setIfMatch(byte[]... etags) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.IF_MATCH);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(OptionRegistry.OptionName.IF_MATCH, etag);
                optionList.addOption(header.getCode(), OptionRegistry.OptionName.IF_MATCH, option);
            }
        }
        catch (InvalidOptionException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.IF_MATCH);
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.IF_MATCH + ") could not be added.", e);
            }
            throw e;
        }
        catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.IF_MATCH);
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.IF_MATCH + ") could not be added.", e);
            }
            throw e;
        }
    }

    /**
     * Set the if-non-match option. This causes eventually already contained if-non-match options to be removed from
     * the list even in case of an exception.
     *
     * @throws ToManyOptionsException if adding an if-non-match options would exceed the maximum number of
     * options per message.
     */
    public void setIfNoneMatch() throws ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.IF_NONE_MATCH);
        try{
            Option option = Option.createEmptyOption(OptionRegistry.OptionName.IF_NONE_MATCH);
            optionList.addOption(header.getCode(), OptionRegistry.OptionName.IF_NONE_MATCH, option);
        } catch (InvalidOptionException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.IF_NONE_MATCH);
            log.fatal("[Message] This should never happen!", e);
        } catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.IF_NONE_MATCH);
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.IF_NONE_MATCH + ") could not be added.", e);
            }
            throw e;
        }
    }
}
