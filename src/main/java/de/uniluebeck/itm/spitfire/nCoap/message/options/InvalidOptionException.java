/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;

/**
 *
 * @author Oliver Kleine
 */
public class InvalidOptionException extends Exception{

    private OptionName optionName;
    private boolean critical;


    public InvalidOptionException(OptionName optionName, String msg){
        super(msg);
        this.optionName = optionName;
        this.critical = OptionRegistry.isCritial(optionName);
    }

     public InvalidOptionException(int opt_number, String msg){
        super(msg);
        this.critical = OptionRegistry.isCritical(opt_number);
    }

    /**
     * Returns true if this Exception has been caused by a critical option. Otherwise (in case of elective options)
     * it returns false.
     * @return whether the Exception was caused by a critical option
     */
    public boolean isCritical(){
       return critical;
    }

    /**
     * Returns the OptionName of the option that caused this exception
     * @return the OptionName of the option that caused this exception
     */
    public OptionName getOptionName(){
        return optionName;
    }
}
