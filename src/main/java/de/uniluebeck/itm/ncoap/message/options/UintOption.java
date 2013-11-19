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

import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This class contains all specific functionality for {@link Option} instances of {@link OptionType#UINT}. If there is
 * any need to access {@link Option} instances directly, e.g. to retrieve its value, one could either cast the option
 * to {@link UintOption} and call {@link #getDecodedValue()} or one could all {@link Option#getDecodedValue()} and
 * cast the return value to {@link Long}.
 *
 * @author Oliver Kleine
 */
public class UintOption extends Option<Long>{

    private static Logger log = LoggerFactory.getLogger(UintOption.class.getName());

    public UintOption(int optionNumber, byte[] value) throws InvalidOptionException, UnknownOptionException {
        super(optionNumber, shortenValue(value));
    }

    public UintOption(int optionNumber, long value) throws InvalidOptionException, UnknownOptionException {
        this(optionNumber, value == 0 ? new byte[0] : new BigInteger(1, Longs.toByteArray(value)).toByteArray());
    }

    @Override
    public Long getDecodedValue() {
        return new BigInteger(1, value).longValue();
    }


    public static byte[] shortenValue(byte[] value){
        int index = 0;
        while(index < value.length - 1 && value[index] == 0)
            index++;

        return Arrays.copyOfRange(value, index, value.length);
    }

//    @Override
//    public boolean equals(Object object) {
//        if(!(object instanceof UintOption))
//            return false;
//
//        UintOption other = (UintOption) object;
//        return this.getDecodedValue() == other.getDecodedValue();
//    }
}
