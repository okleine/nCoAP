package de.uniluebeck.itm.ncoap.message.options;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 03.12.13
 * Time: 16:15
 * To change this template use File | Settings | File Templates.
 */
public abstract class OptionException extends Exception {

    private int optionNumber;

    protected OptionException(int optionNumber, String message){
        super(message);
        this.optionNumber = optionNumber;
    }

    /**
     * Returns the number of the option that caused the exception
     * @return the number of the option that caused the exception
     */
    public int getOptionNumber(){
        return this.optionNumber;
    }

    /**
     * Returns true if this Exception has been caused by a critical option. Otherwise (in case of elective options)
     * it returns false.
     *
     * @return whether the Exception was caused by a critical option
     */
    public boolean isCritical(){
        return Option.isCritical(this.optionNumber);
    }
}
