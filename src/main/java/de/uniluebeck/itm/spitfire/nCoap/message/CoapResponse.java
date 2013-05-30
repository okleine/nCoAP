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
import de.uniluebeck.itm.spitfire.nCoap.message.options.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * @author Oliver Kleine
 */
public class CoapResponse extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());

    private String servicePath;

    public CoapResponse(Code code){
        super(code);
    }

//    public CoapResponse(Code code, int messageID) throws InvalidHeaderException {
//        this(code);
//        this.setMessageID(messageID);
//    }

    public CoapResponse(CoapResponse coapResponse) throws InvalidHeaderException {
        super(coapResponse);
    }

    public CoapResponse(Header header, OptionList optionList, ChannelBuffer payload){
        super(header, optionList, payload);
    }

     /**
     * Adds the max age option for the messages content. This is to indicate the maximum time a response may be
     * cached until it is considered not fresh. This causes an eventually already contained
     * max age option to be removed from the list even in case of an exception.
     *
     * @param maxAge The maximum allowed time to cache the response in seconds
     * @throws de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException if the given max age is out of a valid range
     * @throws de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException if adding this option would exceed the maximum number of allowed options per
     * message
     * @return <code>true</code> if content type was succesfully set, <code>false</code> if max age option is not
     * meaningful with the message code and thus silently ignored or if max age is equal to
     * OptionRegistry.MAX_AGE_DEFAULT
     */
    public boolean setMaxAge(long maxAge) throws InvalidOptionException, ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.MAX_AGE);

        if(maxAge == OptionRegistry.MAX_AGE_DEFAULT){
            return false;
        }

        try{
            Option option = Option.createUintOption(OptionRegistry.OptionName.MAX_AGE, maxAge);
            optionList.addOption(header.getCode(), OptionRegistry.OptionName.MAX_AGE, option);
            return true;
        }
        catch(InvalidOptionException e){
            log.debug("Elective option (" + OptionRegistry.OptionName.MAX_AGE + ") could not be added.", e);
            optionList.removeAllOptions(OptionRegistry.OptionName.MAX_AGE);
            return false;
        }
        catch(ToManyOptionsException e){
            log.debug("Elective option (" + OptionRegistry.OptionName.MAX_AGE + ") could not be added.", e);
            optionList.removeAllOptions(OptionRegistry.OptionName.MAX_AGE);
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
        optionList.removeLocationURI();
        try{
            Collection<Option> locationUriOptions = Option.createLocationUriOptions(locationURI);
            for(Option option : locationUriOptions){
                OptionRegistry.OptionName optionName = OptionRegistry.getOptionName(option.getOptionNumber());
                optionList.addOption(header.getCode(), optionName, option);
            }
        }
        catch (ToManyOptionsException e) {
            optionList.removeLocationURI();
            log.debug("Critical option for location URI could not be added.", e);
            throw e;
        }
        catch (InvalidOptionException e) {
            optionList.removeLocationURI();
            log.debug("Critical option for location URI could not be added.", e);
            throw e;
        }
    }

    /**
     * Returns the URI reconstructed from the location URI related options contained in the message
     * @return the URI reconstructed from the location URI related options contained in the message or null if there
     * are no location URI related options
     * @throws java.net.URISyntaxException if the URI to be reconstructed from options is invalid
     */
    public URI getLocationURI() throws URISyntaxException {
        Collection<Option> options = optionList.getOption(OptionRegistry.OptionName.LOCATION_PATH);
        if(options.isEmpty()){
            return null;
        }
        String result = "";
        for(Option option : options){
            result += ("/" + ((StringOption)option).getDecodedValue());
        }

        options = optionList.getOption((OptionRegistry.OptionName.LOCATION_QUERY));
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
     * Set the observe option. This causes eventually already contained observe options to be removed from
     * the list even in case of an exception.
     *
     * @param sequenceNumber the sequence number for the observe option
     *
     * @throws ToManyOptionsException if adding an observe options would exceed the maximum number of
     * options per message.
     */
    public void setObserveOptionResponse(long sequenceNumber) throws ToManyOptionsException {
        optionList.removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
        try{
            Option option = Option.createUintOption(OptionRegistry.OptionName.OBSERVE_RESPONSE, sequenceNumber);
            optionList.addOption(header.getCode(), OptionRegistry.OptionName.OBSERVE_RESPONSE, option);
        } catch (InvalidOptionException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
            log.error("This should never happen!", e);
        } catch (ToManyOptionsException e) {
            optionList.removeAllOptions(OptionRegistry.OptionName.OBSERVE_RESPONSE);
            log.debug("Critical option (" + OptionRegistry.OptionName.OBSERVE_RESPONSE + ") could not be added.", e);
            throw e;
        }
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }
}
