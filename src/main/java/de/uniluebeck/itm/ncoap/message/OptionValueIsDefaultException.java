package de.uniluebeck.itm.ncoap.message;

import java.math.BigInteger;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 05.11.13
 * Time: 16:02
 * To change this template use File | Settings | File Templates.
 */
public class OptionValueIsDefaultException extends Exception {


    private int optionNumber;
    private final byte[] givenValue;
    private final byte[] defaultValue;

    public OptionValueIsDefaultException(int optionNumber, byte[] givenValue, byte[] defaultValue){
        super("Can not create option no. " + optionNumber  + " with default value (given: " + givenValue +
                ", default: " + defaultValue + ")");
        this.optionNumber = optionNumber;
        this.givenValue = givenValue;
        this.defaultValue = defaultValue;
    }
    
    public String getGivenValueAsString() throws UnknownOptionException {
        return valueToString(this.optionNumber, this.givenValue);
    }

    public String getDefaultValueAsString() throws UnknownOptionException{
        return valueToString(this.optionNumber, this.defaultValue);
    }

    private static String valueToString(int optionNumber, byte[] value) throws UnknownOptionException {
        switch(Option.getOptionType(optionNumber)){
            case OptionType.STRING:
                return new String(value, CoapMessage.CHARSET);
            case OptionType.OPAQUE:
                if(value.length == 0)
                    return "<empty>";
                else
                    return new BigInteger(1, value).toString(16);
            case OptionType.UINT:
                return "" + new BigInteger(1, value).longValue();
            default:
                return "<empty>";
        }    
    }
}
