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

import com.google.common.collect.TreeMultimap;
import com.google.common.net.InetAddresses;
import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.options.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static de.uniluebeck.itm.ncoap.message.MessageType.*;
import static de.uniluebeck.itm.ncoap.message.MessageCode.*;

/**
 * This class is the base class for inheriting subtypes, i.e. requests and responses. This abstract class provides the
 * cut-set in terms of functionality of {@link CoapRequest} and {@link CoapResponse}.
 *
 * @author Oliver Kleine
 */
public abstract class CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());

    public static final int VERSION = 1;
    public static final String CHARSET = "UTF-8";

    protected InetAddress rcptAddress;

    private MessageType messageType;
    private MessageCode messageCode;
    private int messageID;
    private long token = 0;
    private TreeMultimap<Integer, Option> options;
    private ChannelBuffer payload;


//    protected CoapMessage(MessageType messageType, MessageCode messageCode, int messageID,
//                          TreeMultimap<Integer, Option> options, ChannelBuffer payload){
//
//    }


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

    /**
     * Returns the CoAP protocol version used for this message
     * @return the CoAP protocol version used for this message
     */
    public int getVersion() {
        return VERSION;
    }

    /**
     * Returns the message ID
     * @return the message ID
     */
    public int getMessageID() {
        return messageID;
    }

    /**
     * This is a shortcut for {@link #getHeader().getMessageType()}
     * @return the {@link MessageType} of this message
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * This is a shortcut for {@link #getHeader().getCode()}
     * @return the {@link MessageCode} of this message
     */
    public MessageCode getMessageCode() {
        return messageCode;
    }

    /**
     * Returns the value of the token option or an empty byte array b with <messageCode>(b.length == 0) == true</messageCode>.
     * @return the value of the messages token option
     */
    public long getToken() {
        return token;
    }

    /**
     * Sets the recipients IP address. Usually there is no need to set this value manually. It is only used to
     * define the default value of the URI host option and is invoked automatically during construction if necessary.
     *
     * @param rcptAddress The recipients IP address
     */
    public void setRcptAdress(InetAddress rcptAddress){
        this.rcptAddress = rcptAddress;
    }

    /**
     * Sets the message ID for this message. Normally, there is no need to set the message ID manually. It is set or
     * overwritten automatically by the
     * {@link de.uniluebeck.itm.ncoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler}.
     *
     * @param messageId the message ID for the message
     * @throws InvalidHeaderException if the message ID to be set is Invalid
     */
    public void setMessageID(int messageId) throws InvalidHeaderException {
        this.getHeader().setMsgID(messageId);
    }

    /**
     * Returns the {@link MediaType} contained as {@link OptionName#CONTENT_TYPE} option in this {@link CoapMessage} instance.
     * @return the {@link MediaType} contained as {@link OptionName#CONTENT_TYPE} option or null if the option is not set.
     */
    public MediaType getContentType(){
        if(!options.getOption(CONTENT_TYPE).isEmpty()){
            return MediaType.getByNumber((Long) options.getOption(CONTENT_TYPE).get(0).getDecodedValue());
        }
        return null;
    }

    /**
     * Adds the option representing the content type to the option list. This causes an eventually already contained
     * content type option to be removed from the list even in case of an exception.
     *
     * @param mediaType The media type of the content
     * @throws de.uniluebeck.itm.ncoap.message.options.InvalidOptionException if there is a content type option already contained in the option list.
     * @throws de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     */
    public void setContentType(MediaType mediaType) throws InvalidOptionException, ToManyOptionsException {
        options.removeAllOptions(CONTENT_TYPE);

        try{
            Option option = Option.createUintOption(CONTENT_TYPE, mediaType.number);
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
     * Adds an appropriate number of {@link OptionName#ETAG} options to the list. This causes eventually already
     * contained ETAG options to be removed from the list even in case of an exception.
     *
     * @param etags The set of ETAGs to be added as options
     * @throws InvalidOptionException if at least one of the options to be added is invalid
     * @throws ToManyOptionsException if adding all ETAG options would exceed the maximum number of options per
     * message
     * @return <messageCode>true</messageCode> if ETAGs were succesfully set, <messageCode>false</messageCode> if ETAG option is not
     * meaningful with the message messageCode and thus silently ignored
     */
    public boolean setETAG(byte[]... etags) throws InvalidOptionException, ToManyOptionsException {
        options.removeAllOptions(ETAG);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(ETAG, etag);
                options.addOption(header.getMessageCode(), ETAG, option);
            }
            return true;
        }
        catch(InvalidOptionException e){
            log.debug("Elective option (" + MAX_AGE + ") could not be added.", e);

            options.removeAllOptions(ETAG);
            return false;
        }
        catch(ToManyOptionsException e){
            log.debug("Elective option (" + MAX_AGE + ") could not be added.", e);

            options.removeAllOptions(ETAG);
            return false;
        }
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
        options.removeAllOptions(TOKEN);
        try{
            Option option = Option.createOpaqueOption(TOKEN, token);
            options.addOption(header.getMessageCode(), TOKEN, option);
        }
        catch (InvalidOptionException e) {
            options.removeAllOptions(TOKEN);
            log.debug("Critical option " + TOKEN + " could not be added.", e);

            throw e;
        }
        catch (ToManyOptionsException e) {
            options.removeAllOptions(TOKEN);
            log.debug("Critical option " + TOKEN + " could not be added.", e);

            throw e;
        }
    }

    /**
     * Sets the blocksize for requests, i.e. {@link OptionName#BLOCK_1}.
     *
     * @param blocksize the {@link Blocksize}
     *
     * @throws ToManyOptionsException
     * @throws InvalidOptionException
     */
    @Beta
    public void setMaxBlocksizeForRequest(Blocksize blocksize) throws ToManyOptionsException, InvalidOptionException {
        setBlockOption(BLOCK_1, 0, true, blocksize);
    }

    /**
     * Sets the blocksize for respones, i.e. {@link OptionName#BLOCK_2}.
     *
     * @param blocksize the {@link Blocksize}
     *
     * @throws ToManyOptionsException
     * @throws InvalidOptionException
     */
    @Beta
    public void setMaxBlocksizeForResponse(Blocksize blocksize) throws ToManyOptionsException, InvalidOptionException {
        setBlockOption(BLOCK_2, 0, true, blocksize);
    }

    /**
     * This method is to set a block option. This method is not intended to be used. Its purpose is internal.
     * If you want to activate one or both of the block options, please either use {@link this.setRequestBlocksize}
     * or (@link this.setResponseBlocksize}.
     *
     * @param optionName One of BLOCK_1 or BLOCK_2
     * @param blockNumber The number of the requested block
     * @param isLastBlock <messageCode>true</messageCode> if the requested/contained block is the last block, <messageCode>false</messageCode>
     *                    otherwise.
     * @param blocksize The blocksize
     * @throws InvalidOptionException
     * @throws ToManyOptionsException
     */
    @Beta
    public void setBlockOption(OptionName optionName, long blockNumber, boolean isLastBlock,
                               Blocksize blocksize) throws InvalidOptionException, ToManyOptionsException {

        options.removeAllOptions(optionName);

        if(optionName != BLOCK_1 && optionName != BLOCK_2){
            String msg = "Option " + optionName + " is not a block option and thus not set.";
            InvalidOptionException e = new InvalidOptionException(optionName.getNumber(), msg);
            log.error(msg, e);
            throw e;
        }

        long value = blockNumber;
        value = value << 1;
        value = value | (isLastBlock ? 0 : 1);
        value = value << 3;
        value = value | blocksize.szx;

        log.debug("Add block option " + optionName + " with value: " + value);

        Option option = Option.createUintOption(optionName, value);
        options.addOption(getMessageCode(), optionName, option);
    }

    /**
     * Returns the blocksize for requests, i.e. {@link OptionName#BLOCK_1}.
     * @return the blocksize for requests, i.e. {@link OptionName#BLOCK_1} or <messageCode>null</messageCode> if option is not set
     */
    @Beta
    public Blocksize getMaxBlocksizeForRequest(){
        try {
            return getBlocksize(BLOCK_1);
        } catch (InvalidOptionException e) {
            log.error("This should never happen!", e);
            return null;
        }
    }

    /**
     * Returns the blocksize for responses, i.e. {@link OptionName#BLOCK_2}.
     * @return the blocksize for responses, i.e. {@link OptionName#BLOCK_2} or <messageCode>null</messageCode> if option is not set
     */
    @Beta
    public Blocksize getMaxBlocksizeForResponse(){
        try {
            return getBlocksize(BLOCK_2);
        } catch (InvalidOptionException e) {
            log.error("This should never happen!", e);
            return null;
        }
    }

    @Beta
    private Blocksize getBlocksize(OptionName optionName) throws InvalidOptionException {

        if(optionName != BLOCK_1 && optionName != BLOCK_2){
            String msg = "Option " + optionName + " is not a block option and as such does not contain a blocksize.";
            InvalidOptionException e = new InvalidOptionException(optionName.getNumber(), msg);
            log.error(msg, e);
            throw e;
        }

        List<Option> tmp = options.getOption(optionName);

        if(tmp.size() > 0){
            long exponent = (Long) options.getOption(optionName).get(0).getDecodedValue() & 7;

            Blocksize result = Blocksize.getByExponent(exponent);

            if(result != null){
                return result;
            }
            else{
                String msg = "SZX field with value " + exponent + " is not valid for Option " + optionName + ".";
                InvalidOptionException e = new InvalidOptionException(optionName.getNumber(), msg);
                log.error(msg, e);
                throw e;
            }
        }
        return null;
    }

    /**
     * Returns whether this message contains the last block of a blockwise transfered message, i.e. if a message
     * with the full payload can be delivered to the application.
     *
     * @param optionName one of {@link OptionName#BLOCK_1} or {@link OptionName#BLOCK_2}
     *
     * @return <messageCode>true</messageCode> if this messages contains the last block, <messageCode>false</messageCode> otherwise
     *
     * @throws InvalidOptionException if the given {@link OptionName} is neither {@link OptionName#BLOCK_1} nor
     * {@link OptionName#BLOCK_2}
     */
    @Beta
    public boolean isLastBlock(OptionName optionName) throws InvalidOptionException {

            if(optionName != BLOCK_1 && optionName != BLOCK_2){
                String msg = "Option " + optionName +
                        " is not a block option and as such does not contain 'isLastBlock' field.";
                InvalidOptionException e = new InvalidOptionException(optionName.getNumber(), msg);
                log.error(msg, e);
                throw e;
            }

            long value = (Long) options.getOption(optionName).get(0).getDecodedValue() >> 3 & 1;
            return value == 0;

    }

    /**
     * Returns the position of this messages payload in the full payload.
     *
     * @param optionName one of {@link OptionName#BLOCK_1} or {@link OptionName#BLOCK_2}
     *
     * @return the position of this messages payload in the full payload.
     *
     * @throws InvalidOptionException if the given {@link OptionName} is neither {@link OptionName#BLOCK_1} nor
     * {@link OptionName#BLOCK_2}
     */
    @Beta
    public long getBlockNumber(OptionName optionName) throws InvalidOptionException {

        if(optionName != BLOCK_1 && optionName != BLOCK_2){
            String msg = "Option " + optionName +
                    " is not a block option and as such does not contain 'blocknumber' field.";
            InvalidOptionException e = new InvalidOptionException(optionName.getNumber(), msg);
            log.error(msg, e);
            throw e;
        }

        try{
            Option option = getOption(optionName).get(0);
            log.debug("Option " + option.toString() + ", value: " + option.getDecodedValue());

            return (Long) option.getDecodedValue() >>> 4;
        }
        catch (IndexOutOfBoundsException e){
            return 0;
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
        if(header.getMessageCode().allowsPayload()){
            payload = buf;
            return payload.readableBytes();
        }
        String msg = "Message Type " + header.getMessageType() + " does not allow payload.";
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
