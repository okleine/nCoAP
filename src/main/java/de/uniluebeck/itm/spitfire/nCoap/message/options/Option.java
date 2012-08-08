package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.apache.log4j.Logger;
import sun.net.util.IPAddressUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
*
 * @author Oliver Kleine
 */
public abstract class Option{

    private static Logger log = Logger.getLogger(Option.class.getName());

    public static final Charset charset = Charset.forName("UTF-8");

    protected int optionNumber;
    protected byte[] value = new byte[0];

    protected Option(OptionName optionName){
        optionNumber = optionName.number;
    }

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
            String msg = "[Option] URI must be absolute and " +
                    "scheme must be either \"coap\" or \"coaps\" but is " + scheme;
            throw new URISyntaxException(uri.toString(), msg);
        }
        else{
            scheme = scheme.toLowerCase();
            if(!(scheme.equals("coap") || scheme.equals("coaps"))){
                String msg = "[Option] URI scheme must be either \"coap\" or \"coaps\" but is " + scheme;
                throw new URISyntaxException(uri.toString(), msg);
            }
        }

        //Target URI must not have fragment part
        if(uri.getFragment() != null){
            String msg = "[Option] Target URI must not have a fragment part.";
            throw new URISyntaxException(uri.toString(), msg);
        }

        //Create URI-host option
        String host = uri.getHost();

        //Do only add an URI host option if the host is no IP-Address
        if(log.isDebugEnabled()){
            log.debug("[Option] Target URI host: " + host);
        }

        if(host.startsWith("[") && host.endsWith("]")){
            host = host.substring(1, host.length() - 1);
        }
        if(!IPAddressUtil.isIPv6LiteralAddress(host) && !IPAddressUtil.isIPv4LiteralAddress(host)){
            result.add(new StringOption(OptionName.URI_HOST, host.getBytes(Option.charset)));
            if(log.isDebugEnabled()){
                log.debug("[Option] URI-Host option added to result list.");
            }
        }
        else{
            if(log.isDebugEnabled()){
                log.debug("[Option] URI-Host is an IP literal and thus not added to the result list.");
            }
        }

        //Create URI-port option
        int port = uri.getPort();

        //Check whether the port value is non-standard
        if(log.isDebugEnabled()){
            log.debug("[Option] Target URI-Port: " + port);
        }
        if(port > 0 && port != OptionRegistry.COAP_PORT_DEFAULT){
            result.add(new UintOption(OptionName.URI_PORT, port));
            if(log.isDebugEnabled()){
                log.debug("[Option] Target URI-Port option added to result list.");
            }
        }
        else{
            if(log.isDebugEnabled()){
                log.debug("[Option] URI-Port is either empty or default (= " + OptionRegistry.COAP_PORT_DEFAULT +
                        ") and thus not added as option.");
            }
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
     * @throws InvalidOptionException if one of the proxy URI options to be created is not valid
     */
    public static Collection<Option> createProxyUriOptions(URI uri) throws InvalidOptionException, URISyntaxException {
        uri = uri.normalize();
        ArrayList<Option> proxyOptions = new ArrayList<Option>();

        if(!uri.isAbsolute()){
            String msg = "[Option] URI to be added as proxy URI (" + uri.toString() + ") is not absolute.";
            throw new URISyntaxException(uri.toString(), msg);
        }

        byte[] encodedUri = uri.toString().getBytes(StringOption.charset);
        if(log.isDebugEnabled()){
            log.debug("[Option] Length of encoded proxy URI: " + encodedUri.length + " bytes.");
        }

        int startPos = 0;
        while(startPos < encodedUri.length){
            //All but the last option must contain a payload of 270 bytes
            int endPos = Math.min(startPos + OptionRegistry.getMaxLength(OptionName.PROXY_URI),
                                   encodedUri.length);
            proxyOptions.add(new StringOption(OptionName.PROXY_URI, Arrays.copyOfRange(encodedUri, startPos, endPos)));
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
            String msg = "[Option] Cannot create option " + optionName + " with string value.";
            throw new InvalidOptionException(optionName, msg);
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
            String msg = "[Option] Cannot create option " + optionName + " with uint value.";
            throw new InvalidOptionException(optionName, msg);
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
            throw new InvalidOptionException(optionName, msg);
        }
        
        if(optionName == OptionName.TOKEN && value.length == 0){
            String msg = "[Option] Empty byte[] is the default value for option " + optionName + ". No option created.";
            throw new InvalidOptionException(optionName, msg);
        }

        return new OpaqueOption(optionName, value);
    }

    public static EmptyOption createEmptyOption(OptionName optionName) throws InvalidOptionException{
        //Check whether current number is appropriate for an EmptyOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.EMPTY){
            String msg = "[Option] Cannot create option " + optionName + " as instance of EmptyOption.";
            throw new InvalidOptionException(optionName, msg);
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
                throw new InvalidOptionException(optionName, "Type of option number " +  optionName.number +
                        " is unknown.");
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

        if(log.isDebugEnabled()){
            log.debug("[Option] Create " + optionName + " options for value '" + value + "'.");
        }

        String[] parts = value.split(seperator);
        ArrayList<Option> options = new ArrayList<Option>(parts.length);
        for(String part : parts){
            options.add(new StringOption(optionName, part));
            if(log.isDebugEnabled()){
                log.debug("[Option] " + optionName + " option instance for part '" + part + "' successfully created " +
                    "(but not yet added to the option list!)");
            }
        }

        return options;
    }

    //This method is just for logging purposes
    public static String getHexString(byte[] b){
      String result = "";
      for (int i=0; i < b.length; i++) {
        result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1) + "-";
      }
      return (String)result.subSequence(0, Math.max(result.length() - 1, 0));
    }

    public int getOptionNumber(){
        return optionNumber;
    }
    @Override
    public abstract boolean equals(Object o);

    @Override
    public int hashCode(){
        return optionNumber;
    }
}
