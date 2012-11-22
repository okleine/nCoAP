package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;

import java.io.ByteArrayOutputStream;

/**
 *
 * @author Oliver Kleine
 */
public class EmptyOption extends Option{

    //Constructor with package visibility
    EmptyOption(OptionName opt_name) throws InvalidOptionException{
        super(opt_name);
    }

    static void encodeFencepostOptions (ByteArrayOutputStream baos, int number, int prevNumber){
        baos.write((byte)((number - prevNumber) << 4));
    }

    @Override
    public String getDecodedValue(){
        return "NONE";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EmptyOption)){
            return false;
        }

        EmptyOption opt = (EmptyOption) o;
        if(this.optionNumber == opt.optionNumber){
            return true;
        }
        else{
            return false;
        }
    }
}
