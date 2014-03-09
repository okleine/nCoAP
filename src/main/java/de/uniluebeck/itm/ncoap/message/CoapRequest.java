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

import java.net.*;
import java.util.*;

/**
 * @author Oliver Kleine
 */
public class CoapRequest extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapRequest.class.getName());

    /**
     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     *
     * @param messageType  A {@link MessageType.Name}
     * @param messageCode A {@link MessageCode}
     * @param targetUri the recipients URI
     *
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws URISyntaxException if the URI is not appropriate for a CoAP message.
     * @throws InvalidHeaderException if the given messageCode is not suitable for a request
     * @throws UnknownHostException if the host given in the targetUri could not be resolved to an IP address
     */
    public CoapRequest(MessageType.Name messageType, MessageCode.Name messageCode, URI targetUri) throws
            InvalidOptionException, URISyntaxException, UnknownHostException, InvalidHeaderException {

        this(messageType.getNumber(), messageCode.getNumber(), targetUri, false);
    }

    public CoapRequest(MessageType.Name messageType, MessageCode.Name messageCode, URI targetUri, boolean useProxy)
            throws InvalidOptionException, InvalidHeaderException, URISyntaxException {

        this(messageType.getNumber(), messageCode.getNumber(), targetUri, useProxy);
    }


    /**
     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     *
     * @param messageType  A {@link MessageType}
     * @param messageCode A {@link MessageCode}
     * @param targetUri the recipients URI
     *
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws URISyntaxException if the URI is not appropriate for a CoAP message.
     * @throws InvalidHeaderException if the given messageCode is not suitable for a request
     * @throws UnknownHostException if the host given in the targetUri could not be resolved to an IP address
     */
    public CoapRequest(int messageType, int messageCode, URI targetUri) throws
            InvalidOptionException, URISyntaxException, InvalidHeaderException {

        this(messageType, messageCode, targetUri, false);
    }

    /**
     * Creates a {@link CoapRequest} instance with initial CoAP header and options according to the given parameters.
     *
     * @param messageType the number representing the message type for the {@link CoapRequest}.
     * @param messageCode the number representing the message code for the {@link CoapRequest}.
     * @param targetUri the {@link URI} representing the webservice this {@link CoapRequest} is to be sent to.
     * @param useProxy <code>true</code> if the {@link CoapRequest} is to be sent via forward-proxy or
     *                 <code>false</code> if the {@link CoapRequest} is to be sent directly to the addressed webservice
     *                 host.
     *                 <br>
     *                 <b>Note:</b>the value of this parameter only affects the way, the given target URI is
     *                 represented in the created {@link CoapRequest}, i.e. as {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#PROXY_URI} if
     *                 <code>useProxy</code> is set to <code>true</code> or as {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#URI_HOST},
     *                 {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#URI_PORT}, {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#URI_PATH}, and {@link de.uniluebeck.itm.ncoap.message.options.OptionValue.Name#URI_QUERY} if
     *                 <code>useProxy</code> is set to <code>false</code.
     *
     * @throws InvalidOptionException if an error occurred while setting the target URI related options
     * @throws InvalidHeaderException if the given message type or message code are not suitable for requests
     * @throws URISyntaxException if the given target URI is not suitable for any reason
     */
    public CoapRequest(int messageType, int messageCode, URI targetUri, boolean useProxy)
            throws InvalidOptionException, InvalidHeaderException, URISyntaxException {

        super(messageType, messageCode);

        if(messageType > MessageType.Name.NON.getNumber() || messageType < MessageType.Name.CON.getNumber())
            throw new InvalidHeaderException("Message type for requests must either be CON (0) or NON (-1)");


        if(!(MessageCode.isRequest(messageCode)))
            throw new InvalidHeaderException("Message code no." + messageCode + " is no request code.");


        if(useProxy)
            setProxyURIOption(targetUri);
        else
            setTargetUriOptions(targetUri);

        log.debug("New request created: {}.", this);
    }

    /**
     * This constructor is only intended for internal use. Please use one of the other constructors to avoid
     * unexpected behaviour.
     *
     * @param messageType
     * @param messageCode
     * @throws InvalidHeaderException
     */
    public CoapRequest(int messageType, int messageCode) throws InvalidHeaderException {
        super(messageType, messageCode);

        if(!MessageCode.isRequest(messageCode))
            throw new InvalidHeaderException("MessageCode " + messageCode + " is not for requests.");
    }


    private void setProxyURIOption(URI targetUri) throws InvalidOptionException {
        try{
            this.addStringOption(OptionValue.Name.PROXY_URI, targetUri.toString());
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }


    private void setTargetUriOptions(URI targetUri) throws URISyntaxException, InvalidOptionException {
        targetUri = targetUri.normalize();

        //URI must be absolute and thus contain a scheme part (must be one of "coap" or "coaps")
        String scheme = targetUri.getScheme();
        if(scheme == null)
            throw new URISyntaxException(targetUri.toString(), "Scheme of target URI must not be null");

        scheme = scheme.toLowerCase(Locale.ENGLISH);
        if(!(scheme.equals("coap") || scheme.equals("coaps")))
            throw new URISyntaxException(targetUri.toString(),
                    "URI scheme must be either \"coap\" or \"coaps\" but is " + scheme);

        //Target URI must not have fragment part
        if(targetUri.getFragment() != null)
            throw new URISyntaxException(targetUri.toString(), "Target URI must not have a fragment part.");

        //Create target URI options
        try{
            if(!(OptionValue.isDefaultValue(OptionValue.Name.URI_HOST, targetUri.getHost().getBytes(CoapMessage.CHARSET))))
                addUriHostOption(targetUri.getHost());

            if(targetUri.getPort() != -1 && targetUri.getPort() != OptionValue.URI_PORT_DEFAULT)
                addUriPortOption(targetUri.getPort());

            addUriPathOptions(targetUri.getRawPath());
            addUriQueryOptions(targetUri.getRawQuery());
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }


    private void addUriQueryOptions(String uriQuery) throws UnknownOptionException, InvalidOptionException {
        if(uriQuery != null){
            for(String queryComponent : uriQuery.split("&")){
                this.addStringOption(OptionValue.Name.URI_QUERY, queryComponent);
                log.debug("Added URI query option for {}", queryComponent);
            }
        }
    }

    
    private void addUriPathOptions(String uriPath) throws UnknownOptionException, InvalidOptionException {
        if(uriPath != null){
            //Path must not start with "/" to be further processed
            if(uriPath.startsWith("/"))
                uriPath = uriPath.substring(1);

            for(String pathComponent : uriPath.split("/")){
                this.addStringOption(OptionValue.Name.URI_PATH, pathComponent);
                log.debug("Added URI path option for {}", pathComponent);
            }
        }
    }


    private void addUriPortOption(int uriPort) throws UnknownOptionException, InvalidOptionException {
        if(uriPort > 0 && uriPort != OptionValue.URI_PORT_DEFAULT)
            this.addUintOption(OptionValue.Name.URI_PORT, uriPort);
    }


    private void addUriHostOption(String uriHost) throws UnknownOptionException, InvalidOptionException {
        addStringOption(OptionValue.Name.URI_HOST, uriHost);
    }


    /**
     * Sets the If-Match options according to the given {@link Collection<byte[]>} containing ETAGs. If there were any
     * If-Match options present in this {@link CoapRequest} prior to the invocation of this method, these options are
     * removed.
     *
     * @param etags the ETAGs to be set as values for the If-Match options
     *
     * @throws InvalidOptionException if at least one of the given <code>byte[]</code> to be set as values for If-Match
     * options is invalid.
     */
    public void setIfMatch(byte[]... etags) throws InvalidOptionException {

        this.removeOptions(OptionValue.Name.IF_MATCH);

        try{
            for(byte[] etag : etags)
                this.addOpaqueOption(OptionValue.Name.IF_MATCH, etag);
        }
        catch (InvalidOptionException e) {
            this.removeOptions(OptionValue.Name.IF_MATCH);
            throw e;
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    /**
     * Returns a {@link Set<byte[]>} containing the values of the If-Match options. If no such option is present in
     * this {@link CoapRequest} the returned set is empty.
     *
     * @return a {@link Set<byte[]>} containing the values of the If-Match options. If no such option is present in
     * this {@link CoapRequest} the returned set is empty.
     */
    public Set<byte[]> getIfMatch(){

        Set<OptionValue> ifMatchOptionValues = options.get(OptionValue.Name.IF_MATCH);
        Set<byte[]> result = new HashSet<byte[]>(ifMatchOptionValues.size());

        if(ifMatchOptionValues.size() > 0){
            Iterator<OptionValue> iterator = ifMatchOptionValues.iterator();
            while(iterator.hasNext())
                result.add(((OpaqueOptionValue) iterator.next()).getDecodedValue());
        }

        return result;
    }

    /**
     * Returns the value of the URI host option or a literal representation of the recipients IP address if the URI
     * host option is not present in this {@link CoapRequest}.
     *
     * @return the value of the URI host option or <code>null</code> if the URI
     * host option is not present in this {@link CoapRequest}.
     */
    public String getUriHost(){

        if(options.containsKey(OptionValue.Name.URI_HOST))
            return ((StringOptionValue) options.get(OptionValue.Name.URI_HOST).iterator().next()).getDecodedValue();

        return null;
    }



    /**
     * Sets the ETAG options of this {@link CoapRequest}. If there are any ETAG options present in this
     * {@link CoapRequest} prior to the invocation of this method, those options are removed.
     *
     * @param etags the values for the ETAG options to be set
     *
     * @throws InvalidOptionException if at least one of the given ETAGs is not suitable to be the value of an ETAG
     * option.
     */
    public void setEtags(byte[]... etags) throws InvalidOptionException {

        this.removeOptions(OptionValue.Name.ETAG);

        try{
            for(byte[] etag : etags)
                this.addOpaqueOption(OptionValue.Name.ETAG, etag);
        }
        catch(InvalidOptionException e){
            this.removeOptions(OptionValue.Name.ETAG);
            throw e;
        }
        catch(UnknownOptionException e){
            log.error("This should never happen.", e);
        }
    }

    /**
     * Returns a {@link Set<byte[]>} containing the values of the ETAG options that are present in this
     * {@link CoapRequest}. If there is no such option, then the returned set is empty.
     *
     * @return  a {@link Set<byte[]>} containing the values of the ETAG options that are present in this
     * {@link CoapRequest}. If there is no such option, then the returned set is empty.
     */
    public Set<byte[]> getEtags(){
        Set<byte[]> result = new HashSet<byte[]>();

        if(options.containsKey(OptionValue.Name.ETAG)){
            Iterator<OptionValue> iterator = options.get(OptionValue.Name.ETAG).iterator();
            while(iterator.hasNext())
                result.add(((OpaqueOptionValue) iterator.next()).getDecodedValue());
        }

        return result;
    }

    /**
     * Sets the If-Non-Match option in this {@link CoapRequest}.
     *
     * @throws InvalidOptionException if the If-Non-Match option has no meaning with the {@link MessageCode} of this
     * {{@link CoapRequest}}.
     */
    public void setIfNonMatch(boolean ifNonMatch) throws InvalidOptionException {

        try{
            if(ifNonMatch){
                if(!options.containsKey(OptionValue.Name.IF_NONE_MATCH)){
                    this.addEmptyOption(OptionValue.Name.IF_NONE_MATCH);
                }
            }
            else{
                this.removeOptions(OptionValue.Name.IF_NONE_MATCH);
            }
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    /**
     * Returns <code>true</code> if the If-Non-Match option is present or <code>false</code> if there is
     * no such option present in this {@link CoapRequest}.
     *
      * @return <code>true</code> if the If-Non-Match option is present or <code>false</code> if there is
     * no such option present in this {@link CoapRequest}.
     */
    public boolean isIfNonMatchSet(){
        return options.containsKey(OptionValue.Name.IF_NONE_MATCH);
    }

    public void setObserve(boolean observe){
        try{
            if(observe){
                if(!options.containsKey(OptionValue.Name.OBSERVE)){
                    this.addOption(OptionValue.Name.OBSERVE, new UintOptionValue(OptionValue.Name.OBSERVE, 0));
                }
            }
            else{
                this.removeOptions(OptionValue.Name.OBSERVE);
            }
        }
        catch (InvalidOptionException e) {
            log.error("This should never happen.", e);
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    public boolean isObserveSet(){
        return options.containsKey(OptionValue.Name.OBSERVE);
    }

    /**
     * Returns the value of the URI port option or {@link de.uniluebeck.itm.ncoap.message.options.OptionValue#URI_PORT_DEFAULT} if the URI port option is not
     * present in this {@link CoapRequest}.
     *
     * @return the value of the URI port option or {@link de.uniluebeck.itm.ncoap.message.options.OptionValue#URI_PORT_DEFAULT} if the URI port option is not
     * presentin this {@link CoapRequest}.
     */
    public long getUriPort(){
        if(options.containsKey(OptionValue.Name.URI_PORT))
            return ((UintOptionValue) options.get(OptionValue.Name.URI_PORT).iterator().next()).getDecodedValue();

        return OptionValue.URI_PORT_DEFAULT;
    }


    /**
     * Returns the full path of the request URI reconstructed from the URI path options present in this
     * {@link CoapRequest}. If no such option is set, the returned value is "/".
     *
     * @return the full path of the request URI reconstructed from the URI path options present in this
     * {@link CoapRequest}.
     */
    public String getUriPath(){
        if(options.containsKey(OptionValue.Name.URI_PATH)){
            StringBuffer result = new StringBuffer();
            Iterator<OptionValue> iterator = options.get(OptionValue.Name.URI_PATH).iterator();
            while(iterator.hasNext())
                result.append("/" + ((StringOptionValue) iterator.next()).getDecodedValue());

            return result.toString();
        }

        return "/";
    }

    /**
     * Returns the full query of the request URI reconstructed from the URI query options present in this
     * {@link CoapRequest}.
     *
     * @return the full query of the request URI reconstructed from the URI query options present in this
     * {@link CoapRequest}.
     */
    public String getUriQuery(){
        if(options.containsKey(OptionValue.Name.URI_QUERY)){
            StringBuffer result = new StringBuffer();
            Iterator<OptionValue> iterator = options.get(OptionValue.Name.URI_QUERY).iterator();
            result.append(((StringOptionValue) iterator.next()).getDecodedValue());
            while(iterator.hasNext())
                result.append("&" + ((StringOptionValue) iterator.next()).getDecodedValue());

            return result.toString();
        }

        return "";
    }

    public String getUriQueryParameterValue(String parameter){
        if(!parameter.endsWith("="))
            parameter += "=";

        for(OptionValue optionValue : options.get(OptionValue.Name.URI_QUERY)){
            String value = ((StringOptionValue) optionValue).getDecodedValue();

            if(value.startsWith(parameter))
                return value.substring(parameter.length());
        }

        return null;
    }

    /**
     * Sets the content formats the client is willing to accept. See {@link ContentFormat} for a predefined set of such
     * numbers.
     *
     * @param contentFormatNumbers a {@link Collection} containing the content formats the client is willing to accept.
     *
     * @throws InvalidOptionException if one of the given numbers is not capable to represent a content format
     */
    public void setAccept(long... contentFormatNumbers) throws InvalidOptionException {
        options.removeAll(OptionValue.Name.ACCEPT);
        try{
            for(long contentFormatNumber : contentFormatNumbers)
                this.addUintOption(OptionValue.Name.ACCEPT, contentFormatNumber);
        }
        catch (UnknownOptionException e) {
            log.error("This should never happen.", e);
        }
    }

    /**
     * Returns a {@link Set<Long>} containing the numbers representing the accepted content formats. See
     * {@link ContentFormat} for a predefined set of such numbers. If no such option is present in this
     * {@link CoapRequest}, then the returned set is empty.
     *
     * @return a {@link Set<Long>} containing the numbers representing the accepted content formats.
     */
    public Set<Long> getAcceptedContentFormats(){
        Set<Long> result = new HashSet<Long>();

        for(OptionValue optionValue : options.get(OptionValue.Name.ACCEPT))
            result.add(((UintOptionValue) optionValue).getDecodedValue());

        return result;
    }

//    public long[] getAcceptedContentFormatsAsArray(){
//        Set<Long> tmp = getAcceptedContentFormats();
//        long[] result = new long[tmp.size()];
//
//        int i = 0;
//        Iterator<Long> iterator = tmp.iterator();
//        while(iterator.hasNext()){
//            result[i] = iterator.next();
//            i++;
//        }
//
//        return result;
//    }

    /**
     * Returns the value of the Proxy URI option if such an option is present in this {@link CoapRequest}. If no such
     * option is present but a Proxy Scheme option, then the returned {@link URI} is reconstructed from the
     * Proxy Scheme option and the URI host, URI port, URI path and URI query options.
     *
     * If both options, Proxy URI and Proxy Scheme are not present in this {@link CoapRequest} this
     * method returns <code>null</code>.
     *
     * @return the URI of the requested resource if this {@link CoapRequest} was (or is supposed to be) sent via a
     * proxy or <code>null</code> if the request was (or is supposed to be) sent directly.
     *
     * @throws URISyntaxException if the value of the proxy URI option or the reconstruction from Proxy Scheme,
     * URI host, URI port, URI path, and URI query options is invalid.
     */
    public URI getProxyURI() throws URISyntaxException {
        if(options.containsKey(OptionValue.Name.PROXY_URI))
            return new URI(((StringOptionValue) options.get(OptionValue.Name.PROXY_URI).iterator().next()).getDecodedValue());

        if(options.get(OptionValue.Name.PROXY_SCHEME).size() == 1){
            String scheme = ((StringOptionValue) options.get(OptionValue.Name.PROXY_SCHEME).iterator().next()).getDecodedValue();
            String uriHost = getUriHost();
            int uriPort = ((UintOptionValue) options.get(OptionValue.Name.URI_PORT).iterator().next()).getDecodedValue().intValue();
            String uriPath = getUriPath();
            String uriQuery = getUriQuery();

            return new URI(scheme, null, uriHost, uriPort == OptionValue.URI_PORT_DEFAULT ? -1 : uriPort, uriPath, uriQuery,
                    null);
        }

        return null;
    }

//    public InetAddress getFinalDestinationHostAddress() {
//        return recipientAddress;
//    }

//
//    /**
//     * Set the observe option. This causes eventually already contained observe options to be removed from
//     * the list even in case of an exception.
//     *
//     * @throws {@link ToManyOptionsException} if adding an observe options would exceed the maximum number of
//     * options per message.
//     */
//    public void setObserveOptionRequest() throws ToManyOptionsException {
//        options.removeAllOptions(OBSERVE_REQUEST);
//        try{
//            Option option = Option.createEmptyOption(OBSERVE_REQUEST);
//            options.addOption(header.getMessageCode(), OBSERVE_REQUEST, option);
//        } catch (InvalidOptionException e) {
//            options.removeAllOptions(OBSERVE_REQUEST);
//            log.error("This should never happen!", e);
//        } catch (ToManyOptionsException e) {
//            options.removeAllOptions(OBSERVE_REQUEST);
//            log.debug("Critical option (" + OBSERVE_REQUEST + ") could not be added.", e);
//            throw e;
//        }
//    }
//
//    public boolean isObservationRequest(){
//        if(this.getOption(OBSERVE_REQUEST).isEmpty())
//            return false;
//        else
//            return true;
//    }



}
