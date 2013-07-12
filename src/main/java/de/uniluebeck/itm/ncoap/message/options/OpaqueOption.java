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

import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionType;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * This class contains all specific functionality for {@link Option} instances of {@link OptionType#OPAQUE}. If there is
 * any need to access {@link Option} instances directly, e.g. to retrieve its value, one could either cast the option
 * to {@link OpaqueOption} and call {@link #getDecodedValue()} or one could call {@link Option#getDecodedValue()} and
 * cast the return value to {@link String}.
 *
 * @author Oliver Kleine
 */
class OpaqueOption extends Option{

    private static Logger log = LoggerFactory.getLogger(OpaqueOption.class.getName());
    
    //Constructor with package visibility
    OpaqueOption(OptionName optionName, byte[] value) throws InvalidOptionException{
        super(optionName);
        //Check whether current number is appropriate for an OpaqueOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.OPAQUE){
            String msg = optionName + " is no opaque option.";
            throw new InvalidOptionException(optionNumber, msg);
        }

        setValue(optionName, value);

        log.debug("{} option with value {} created.", optionName, new ByteArrayWrapper(value));
    }

    private void setValue(OptionName optionName, byte[] value) throws InvalidOptionException{

        int min_length = OptionRegistry.getMinLength(optionName);
        int max_length = OptionRegistry.getMaxLength(optionName);

        //Check whether length constraints are fulfilled
        if(value.length < min_length || value.length > max_length){
            String msg = "Value length for " + OptionName.getByNumber(optionNumber) + " must be between " +
                    min_length + " and " +  max_length + " but is " + value.length;
            throw new InvalidOptionException(optionNumber, msg);
        }

        //Set value if there was no Exception thrown so far
        this.value = value;
    }

    /**
     * This method is just to implement the satisfy the {@link Option} abstract method from {@link Option}.
     * The return value is exactly the same as {@link #getValue()}.
     *
     * @return the byte[] containing the options value
     */
    @Override
    public byte[] getDecodedValue(){
       return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof OpaqueOption)){
            return false;
        }
        OpaqueOption opt = (OpaqueOption) o;
        if((optionNumber == opt.optionNumber) && Arrays.equals(this.value, opt.value)){
            return true;
        }
        return false;
    }

    @Override
    public String toString(){
        return new ByteArrayWrapper(getValue()).toString() + " (" + OptionName.getByNumber(optionNumber) + ")";
    }
}
