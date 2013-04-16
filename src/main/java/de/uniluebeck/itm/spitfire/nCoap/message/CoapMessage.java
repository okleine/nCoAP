package de.uniluebeck.itm.spitfire.nCoap.message;

import com.google.common.net.InetAddresses;
import de.uniluebeck.itm.spitfire.nCoap.communication.blockwise.Blocksize;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.*;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.ByteArrayWrapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;

/**
 * @author Oliver Kleine
 */
public abstract class CoapMessage implements Cloneable {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());

    protected InetAddress rcptAddress;

    //protected Blocksize maxBlocksizeForRequest;
    //protected Blocksize maxBlocksizeForResponse;

    protected Header header;
    protected OptionList optionList;
    private ChannelBuffer payload;

    private CoapMessage(){
        this.optionList = new OptionList();
        this.payload = ChannelBuffers.buffer(0);
    }

    protected CoapMessage(Code code){
        this();
        this.header = new Header(code);

    }

    protected CoapMessage(MsgType msgType, Code code) {
        this();
        this.header = new Header(msgType, code);
    }

    protected CoapMessage(Header header, OptionList optionList, ChannelBuffer payload){
        this.header = header;
        this.optionList = optionList;
        this.payload = payload;
    }

    protected CoapMessage(CoapMessage coapMessage) throws InvalidHeaderException {
        this(new Header(coapMessage.getHeader()),
             new OptionList(coapMessage.getOptionList()),
             coapMessage.getPayload());
        this.rcptAddress = coapMessage.rcptAddress;
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
            return optionList.getOption(TOKEN)
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
     * {@link de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.OutgoingMessageReliabilityHandler}.
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
    public void setContentType(MediaType mediaType) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(CONTENT_TYPE);

        try{
            Option option = Option.createUintOption(CONTENT_TYPE, mediaType.number);
            optionList.addOption(header.getCode(), CONTENT_TYPE, option);
        }
        catch(InvalidOptionException e){
            optionList.removeAllOptions(CONTENT_TYPE);

            log.debug("Critical option (" + CONTENT_TYPE + ") could not be added.", e);

            throw e;
        }
        catch(ToManyOptionsException e){
            optionList.removeAllOptions(CONTENT_TYPE);

            log.debug("Critical option (" + CONTENT_TYPE + ") could not be added.", e);

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
        optionList.removeAllOptions(ETAG);
        try{
            for(byte[] etag : etags){
                Option option = Option.createOpaqueOption(ETAG, etag);
                optionList.addOption(header.getCode(), ETAG, option);
            }
            return true;
        }
        catch(InvalidOptionException e){
            log.debug("Elective option (" + MAX_AGE + ") could not be added.", e);

            optionList.removeAllOptions(ETAG);
            return false;
        }
        catch(ToManyOptionsException e){
            log.debug("Elective option (" + MAX_AGE + ") could not be added.", e);

            optionList.removeAllOptions(ETAG);
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
        optionList.removeAllOptions(TOKEN);
        try{
            Option option = Option.createOpaqueOption(TOKEN, token);
            optionList.addOption(header.getCode(), TOKEN, option);
        }
        catch (InvalidOptionException e) {
            optionList.removeAllOptions(TOKEN);
            log.debug("Critical option " + TOKEN + " could not be added.", e);

            throw e;
        }
        catch (ToManyOptionsException e) {
            optionList.removeAllOptions(TOKEN);
            log.debug("Critical option " + TOKEN + " could not be added.", e);

            throw e;
        }
    }

    public void setMaxBlocksizeForRequest(Blocksize blocksize) throws ToManyOptionsException, InvalidOptionException {
        setBlockOption(BLOCK_1, 0, true, blocksize);
    }

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
     * @param isLastBlock <code>true</code> if the requested/contained block is the last block, <code>false</code>
     *                    otherwise.
     * @param blocksize The blocksize
     * @throws InvalidOptionException
     * @throws ToManyOptionsException
     */
    public void setBlockOption(OptionName optionName, long blockNumber, boolean isLastBlock,
                               Blocksize blocksize) throws InvalidOptionException, ToManyOptionsException {

        optionList.removeAllOptions(optionName);

        if(optionName != BLOCK_1 && optionName != BLOCK_2){
            String msg = "Option " + optionName + " is not a block option and thus not set.";
            InvalidOptionException e = new InvalidOptionException(optionName.number, msg);
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
        optionList.addOption(getCode(), optionName, option);
    }

    public Blocksize getMaxBlocksizeForRequest(){
        try {
            return getBlocksize(BLOCK_1);
        } catch (InvalidOptionException e) {
            log.error("This should never happen!", e);
            return null;
        }
    }

    public Blocksize getMaxBlocksizeForResponse(){
        try {
            return getBlocksize(BLOCK_2);
        } catch (InvalidOptionException e) {
            log.error("This should never happen!", e);
            return null;
        }
    }

    private Blocksize getBlocksize(OptionName optionName) throws InvalidOptionException {

        if(optionName != BLOCK_1 && optionName != BLOCK_2){
            String msg = "Option " + optionName + " is not a block option and as such does not contain a blocksize.";
            InvalidOptionException e = new InvalidOptionException(optionName.number, msg);
            log.error(msg, e);
            throw e;
        }

        List<Option> tmp = optionList.getOption(optionName);

        if(tmp.size() > 0){
            long exponent = ((UintOption) optionList.getOption(optionName).get(0)).getDecodedValue() & 7;

            Blocksize result = Blocksize.getByExponent(exponent);

            if(result != null){
                return result;
            }
            else{
                String msg = "SZX field with value " + exponent + " is not valid for Option " + optionName + ".";
                InvalidOptionException e = new InvalidOptionException(optionName.number, msg);
                log.error(msg, e);
                throw e;
            }
        }
        return null;
    }

    public boolean isLastBlock(OptionName optionName) throws InvalidOptionException {

            if(optionName != BLOCK_1 && optionName != BLOCK_2){
                String msg = "Option " + optionName +
                        " is not a block option and as such does not contain 'isLastBlock' field.";
                InvalidOptionException e = new InvalidOptionException(optionName.number, msg);
                log.error(msg, e);
                throw e;
            }

            long value = ((UintOption) optionList.getOption(optionName).get(0)).getDecodedValue() >> 3 & 1;
            return value == 0;

    }

    public long getBlockNumber(OptionName optionName) throws InvalidOptionException {

        if(optionName != BLOCK_1 && optionName != BLOCK_2){
            String msg = "Option " + optionName +
                    " is not a block option and as such does not contain 'blocknumber' field.";
            InvalidOptionException e = new InvalidOptionException(optionName.number, msg);
            log.error(msg, e);
            throw e;
        }

        try{
            UintOption option = (UintOption) getOption(optionName).get(0);
            log.debug("Option " + option.toString() + ", value: " + option.getDecodedValue());

            return option.getDecodedValue() >>> 4;
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
        if(header.getCode().allowsPayload()){
            payload = buf;
            return payload.readableBytes();
        }
        String msg = "Message Type " + header.getMsgType() + " does not allow payload.";
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
     * Returns the messages header
     * @return the messages header
     */
    public Header getHeader(){
        return header;
    }

    public boolean equals(Object obj){
        if(!(obj instanceof CoapMessage)){
            return false;
        }

        CoapMessage msg = (CoapMessage) obj;
        return this.getHeader().equals(msg.getHeader())
            && optionList.equals(msg.getOptionList())
            && payload.equals(msg.getPayload());
    }

    @Override
    public String toString(){
        return this.getClass().getName() + ":" +
                " MsgID " + getMessageID() +
                ", MygType " + getMessageType() +
                ", Code " + getCode() +
                ", Token " + new ByteArrayWrapper(getToken()).toHexString() +
                ", Block_2 " + getMaxBlocksizeForResponse();
    }

}
