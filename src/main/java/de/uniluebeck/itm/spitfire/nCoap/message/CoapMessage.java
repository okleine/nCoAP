package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.*;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import sun.net.util.IPAddressUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Oliver Kleine
 */
public abstract class CoapMessage {

    private static Logger log = Logger.getLogger(CoapMessage.class.getName());

    protected InetAddress rcptAddress;

    protected Header header;
    protected OptionList optionList;
    private ChannelBuffer payload;

    protected CoapMessage(Code code){
        header = new Header(code);
        optionList = new OptionList();
    }

    protected CoapMessage(MsgType msgType, Code code) {

        header = new Header(msgType, code);

        if(log.isDebugEnabled()){
            log.debug("[Message] Created new message instance " + "(MsgType: " + msgType + ", Code: " + code + ")");
        }

        //create option list
        optionList = new OptionList();
    }

    protected CoapMessage(Header header, OptionList optionList, ChannelBuffer payload){
        this.header = header;
        this.optionList = optionList;
        this.payload = payload;
    }

    /**
     * Returns the CoAP protocol version used for this message
     * @return the CoAP protocol version used for this message
     */
    public int getVersion() {
        return header.getVersion();
    }

    /**
     * Returns the message ID
     * @return the message ID
     */
    public int getMessageID() {
        return header.getMsgID();
    }

    public MsgType getMessageType() {
        return header.getMsgType();
    }

    /**
     * Returns the number of {@link de.uniluebeck.itm.spitfire.nCoap.message.options.Option} instances contained in the option list. Note that only options with
     * non-default values are contained in the option list.
     * @return the number of options contained in the messages option list
     */
    public int getOptionCount(){
        return optionList.getOptionCount();
    }

    /**
     * Returns the message Code (method code for requests, status code for responses or empty)
     * @return the message code
     */
    public Code getCode() {
        return header.getCode();
    }

    /**
     * Returns the value of the token option or an empty byte array b with <code>(b.length == 0) == true</code>.
     * @return the value of the messages token option
     */
    public byte[] getToken() {
        try{
            return optionList.getOption(OptionRegistry.OptionName.TOKEN)
                         .get(0)
                         .getValue();
        }
        catch(IndexOutOfBoundsException e){
            return new byte[0];
        }
    }

    /**
     * Returns <code>true</code> is the message is a request (as indicated by the message code) or <code>false</code>
     * otherwise
     * @return  <code>true</code> if message is request, <code>false</code> otherwise
     */
    public boolean isRequest() {
        return header.getCode().isRequest();
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
     * {@link de.uniluebeck.itm.spitfire.nCoap.communication.reliability.OutgoingMessageReliabilityHandler}.
     *
     * @param messageId the message ID for the message
     * @throws InvalidHeaderException if the message ID to be set is Invalid
     */
    public void setMessageID(int messageId) throws InvalidHeaderException {
        this.getHeader().setMsgID(messageId);
    }

    /**
     * Adds the option representing the content type to the option list. This causes an eventually already contained
     * content type option to be removed from the list even in case of an exception.
     *
     * @param mediaType The media type of the content
     * @throws de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException if there is a content type option already contained in the option list.
     * @throws de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     */
    public void setContentType(OptionRegistry.MediaType mediaType) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.CONTENT_TYPE);

        try{
            Option option = Option.createUintOption(OptionRegistry.OptionName.CONTENT_TYPE, mediaType.number);
            optionList.addOption(header.getCode(), OptionRegistry.OptionName.CONTENT_TYPE, option);
        }
        catch(InvalidOptionException e){
            optionList.removeAllOptions(OptionRegistry.OptionName.CONTENT_TYPE);

            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.CONTENT_TYPE + ") could not be added.", e);
            }

            throw e;
        }
        catch(ToManyOptionsException e){
            optionList.removeAllOptions(OptionRegistry.OptionName.CONTENT_TYPE);

            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option (" + OptionRegistry.OptionName.CONTENT_TYPE + ") could not be added.", e);
            }

            throw e;
        }
    }

    /**
     * Adds an appropriate number of proxy URI options to the list. This causes eventually already contained
     * ETAG options to be removed from the list even in case of an exception.
     *
     * @param etags The set of ETAGs to be added as options
     * @throws InvalidOptionException if at least one of the options to be added is invalid
     * @throws ToManyOptionsException if adding all ETAG options would exceed the maximum number of options per
     * message
     * @return <code>true</code> if ETAGs were succesfully set, <code>false</code> if ETAG option is not
     * meaningful with the message code and thus silently ignored
     */
    public boolean setETAG(byte[]... etags) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.ETAG);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(OptionRegistry.OptionName.ETAG, etag);
                optionList.addOption(header.getCode(), OptionRegistry.OptionName.ETAG, option);
            }
            return true;
        }
        catch(InvalidOptionException e){
            if(log.isDebugEnabled()){
                log.debug("[Message] Elective option (" + OptionRegistry.OptionName.MAX_AGE + ") could not be added.", e);
            }
            optionList.removeAllOptions(OptionRegistry.OptionName.ETAG);
            return false;
        }
        catch(ToManyOptionsException e){
            if(log.isDebugEnabled()){
                log.debug("[Message] Elective option (" + OptionRegistry.OptionName.MAX_AGE + ") could not be added.", e);
            }
            optionList.removeAllOptions(OptionRegistry.OptionName.ETAG);
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
        optionList.removeAllOptions(OptionRegistry.OptionName.TOKEN);
        try{
            Option option = Option.createOpaqueOption(OptionRegistry.OptionName.TOKEN, token);
            optionList.addOption(header.getCode(), OptionRegistry.OptionName.TOKEN, option);
        }
        catch (InvalidOptionException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.TOKEN);
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option " + OptionRegistry.OptionName.TOKEN + " could not be added.", e);
            }
            throw e;
        }
        catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.TOKEN);
            if(log.isDebugEnabled()){
                log.debug("[Message] Critical option " + OptionRegistry.OptionName.TOKEN + " could not be added.", e);
            }
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
     * Returns the set of {@link Option} instances of the given {@link de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName} contained in the messages
     * {@link OptionList} or an eventual default value if there is no matching option contained. The set is empty
     * if there is neither an Option instance contained in the OptionList or a default value for the OptionName.
     *
     * @param optionName The name of the option to be looked up.
     * @return The set of Option instances for the message matching the given OptionName
     */
    public List<Option> getOption(OptionRegistry.OptionName optionName){
        try{

            List<Option> result = optionList.getOption(optionName);

            if(!result.isEmpty()){
               return result;
            }

            //Default values to be assumed when explicitly defined options are missing
            switch(optionName){
                case URI_HOST:
                    result = new ArrayList<Option>(1);
                    String targetIP = rcptAddress.getHostAddress();
                    if(IPAddressUtil.isIPv6LiteralAddress(targetIP)){
                        targetIP = "[" + targetIP + "]";
                    }
                    result.add(Option.createStringOption(OptionRegistry.OptionName.URI_HOST, targetIP));
                    break;
                case URI_PORT:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createUintOption(OptionRegistry.OptionName.URI_PORT, OptionRegistry.COAP_PORT_DEFAULT));
                    break;
                case MAX_AGE:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createUintOption(OptionRegistry.OptionName.MAX_AGE, OptionRegistry.MAX_AGE_DEFAULT));
                    break;
                case TOKEN:
                    result = new ArrayList<Option>(1);
                    result.add(Option.createOpaqueOption(OptionRegistry.OptionName.TOKEN, new byte[0]));
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


}
