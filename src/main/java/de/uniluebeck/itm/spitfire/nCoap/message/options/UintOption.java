package de.uniluebeck.itm.spitfire.nCoap.message.options;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Oliver Kleine
 */
public class UintOption extends Option{

    private static Logger log = LoggerFactory.getLogger(UintOption.class.getName());

    //constructor with encoded value should only be used for incoming messages
    UintOption(OptionName optionName, byte[] value) throws InvalidOptionException{
        super(optionName);
        setValue(optionName, value);

        log.debug("New Option (" + optionName + ")" + " created (Value: " + getHexString(value)+ ")");
    }

    //constructor with decoded value should only be used for outgoing messages
    UintOption(OptionName optionName, long value) throws InvalidOptionException{
        super(optionName);
        setValue(optionName, value);

        log.debug("New Option (" + optionName + ")" + " created (Value: " + getHexString(this.value)+ ")");
    }

    private void setValue(OptionName optionName, byte[] bytes) throws InvalidOptionException {
        //Check value length constraints
        if(bytes.length > OptionRegistry.getMaxLength(optionName)){
            String msg = "[UintOption] Value length for " + optionName +
                     " option must not be longer than " + OptionRegistry.getMaxLength(optionName) +
                     " but is " + bytes.length + ".";
            throw new InvalidOptionException(optionNumber, msg);
        }

        this.value = removeLeadingZeroBytes(bytes);
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
        this.value = removeLeadingZeroBytes(Longs.toByteArray(value));
    }

    private byte[] removeLeadingZeroBytes(byte[] bytes) throws InvalidOptionException {
        //Empty array and thus nothing to remove
        if(bytes.length == 0){
            return bytes;
        }

        //Remove eventual leading zeros
        for(int i = 0; i < bytes.length; i++){
            if(bytes[i] != 0){
                return Tools.getByteArrayRange(bytes, i, bytes.length);
            }
        }

        //All elements were zeros so return empty array
        return new byte[0];
    }

    public long getDecodedValue(){
        return Longs.fromByteArray(Bytes.concat(new byte[8-value.length], value));
    }
    /**
     * This method checks the equality of any given object with the current UintOption from which the method
     * is called. The method returns <code>true</code> if and only if the given object is an instance of
     * UintOption and has both the same {@link OptionName} and value as the current UintOption.
     * @param o
     * @return   <code>true</code> if and only if the given object is an instance of
     * UintOption and has both the same OptionName and value as the current UintOption. Otherwise it returns
     * <code>false</code>.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof UintOption)){
            return false;
        }
        UintOption opt = (UintOption) o;
        if((optionNumber == opt.optionNumber) && (this.getDecodedValue() == opt.getDecodedValue())){
            return true;
        }
        return false;
    }
}
