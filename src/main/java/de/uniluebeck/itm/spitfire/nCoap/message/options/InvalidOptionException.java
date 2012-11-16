/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;

import javax.annotation.Nullable;

/**
 *
 * @author Oliver Kleine
 */
public class InvalidOptionException extends Exception{

    @Nullable
    private Header messageHeader;
    private OptionName optionName;
    private boolean critical;

    public InvalidOptionException(Header header, OptionName optionName, String msg){
        this(optionName, msg);
        this.messageHeader = header;
    }

    public InvalidOptionException(OptionName optionName, String msg){
        super(msg);
        this.optionName = optionName;
        this.critical = OptionRegistry.isCritial(optionName);
    }

     public InvalidOptionException(int optNumber, String msg){
        super(msg);
        this.critical = OptionRegistry.isCritical(optNumber);
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

    /**
     * Returns the {@link Header} instance that caused this exception (if available) or null otherwise
     * @return the {@link Header} instance that caused this exception (if available) or null otherwise
     */
    @Nullable
    public Header getMessageHeader() {
        return messageHeader;
    }
}
