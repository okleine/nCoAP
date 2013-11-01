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

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.ncoap.message.options.OptionName.MAX_AGE;
import static de.uniluebeck.itm.ncoap.message.options.OptionName.URI_PORT;
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

    public static final int MAX_AGE_DEFAULT = 60;
    public static final int URI_PORT_DEFAULT = 5683;


    //constructor with encoded value should only be used for incoming messages
    public UintOption(OptionName optionName, long value) throws InvalidOptionException{
        if(optionName == URI_PORT && )
        super(optionName, value);
        setValue(optionName, value);

        log.debug("{} option with value {} created.", optionName, new Token(value));

        ByteBuffer.
    }

    @Override
    public Long getValue() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //constructor with decoded value should only be used for outgoing messages
    UintOption(OptionName optionName, long value) throws InvalidOptionException{
        super(optionName);
        setValue(optionName, value);

        log.debug("{} option with value {} created.", optionName, new Token(this.encodedValue));
    }

    private void setValue(OptionName optionName, byte[] bytes) throws InvalidOptionException {
        //Check value length constraints
        if(bytes.length > OptionRegistry.getMaxLength(optionName)){
            String msg = "[UintOption] Value length for " + optionName +
                     " option must not be longer than " + OptionRegistry.getMaxLength(optionName) +
                     " but is " + bytes.length + ".";
            throw new InvalidOptionException(optionNumber, msg);
        }

        this.encodedValue = Token.removeLeadingZerosFromByteArray(bytes);
    }

    //Sets the options value after checking its correctness
    private void setValue(OptionName optionName, long value) throws InvalidOptionException{
        //Check value constraints
        int max_length = OptionRegistry.getMaxLength(optionName);
        long max_value = (long)Math.pow(2, max_length * 8) - 1;
        
        if(value < 0 || value > max_value){
            String msg = "[UintOption] Value for " + optionName +
                     " option must not be negative or greater than " + max_value + " but is " + value + ".";
            throw new InvalidOptionException(optionNumber, msg);
        }

        //Set value if there was no Exception thrown so far
        this.encodedValue = Token.removeLeadingZerosFromByteArray(Longs.toByteArray(value));
    }

    /**
     * Returns the decoded value of this option ({@link OptionType#UINT}
     * @return the decoded value of this option ({@link OptionType#UINT}
     */
    @Override
    public Long getDecodedValue(){
        return Longs.fromByteArray(Bytes.concat(new byte[8- encodedValue.length], encodedValue));
    }

    /**
     * This method checks the equality of any given object with this {@link UintOption} instance. The method returns
     * <code>true</code> if and only if the given object is an instance of
     * {@link UintOption} and has both the same {@link OptionName} and value.
     *
     * @param o the {@link Object} to check for equality
     *
     * @return   <code>true</code> if and only if the given object is an instance of
     * {@link UintOption} and has both the same {@link OptionName} and value. Otherwise it returns
     * <code>false</code>.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof UintOption)){
            return false;
        }
        UintOption opt = (UintOption) o;
        if((optionNumber == opt.optionNumber) && (this.getDecodedValue().equals(opt.getDecodedValue()))){
            return true;
        }
        return false;
    }
}
