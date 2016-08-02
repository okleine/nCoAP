/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.message.options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This class contains all specific functionality for {@link OptionValue} instances with unsigned integer values. If there is
 * any need to access {@link OptionValue} instances directly, e.g. to retrieve its value, one could either cast the option
 * to {@link UintOptionValue} and call {@link #getDecodedValue()} or one could all {@link OptionValue#getDecodedValue()} and
 * cast the return value to {@link Long}.
 *
 * @author Oliver Kleine
 */
public class UintOptionValue extends OptionValue<Long> {

    /**
     * Corresponds to a value of <code>-1</code> to indicate that there is no value for that option set.
     */
    public static final long UNDEFINED = -1;

    private static Logger log = LoggerFactory.getLogger(UintOptionValue.class.getName());

    /**
     * @param optionNumber the option number of the {@link StringOptionValue} to be created
     * @param value the value of the {@link StringOptionValue} to be created
     *
     * @throws java.lang.IllegalArgumentException if the given option number is unknown, or if the given value is
     * either the default value or exceeds the defined length limits for options with the given option number
     */
    public UintOptionValue(int optionNumber, byte[] value) throws IllegalArgumentException {
        this(optionNumber, shortenValue(value), false);
    }

    /**
     * @param optionNumber the option number of the {@link StringOptionValue} to be created
     * @param value the value of the {@link StringOptionValue} to be created
     * @param allowDefault if set to <code>true</code> no {@link IllegalArgumentException} is thrown if the given
     *                     value is the default value. This may be useful in very special cases, so do not use this
     *                     feature if you are not absolutely sure that it is necessary!
     *
     * @throws java.lang.IllegalArgumentException if the given option number is unknown, or if the given value is
     * either the default value or exceeds the defined length limits for options with the given option number
     */
    public UintOptionValue(int optionNumber, byte[] value, boolean allowDefault) throws IllegalArgumentException {
        super(optionNumber, shortenValue(value), allowDefault);

        log.debug("Uint Option (#{}) created with value: {}", optionNumber, this.getDecodedValue());
    }


//    public UintOptionValue(int optionNumber, long value) throws IllegalArgumentException {
//        this(optionNumber, value == 0 ? new byte[0] : new BigInteger(1, Longs.toByteArray(value)).toByteArray());
//    }

    @Override
    public Long getDecodedValue() {
        return new BigInteger(1, value).longValue();
    }


    @Override
    public int hashCode() {
        return getDecodedValue().hashCode();
    }


    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UintOptionValue))
            return false;

        UintOptionValue other = (UintOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }


    public static byte[] shortenValue(byte[] value) {
        int index = 0;
        while(index < value.length - 1 && value[index] == 0)
            index++;

        return Arrays.copyOfRange(value, index, value.length);
    }

}
