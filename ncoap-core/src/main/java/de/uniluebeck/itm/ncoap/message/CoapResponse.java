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

import de.uniluebeck.itm.ncoap.communication.codec.OptionCodecException;
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

    private static final String NO_ERRROR_CODE = "Code no. %s is no error code!";

    /**
     * Creates a new instance of {@link CoapResponse}.
     *
     * <b>Note:</b> The given {@link MessageType.Name} (one of {@link MessageType.Name#CON} or
     * {@link MessageType.Name#NON}) may be changed
     * by the framework before it is sent to the other CoAP endpoints. Such a change might e.g. happen if this
     * {@link CoapResponse} was created with {@link MessageType.Name#CON} to answer a {@link CoapRequest} with
     * {@link MessageType.Name#CON} and the framework did not yet send an empty {@link CoapMessage} with
     * {@link MessageType.Name#ACK}. Then the framework will ensure the {@link MessageType} of this
     * {@link CoapResponse} to be set to {@link MessageType.Name#ACK} to make it a piggy-backed response.
     *
     *
     * @param messageType the {@link MessageType.Name} (one of {@link MessageType.Name#CON} or
     *                    {@link MessageType.Name#NON}). <b>Note:</b> the {@link MessageType} might be changed by the
     *                    framework (see above).
     *
     * @param messageCode the {@link MessageCode.Name} for this {@link CoapResponse}
     *
     * @throws java.lang.IllegalArgumentException
     */
    public CoapResponse(MessageType.Name messageType, MessageCode.Name messageCode) throws IllegalArgumentException {
        this(messageType.getNumber(), messageCode.getNumber());
    }


    /**
     * Creates a new instance of {@link CoapResponse}.
     *
     * <b>Note:</b> The given message type (either 0 or 1) may be changed by the framework before it is sent to the
     * other CoAP endpoints. Such a change might e.g. happen if this {@link CoapResponse} was created with 0 (for
     * {@link MessageType.Name#CON} or 1 (for {@link MessageType.Name#NON}) and is supposed to answer a
     * {@link CoapRequest} with {@link MessageType.Name#CON} and the framework did not yet send an empty
     * {@link CoapMessage} with {@link MessageType.Name#ACK}. Then the framework will ensure the {@link MessageType}
     * of this {@link CoapResponse} to be set to {@link MessageType.Name#ACK} to make it a piggy-backed response.
     *
     * @param messageType the number representing the {@link MessageType} (<b>Note:</b> the {@link MessageType} might
     *                    be changed by the framework (see above)).
     *
     * @param messageCode the {@link MessageCode.Name} for this {@link CoapResponse}
     *
     * @throws java.lang.IllegalArgumentException
     */
    public CoapResponse(int messageType, int messageCode) throws IllegalArgumentException {
        super(messageType, messageCode);

        if(!MessageCode.isResponse(messageCode))
            throw new IllegalArgumentException("Message code no." + messageCode + " is no response code.");
    }


    /**
     * Creates a new instance of {@link CoapResponse} with {@link MessageCode.Name#INTERNAL_SERVER_ERROR_500} and
     * the stacktrace of the given {@link Throwable} as payload (this is particularly useful for debugging). Basically,
     * this can be considered a shortcut to create error responses.
     *
     * <b>Note:</b> The given {@link MessageType.Name} (one of {@link MessageType.Name#CON} or
     * {@link MessageType.Name#NON}) may be changed by the framework before it is sent to the other CoAP endpoints.
     * Such a change might e.g. happen if this {@link CoapResponse} was created with {@link MessageType.Name#CON} to
     * answer a {@link CoapRequest} with {@link MessageType.Name#CON} and the framework did not yet send an empty
     * {@link CoapMessage} with {@link MessageType.Name#ACK}. Then the framework will ensure the {@link MessageType} of
     * this {@link CoapResponse} to be set to {@link MessageType.Name#ACK} to make it a piggy-backed response.
     *
     * @param messageType the {@link MessageType.Name} (one of {@link MessageType.Name#CON} or
     *                    {@link MessageType.Name#NON}). <b>Note:</b> the {@link MessageType} might be changed by the
     *                    framework (see above).
     * @param messageCode the {@link MessageCode.Name} for this {@link CoapResponse}
     *
     * @return a new instance of {@link CoapResponse} with the {@link Throwable#getMessage} as content (payload).
     *
     * @throws java.lang.IllegalArgumentException if the given message code does not refer to an error
     */
    public static CoapResponse createErrorResponse(MessageType.Name messageType, MessageCode.Name messageCode,
                                                   String content) throws IllegalArgumentException{

        if(!MessageCode.isErrorMessage(messageCode.getNumber()))
            throw new IllegalArgumentException(String.format(NO_ERRROR_CODE, messageCode.toString()));

        CoapResponse errorResponse = new CoapResponse(messageType, messageCode);
        errorResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        return errorResponse;
    }


    /**
     * Sets the {@link OptionValue.Name#ETAG} of this {@link CoapResponse}.
     *
     * @param etag the byte array that is supposed to represent the ETAG of the content returned by
     *             {@link #getContent()}.
     *
     * @throws IllegalArgumentException if the given byte array is invalid to be considered an ETAG
     */
    public void setEtag(byte[] etag) throws IllegalArgumentException {
        this.addOpaqueOption(OptionValue.Name.ETAG, etag);
    }

    /**
     * Returns the byte array representing the ETAG of the content returned by {@link #getContent()}
     *
     * @return the byte array representing the ETAG of the content returned by {@link #getContent()}
     */
    public byte[] getEtag(){
        if(options.containsKey(OptionValue.Name.ETAG))
            return ((OpaqueOptionValue) options.get(OptionValue.Name.ETAG).iterator().next()).getDecodedValue();
        else
            return null;
    }

    /**
     * Adds an {@link OptionValue.Name#OBSERVE} option with the given sequence number. The value of the option will
     * correspond to the 3 least significant bytes of a (big endian) byte representation of the given sequence number,
     * i.e. a given sequence number of <code>2^24 + 1</code> leads to a value of <code>1</code>.
     *
     * <b>Note:</b> This method will override a possibly previously contained {@link OptionValue.Name#OBSERVE} option.
     *
     * @param sequenceNumber the sequence number for the {@link OptionValue.Name#OBSERVE} to be set.
     */
    public void setObserveOption(long sequenceNumber){
        try {
            this.removeOptions(OptionValue.Name.OBSERVE);
            sequenceNumber = sequenceNumber & 0xFFFFFF;
            this.addUintOption(OptionValue.Name.OBSERVE, sequenceNumber);
        }
        catch (IllegalArgumentException e){
            this.removeOptions(OptionValue.Name.OBSERVE);
            log.error("This should never happen.", e);
        }
    }

    /**
     * Returns the decoded value of {@link OptionValue.Name#OBSERVE} if such an option is contained in this
     * {@link CoapResponse} or <code>null</code> if there is no such option.
     *
     * @return the decoded value of {@link OptionValue.Name#OBSERVE} if such an option is contained in this
     * {@link CoapResponse} or <code>null</code> if there is no such option.
     */
    public Long getObservationSequenceNumber(){
        if(!options.containsKey(OptionValue.Name.OBSERVE))
            return null;
        else
            return (Long) options.get(OptionValue.Name.OBSERVE).iterator().next().getDecodedValue();
    }


    /**
     * Returns <code>true</code> if this {@link CoapResponse} is an update notification and <code>false</code>
     * otherwise. A {@link CoapResponse} is considered an update notification if the  invocation of
     * {@link #getObservationSequenceNumber()} returns a value other than <code>null</code>.
     *
     * @return <code>true</code> if this {@link CoapResponse} is an update notification and <code>false</code>
     * otherwise.
     */
    public boolean isUpdateNotification(){
        return this.getObservationSequenceNumber() != null;
    }


    /**
     * Adds all necessary location URI related options to the list. This causes eventually already contained
     * location URI related options to be removed from the list even in case of an exception.
     *
     * @param locationURI The location URI of the newly created resource. The parts scheme, host, and port are
     * ignored anyway and thus may not be included in the URI object
     *
     * @throws de.uniluebeck.itm.ncoap.communication.codec.OptionCodecException if at least one of the options to be added is not valid
     */
    public void setLocationURI(URI locationURI) throws OptionCodecException {

        options.removeAll(OptionValue.Name.LOCATION_PATH);
        options.removeAll(OptionValue.Name.LOCATION_QUERY);

        String locationPath = locationURI.getRawPath();
        String locationQuery = locationURI.getRawQuery();

        try{
            if(locationPath != null){
                //Path must not start with "/" to be further processed
                if(locationPath.startsWith("/"))
                    locationPath = locationPath.substring(1);

                for(String pathComponent : locationPath.split("/"))
                    this.addStringOption(OptionValue.Name.LOCATION_PATH, pathComponent);
            }

            if(locationQuery != null){
                for(String queryComponent : locationQuery.split("&"))
                    this.addStringOption(OptionValue.Name.LOCATION_QUERY, queryComponent);
            }
        }
        catch(IllegalArgumentException e){
            options.removeAll(OptionValue.Name.LOCATION_PATH);
            options.removeAll(OptionValue.Name.LOCATION_QUERY);
            throw e;
        }
    }


    /**
     * Returns the URI reconstructed from the location URI related options contained in the message
     * @return the URI reconstructed from the location URI related options contained in the message or null if there
     * are no location URI related options
     *
     * @throws URISyntaxException if the URI to be reconstructed from options is invalid
     */
    public URI getLocationURI() throws URISyntaxException {

        //Reconstruct path
        StringBuilder locationPath = new StringBuilder();

        if(options.containsKey(OptionValue.Name.LOCATION_PATH)){
            for (OptionValue optionValue : options.get(OptionValue.Name.LOCATION_PATH))
                locationPath.append("/").append(((StringOptionValue) optionValue).getDecodedValue());
        }

       //Reconstruct query
        StringBuilder locationQuery = new StringBuilder();

        if(options.containsKey(OptionValue.Name.LOCATION_QUERY)){
            Iterator<OptionValue> queryComponentIterator = options.get(OptionValue.Name.LOCATION_QUERY).iterator();
            locationQuery.append(((StringOptionValue) queryComponentIterator.next()).getDecodedValue());
            while(queryComponentIterator.hasNext())
                locationQuery.append("&")
                             .append(((StringOptionValue) queryComponentIterator.next()).getDecodedValue());
        }

        if(locationPath.length() == 0 && locationQuery.length() == 0)
            return null;

        return new URI(null, null, null, (int) UintOptionValue.UNDEFINED, locationPath.toString(),
                locationQuery.toString(), null);
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
//        } catch (OptionCodecException e) {
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
