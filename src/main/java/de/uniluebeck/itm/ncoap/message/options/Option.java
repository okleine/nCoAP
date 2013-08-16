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
package de.uniluebeck.itm.ncoap.message.options;

import com.google.common.net.InetAddresses;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
*
 * @author Oliver Kleine
 */
public abstract class Option{

    private static Logger log = LoggerFactory.getLogger(Option.class.getName());

    //public static final Charset charset = Charset.forName("UTF-8");
    public static final String CHARSET = "UTF-8";

    protected int optionNumber;
    protected byte[] value = new byte[0];

    protected Option(OptionName optionName){
        optionNumber = optionName.getNumber();
    }

//    protected Option(Option option) throws InvalidOptionException {
//        this(OptionName.getByNumber(option.getOptionNumber()));
//        this.value = option.getValue();
//    }

    /**
     * Creates all URI related options from the given URI
     * @param uri URI to extract the URI options from
     * @throws URISyntaxException if the URI is not absolute, has scheme which is not coap or coaps, or has a fragment
     * part
     * @throws InvalidOptionException if at least one of the target URI related options to be created does not
     * match the criteria defined for the option type
     * @return A collection containing the appropriate amount of target URI related options
     */
    public static Collection<Option> createTargetURIOptions(URI uri) throws URISyntaxException, InvalidOptionException {
        uri = uri.normalize();

        ArrayList<Option> result = new ArrayList<Option>();

        //URI must be absolute and thus contain a scheme part (must be one of "coap" or "coaps")
        String scheme = uri.getScheme();
        if(scheme == null){
            String msg = "URI must be absolute and " +
                    "scheme must be either \"coap\" or \"coaps\" but is " + scheme;
            throw new URISyntaxException(uri.toString(), msg);
        }
        else{
            scheme = scheme.toLowerCase();
            if(!(scheme.equals("coap") || scheme.equals("coaps"))){
                String msg = "URI scheme must be either \"coap\" or \"coaps\" but is " + scheme;
                throw new URISyntaxException(uri.toString(), msg);
            }
        }

        //Target URI must not have fragment part
        if(uri.getFragment() != null){
            String msg = "Target URI must not have a fragment part.";
            throw new URISyntaxException(uri.toString(), msg);
        }

        //Create URI-host option
        String host = uri.getHost();
        log.debug("Host: " + uri.getHost() + ", Path: " + uri.getPath() + ", Auth.: " + uri.getAuthority());

        //Do only add an URI host option if the host is no IP-Address
        log.debug("Target URI host: " + host);

        if(host.startsWith("[") && host.endsWith("]")){
            host = host.substring(1, host.length() - 1);
        }

        if(!InetAddresses.isInetAddress(host)){
            try {
                result.add(new StringOption(OptionName.URI_HOST, host.getBytes(Option.CHARSET)));
            } catch (UnsupportedEncodingException e) {
                log.debug("This should never happen:\n", e);
            }
            log.debug("URI-Host option added to result list.");
        }
        else{
            log.debug("URI-Host " + host + " is an IP literal and thus not added to the result list.");
        }

        //Create URI-port option
        int port = uri.getPort();

        //Check whether the port value is non-standard
        log.debug("Target URI-Port: " + port);

        if(port > 0 && port != OptionRegistry.COAP_PORT_DEFAULT){
            result.add(new UintOption(OptionName.URI_PORT, port));
            log.debug("Target URI-Port option added to result list.");
        }
        else{
            log.debug("URI-Port is either empty or default (= " + OptionRegistry.COAP_PORT_DEFAULT +
                        ") and thus not added as option.");
        }

        //Add URI-Path option(s)
        String path = uri.getRawPath();
        if(path != null){
            //Path must not start with "/" to be processed further
            if(path.startsWith("/")){
                path = path.substring(1);
            }

            //Each URI-path option to be added contains one fragment as payload. The fragments of the path
            //are the substrings of the full path seperated by "/"
            if(path.length() > 0){
                result.addAll(createSeparatedOptions(OptionName.URI_PATH, "/", path));
            }
        }

        //Add URI-Query option(s)
        String query = uri.getRawQuery();

        if(query != null){
            //Each URI-query option to be added contains one fragment as payload. The fragments of the query
            //are the substrings of the full query seperated by "&"
            result.addAll(createSeparatedOptions(OptionName.URI_QUERY, "&", query));
        }
        
        return result;
    }


    /**
     * This method creates one or more Proxy-URI options. More than one option is created if the
     * length of the encoded URI string is more than 270. All but the last created options payload has a length
     * of 270 bytes. Note that only absolute URIs are allowed to be added as Proxy URI.
     *
     * @param uri The URI to be added as Proxy URI option(s)
     * @return The amount of options added to the list
     * @throws URISyntaxException  if the URI to be added is not absolute
     * @throws InvalidOptionException if one of the proxyservicemanagement URI options to be created is not valid
     */
    public static Collection<Option> createProxyUriOptions(URI uri) throws InvalidOptionException, URISyntaxException {
        uri = uri.normalize();
        ArrayList<Option> proxyOptions = new ArrayList<Option>();

        if(!uri.isAbsolute()){
            String msg = "URI to be added as proxy URI (" + uri.toString() + ") is not absolute.";
            throw new URISyntaxException(uri.toString(), msg);
        }

        byte[] encodedUri = new byte[0];
        try {
            encodedUri = uri.toString().getBytes(StringOption.CHARSET);
        } catch (UnsupportedEncodingException e) {
            log.debug("This should never happen:\n", e);
        }
        log.debug("Length of encoded proxy URI: " + encodedUri.length + " bytes.");

        int startPos = 0;
        while(startPos < encodedUri.length){
            //All but the last option must contain a payload of 270 bytes
            int endPos = Math.min(startPos + OptionRegistry.getMaxLength(OptionName.PROXY_URI),
                                   encodedUri.length);
            proxyOptions.add(new StringOption(OptionName.PROXY_URI,
                    Arrays.copyOfRange(encodedUri, startPos, endPos)));
            startPos = endPos;
        }
        return proxyOptions;
    }

    /**
     * Location path and location query options are used to define the path of a newly created resource in a response
     * to a POST message. Each option in the returned Option[] contains one fragement of the full path, resp.
     * full query string. Fragments are
     * created by seperating the full string using a seperator ("/" for the path and "&" for the query).
     * E.g. The relative URI "path/example?value1=1&value2=2" results into 4 options: 2 location path options
     * (path and example) and 2 location query options (value1=1 and value2=2)
     *
     * @param uri URI to be added as location path and location query option(s)
     * @return A collection of options containing the appropriate amount of Location URI related options.
     * @throws InvalidOptionException if one of the location URI related options to be created is not valid
     */
    public static Collection<Option> createLocationUriOptions(URI uri) throws InvalidOptionException{
        ArrayList<Option> options = new ArrayList<Option>();

        //Add Location-Path option(s)
        String path = uri.getRawPath();
        if(path != null){
            //Path must not start with "/" to be processed further
            if(path.startsWith("/")){
                path = path.substring(1);
            }

            //Each URI-path option to be added contains one fragment as payload. The fragments of the path
            //are the substrings of the full path seperated by "/"
            options.addAll(createSeparatedOptions(OptionName.LOCATION_PATH, "/", path));
        }

        //Add Location-Query option(s)
        String query = uri.getRawQuery();

        if(query != null){
            //Each URI-query option to be added contains one fragment as payload. The fragments of the query
            //are the substrings of the full query seperated by "&"
            options.addAll(createSeparatedOptions(OptionName.LOCATION_QUERY, "&", query));
        }

        return options;
    }

    /**
     * Creates and returns a {@link StringOption} instance with given name and value
     *
     * @param optionName The name of the option
     * @param value A String representing the options value
     * @return appropriate StringOption instance
     * @throws InvalidOptionException if the given name is not a StringOptions name
     */
    public static StringOption createStringOption(OptionName optionName, String value) throws InvalidOptionException{
        //Check whether current number is appropriate for a StringOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.STRING){
            String msg = "Cannot create option " + optionName + " with string value.";
            throw new InvalidOptionException(optionName.getNumber(), msg);
        }
        return new StringOption(optionName, value);
    }

     /**
     * Creates and returns a {@link UintOption} instance with given name and value
     *                                                                                 *
     * @param optionName The name of the option
     * @param value A long representing the options value (which is of type unsigned int)
     * @return appropriate UintOption instance
     * @throws InvalidOptionException if the given name is not a UintOptions name
     */
    public static UintOption createUintOption(OptionName optionName, long value) throws InvalidOptionException{
        //Check whether current number is appropriate for a UintOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.UINT){
            String msg = "Cannot create option " + optionName + " with uint value.";
            throw new InvalidOptionException(optionName.getNumber(), msg);
        }
        return new UintOption(optionName, value);
    }

    /**
     * Creates and returns a {@link OpaqueOption} instance with given name and value
     *
     * @param optionName The name of the option
     * @param value A long representing the options value (which is of type unsigned int)
     * @return appropriate UintOption instance
     * @throws InvalidOptionException if the given name is not a OpaqueOptions name
     */
    public static OpaqueOption createOpaqueOption(OptionName optionName, byte[] value) throws InvalidOptionException{
        //Check whether current number is appropriate for a OpaqueOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.OPAQUE){
            String msg = "[Option] Cannot create option " + optionName + " with opaque value.";
            throw new InvalidOptionException(optionName.getNumber(), msg);
        }
        
        if(optionName == OptionName.TOKEN && value.length == 0){
            String msg = "[Option] Empty byte[] is the default value for option " + optionName + ". No option created.";
            throw new InvalidOptionException(optionName.getNumber(), msg);
        }

        return new OpaqueOption(optionName, value);
    }

    public static EmptyOption createEmptyOption(OptionName optionName) throws InvalidOptionException{
        //Check whether current number is appropriate for an EmptyOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.EMPTY){
            String msg = "[Option] Cannot create option " + optionName + " as instance of EmptyOption.";
            throw new InvalidOptionException(optionName.getNumber(), msg);
        }
        return new EmptyOption(optionName);
    }

    /**
     * Creates and returns option instance of the appropriate type according to the given name
     * @param optionName The name of the option
     * @param value A long representing the options value (which is of type unsigned int)
     * @return appropriate Option instance (may be StringOption, UintOption, OpaqueOption or EmptyOption)
     * @throws InvalidOptionException
     */
    public static Option createOption(OptionName optionName, byte[] value) throws InvalidOptionException{
        //Option numbers multiple of 14 are reserved as intermediate options for larger
        //deltas than 15. Option number 21 is "If-none-match" and must not contain any value
        //(e.g. for PUT requests not being supposed to overwrite existing resources)
        if (optionName == OptionName.FENCEPOST || optionName == OptionName.IF_NONE_MATCH){
            return new EmptyOption(optionName);
        }

        switch (OptionRegistry.getOptionType(optionName)){
            case UINT:
                return new UintOption(optionName, value);
            case STRING:
                 return new StringOption(optionName, value);
            case OPAQUE:
                return new OpaqueOption(optionName, value);
            case EMPTY:
                return new EmptyOption(optionName);
            default:
                throw new InvalidOptionException(optionName.getNumber(), "Type of option number " +
                        optionName.getNumber() + " is unknown.");
        }
    }

    /**
     * Returns the options value as byte array
     * @return the options value as byte array
     */
    public byte[] getValue(){
        return value;
    }


    //Seperates a String into fragments using the given seperator and returns a Collection of options of the given
    //type each containg one fragment as payload
    private static Collection<Option> createSeparatedOptions(OptionName optionName, String seperator, String value)
           throws InvalidOptionException{

        log.debug("Create " + optionName + " options for value '" + value + "'.");

        String[] parts = value.split(seperator);
        ArrayList<Option> options = new ArrayList<Option>(parts.length);
        for(String part : parts){
            options.add(new StringOption(optionName, part));
            log.debug("" + optionName + " option instance for part '" + part + "' successfully created " +
                    "(but not yet added to the option list!)");
        }

        return options;
    }

//    /**
//     * Returns a hex string representation of the options value
//     *
//     * @param b
//     * @return
//     */
//    public static String getHexString(byte[] b){
//      String result = "";
//      for (int i=0; i < b.length; i++) {
//        result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1) + "-";
//      }
//      return (String)result.subSequence(0, Math.max(result.length() - 1, 0));
//    }

    /**
     * Returns the option number internally representing this options {@link OptionName}
     * @return the option number internally representing this options {@link OptionName}
     */
    public int getOptionNumber(){
        return optionNumber;
    }

    /**
     * Returns the decoded, i.e. deserialized value of this option
     * @return the decoded, i.e. deserialized value of this option
     */
    public abstract Object getDecodedValue();


    @Override
    public abstract boolean equals(Object o);

    @Override
    public int hashCode(){
        return optionNumber;
    }

    @Override
    public String toString(){
        return "" + getDecodedValue() + " (" + OptionName.getByNumber(optionNumber) + ")";
    }
}
