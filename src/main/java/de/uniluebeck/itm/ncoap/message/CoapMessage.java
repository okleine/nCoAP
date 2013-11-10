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
package de.uniluebeck.itm.ncoap.message;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.primitives.Longs;
import de.uniluebeck.itm.ncoap.message.options.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.*;


/**
 * This class is the base class for inheriting subtypes, i.e. requests and responses. This abstract class provides the
 * cut-set in terms of functionality of {@link CoapRequest} and {@link CoapResponse}.
 *
 * @author Oliver Kleine
 */
public abstract class CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());

    public static final int PROTOCOL_VERSION = 1;
    public static final Charset CHARSET = Charset.forName("UTF-8");
    public static final int UNDEFINED = -1;
    public static final int MAX_TOKEN_LENGTH = 8;

    private static final int ONCE       = 1;
    private static final int MULTIPLE   = 2;
    
    private static HashBasedTable<Integer, Integer, Integer> optionOccurenceConstraints = HashBasedTable.create();
    static{
        //Requests
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.URI_HOST,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.URI_PORT,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.PROXY_URI,          ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.PROXY_SCHEME,       ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.ACCEPT,             MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.GET,      Option.Name.ETAG,               MULTIPLE);

        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.URI_HOST,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.URI_PORT,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.PROXY_URI,          ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.PROXY_SCHEME,       ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.POST,     Option.Name.CONTENT_FORMAT,     ONCE);

        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.URI_HOST,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.URI_PORT,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.PROXY_URI,          ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.PROXY_SCHEME,       ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.IF_MATCH,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PUT,      Option.Name.IF_NONE_MATCH,      ONCE);

        optionOccurenceConstraints.put(MessageCode.Name.DELETE,   Option.Name.URI_HOST,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.DELETE,   Option.Name.URI_PORT,           ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.DELETE,   Option.Name.URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.DELETE,   Option.Name.URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.DELETE,   Option.Name.PROXY_URI,          ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.DELETE,   Option.Name.PROXY_SCHEME,       ONCE);

        //Response success (2.x)
        optionOccurenceConstraints.put(MessageCode.Name.CREATED_201,  Option.Name.LOCATION_PATH,      MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.CREATED_201,  Option.Name.LOCATION_QUERY,     MULTIPLE);
        optionOccurenceConstraints.put(MessageCode.Name.VALID_203,    Option.Name.ETAG,               ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.VALID_203,    Option.Name.MAX_AGE,            ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.CONTENT_205,  Option.Name.CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.CONTENT_205,  Option.Name.MAX_AGE,            ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.CONTENT_205,  Option.Name.ETAG,               ONCE);

        //Client errors (4.x)
        optionOccurenceConstraints.put(MessageCode.Name.BAD_REQUEST_400,                  Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.UNAUTHORIZED_401,                 Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.BAD_OPTION_402,                   Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.FORBIDDEN_403,                    Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.NOT_FOUND_404,                    Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.METHOD_NOT_ALLOWED_405,           Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.NOT_ACCEPTABLE_406,               Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PRECONDITION_FAILED_412,          Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.REQUEST_ENTITY_TOO_LARGE_413,     Option.Name.MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.REQUEST_ENTITY_TOO_LARGE_413,     Option.Name.SIZE_1,     ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415,   Option.Name.MAX_AGE,    ONCE);
        
        //Server errors (5.x)
        optionOccurenceConstraints.put(MessageCode.Name.INTERNAL_SERVER_ERROR_500,    Option.Name.MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.NOT_IMPLEMENTED_501,          Option.Name.MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.BAD_GATEWAY_502,              Option.Name.MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.GATEWAY_TIMEOUT_504,          Option.Name.MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(MessageCode.Name.PROXYING_NOT_SUPPORTED_505,   Option.Name.MAX_AGE,   ONCE);
    }



    protected InetAddress recipientAddress;

    private int messageType;
    private int messageCode;
    private int messageID;
    private byte[] token;
    protected SetMultimap<Integer, Option> options;
    private ChannelBuffer content;

    public static CoapMessage createCoapMessage(int messageType, int messageCode, int messageID, byte[] token)
            throws InvalidHeaderException {

        if(MessageCode.isRequest(messageCode)){
            CoapRequest coapRequest= new  CoapRequest(messageType, messageCode);
            coapRequest.setMessageID(messageID);
            coapRequest.setToken(token);
            return coapRequest;
        }
        else if(MessageCode.isResponse(messageCode)){
            CoapResponse coapResponse = new  CoapResponse(messageCode);
            coapResponse.setMessageType(messageType);
            coapResponse.setMessageID(messageID);
            coapResponse.setToken(token);
            return coapResponse;
        }
        else if(messageCode == MessageCode.Name.EMPTY){
            if(messageType == MessageType.Name.ACK){
                if(token.length == 0)
                    return createEmptyAcknowledgement(messageID);
                else
                    throw new InvalidHeaderException("Empty ACK must have token length of 0");
            }
            else if(messageType == MessageType.Name.RST){
                if(token.length == 0)
                    return createEmptyReset(messageID);
                else
                    throw new InvalidHeaderException("Empty RST must have token length of 0");
            }
            else{
                throw new InvalidHeaderException("Code EMPTY but neither ACK or RST.");
            }
        }

        throw new InvalidHeaderException("Message is neither request, nor response nor EMPTY.");


    }

    protected CoapMessage(int messageType, int messageCode, int messageID, byte[] token) throws InvalidHeaderException {
        this(messageType, messageCode);
        this.setMessageID(messageID);
        this.setToken(token);
    }

    protected CoapMessage(int messageType, int messageCode) throws InvalidHeaderException {
        this();
        this.setMessageType(messageType);
        this.messageCode = messageCode;
    }


    protected CoapMessage(int messageCode){
        this();
        this.messageCode = messageCode;
    }

    private CoapMessage(){
        this.options = Multimaps.newSetMultimap(new TreeMap<Integer, Collection<Option>>(),
                LinkedHashSetSupplier.getInstance());
        this.messageID = UNDEFINED;
        this.token = new byte[0];
        this.content = ChannelBuffers.EMPTY_BUFFER;
    }

    /**
     * Method to create an empty reset message which is strictly speaking neither a request nor a response
     * @param messageID the message ID of the reset message.
     *
     * @return an instance of {@link CoapMessage} with {@link MessageType.Name#RST}
     */
    public static CoapMessage createEmptyReset(int messageID){
        CoapMessage emptyRST = new CoapMessage(){};
        emptyRST.messageType = MessageType.Name.RST;
        emptyRST.messageCode = MessageCode.Name.EMPTY;
        emptyRST.messageID = messageID;
        return emptyRST;
    }

    /**
     * Method to create an empty acknowledgement message which is strictly speaking neither a request nor a response
     * @param messageID the message ID of the acknowledgement message.
     *
     * @return an instance of {@link CoapMessage} with {@link MessageType.Name#ACK}
     */
    public static CoapMessage createEmptyAcknowledgement(int messageID){
        CoapMessage emptyACK = new CoapMessage(){};
        emptyACK.messageType = MessageType.Name.ACK;
        emptyACK.messageCode = MessageCode.Name.EMPTY;
        emptyACK.messageID = messageID;
        return emptyACK;
    }

    /**
     * Sets the message type of this {@link CoapMessage}. Usually there is no need to use this method as the value
     * is either set via constructor parameter (for requests) or automatically by the nCoAP framework (for responses).
     *
     * @param messageType the number representing the message type of this method
     *
     * @throws InvalidMessageException if the given message type is not supported.
     */
    public void setMessageType(int messageType) throws InvalidHeaderException {
        if(messageType < 0 || messageType > 3)
            throw new InvalidHeaderException("Invalid message type (" + messageType +
                    "). Only numbers 0-3 are allowed.");

        this.messageType = messageType;
    }

    public void setRecipientAddress(InetAddress recipientAddress){
        this.recipientAddress = recipientAddress;
    }


    /**
     * Adds
     * @param optionNumber
     * @param option
     * @throws InvalidOptionException
     */
    public void addOption(int optionNumber, Option option) throws InvalidOptionException {
        this.checkOptionPermission(optionNumber);

        for(int containedOption : options.keySet()){
            if(Option.mutuallyExcludes(containedOption, optionNumber))
                throw new InvalidOptionException(optionNumber, "Already contained option no. " + containedOption +
                        " excludes option no. " + optionNumber);
        }

        options.put(optionNumber, option);

        log.debug("Added option (number: {}, value: {})", optionNumber, option.toString());

    }

    protected void addStringOption(int optionNumber, String value) throws UnknownOptionException,
            InvalidOptionException {

        if(!(Option.getOptionType(optionNumber) == OptionType.Name.STRING))
            throw new InvalidOptionException(optionNumber, "Option number {} is no string-option.");

        //Add new option to option list
        StringOption option = new StringOption(optionNumber, value);
        addOption(optionNumber, option);
    }


    protected void addUintOption(int optionNumber, long value) throws UnknownOptionException, InvalidOptionException {

        if(!(Option.getOptionType(optionNumber) == OptionType.Name.UINT))
            throw new InvalidOptionException(optionNumber, "Option number {} is no uint-option.");

        //Add new option to option list
        UintOption option = new UintOption(optionNumber, new BigInteger(1, Longs.toByteArray(value)).toByteArray());
        addOption(optionNumber, option);

    }

    protected void addOpaqueOption(int optionNumber, byte[] value) throws InvalidOptionException, UnknownOptionException {

        if(!(Option.getOptionType(optionNumber) == OptionType.Name.OPAQUE))
            throw new InvalidOptionException(optionNumber, "Option number {} is no opaque option.");

        //Add new option to option list
        OpaqueOption option = new OpaqueOption(optionNumber, value);
        addOption(optionNumber, option);

    }

    protected void addEmptyOption(int optionNumber) throws InvalidOptionException, UnknownOptionException {

        if(!(Option.getOptionType(optionNumber) == OptionType.Name.EMPTY))
            throw new InvalidOptionException(optionNumber, "Option number {} is no empty option.");

        //Add new option to option list
        options.put(optionNumber, new EmptyOption(optionNumber));

        log.debug("Added empty option (number: {})", optionNumber);
    }

    /**
     * Removes all options with the given option number from this {@link CoapMessage} instance.
     * @param optionNumber the option number to remove from this message
     * @return the number of options that were removed, i.e. count
     */
    public int removeOptions(int optionNumber){
        int result = options.removeAll(optionNumber).size();
        log.debug("Removed {} options with number {}.", result, optionNumber);
        return result;
    }

    private void checkOptionPermission(int optionNumber) throws InvalidOptionException {
        Integer allowedOccurence = optionOccurenceConstraints.get(this.messageCode, optionNumber);
        if(allowedOccurence == null)
            throw new InvalidOptionException(optionNumber, "Option no. " + optionNumber + " not allowed with" +
                    "message code " + this.messageCode + ".");

        if(options.containsKey(optionNumber)){
            if(optionOccurenceConstraints.get(this.messageCode, optionNumber) == ONCE)
                throw new InvalidOptionException(optionNumber, "Option no. " + optionNumber + " already set.");
        }
    }

    /**
     * Returns the CoAP protocol version used for this message
     * @return the CoAP protocol version used for this message
     */
    public int getProtocolVersion() {
        return PROTOCOL_VERSION;
    }

    /**
     * Sets the message ID for this message. However, there is no need to set the message ID manually. It is set (or
     * overwritten) automatically by the nCoAP framework.
     *
     * @param messageID the message ID for the message
     */

    public void setMessageID(int messageID) throws InvalidHeaderException {

        if(messageID < 0 || messageID > 65535)
            throw new InvalidHeaderException("Message ID " + messageID + " is either negative or greater than 65535");

        this.messageID = messageID;
    }

    /**
     * Returns the message ID (-1 if not set, yet)
     *
     * @return the message ID (-1 if not set, yet)
     */
    public int getMessageID() {
        return messageID;
    }


    public int getMessageType() {
        return messageType;
    }


    public int getMessageCode() {
        return messageCode;
    }

    /**
     * Adds token option to the option list. This causes eventually already contained
     * token options to be removed from the list even in case of an exception.
     *
     * @param token the messages token
     *
     * @throws InvalidOptionException if the token does not match the token constraints
     * options per message.
     */
    public void setToken(byte[] token) throws InvalidHeaderException{
        if(token.length > 8)
            throw new InvalidHeaderException("Token is too long (" + token.length + " bytes).");

        this.token = token;
    }

    /**
     * Returns the value of the token option or an empty byte array b with <messageCode>(b.length == 0) == true</messageCode>.
     * @return the value of the messages token option
     */
    public byte[] getToken() {
        return this.token;
    }

    /**
     * Returns the number representing the format of the content or {@link CoapMessage#UNDEFINED} if no such option is
     * present in this {@link CoapMessage}. See {@link de.uniluebeck.itm.ncoap.message.options.ContentFormat} for some constants for predefined numbers.
     *
     * @return the number representing the format of the content or <code>null</code> if no such option is present
     * in this {@link CoapMessage}.
     */
    public long getContentFormat(){
        if(options.containsKey(Option.Name.CONTENT_FORMAT))
            return ((UintOption) options.get(Option.Name.CONTENT_FORMAT).iterator().next()).getDecodedValue();

        return ContentFormat.Name.UNDEFINED;
    }

    /**
     * Sets the Max-Age option of this {@link CoapMessage}. If there was a Max-Age option set prior to the
     * invocation of this method, the previous value is overwritten.
     *
     * @param maxAge the value for the Max-Age option to be set
     * @throws InvalidOptionException
     */
    public void setMaxAge(long maxAge) throws InvalidOptionException {
        try{
            this.options.removeAll(Option.Name.MAX_AGE);
            this.addUintOption(Option.Name.MAX_AGE, maxAge);
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    /**
     * Returns the value of the Max-Age option of this {@link CoapMessage}. If no such option exists, this method
     * returns {@link Option#MAX_AGE_DEFAULT}.
     *
     * @return the value of the Max-Age option of this {@link CoapMessage}. If no such option exists, this method
     * returns {@link Option#MAX_AGE_DEFAULT}.
     */
    public long getMaxAge(){
        if(options.containsKey(Option.Name.MAX_AGE))
            return ((UintOption) options.get(Option.Name.MAX_AGE).iterator().next()).getDecodedValue();
        else
            return Option.MAX_AGE_DEFAULT;
    }

    /**
     * Adds the content to the message. If this {@link CoapMessage} contained any content prior to the invocation of
     * method, the previous content is removed.
     *
     * @param content ChannelBuffer containing the message content
     *
     * @throws InvalidMessageException if the messages code does not allow content
     */
    public void setContent(ChannelBuffer content) throws InvalidMessageException {

        if(!(MessageCode.allowsContent(this.messageCode)) && content.readableBytes() > 0)
            throw new InvalidMessageException("Message Code " + this.messageCode + " does not allow content.");

        this.content = content;
    }

    /**
     * Adds the content to the message. If this {@link CoapMessage} contained any content prior to the invocation of
     * method, the previous content is removed.
     *
     * @param content ChannelBuffer containing the message content
     * @param contentFormat a long value representing the format of the content
     *
     * @throws InvalidMessageException if the messages code does not allow content
     * @throws InvalidOptionException if the content format option could not be set
     */
    public void setContent(ChannelBuffer content, long contentFormat) throws InvalidMessageException,
            InvalidOptionException {

        try {
            this.addUintOption(Option.Name.CONTENT_FORMAT, contentFormat);
            setContent(content);
        }
        catch (InvalidOptionException | InvalidMessageException e) {
            this.content = ChannelBuffers.EMPTY_BUFFER;
            this.removeOptions(Option.Name.CONTENT_FORMAT);
            throw e;
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }

    }


    /**
     * Adds the content to the message. If this {@link CoapMessage} contained any content prior to the invocation of
     * method, the previous content is removed.
     *
     * @param content ChannelBuffer containing the message content
     *
     * @throws InvalidMessageException if the messages code does not allow content
     */
    public void setContent(byte[] content) throws InvalidMessageException {

        setContent(ChannelBuffers.wrappedBuffer(content));

    }


    /**
     * Adds the content to the message. If this {@link CoapMessage} contained any content prior to the invocation of
     * method, the previous content is removed.
     *
     * @param content ChannelBuffer containing the message content
     * @param contentFormat a long value representing the format of the content
     *
     * @throws InvalidMessageException if the messages code does not allow content
     * @throws InvalidOptionException if the content format option could not be set
     */
    public void setContent(byte[] content, long contentFormat) throws InvalidMessageException,
            InvalidOptionException {

        setContent(ChannelBuffers.wrappedBuffer(content), contentFormat);

    }

    /**
     * Returns the messages content. If the message does not contain any content, this method returns an empty
     * {@link ChannelBuffer} ({@link ChannelBuffers#EMPTY_BUFFER}).
     *
     * @return Returns the messages content.
     */
    public ChannelBuffer getContent(){
        return content;
    }


    /**
     * Returns a {@link Multimap<Integer, Option>} with the option numbers as keys and {@link Option}s as values.
     * The returned multimap does not contain options with default values.
     *
     * @return a {@link Multimap<Integer, Option>} with the option numbers as keys and {@link Option}s as values.
     */
    public Multimap<Integer, Option> getAllOptions(){
        return this.options;
    }


    /**
     * Returns a {@link Set<Option>} containing the options that are explicitly set in this {@link CoapMessage}. The
     * returned set does not contain options with default values. If this {@link CoapMessage} does not contain any
     * options of the given option number, then the returned set is empty.
     *
     * @param optionNumber the option number
     *
     * @return a {@link Set<Option>} containing the options that are explicitly set in this {@link CoapMessage}.
     */
    public Set<Option> getOptions(int optionNumber){
        return this.options.get(optionNumber);
    }


    @Override
    public int hashCode(){
        return toString().hashCode() + content.hashCode();
    }

    /**
     * Returns <code>true</code> if and only if the given object is an instance of {@link CoapMessage}
     * and if the header, the token, the options and the content of both instances equal.
     *
     * @param object another object to compare this {@link CoapMessage} with
     *
     * @return <code>true</code> if and only if the given object is an instance of {@link CoapMessage}
     * and if the header, the token, the options and the content of both instances equal.
     */
    @Override
    public boolean equals(Object object){

        if(!(object instanceof CoapMessage)){
            return false;
        }

        CoapMessage other = (CoapMessage) object;
        return this.getProtocolVersion() == other.getProtocolVersion()
            && this.getMessageType() == other.getMessageType()
            && this.getMessageCode() == other.getMessageCode()
            && this.getMessageID() == other.getMessageID()
            && Arrays.equals(this.getToken(), other.getToken())
            && this.getAllOptions().equals(other.getAllOptions())
            && this.getContent().equals(other.getContent());

    }

    @Override
    public String toString(){

        StringBuffer result =  new StringBuffer();

        //Header + Token
        result.append("CoAP Message: [Header: (V) " + getProtocolVersion() + ", (T) " + getMessageType() + ", (TKL) "
            + getToken().length + ", (C) " + getMessageCode() + ", (ID) " + getMessageID() + " | (Token) "
            + new BigInteger(1, getToken()).toString(16) + " | ");

        //Options
        result.append("Options:");
        for(int optionNumber : getAllOptions().keySet()){
            result.append(" (No. " + optionNumber + ") ");
            Iterator<Option> iterator = this.getOptions(optionNumber).iterator();
            Option option = iterator.next();
            result.append(option.toString());
            while(iterator.hasNext())
                result.append(" / " + iterator.next().toString());
        }
        result.append(" | ");

        //Content
        result.append("Content: ");
        long payloadLength = getContent().readableBytes();
        if(payloadLength == 0)
            result.append("<no content>]");
        else
            result.append(getContent().toString(0, Math.min(getContent().readableBytes(), 20), CoapMessage.CHARSET)
                + "... ( " + payloadLength + " bytes)]");

        return result.toString();

    }
}
