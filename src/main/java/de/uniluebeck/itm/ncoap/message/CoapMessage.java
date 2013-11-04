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
package de.uniluebeck.itm.ncoap.message;

import com.google.common.base.Supplier;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;
import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.header.InvalidHeaderException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.*;

import static de.uniluebeck.itm.ncoap.message.MessageCode.*;
import static de.uniluebeck.itm.ncoap.message.MessageType.ACK;
import static de.uniluebeck.itm.ncoap.message.MessageType.RST;
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

    private static final int ONCE       = 1;
    private static final int MULTIPLE   = 2;
    
    private static HashBasedTable<Integer, Integer, Integer> optionOccurenceConstraints = HashBasedTable.create();
    static{
        //Requests
        optionOccurenceConstraints.put(GET,      URI_HOST,           ONCE);
        optionOccurenceConstraints.put(GET,      URI_PORT,           ONCE);
        optionOccurenceConstraints.put(GET,      URI_PATH,           ONCE);
        optionOccurenceConstraints.put(GET,      URI_QUERY,          ONCE);
        optionOccurenceConstraints.put(GET,      ACCEPT,             MULTIPLE);
        optionOccurenceConstraints.put(GET,      ETAG,               MULTIPLE);
        optionOccurenceConstraints.put(POST,     URI_HOST,           ONCE);
        optionOccurenceConstraints.put(POST,     URI_PORT,           ONCE);
        optionOccurenceConstraints.put(POST,     URI_PATH,           ONCE);
        optionOccurenceConstraints.put(POST,     URI_QUERY,          ONCE);
        optionOccurenceConstraints.put(POST,     CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(PUT,      URI_HOST,           ONCE);
        optionOccurenceConstraints.put(PUT,      URI_PORT,           ONCE);
        optionOccurenceConstraints.put(PUT,      URI_PATH,           ONCE);
        optionOccurenceConstraints.put(PUT,      URI_QUERY,          ONCE);
        optionOccurenceConstraints.put(PUT,      CONTENT_FORMAT,     ONCE);
        optionOccurenceConstraints.put(PUT,      IF_MATCH,           ONCE);
        optionOccurenceConstraints.put(PUT,      IF_NONE_MATCH,      ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_HOST,           ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_PORT,           ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_PATH,           ONCE);
        optionOccurenceConstraints.put(DELETE,   URI_QUERY,          ONCE);

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

    protected InetAddress rcptAddress;

    private int messageType;
    private int messageCode;
    private int messageID;
    private long token = 0;
    protected SetMultimap<Integer, Option> options;
    private ChannelBuffer payload;

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
        options = Multimaps.newSetMultimap(new TreeMap<Integer, Collection<Option>>(), linkedHashSetSupplier);
    }

    /**
     * Method to create an empty reset message which is strictly speaking neither a request nor a response
     * @param messageID the message ID of the reset message.
     *
     * @return an instance of {@link CoapMessage} with {@link MessageType#RST}
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
     * @return an instance of {@link CoapMessage} with {@link MessageType#ACK}
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

        log.debug("Added option (number: {}, value: {})", optionNumber, option.getValue());
    }


    protected void addUintOption(int optionNumber, long value) throws UnknownOptionException, InvalidOptionException {

        this.checkOptionPermission(optionNumber);
        if(!(Option.getOptionType(optionNumber) == OptionType.UINT))
            throw new InvalidOptionException(optionNumber, "Option number {} is no uint-option.");

        //Add new option to option list
        UintOption option = new UintOption(optionNumber, new BigInteger(1, Longs.toByteArray(value)).toByteArray());
        options.put(optionNumber, option);

        log.debug("Added option (number: {}, value: {})", optionNumber, option.getValue());
    }

    protected void addOpaqueOption(int optionNumber, byte[] value) throws InvalidOptionException, UnknownOptionException {

        this.checkOptionPermission(optionNumber);

        if(!(Option.getOptionType(optionNumber) == OptionType.OPAQUE))
            throw new InvalidOptionException(optionNumber, "Option number {} is no opaque option.");

        //Add new option to option list
        OpaqueOption option = new OpaqueOption(optionNumber, value);
        options.put(optionNumber, option);

        log.debug("Added option (number: {}, value: {})", optionNumber,
                new BigInteger(1, option.getValue()).toString(16));
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
     * @throws InvalidHeaderException if the message ID to be set is invalid
     */

    public void setMessageID(int messageID) throws InvalidHeaderException {

        if(messageID < -1 || messageID > 65535)
            throw new InvalidHeaderException("Message ID must not be negative or greater than 65535");

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

    /**
     * This is a shortcut for {@link #getHeader().getMessageType()}
     * @return the {@link MessageType} of this message
     */
    public int getMessageType() {
        return messageType;
    }

    /**
     * This is a shortcut for {@link #getHeader().getCode()}
     * @return the {@link MessageCode} of this message
     */
    public int getMessageCode() {
        return messageCode;
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
    public void setToken(long token) throws InvalidOptionException, ToManyOptionsException {
        this.token = token;
    }

    /**
     * Returns the value of the token option or an empty byte array b with <messageCode>(b.length == 0) == true</messageCode>.
     * @return the value of the messages token option
     */
    public byte[] getToken() {
        return new BigInteger(1, Longs.toByteArray(token)).toByteArray();
    }

    /**
     * Returns the number representing the format of the payload or <code>null</code> if no such option is present
     * in this {@link CoapMessage}
     * @return
     */
    public Long getContentFormat(){
        if(options.containsKey(OptionName.CONTENT_FORMAT))
            return ((UintOption) options.get(OptionName.CONTENT_FORMAT).iterator().next()).getValue();

        return null;
    }

//    /**
//     * Sets the recipients IP address. Usually there is no need to set this value manually. It is only used to
//     * define the default value of the URI host option and is invoked automatically during construction if necessary.
//     *
//     * @param rcptAddress The recipients IP address
//     */
//    public void setRcptAdress(InetAddress rcptAddress){
//        this.rcptAddress = rcptAddress;
//    }



//    /**
//     * Returns the {@link ContentFormat} contained as {@link OptionName#CONTENT_TYPE} option in this {@link CoapMessage} instance.
//     * @return the {@link ContentFormat} contained as {@link OptionName#CONTENT_TYPE} option or null if the option is not set.
//     */
//    public ContentFormat getContentType(){
//        if(!options.getOption(OptionName.CONTENT_TYPE).isEmpty()){
//            return ContentFormat.getByNumber((Long) options.getOption(CONTENT_TYPE).get(0).getDecodedValue());
//        }
//        return null;
//    }

    public void



    /**
     * Adds the option representing the content type to the option list. This causes an eventually already contained
     * content type option to be removed from the list even in case of an exception.
     *
     * @param contentFormat The media type of the content
     * @throws InvalidOptionException if there is a content type option already contained in the option list.
     * @throws ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     */
    public void setContentType(ContentFormat contentFormat) throws InvalidOptionException, ToManyOptionsException {
        options.removeAllOptions(CONTENT_TYPE);

        try{
            Option option = Option.createUintOption(CONTENT_TYPE, contentFormat.number);
            options.addOption(header.getMessageCode(), CONTENT_TYPE, option);
        }
        catch(InvalidOptionException e){
            options.removeAllOptions(CONTENT_TYPE);

            log.debug("Critical option (" + CONTENT_TYPE + ") could not be added.", e);

            throw e;
        }
        catch(ToManyOptionsException e){
            options.removeAllOptions(CONTENT_TYPE);

            log.debug("Critical option (" + CONTENT_TYPE + ") could not be added.", e);

            throw e;
        }
    }

    /**
     * Adds the content to the message. If this {@link CoapMessage} contained any content prior to the invocation of
     * method, the previous content is removed.
     *
     * @param content ChannelBuffer containing the message content
     *
     * @throws InvalidMessageException if the messages code does not allow content
     *
     * @return the size of the content as number of bytes
     */
    public int setContent(ChannelBuffer content) throws InvalidMessageException {
        if(MessageCode.allowsContent(this.messageCode)){
            this.payload = content;
            return this.payload.readableBytes();
        }

        throw new InvalidMessageException("Message Code " + this.messageCode + " does not allow payload.");
    }

    /**
     * Adds the content to the message. If this {@link CoapMessage} contained any content prior to the invocation of
     * method, the previous content is removed.
     *
     * @param content ChannelBuffer containing the message content
     *
     * @throws InvalidMessageException if the messages code does not allow content
     *
     * @return the size of the content as number of bytes
     */
    public int setPayload(byte[] content) throws InvalidMessageException {
        return setContent(ChannelBuffers.wrappedBuffer(content));
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
    public OptionList getOptions(){
        return options;
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

            List<Option> result = options.getOption(optionName);

            if(!result.isEmpty()){
               return result;
            }

            //Default values to be assumed when explicitly defined options are missing
            switch(optionName){
                case URI_HOST:
                    result = new ArrayList<Option>(1);
                    String targetIP = rcptAddress.getHostAddress();
                    try{
                        if(InetAddresses.forString(targetIP) instanceof Inet6Address){
                           targetIP = "[" + targetIP + "]";
                        }
                     }
                    catch (IllegalArgumentException e){
                        log.debug("No IP address: " + targetIP, e);
                    }
                    result.add(Option.createStringOption(URI_HOST, targetIP));
                    break;
                case URI_PORT:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createUintOption(URI_PORT, OptionRegistry.COAP_PORT_DEFAULT));
                    break;
                case MAX_AGE:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createUintOption(MAX_AGE, OptionRegistry.MAX_AGE_DEFAULT));
                    break;
                case TOKEN:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createOpaqueOption(TOKEN, new byte[0]));
                    break;
                case BLOCK_1:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createUintOption(BLOCK_1, 0));
                    break;
                case BLOCK_2:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createUintOption(BLOCK_2, 0));
                    break;
            }

            return result;
        }
        catch(InvalidOptionException e){
            log.error("This should never happen.", e);
            return null;
        }
    }

    /**
     * Returns the messages {@link Header}
     * @return the messages {@link Header}
     */
    public Header getHeader(){
        return header;
    }

    //TODO: Improve hash messageCode
    @Override
    public int hashCode(){
        return toString().hashCode() + payload.hashCode();
    }

    /**
     * Returns <messageCode>true</messageCode> if and only if the given object is an instance of {@link CoapMessage} and if
     * the {@link Header}, the {@link OptionList} and the payload of both instances equal.
     *
     * @param object another object to compare this {@link CoapMessage} with
     *
     * @return <messageCode>true</messageCode> if and only if the given object is an instance of {@link CoapMessage} and if
     * the {@link Header}, the {@link OptionList} and the payload of both instances equal.
     */
    @Override
    public boolean equals(Object object){
        if(!(object instanceof CoapMessage)){
            return false;
        }

        CoapMessage msg = (CoapMessage) object;
        return this.getHeader().equals(msg.getHeader())
            && options.equals(msg.getOptions())
            && payload.equals(msg.getPayload());
    }

    @Override
    public String toString(){
        String result =  "CoAP message: " + getHeader() + " | " + getOptions() + " | ";

        long payloadLength = getPayload().readableBytes();
        if(payloadLength == 0)
            result +=  "no payload";
        else
            result += "[PAYLOAD] " + getPayload().toString(0, Math.min(getPayload().readableBytes(), 20),
                    Charset.forName("UTF-8")) +  "... ( " + payloadLength + " bytes)";

        return result;
    }


}
