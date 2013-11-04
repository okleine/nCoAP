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

import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;

/**
 * @author Oliver Kleine
 */
public class CoapResponse extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());

    private String servicePath;

    /**
     * This is the constructor supposed to be used to create {@link CoapResponse} instances. The {@link MessageType} is
     * automatically set by the nCoAP framework.
     *
     * @param messageCode The {@link MessageCode} of the response
     */
    public CoapResponse(MessageCode messageCode){
        super(messageCode);
    }

//    /**
//     * This is the constructor basically supposed to be used internally, in particular with the decoding process of
//     * incoming {@link CoapResponse}s. In other cases it is recommended to use {@link #CoapResponse(MessageCode)} and
//     * set options and payload by invoking the appropriate methods and let the nCoAP framework do the rest.
//     *
//     * @param header the {@link Header} of the {@link CoapResponse}
//     * @param optionList  the {@link OptionList} of the {@link CoapResponse}
//     * @param payload the {@link ChannelBuffer} containing the payload of the {@link CoapResponse}
//     */
//    public CoapResponse(Header header, OptionList optionList, ChannelBuffer payload){
//        super(header, optionList, payload);
//    }

     /**
     * Adds the max age option for the messages content. This is to indicate the maximum time a response may be
     * cached until it is considered not fresh. This causes an eventually already contained
     * max age option to be removed from the list even in case of an exception.
     *
     * @param maxAge The maximum allowed time to cache the response in seconds
     * @throws InvalidOptionException if the given max age is out of a valid range
     * @throws ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     * @return <code>true</code> if content type was succesfully set, <code>false</code> if max age option is not
     * meaningful with the message code and thus silently ignored or if max age is equal to
     * OptionRegistry.ENCODED_MAX_AGE_DEFAULT
     */
    public boolean setMaxAge(long maxAge) throws InvalidOptionException, ToManyOptionsException {
        options.removeAllOptions(MAX_AGE);

        if(maxAge == OptionRegistry.MAX_AGE_DEFAULT){
            return false;
        }

        try{
            Option option = Option.createUintOption(MAX_AGE, maxAge);
            options.addOption(header.getMessageCode(), MAX_AGE, option);
            return true;
        }
        catch(InvalidOptionException e){
            log.debug("Elective option (" + MAX_AGE + ") could not be added.", e);
            options.removeAllOptions(MAX_AGE);
            return false;
        }
        catch(ToManyOptionsException e){
            log.debug("Elective option (" + MAX_AGE + ") could not be added.", e);
            options.removeAllOptions(MAX_AGE);
            return false;
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
        options.removeLocationURI();
        try{
            Collection<Option> locationUriOptions = Option.createLocationUriOptions(locationURI);
            for(Option option : locationUriOptions){
                OptionName optionName = OptionName.getByNumber(option.getOptionNumber());
                options.addOption(header.getMessageCode(), optionName, option);
            }
        }
        catch (ToManyOptionsException e) {
            options.removeLocationURI();
            log.debug("Critical option for location URI could not be added.", e);
            throw e;
        }
        catch (InvalidOptionException e) {
            options.removeLocationURI();
            log.debug("Critical option for location URI could not be added.", e);
            throw e;
        }
    }

    /**
     * Returns the URI reconstructed from the location URI related options contained in the message
     * @return the URI reconstructed from the location URI related options contained in the message or null if there
     * are no location URI related options
     *
     * @throws {@link java.net.URISyntaxException} if the URI to be reconstructed from options is invalid
     */
    public URI getLocationURI() throws URISyntaxException {
        Collection<Option> options = this.options.getOption(LOCATION_PATH);
        if(options.isEmpty()){
            return null;
        }
        String result = "";
        for(Option option : options){
            result += ("/" + option.getDecodedValue());
        }

        options = this.options.getOption((LOCATION_QUERY));
        if(!options.isEmpty()){
            result += "?";
            for(Option option : options){
                result += ("&" + ((StringOption)option).getDecodedValue());
            }
            result = result.substring(0, result.length() - 1);
        }

        if(result.equals("")){
            return null;
        }

        return new URI(result);
    }

    /**
     * Set the observe option. This causes eventually already contained observe options to be removed from
     * the list even in case of an exception.
     *
     * @param sequenceNumber the sequence number for the observe option
     *
     * @throws ToManyOptionsException if adding an observe options would exceed the maximum number of
     * options per message.
     */
    public void setObserveOptionValue(long sequenceNumber) throws ToManyOptionsException {
        options.removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
        try{
            Option option = Option.createUintOption(OptionRegistry.OptionName.OBSERVE_RESPONSE, sequenceNumber);
            options.addOption(header.getMessageCode(), OptionRegistry.OptionName.OBSERVE_RESPONSE, option);
        } catch (InvalidOptionException e) {
            options.removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
            log.error("This should never happen!", e);
        } catch (ToManyOptionsException e) {
            options.removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
            log.debug("Critical option (" + OptionRegistry.OptionName.OBSERVE_RESPONSE + ") could not be added.", e);
            throw e;
        }
    }

    /**
     * Returns <code>true</code> if the {@link OptionName#OBSERVE_RESPONSE} option is set and <code>false</code>
     * otherwise
     * @return <code>true</code> if the {@link OptionName#OBSERVE_RESPONSE} option is set and <code>false</code>
     * otherwise
     */
    public boolean isUpdateNotification(){
        return !(this.getOption(OptionRegistry.OptionName.OBSERVE_RESPONSE).isEmpty());
    }

    /**
     * Returns the value of the {@link OptionName#OBSERVE_RESPONSE} option
     * @return the value of the {@link OptionName#OBSERVE_RESPONSE} option
     * @throws NullPointerException if this response does not contain an {@link OptionName#OBSERVE_RESPONSE} option
     */
    public long getObserveOptionValue() throws NullPointerException{
       return (Long) this.getOption(OptionRegistry.OptionName.OBSERVE_RESPONSE).get(0).getDecodedValue();
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public long getMaxAge(){
        return ((UintOption) this.getOption(OptionRegistry.OptionName.MAX_AGE).get(0)).getDecodedValue();
    }
}
