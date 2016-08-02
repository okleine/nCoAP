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

import java.util.Arrays;

/**
 * An empty option achieves it's goal just by being present in a message or not. However, as the internal
 * representation of options needs an instance of {@link OptionValue} empty options are represented using
 * {@link EmptyOptionValue}.
 *
 * @author Oliver Kleine
 */
public final class EmptyOptionValue extends OptionValue<Void> {

    private static Logger log = LoggerFactory.getLogger(EmptyOptionValue.class.getName());

    /**
     * @param optionNumber the option number of the {@link EmptyOptionValue} to be created
     *
     * @throws java.lang.IllegalArgumentException if the given option number does not refer to an empty option
     */
    public EmptyOptionValue(int optionNumber) throws IllegalArgumentException {
       super(optionNumber, new byte[0], false);

        log.debug("Empty Option (#{}) created.", optionNumber);
    }


    /**
     * Returns <code>null</code>
     * @return <code>null</code>
     */
    @Override
    public Void getDecodedValue() {
        return null;
    }


    /**
     * Returns <code>0</code>
     * @return <code>0</code>
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Checks if a given {@link Object} equals this {@link EmptyOptionValue} instance. A given {@link Object} equals
     * this {@link EmptyOptionValue} if and only if the {@link Object} is an instance of {@link EmptyOptionValue}.
     *
     * @param object the object to check for equality with this instance of {@link EmptyOptionValue}
     *
     * @return <code>true</code> if the given {@link Object} is an instance of {@link EmptyOptionValue} and
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EmptyOptionValue))
            return false;

        EmptyOptionValue other = (EmptyOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }
}
