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

import com.google.common.base.Supplier;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.primitives.Longs;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.*;

import static de.uniluebeck.itm.ncoap.message.MessageCodeNames.*;
import static de.uniluebeck.itm.ncoap.message.MessageTypeNames.ACK;
import static de.uniluebeck.itm.ncoap.message.MessageTypeNames.RST;
import static de.uniluebeck.itm.ncoap.message.OptionName.*;

/**
 * This class is the base class for inheriting subtypes, i.e. requests and responses. This abstract class provides the
 * cut-set in terms of functionality of {@link CoapRequest} and {@link CoapResponse}.
 *
 * @author Oliver Kleine
 */
public abstract class CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());

    public static final int VERSION = 1;
    public static final Charset CHARSET = Charset.forName("UTF-8");
    public static final int UNDEFINED = -1;

    private static final int ONCE       = 1;
    private static final int MULTIPLE   = 2;
    
    private static HashBasedTable<Integer, Integer, Integer> optionOccurenceConstraints = HashBasedTable.create();
    static{
        //Requests
        optionOccurenceConstraints.put(GET,      URI_HOST,           ONCE);
        optionOccurenceConstraints.put(GET,      URI_PORT,           ONCE);
        optionOccurenceConstraints.put(GET,      URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(GET,      URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(GET,      ACCEPT,             MULTIPLE);
        optionOccurenceConstraints.put(GET,      ETAG,               MULTIPLE);
        optionOccurenceConstraints.put(POST,     URI_HOST,           ONCE);
        optionOccurenceConstraints.put(POST,     URI_PORT,           ONCE);
        optionOccurenceConstraints.put(POST,     URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(POST,     URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(POST,     CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(PUT,      URI_HOST,           ONCE);
        optionOccurenceConstraints.put(PUT,      URI_PORT,           ONCE);
        optionOccurenceConstraints.put(PUT,      URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(PUT,      URI_QUERY,          MULTIPLE);
        optionOccurenceConstraints.put(PUT,      CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(PUT,      IF_MATCH,           ONCE);
        optionOccurenceConstraints.put(PUT,      IF_NONE_MATCH,      ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_HOST,           ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_PORT,           ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_PATH,           MULTIPLE);
        optionOccurenceConstraints.put(DELETE,   URI_QUERY,          MULTIPLE);

        //Response success (2.x)
        optionOccurenceConstraints.put(CREATED_201,  LOCATION_PATH,      MULTIPLE);
        optionOccurenceConstraints.put(CREATED_201,  LOCATION_QUERY,     MULTIPLE);
        optionOccurenceConstraints.put(VALID_203,    ETAG,               ONCE);
        optionOccurenceConstraints.put(VALID_203,    MAX_AGE,            ONCE);
        optionOccurenceConstraints.put(CONTENT_205,  CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(CONTENT_205,  MAX_AGE,            ONCE);
        optionOccurenceConstraints.put(CONTENT_205,  ETAG,               ONCE);

        //Client errors (4.x)
        optionOccurenceConstraints.put(BAD_REQUEST_400,                  MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(UNAUTHORIZED_401,                 MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(BAD_OPTION_402,                   MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(FORBIDDEN_403,                    MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(NOT_FOUND_404,                    MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(METHOD_NOT_ALLOWED_405,           MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(NOT_ACCEPTABLE_406,               MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(PRECONDITION_FAILED_412,          MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(REQUEST_ENTITY_TOO_LARGE_413,     MAX_AGE,    ONCE);
        optionOccurenceConstraints.put(REQUEST_ENTITY_TOO_LARGE_413,     SIZE_1,     ONCE);
        optionOccurenceConstraints.put(UNSUPPORTED_CONTENT_FORMAT_415,   MAX_AGE,    ONCE);
        
        //Server errors (5.x)
        optionOccurenceConstraints.put(INTERNAL_SERVER_ERROR_500,    MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(NOT_IMPLEMENTED_501,          MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(BAD_GATEWAY_502,              MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(GATEWAY_TIMEOUT_504,          MAX_AGE,   ONCE);
        optionOccurenceConstraints.put(PROXYING_NOT_SUPPORTED_505,   MAX_AGE,   ONCE);
    }

    private static Supplier linkedHashSetSupplier = new Supplier<LinkedHashSet<Option>>() {
        @Override
        public LinkedHashSet<Option> get() {
            return new LinkedHashSet<>();
        }
    };

    protected InetAddress recipientAddress;

    private int messageType;
    private int messageCode;
    private int messageID;
    private long token;
    protected SetMultimap<Integer, Option> options;
    private ChannelBuffer content;


    protected CoapMessage(int messageType, int messageCode){
        this();
        this.messageType = messageType;
        this.messageCode = messageCode;
    }


    protected CoapMessage(int messageCode){
        this();
        this.messageCode = messageCode;
    }


    private CoapMessage(){
        this.options = Multimaps.newSetMultimap(new TreeMap<Integer, Collection<Option>>(), linkedHashSetSupplier);
        this.messageID = UNDEFINED;
        this.token = 0;
        this.content = ChannelBuffers.EMPTY_BUFFER;
    }

    /**
     * Method to create an empty reset message which is strictly speaking neither a request nor a response
     * @param messageID the message ID of the reset message.
     *
     * @return an instance of {@link CoapMessage} with {@link MessageTypeNames#RST}
     */
    public static CoapMessage createEmptyReset(int messageID){
        CoapMessage emptyRST = new CoapMessage(){};
        emptyRST.messageType = RST;
        emptyRST.messageCode = EMPTY;
        emptyRST.messageID = messageID;
        return emptyRST;
    }

    /**
     * Method to create an empty acknowledgement message which is strictly speaking neither a request nor a response
     * @param messageID the message ID of the acknowledgement message.
     *
     * @return an instance of {@link CoapMessage} with {@link MessageTypeNames#ACK}
     */
    public static CoapMessage createEmptyAcknowledgement(int messageID){
        CoapMessage emptyACK = new CoapMessage(){};
        emptyACK.messageType = ACK;
        emptyACK.messageCode = EMPTY;
        emptyACK.messageID = messageID;
        return emptyACK;
    }

    protected void addStringOption(int optionNumber, String value) throws UnknownOptionException,
            InvalidOptionException {

        this.checkOptionPermission(optionNumber);
        if(!(Option.getOptionType(optionNumber) == OptionType.STRING))
            throw new InvalidOptionException(optionNumber, "Option number {} is no string-option.");

        //Add new option to option list
        StringOption option = new StringOption(optionNumber, value);
        options.put(optionNumber, option);

        log.debug("Added option (number: {}, value: {})", optionNumber, option.getDecodedValue());
    }


    protected void addUintOption(int optionNumber, long value) throws UnknownOptionException, InvalidOptionException {

        this.checkOptionPermission(optionNumber);
        if(!(Option.getOptionType(optionNumber) == OptionType.UINT))
            throw new InvalidOptionException(optionNumber, "Option number {} is no uint-option.");

        //Add new option to option list
        UintOption option = new UintOption(optionNumber, new BigInteger(1, Longs.toByteArray(value)).toByteArray());
        options.put(optionNumber, option);

        log.debug("Added option (number: {}, value: {})", optionNumber, option.getDecodedValue());
    }

    protected void addOpaqueOption(int optionNumber, byte[] value) throws InvalidOptionException, UnknownOptionException {

        this.checkOptionPermission(optionNumber);

        if(!(Option.getOptionType(optionNumber) == OptionType.OPAQUE))
            throw new InvalidOptionException(optionNumber, "Option number {} is no opaque option.");

        //Add new option to option list
        OpaqueOption option = new OpaqueOption(optionNumber, value);
        options.put(optionNumber, option);

        log.debug("Added option (number: {}, value: {})", optionNumber,
                new BigInteger(1, option.getDecodedValue()).toString(16));
    }

    protected void addEmptyOption(int optionNumber) throws InvalidOptionException, UnknownOptionException {

        this.checkOptionPermission(optionNumber);
        if(!(Option.getOptionType(optionNumber) == OptionType.EMPTY))
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
    public int getVersion() {
        return VERSION;
    }

    /**
     * Sets the message ID for this message. However, there is no need to set the message ID manually. It is set (or
     * overwritten) automatically by the nCoAP framework.
     *
     * @param messageID the message ID for the message
     */

    public void setMessageID(int messageID) throws InvalidMessageException {

        if(messageID < 0 || messageID > 65535)
            throw new InvalidMessageException("Message ID " + messageID + " is either negative or greater than 65535");

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
    public void setToken(long token) throws InvalidMessageException{
        BigInteger tmpToken = new BigInteger(1, Longs.toByteArray(token));
        if(tmpToken.toByteArray().length > 8)
            throw new InvalidMessageException("Token is too long (" + tmpToken.toString(16) + ").");

        this.token = token;
    }

    /**
     * Returns the value of the token option or an empty byte array b with <messageCode>(b.length == 0) == true</messageCode>.
     * @return the value of the messages token option
     */
    public byte[] getToken() {
        if(token == 0)
            return new byte[0];
        else
            return new BigInteger(1, Longs.toByteArray(token)).toByteArray();
    }

    /**
     * Returns the number representing the format of the content or {@link CoapMessage#UNDEFINED} if no such option is
     * present in this {@link CoapMessage}. See {@link ContentFormat} for some constants for predefined numbers.
     *
     * @return the number representing the format of the content or <code>null</code> if no such option is present
     * in this {@link CoapMessage}.
     */
    public long getContentFormat(){
        if(options.containsKey(OptionName.CONTENT_FORMAT))
            return ((UintOption) options.get(OptionName.CONTENT_FORMAT).iterator().next()).getDecodedValue();

        return CoapMessage.UNDEFINED;
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
            this.options.removeAll(OptionName.MAX_AGE);
            this.addUintOption(OptionName.MAX_AGE, maxAge);
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
        if(options.containsKey(OptionName.MAX_AGE))
            return ((UintOption) options.get(OptionName.MAX_AGE).iterator().next()).getDecodedValue();
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

        if(MessageCodeNames.allowsContent(this.messageCode)){
            this.content = content;
        }

        throw new InvalidMessageException("Message Code " + this.messageCode + " does not allow content.");
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
            this.addUintOption(OptionName.CONTENT_FORMAT, contentFormat);
            setContent(content);
        }
        catch (InvalidOptionException | InvalidMessageException e) {
            this.content = ChannelBuffers.EMPTY_BUFFER;
            this.removeOptions(OptionName.CONTENT_FORMAT);
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
        return this.getVersion() == other.getVersion()
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
        result.append("CoAP Message: [Header: (V) " + getVersion() + ", (T) " + getMessageType() + ", (TKL) "
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
