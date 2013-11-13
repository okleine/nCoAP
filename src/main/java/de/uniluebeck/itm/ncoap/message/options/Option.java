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
package de.uniluebeck.itm.ncoap.message.options;

import com.google.common.collect.HashMultimap;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

/**
*
 * @author Oliver Kleine
 */
public abstract class Option<T>{

    private static Logger log = LoggerFactory.getLogger(Option.class.getName());

    public static class Name{
        //Name          Number      OptionType  MinLength   MaxLength
        public static final int IF_MATCH        = 1;
        public static final int URI_HOST        = 3;
        public static final int ETAG            = 4;
        public static final int IF_NONE_MATCH   = 5;
        public static final int OBSERVE         = 6;
        public static final int URI_PORT        = 7;
        public static final int LOCATION_PATH   = 8;
        public static final int URI_PATH        = 11;
        public static final int CONTENT_FORMAT  = 12;
        public static final int MAX_AGE         = 14;
        public static final int URI_QUERY       = 15;
        public static final int ACCEPT          = 17;
        public static final int LOCATION_QUERY  = 20;
        public static final int PROXY_URI       = 35;
        public static final int PROXY_SCHEME    = 39;
        public static final int SIZE_1          = 60;
    }
    
    public static final long MAX_AGE_DEFAULT = 60;
    public static final long URI_PORT_DEFAULT = 5683;
    public static final int ETAG_LENGTH_DEFAULT = 4;

    public static final byte[] ENCODED_MAX_AGE_DEFAULT =
            new BigInteger(1, Longs.toByteArray(MAX_AGE_DEFAULT)).toByteArray();

    public static final byte[] ENCODED_URI_PORT_DEFAULT = new BigInteger(1, Longs.toByteArray(URI_PORT_DEFAULT)).toByteArray();

    private static HashMap<Integer, Integer[]> characteristics = new HashMap<>();
    static{
        characteristics.put(    Name.IF_MATCH,       new Integer[]{OptionType.Name.OPAQUE,       0,      8       });
        characteristics.put(    Name.URI_HOST,       new Integer[]{OptionType.Name.STRING,       1,      255     });
        characteristics.put(    Name.ETAG,           new Integer[]{OptionType.Name.OPAQUE,       1,      8       });
        characteristics.put(    Name.IF_NONE_MATCH,  new Integer[]{OptionType.Name.EMPTY,        0,      0       });
        characteristics.put(    Name.URI_PORT,       new Integer[]{OptionType.Name.UINT,         0,      2       });
        characteristics.put(    Name.LOCATION_PATH,  new Integer[]{OptionType.Name.STRING,       0,      255     });
        characteristics.put(    Name.OBSERVE,        new Integer[]{OptionType.Name.OPAQUE,       0,      3       });
        characteristics.put(    Name.URI_PATH,       new Integer[]{OptionType.Name.STRING,       0,      255     });
        characteristics.put(    Name.CONTENT_FORMAT, new Integer[]{OptionType.Name.UINT,         0,      2       });
        characteristics.put(    Name.MAX_AGE,        new Integer[]{OptionType.Name.UINT,         0,      4       });
        characteristics.put(    Name.URI_QUERY,      new Integer[]{OptionType.Name.STRING,       0,      255     });
        characteristics.put(    Name.ACCEPT,         new Integer[]{OptionType.Name.UINT,         0,      2       });
        characteristics.put(    Name.LOCATION_QUERY, new Integer[]{OptionType.Name.STRING,       0,      255     });
        characteristics.put(    Name.PROXY_URI,      new Integer[]{OptionType.Name.STRING,       1,      1034    });
        characteristics.put(    Name.PROXY_SCHEME,   new Integer[]{OptionType.Name.STRING,       1,      255     });
        characteristics.put(    Name.SIZE_1,         new Integer[]{OptionType.Name.UINT,         0,      4       });
    }

    private static HashMultimap<Integer, Integer> mutualExclusions = HashMultimap.create();
    static{
        mutualExclusions.put(Name.URI_HOST,     Name.PROXY_URI);
        mutualExclusions.put(Name.PROXY_URI,    Name.URI_HOST);

        mutualExclusions.put(Name.URI_PORT,     Name.PROXY_URI);
        mutualExclusions.put(Name.PROXY_URI,    Name.URI_PORT);

        mutualExclusions.put(Name.URI_PATH,     Name.PROXY_URI);
        mutualExclusions.put(Name.PROXY_URI,    Name.URI_PATH);

        mutualExclusions.put(Name.URI_QUERY,    Name.PROXY_URI);
        mutualExclusions.put(Name.PROXY_URI,    Name.URI_QUERY);

        mutualExclusions.put(Name.PROXY_SCHEME, Name.PROXY_URI);
        mutualExclusions.put(Name.PROXY_URI,    Name.PROXY_SCHEME);
    }

    /**
     * Returns <code>true</code> if and only if the co-existence of both options is not allowed in a single
     * message. As this method checks for mutual exclusion, the order of the given arguments has no impact on the
     * result.
     *
     * @param firstOptionNumber the first option number
     * @param secondOptionNumber the second option number
     *
     * @return <code>true</code> if the co-existence of the given option numbers is not allowed in a single message
     */
    public static boolean mutuallyExcludes(int firstOptionNumber, int secondOptionNumber){
        return mutualExclusions.get(firstOptionNumber).contains(secondOptionNumber);
    }

    /**
     * Returns the minimum length for the given option number in bytes.
     *
     * @param optionNumber the option number to check the minimum length of
     *
     * @return the minimum length for the given option number in bytes
     *
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

    /**
     * Returns <code>true</code> if the given value is the default value for the given option number and
     * <code>false</code> if it is not the default value. Options with default value cannot be created.
     *
     * @param optionNumber the option number
     * @param value the value to check if it is the default value for the option number
     *
     * @return <code>true</code> if the given value is the default value for the given option number
     */
    public static boolean isDefaultValue(int optionNumber, byte[] value){

        if(optionNumber == Name.URI_PORT && Arrays.equals(value, ENCODED_URI_PORT_DEFAULT))
            return true;

        if(optionNumber == Name.MAX_AGE && Arrays.equals(value, ENCODED_MAX_AGE_DEFAULT))
            return true;

        if(optionNumber == Name.URI_HOST){
            String hostName = new String(value, CoapMessage.CHARSET);
            if(hostName.startsWith("[") && hostName.endsWith("]"))
                hostName = hostName.substring(1, hostName.length() - 1);

            if(InetAddresses.isInetAddress(hostName))
                return true;
        }

        return false;
    }

    public static Option createOption(int optionNumber, byte[] value) throws UnknownOptionException,
            InvalidOptionException {

        switch(Option.getOptionType(optionNumber)){
            case OptionType.Name.STRING:
                return new StringOption(optionNumber, value);
            case OptionType.Name.UINT:
                return new UintOption(optionNumber, value);
            case OptionType.Name.OPAQUE:
                return new OpaqueOption(optionNumber, value);
            case OptionType.Name.EMPTY:
                return new EmptyOption(optionNumber);
            default:
                log.error("This should never happen!");
                throw new UnknownOptionException(optionNumber);
        }
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


    public byte[] getValue(){
        return this.value;
    }


    public abstract T getDecodedValue();

    @Override
    public boolean equals(Object object){
        if(!(object instanceof Option))
            return false;

        Option other = (Option) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }

    @Override
    public String toString(){
        return "" + this.getDecodedValue();
    }
}
