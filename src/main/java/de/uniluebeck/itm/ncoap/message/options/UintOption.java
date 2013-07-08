package de.uniluebeck.itm.ncoap.message.options;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionType;
import de.uniluebeck.itm.ncoap.toolbox.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains all specific functionality for {@link Option} instances of {@link OptionType#UINT}. If there is
 * any need to access {@link Option} instances directly, e.g. to retrieve its value, one could either cast the option
 * to {@link UintOption} and call {@link #getDecodedValue()} or one could all {@link Option#getDecodedValue()} and
 * cast the return value to {@link Long}.
 *
 * @author Oliver Kleine
 */
public class UintOption extends Option{

    private static Logger log = LoggerFactory.getLogger(UintOption.class.getName());

    //constructor with encoded value should only be used for incoming messages
    UintOption(OptionName optionName, byte[] value) throws InvalidOptionException{
        super(optionName);
        setValue(optionName, value);

        log.debug("{} option with value {} created.", optionName, new ByteArrayWrapper(value));
    }

    //constructor with decoded value should only be used for outgoing messages
    UintOption(OptionName optionName, long value) throws InvalidOptionException{
        super(optionName);
        setValue(optionName, value);

        log.debug("{} option with value {} created.", optionName, new ByteArrayWrapper(this.value));
    }

    private void setValue(OptionName optionName, byte[] bytes) throws InvalidOptionException {
        //Check value length constraints
        if(bytes.length > OptionRegistry.getMaxLength(optionName)){
            String msg = "[UintOption] Value length for " + optionName +
                     " option must not be longer than " + OptionRegistry.getMaxLength(optionName) +
                     " but is " + bytes.length + ".";
            throw new InvalidOptionException(optionNumber, msg);
        }

        this.value = ByteArrayWrapper.removeLeadingZerosFromByteArray(bytes);
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
        this.value = ByteArrayWrapper.removeLeadingZerosFromByteArray(Longs.toByteArray(value));
    }

    /**
     * Returns the decoded value of this option ({@link OptionType#UINT}
     * @return the decoded value of this option ({@link OptionType#UINT}
     */
    @Override
    public Long getDecodedValue(){
        return Longs.fromByteArray(Bytes.concat(new byte[8-value.length], value));
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
