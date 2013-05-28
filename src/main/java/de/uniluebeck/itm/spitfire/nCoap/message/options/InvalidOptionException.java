/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.communication.core.CoapException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Header;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;


/**
 *
 * @author Oliver Kleine
 */
public class InvalidOptionException extends CoapException {

    private Header messageHeader;
    private int optionNumber;

    public InvalidOptionException(Header header, int optionNumber, String msg){
        this(optionNumber, msg);
        this.messageHeader = header;
    }

    public InvalidOptionException(int optionNumber, String msg){
        super(msg);
        this.optionNumber = optionNumber;
    }

    /**
     * Returns true if this Exception has been caused by a critical option. Otherwise (in case of elective options)
     * it returns false.
     * @return whether the Exception was caused by a critical option
     */
    public boolean isCritical(){
       return OptionRegistry.isCritical(optionNumber);
    }

    /**
     * Returns the number of the option that caused the exception
     * @return the number of the option that caused the exception
     */
    public int getOptionNumber() {
        return optionNumber;
    }

    /**
     * Returns the OptionName of the option that caused this exception
     * @return the OptionName of the option that caused this exception
     */
    public OptionName getOptionName(){
        return OptionRegistry.getOptionName(optionNumber);
    }

    /**
     * Returns the {@link Header} instance that caused this exception (if available) or null otherwise
     * @return the {@link Header} instance that caused this exception (if available) or null otherwise
     */
    public Header getMessageHeader() {
        return messageHeader;
    }
}
