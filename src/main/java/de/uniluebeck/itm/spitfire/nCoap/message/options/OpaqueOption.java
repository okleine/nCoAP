package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 *
 * @author Oliver Kleine
 */
public class OpaqueOption extends Option{

    private static Logger log = LoggerFactory.getLogger(OpaqueOption.class.getName());
    
    //Constructor with package visibility
    OpaqueOption(OptionName optionName, byte[] value) throws InvalidOptionException{
        super(optionName);
        //Check whether current number is appropriate for an OpaqueOption
        if(OptionRegistry.getOptionType(optionName) != OptionRegistry.OptionType.OPAQUE){
            String msg = "[OpaqueOption] Cannot set option " + optionName + " for an OpaqueOption.";
            throw new InvalidOptionException(optionNumber, msg);
        }

        setValue(optionName, value);

        log.debug("New Option (" + optionName + ")" +
                  " created (value: " + getHexString(value) + ", encoded length: " + value.length + ")");
    }

    private void setValue(OptionName optionName, byte[] value) throws InvalidOptionException{

        int min_length = OptionRegistry.getMinLength(optionName);
        int max_length = OptionRegistry.getMaxLength(optionName);

        //Check whether length constraints are fulfilled
        if(value.length < min_length || value.length > max_length){
            String msg = "[OpaqueOption] Value length for option number " + optionNumber + " must be between " +
                    min_length + " and " +  max_length + " but is " + value.length;
            throw new InvalidOptionException(optionNumber, msg);
        }

        //Set value if there was no Exception thrown so far
        this.value = value;
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
}
