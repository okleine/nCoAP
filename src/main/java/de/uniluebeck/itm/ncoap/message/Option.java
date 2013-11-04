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

import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static de.uniluebeck.itm.ncoap.message.OptionName.*;
import static de.uniluebeck.itm.ncoap.message.OptionType.*;

/**
*
 * @author Oliver Kleine
 */
public abstract class Option<T>{

    private static Logger log = LoggerFactory.getLogger(Option.class.getName());

    public static final long MAX_AGE_DEFAULT = 60;
    public static final long URI_PORT_DEFAULT = 5683;

    public static final byte[] ENCODED_MAX_AGE_DEFAULT =
            new BigInteger(1, Longs.toByteArray(MAX_AGE_DEFAULT)).toByteArray();

    public static final byte[] ENCODED_URI_PORT_DEFAULT = new BigInteger(1, Longs.toByteArray(URI_PORT_DEFAULT)).toByteArray();

    private static HashMap<Integer, Integer[]> characteristics = new HashMap<>();
    static{
        characteristics.put(    IF_MATCH,       new Integer[]{OPAQUE,       0,      8       });
        characteristics.put(    URI_HOST,       new Integer[]{STRING,       1,      255     });
        characteristics.put(    ETAG,           new Integer[]{OPAQUE,       1,      8       });
        characteristics.put(    IF_NONE_MATCH,  new Integer[]{EMPTY,        0,      0       });
        characteristics.put(    URI_PORT,       new Integer[]{UINT,         0,      2       });
        characteristics.put(    LOCATION_PATH,  new Integer[]{STRING,       0,      255     });
        characteristics.put(    URI_PATH,       new Integer[]{STRING,       0,      255     });
        characteristics.put(    CONTENT_FORMAT, new Integer[]{UINT,         0,      2       });
        characteristics.put(    MAX_AGE,        new Integer[]{UINT,         0,      4       });
        characteristics.put(    URI_QUERY,      new Integer[]{STRING,       0,      255     });
        characteristics.put(    ACCEPT,         new Integer[]{UINT,         0,      2       });
        characteristics.put(    LOCATION_QUERY, new Integer[]{STRING,       0,      255     });
        characteristics.put(    PROXY_URI,      new Integer[]{STRING,       1,      1034    });
        characteristics.put(    PROXY_SCHEME,   new Integer[]{STRING,       1,      255     });
        characteristics.put(    SIZE_1,         new Integer[]{UINT,         0,      4       });
    }

    /**
     * Returns the minimum length for the given option number in bytes.
     *
     * @param optionNumber the option number to check the minimum length of
     * @return the minimum length for the given option number in bytes
     * @throws UnknownOptionException if the given option number refers to an unknown option
     */
    public static int getMinLength(int optionNumber) throws UnknownOptionException {
        if(!characteristics.containsKey(optionNumber))
            throw new UnknownOptionException(optionNumber);

        return characteristics.get(optionNumber)[1];
    }


    /**
     * Returns the maximum length for the given option number in bytes.
     *
     * @param optionNumber the option number to check the maximum length of
     * @return the maximum length for the given option number in bytes
     * @throws UnknownOptionException if the given option number refers to an unknown option
     */
    public static int getMaxLength(int optionNumber) throws UnknownOptionException {
        if(!characteristics.containsKey(optionNumber))
            throw new UnknownOptionException(optionNumber);

        return characteristics.get(optionNumber)[2];
    }


    /**
     * Returns <code>true</code> if the option is critical and <code>false</code> if the option is elective
     *
     * @return <code>true</code> if the option is critical and <code>false</code> if the option is elective
     */
    public static boolean isCritical(int optionNumber){
        return (optionNumber & 1) == 1;
    }

    /**
     * Returns <code>true</code> if the option is unsafe-to-forward and <code>false</code> if the option is
     * safe-to-forward by a proxy
     *
     * @return <code>true</code> if the option is unsafe-to-forward and <code>false</code> if the option is
     * safe-to-forward by a proxy
     */
    public static boolean isUnsafe(int optionNumber){
        return (optionNumber & 2) == 2;
    }

    public static boolean isNoCacheKey(int optionNumber){
        return (optionNumber & 0x1e) == 0x1c;
    }

    /**
     * Returns the integer value representing the type of the option the given option number refers to (see
     * {@link OptionType} for constants)
     *
     * @param optionNumber the option number to return the type of
     * @return the integer value representing the type of the option the given option number refers to
     * @throws UnknownOptionException if the given option number refers to an unknown option
     */
    public static int getOptionType(int optionNumber) throws UnknownOptionException{
        if(!characteristics.containsKey(optionNumber))
            throw new UnknownOptionException(optionNumber);
        else
            return characteristics.get(optionNumber)[0];
    }

    public static boolean isDefaultValue(int optionNumber, byte[] value){
        if(optionNumber == OptionName.URI_PORT && Arrays.equals(value, ENCODED_URI_PORT_DEFAULT))
            return true;

        if(optionNumber == OptionName.MAX_AGE && Arrays.equals(value, ENCODED_MAX_AGE_DEFAULT))
            return true;

        if(optionNumber == OptionName.URI_HOST && InetAddresses.isInetAddress(new String(value, CoapMessage.CHARSET)))
            return true;

        return false;
    }

    protected byte[] value;

    protected Option(int optionNumber, byte[] value) throws InvalidOptionException, UnknownOptionException {

        if(Option.isDefaultValue(optionNumber, value))
            throw new InvalidOptionException(optionNumber, "The given value is the default value. No option created.");

        if(getMinLength(optionNumber) > value.length || getMaxLength(optionNumber) < value.length)
            throw new InvalidOptionException(optionNumber, "Invalid option length (Actual: " + value.length
                    + ", Minimum: " + getMinLength(optionNumber) + ", Maximum: " + getMaxLength(optionNumber) + ")");

        this.value = value;
    }

    public abstract T getValue();



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

//    /**
//     * Creates and returns option instance of the appropriate type according to the given name
//     * @param optionName The name of the option
//     * @param value A long representing the options value (which is of type unsigned int)
//     * @return appropriate Option instance (may be StringOption, UintOption, OpaqueOption or EmptyOption)
//     * @throws InvalidOptionException
//     */
//    public static Option createOption(OptionName optionName, byte[] value) throws InvalidOptionException{
//        //Option numbers multiple of 14 are reserved as intermediate options for larger
//        //deltas than 15. Option number 21 is "If-none-match" and must not contain any value
//        //(e.g. for PUT requests not being supposed to overwrite existing resources)
//        if (optionName == OptionName.FENCEPOST || optionName == OptionName.IF_NONE_MATCH){
//            return new EmptyOption(optionName);
//        }
//
//        switch (OptionRegistry.getOptionType(optionName)){
//            case UINT:
//                return new UintOption(optionName, value);
//            case STRING:
//                 return new StringOption(optionName, value);
//            case OPAQUE:
//                return new OpaqueOption(optionName, value);
//            case EMPTY:
//                return new EmptyOption(optionName);
//            default:
//                throw new InvalidOptionException(optionName.getNumber(), "Type of option number " +
//                        optionName.getNumber() + " is unknown.");
//        }
//    }

//    /**
//     * Returns the options value as byte array
//     * @return the options value as byte array
//     */
//    public byte[] getEncodedValue(){
//        return encodedValue;
//    }


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

//    /**
//     * Returns the decoded, i.e. deserialized value of this option
//     * @return the decoded, i.e. deserialized value of this option
//     */
//    public abstract Object getDecodedValue();


//    @Override
//    public abstract boolean equals(Object o);

    @Override
    public int hashCode(){
        return optionNumber;
    }

    @Override
    public String toString(){
        return "" + getDecodedValue() + " (" + OptionName.getByNumber(optionNumber) + ")";
    }
}
