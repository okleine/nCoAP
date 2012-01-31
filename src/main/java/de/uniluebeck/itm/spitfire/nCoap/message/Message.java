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

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Oliver Kleine
 */
public class Message {

    private static Logger log = Logger.getLogger("nCoap");

    private Header header;
    private OptionList optionList;
    private ChannelBuffer payload;

    private InetAddress rcptAddress;

    /**
     * This method creates a new Message object and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     * @param msgType  The message type
     * @param code The message code
     * @param targetUri the recipients URI
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws URISyntaxException if the URI is not appropriate for a CoAP message
     * @throws ToManyOptionsException if the target URI needs more than the maximum number of options per message
     * @throws InvalidMessageException if the given code is not suitable for a request
     * @return a new Message instance
     */
    public static Message createRequest(MsgType msgType, Code code, URI targetUri)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {

        if(!code.isRequest()){
            throw new InvalidMessageException("[Message] Code " + code + " is not for a request.");
        }

        Message message = new Message(msgType, code);
        message.setTargetURI(targetUri);
        log.debug("[Message] Created new message instance " +
                "(MsgType: " + msgType + ", Code: " + code + ", TargetURI: " + message.getTargetUri() + ")");

        log.debug("[Message] Option count of new message: " + message.getOptionCount());
        return message;
    }

    /**
     * This method creates a new Message object and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     * @param msgType  The message type
     * @param code The message code
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws ToManyOptionsException if the target URI needs more than the maximum number of options per message
     * @throws InvalidMessageException if the given code is not suitable for a request
     * @return a new Message instance
     */
    public static Message createResponse(MsgType msgType, Code code)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException {
        if(code.isRequest()){
             throw new InvalidMessageException("[Message] Code " + code + " is not for a response.");
        }

        Message message =  new Message(msgType, code);
        log.debug("[Message] Created new message instance " + "(MsgType: " + msgType + ", Code: " + code + ")");
        return message;
    }

    //Private constructor invoked by static method createRequest(MsgType msgType, Code code, URI targetUri)
    protected Message(MsgType msgType, Code code)
            throws InvalidOptionException, ToManyOptionsException {

        try {
            header = new Header(msgType, code);
            log.debug("[Message] Created new message instance " + "(MsgType: " + msgType + ", Code: " + code + ")");
        } catch (InvalidHeaderException e) {
            log.fatal("[Message] This should never happen!", e);
        }

        //create option list
        optionList = new OptionList();
    }

    public Message(Header header, OptionList optionList, ChannelBuffer payload){
        this.header = header;
        this.optionList = optionList;
        this.payload = payload;
    }

    /**
     * Adds the option representing the content type to the option list. This causes an eventually already contained
     * content type option to be removed from the list even in case of an exception.
     *
     * @param mediaType The media type of the content
     * @throws InvalidOptionException if there is a content type option already contained in the option list.
     * @throws ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     */
    public void setContentType(MediaType mediaType) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionName.CONTENT_TYPE);

        try{
            Option option = Option.createUintOption(OptionName.CONTENT_TYPE, mediaType.number);
            optionList.addOption(header.getCode(), OptionName.CONTENT_TYPE, option);
        }
        catch(InvalidOptionException | ToManyOptionsException e){
            optionList.removeAllOptions(OptionName.CONTENT_TYPE);
            log.debug("[Message] Critical option (" + OptionName.CONTENT_TYPE + ") could not be added.", e);
            throw e;
        }
    }

    /**
     * Adds the max age option for the messages content. This is to indicate the maximum time a response may be
     * cached until it is considered not fresh. This causes an eventually already contained
     * max age option to be removed from the list even in case of an exception.
     *
     * @param maxAge The maximum allowed time to cache the response in seconds
     * @throws InvalidOptionException if the given max age is out of a valid range
     * @throws  ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     * @return <code>true</code> if content type was succesfully set, <code>false</code> if max age option is not
     * meaningful with the message code and thus silently ignored or if max age is equal to
     * OptionRegistry.MAX_AGE_DEFAULT
     */
    public boolean setMaxAge(int maxAge) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionName.MAX_AGE);

        if(maxAge == OptionRegistry.MAX_AGE_DEFAULT){
            return false;
        }

        try{
            Option option = Option.createUintOption(OptionName.MAX_AGE, maxAge);
            optionList.addOption(header.getCode(), OptionName.MAX_AGE, option);
            return true;
        }
        catch(InvalidOptionException | ToManyOptionsException e){
            log.debug("[Message] Elective option (" + OptionName.MAX_AGE + ") could not be added.", e);
            optionList.removeAllOptions(OptionName.MAX_AGE);
            return false;
        }
    }

    /**
     * Adds an appropriate number of proxy URI options to the list. This causes eventually already contained
     * proxy URI options to be removed from the list even in case of an exception.
     *
     * @param proxyURI The proxy URI to be added as options
     * @throws InvalidOptionException if at least one of the options to be added is invalid
     * @throws URISyntaxException if the given URI is not valid
     * @throws ToManyOptionsException if adding all proxy URI options would exceed the maximum number of options per
     * message.
     */
    public void setProxyURI(URI proxyURI) throws InvalidOptionException, URISyntaxException, ToManyOptionsException {
        optionList.removeAllOptions(OptionName.PROXY_URI);
        try{
            Collection<Option> options = Option.createProxyUriOptions(proxyURI);
            for(Option option : options){
                optionList.addOption(header.getCode(), OptionName.PROXY_URI, option);
            }
        }
        catch(InvalidOptionException | ToManyOptionsException e){
            optionList.removeAllOptions(OptionName.PROXY_URI);
            log.debug("[Message] Critical option (" + OptionName.PROXY_URI + ") could not be added.", e);
            throw e;
        }
    }

    /**
     * Returns the contained Proxy URI
     * @return  the messages proxy URI (if any) or null otherwise
     * @throws java.net.URISyntaxException
     */
    public URI getProxyURI() throws URISyntaxException {
        Collection<Option> options = optionList.getOption(OptionName.PROXY_URI);

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
     * @param etags The set of ETAGs to be added as options
     * @throws InvalidOptionException if at least one of the options to be added is invalid
     * @throws ToManyOptionsException if adding all ETAG options would exceed the maximum number of options per
     * message
     * @return <code>true</code> if ETAGs were succesfully set, <code>false</code> if ETAG option is not
     * meaningful with the message code and thus silently ignored
     */
    public boolean setETAG(byte[]... etags) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionName.ETAG);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(OptionName.ETAG, etag);
                optionList.addOption(header.getCode(), OptionName.ETAG, option);
            }
            return true;
        }
        catch(InvalidOptionException | ToManyOptionsException e){
            log.debug("[Message] Elective option (" + OptionName.MAX_AGE + ") could not be added.", e);
            optionList.removeAllOptions(OptionName.ETAG);
            return false;
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
                log.debug("[Message] Add " + OptionRegistry.getOptionName(option.getOptionNumber()) + " option with value: " + Option.getHexString(option.getValue()));
                OptionName optionName = OptionRegistry.getOptionName(option.getOptionNumber());
                optionList.addOption(header.getCode(), optionName, option);
            }

            //Try to determine the receipients IP address if there was no URI host option set
            if(optionList.getOption(OptionName.URI_HOST).isEmpty()){
                try{
                    rcptAddress = InetAddress.getByName(targetUri.getHost());
                } catch (UnknownHostException e) {
                    log.debug("[Message] The target hostname " + targetUri.getHost() + " could not be resolved.");
                }
            }
        }
        catch(InvalidOptionException | ToManyOptionsException e){
            optionList.removeTargetURI();
            log.debug("[Message] Critical option for target URI could not be added.", e);
            throw e;
        }
    }

    /**
     * Adds all necessary location URI related options to the list. This causes eventually already contained
     * location URI related options to be removed from the list even in case of an exception.
     *
     * @param locationURI The location URI of the newly created resource. The parts scheme, host, and port are
     * ignored anyway and thus may not be included in the URI object
     * @throws InvalidOptionException if at least one of the options to be added is not valid
     * @throws ToManyOptionsException if adding all location URI related options would exceed the maximum number of
     * options per message.
     */
    public void setLocationURI(URI locationURI) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeLocationURI();
        try{
            Collection<Option> locationUriOptions = Option.createLocationUriOptions(locationURI);
            for(Option option : locationUriOptions){
                OptionName optionName = OptionRegistry.getOptionName(option.getOptionNumber());
                optionList.addOption(header.getCode(), optionName, option);
            }
        }
        catch (InvalidOptionException | ToManyOptionsException e) {
            optionList.removeLocationURI();
            log.debug("[Message] Critical option for location URI could not be added.", e);
            throw e;
        }
    }

    /**
     * Returns the URI reconstructed from the location URI related options contained in the message
     * @return the URI reconstructed from the location URI related options contained in the message or null if there
     * are no location URI related options
     * @throws URISyntaxException if the URI to be reconstructed from options is invalid
     */
    public URI getLocationURI() throws URISyntaxException {
        Collection<Option> options = optionList.getOption(OptionName.LOCATION_PATH);
        if(options.isEmpty()){
            return null;
        }
        String result = "";
        for(Option option : options){
            result += ("/" + ((StringOption)option).getDecodedValue());
        }

        options = optionList.getOption((OptionName.LOCATION_QUERY));
        if(!options.isEmpty()){
            result += "?";
        }
        for(Option option : options){
            result += ("/" + ((StringOption)option).getDecodedValue());
        }

        if(result.equals("")){
            return null;
        }

        return new URI(result);
    }

    /**
     * Adds token option to the option list. This causes eventually already contained
     * token options to be removed from the list even in case of an exception.
     *
     * @param token the messages token
     * @throws InvalidOptionException if the token does not match the token constraints
     * @throws ToManyOptionsException if adding the token option would exceed the maximum number of
     * options per message.
     */
    public void setToken(byte[] token) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionName.TOKEN);
        try{
            Option option = Option.createOpaqueOption(OptionName.TOKEN, token);
            optionList.addOption(header.getCode(), OptionName.TOKEN, option);
        } catch (InvalidOptionException | ToManyOptionsException e) {
            optionList.removeAllOptions(OptionName.TOKEN);
            log.debug("[Message] Critical option " + OptionName.TOKEN + " could not be added.", e);
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
    public boolean setAccept(MediaType... mediaTypes) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionName.ACCEPT);
        try{
            for(MediaType mediaType : mediaTypes){
                Option option = Option.createUintOption(OptionName.ACCEPT, mediaType.number);
                optionList.addOption(header.getCode(), OptionName.ACCEPT, option);
            }
            return true;
        }
        catch (InvalidOptionException | ToManyOptionsException e) {
            optionList.removeAllOptions(OptionName.ACCEPT);
            log.debug("[Message] Elective option (" + OptionName.ACCEPT + ") could not be added.", e);
            return false;
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
        optionList.removeAllOptions(OptionName.IF_MATCH);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(OptionName.IF_MATCH, etag);
                optionList.addOption(header.getCode(), OptionName.IF_MATCH, option);
            }
        }
        catch (InvalidOptionException | ToManyOptionsException e) {
            optionList.removeAllOptions(OptionName.IF_MATCH);
            log.debug("[Message] Critical option (" + OptionName.IF_MATCH + ") could not be added.", e);
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
        optionList.removeAllOptions(OptionName.IF_NONE_MATCH);
        try{
            Option option = Option.createEmptyOption(OptionName.IF_NONE_MATCH);
            optionList.addOption(header.getCode(), OptionName.IF_NONE_MATCH, option);
        } catch (InvalidOptionException e) {
            optionList.removeAllOptions(OptionName.IF_NONE_MATCH);
            log.fatal("[Message] This should never happen!", e);
        } catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OptionName.IF_NONE_MATCH);
            log.debug("[Message] Critical option (" + OptionName.IF_NONE_MATCH + ") could not be added.", e);
            throw e;
        }
    }

    /**
     * Adds the payload to the message. This cause eventually already contained payload to be removed.
     * @param buf ChannelBuffer containing the message payload
     * @throws MessageDoesNotAllowPayloadException if the messages type does not allow payload
     * @return the size of the payload as number of bytes
     */
    public int setPayload(ChannelBuffer buf) throws MessageDoesNotAllowPayloadException {
        payload = null;
        if(header.getCode().allowsPayload()){
            payload = buf;
            return payload.readableBytes();
        }
        String msg = "[Message] Message Type " + header.getMsgType() + " does not allow payload.";
        throw new MessageDoesNotAllowPayloadException(msg);
    }

    /**
     * Adds the payload to the message. This cause eventually already contained payload to be removed.
     * @param bytes Array of bytes containing the message payload
     * @throws MessageDoesNotAllowPayloadException if the messages type does not allow payload
     * @return the size of the payload as number of bytes
     */
    public int setPayload(byte[] bytes) throws MessageDoesNotAllowPayloadException {
        return setPayload(ChannelBuffers.wrappedBuffer(bytes));
    }

    /**
     * Returns the messages payload
     * @return the messages payload as {@link ChannelBuffer} or null if there is no payload
     */
    public ChannelBuffer getPayload(){
        return payload;
    }

    /**
     * Returns the message option list. Note that the option list does only contain options having non-default values.
     * If e.g. the target URI port is 5683 which is the default value, there will be no URI port option contained
     * in the list. Use getOption(OptionName.URI_PORT) to get the actual value.
     *
     * @return the {@link OptionList} instance containing all contained with options non-default values
     */
    public OptionList getOptionList(){
        return optionList;
    }

    /**
     * Returns the set of {@link Option} instances of the given {@link OptionName} contained in the messages
     * {@link OptionList} or an eventual default value if there is no matching option contained. The set is empty
     * if there is neither an Option instance contained in the OptionList or a default value for the OptionName.
     *
     * @param optionName The name of the option to be looked up.
     * @return The set of Option instances for the message matching the given OptionName
     */
    public List<Option> getOption(OptionName optionName){
        try{

            List<Option> result = optionList.getOption(optionName);

            if(!result.isEmpty()){
               return result;
            }

            //Default values to be assumed when explicitly defined options are missing
            switch(optionName){
                case URI_HOST:
                    result = new ArrayList<>(1);
                    result.add(Option.createStringOption(OptionName.URI_HOST,
                            "[" + rcptAddress.getHostAddress() + "]"));
                    break;
                case URI_PORT:
                    result = new ArrayList<>(1);
                    result.add(Option.createUintOption(OptionName.URI_PORT, OptionRegistry.COAP_PORT_DEFAULT));
                    break;
                case MAX_AGE:
                    result = new ArrayList<>(1);
                    result.add(Option.createUintOption(OptionName.MAX_AGE, OptionRegistry.MAX_AGE_DEFAULT));
                    break;
                case TOKEN:
                    result = new ArrayList<>(1);
                    result.add(Option.createOpaqueOption(OptionName.TOKEN, new byte[0]));
                    break;
            }

            return result;
        }
        catch(InvalidOptionException e){
            log.fatal("[Message] This should never happen.", e);
            return null;
        }
    }

    /**
     * Returns the messages header
     * @return the messages header
     */
    public Header getHeader(){
        return header;
    }

    /**
     * Returns the messages target URI
     * @return the messages target URI if the message is a request or null if its a response
     */
    public URI getTargetUri() {
        //Responses must not have target URI options
        if(!(header.getCode().isRequest())){
            return null;
        }

        //Part for requests
        try {
            String uri = "coap://";

            //add host
            List<Option> list = getOption(OptionName.URI_HOST);

            StringOption uriHost = (StringOption) list.toArray()[0];
            uri = uri + uriHost.getDecodedValue();

            //add port
            list = getOption(OptionName.URI_PORT);
            UintOption uriPort = (UintOption) list.toArray()[0];
            uri = uri + ":" + uriPort.getDecodedValue();

            //add path
            list = getOption(OptionName.URI_PATH);
            for(Option option : list){
                StringOption uriPath = (StringOption) option;
                uri = uri + "/" + uriPath.getDecodedValue();
            }

            //add query
            list = getOption(OptionName.URI_QUERY);
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
     * Returns the number of {@link Option} instances contained in the option list. Note that only options with
     * non-default values are contained in the option list.
     * @return the number of options contained in the messages option list
     */
    public int getOptionCount(){
        return optionList.getOptionCount();
    }

    /**
     * Sets the recipients IP address. Usually there is no need to set this value manually. It is only used to
     * define the default value of the URI host option and set automatically if necessary.
     *
     * @param rcptAddress The recipients IP address
     */
    public void setRcptAdress(InetAddress rcptAddress){
        this.rcptAddress = rcptAddress;
    }

    /**
     * Sets the message ID for this message. Normally, there is no need to set the message ID manually. It is set or
     * overwritten automatically by the
     * {@link de.uniluebeck.itm.spitfire.nCoap.communication.reliability.OutgoingMessageReliabilityHandler}.
     *
     * @param messageId the message ID for the message
     * @throws InvalidHeaderException if the message ID to be set is Invalid
     */
    public void setMessageId(int messageId) throws InvalidHeaderException {
       this.getHeader().setMsgID(messageId);
    }
}
