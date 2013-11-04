package de.uniluebeck.itm.ncoap.message;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 04.11.13
 * Time: 13:09
 * To change this template use File | Settings | File Templates.
 */
public class UnknownOptionException extends Exception{

    private int optionNumber;

    public UnknownOptionException(int optionNumber){
        super("Unknown option number: " + optionNumber);
        this.optionNumber = optionNumber;
    }

    public int getOptionNumber() {
        return optionNumber;
    }
}
