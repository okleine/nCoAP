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

import de.uniluebeck.itm.ncoap.message.options.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;


/**
 * @author Oliver Kleine
 */
public class CoapResponse extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapMessage.class.getName());


    /**
     * This is the constructor supposed to be used to create {@link CoapResponse} instances. The {@link MessageType} is
     * automatically set by the nCoAP framework.
     *
     * @param messageCode The {@link MessageCode} of the response
     *
     * @throws
     */
    public CoapResponse(int messageCode) throws InvalidHeaderException {
        super(messageCode);

        if(!MessageCode.isResponse(messageCode))
            throw new InvalidHeaderException("Message code no." + messageCode + " is no response code.");
    }


    public CoapResponse(MessageCode.Name messageCode) throws InvalidHeaderException {
        this(messageCode.getNumber());
    }


    public void setEtag(byte[] etag) throws InvalidOptionException {
        try {
            this.addOpaqueOption(Option.Name.ETAG, etag);
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }


    public byte[] getEtag(){
        if(options.containsKey(Option.Name.ETAG))
            return ((OpaqueOption) options.get(Option.Name.ETAG).iterator().next()).getDecodedValue();
        else
            return null;
    }


    public void setObservationSequenceNumber(long sequenceNumber){
        sequenceNumber = sequenceNumber & 0xFFFFFF;
        try {
            this.addUintOption(Option.Name.OBSERVE, sequenceNumber);
        }
        catch (UnknownOptionException | InvalidOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    public long getObservationSequenceNumber(){
        if(!options.containsKey(Option.Name.OBSERVE))
            return -1;
        else
            return (Long) options.get(Option.Name.OBSERVE).iterator().next().getDecodedValue();
    }

    public boolean isUpdateNotification(){
        return this.getObservationSequenceNumber() != -1;
    }

    /**
     * Adds all necessary location URI related options to the list. This causes eventually already contained
     * location URI related options to be removed from the list even in case of an exception.
     *
     * @param locationURI The location URI of the newly created resource. The parts scheme, host, and port are
     * ignored anyway and thus may not be included in the URI object
     *
     * @throws de.uniluebeck.itm.ncoap.message.options.InvalidOptionException if at least one of the options to be added is not valid
     */
    public void setLocationURI(URI locationURI) throws InvalidOptionException {

        options.removeAll(Option.Name.LOCATION_PATH);
        options.removeAll(Option.Name.LOCATION_QUERY);

        String locationPath = locationURI.getRawPath();
        String locationQuery = locationURI.getRawQuery();

        try{
            if(locationPath != null){
                //Path must not start with "/" to be further processed
                if(locationPath.startsWith("/"))
                    locationPath = locationPath.substring(1);

                for(String pathComponent : locationPath.split("/"))
                    this.addStringOption(Option.Name.LOCATION_PATH, pathComponent);
            }

            if(locationQuery != null){
                for(String queryComponent : locationQuery.split("&"))
                    this.addStringOption(Option.Name.LOCATION_QUERY, queryComponent);
            }
        }
        catch(InvalidOptionException e){
            options.removeAll(Option.Name.LOCATION_PATH);
            options.removeAll(Option.Name.LOCATION_QUERY);
            throw e;
        }
        catch(UnknownOptionException e){
            log.error("This should never happen!", e);
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

        //Reconstruct path
        StringBuffer locationPath = new StringBuffer();

        if(options.containsKey(Option.Name.LOCATION_PATH)){
            Iterator<Option> pathComponentIterator = options.get(Option.Name.LOCATION_PATH).iterator();
            while(pathComponentIterator.hasNext())
                    locationPath.append("/" + ((StringOption) pathComponentIterator.next()).getDecodedValue());
        }

       //Reconstrut query
        StringBuffer locationQuery = new StringBuffer();

        if(options.containsKey(Option.Name.LOCATION_QUERY)){
            Iterator<Option> queryComponentIterator = options.get(Option.Name.LOCATION_QUERY).iterator();
            locationQuery.append(((StringOption) queryComponentIterator.next()).getDecodedValue());
            while(queryComponentIterator.hasNext())
                locationQuery.append("&" + ((StringOption) queryComponentIterator.next()).getDecodedValue());
        }

        if(locationPath.length() == 0 && locationQuery.length() == 0)
            return null;

        return new URI(null, null, null, -1, locationPath.toString(), locationQuery.toString(), null);
    }

//    /**
//     * Set the observe option. This causes eventually already contained observe options to be removed from
//     * the list even in case of an exception.
//     *
//     * @param sequenceNumber the sequence number for the observe option
//     *
//     * @throws ToManyOptionsException if adding an observe options would exceed the maximum number of
//     * options per message.
//     */
//    public void setObserveOptionValue(long sequenceNumber) throws ToManyOptionsException {
//        options.removeAllOptions(OptionRegistry.Option.Name.OBSERVE_RESPONSE);
//        try{
//            Option option = Option.createUintOption(OptionRegistry.Option.Name.OBSERVE_RESPONSE, sequenceNumber);
//            options.addOption(header.getMessageCode(), OptionRegistry.Option.Name.OBSERVE_RESPONSE, option);
//        } catch (InvalidOptionException e) {
//            options.removeAllOptions(OptionRegistry.Option.Name.OBSERVE_RESPONSE);
//            log.error("This should never happen!", e);
//        } catch (ToManyOptionsException e) {
//            options.removeAllOptions(OptionRegistry.Option.Name.OBSERVE_RESPONSE);
//            log.debug("Critical option (" + OptionRegistry.Option.Name.OBSERVE_RESPONSE + ") could not be added.", e);
//            throw e;
//        }
//    }

//    /**
//     * Returns <code>true</code> if the {@link Name#OBSERVE_RESPONSE} option is set and <code>false</code>
//     * otherwise
//     * @return <code>true</code> if the {@link Name#OBSERVE_RESPONSE} option is set and <code>false</code>
//     * otherwise
//     */
//    public boolean isUpdateNotification(){
//        return !(this.getOption(OptionRegistry.Option.Name.OBSERVE_RESPONSE).isEmpty());
//    }
//
//    /**
//     * Returns the value of the {@link Name#OBSERVE_RESPONSE} option
//     * @return the value of the {@link Name#OBSERVE_RESPONSE} option
//     * @throws NullPointerException if this response does not contain an {@link Name#OBSERVE_RESPONSE} option
//     */
//    public long getObserveOptionValue() throws NullPointerException{
//       return (Long) this.getOption(OptionRegistry.Option.Name.OBSERVE_RESPONSE).get(0).getDecodedValue();
//    }

//    public String getServicePath() {
//        return servicePath;
//    }
//
//    public void setServicePath(String servicePath) {
//        this.servicePath = servicePath;
//    }

}
