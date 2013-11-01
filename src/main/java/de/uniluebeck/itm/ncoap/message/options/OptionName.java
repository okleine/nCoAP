package de.uniluebeck.itm.ncoap.message.options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.uniluebeck.itm.ncoap.message.options.OptionType.*;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 31.10.13
 * Time: 15:05
 * To change this template use File | Settings | File Templates.
 */
public enum OptionName {

    //Name          Number      OptionType  MinLength   MaxLength
    IF_MATCH        (1,         OPAQUE,     0,          8      ),
    URI_HOST        (3,         STRING,     1,          255    ),
    ETAG            (4,         OPAQUE,     1,          8      ),
    IF_NONE_MATCH   (5,         EMPTY,      0,          0      ),
    URI_PORT        (7,         UINT,       0,          2      ),
    LOCATION_PATH   (8,         STRING,     0,          255    ),
    URI_PATH        (11,        STRING,     0,          255    ),
    CONTENT_FORMAT  (12,        UINT,       0,          2      ),
    MAX_AGE         (14,        UINT,       0,          4      ),
    URI_QUERY       (15,        STRING,     0,          255    ),
    ACCEPT          (17,        UINT,       0,          2      ),
    LOCATION_QUERY  (20,        STRING,     0,          255    ),
    PROXY_URI       (35,        STRING,     1,          1034   ),
    PROXY_SCHEME    (39,        STRING,     1,          255    ),
    SIZE_1          (60,        UINT,       0,          4      );

    private static Logger log = LoggerFactory.getLogger(OptionName.class.getName());

    private int optionNumber;
    private OptionType optionType;
    private final int minLength;
    private final int maxLength;


    private OptionName(int optionNumber, OptionType optionType, int minLength, int maxLength){
        this.optionNumber = optionNumber;
        this.optionType = optionType;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * Returns the option number of corresponding to this option name
     * @return the option number of corresponding to this option name
     */
    public int getNumber(){
        return optionNumber;
    }

    /**
     * Returns <code>true</code> if the option is critical and <code>false</code> if the option is elective
     * @return <code>true</code> if the option is critical and <code>false</code> if the option is elective
     */
    public boolean isCritical(){
        return (optionNumber & 1) == 1;
    }

    /**
     * Returns <code>true</code> if the option is unsafe-to-forward and <code>false</code> if the option is
     * safe-to-forward by a proxy
     * @return <code>true</code> if the option is unsafe-to-forward and <code>false</code> if the option is
     * safe-to-forward by a proxy
     */
    public boolean isUnsafe(){
        return (optionNumber & 2) == 2;
    }

    public boolean isNoCacheKey(){
        return (optionNumber & 0x1e) == 0x1c;
    }

    /**
     * Returns the minimum length for options with this name in bytes
     * @return the minimum length for options with this name in bytes
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Returns the maximum length for options with this name in bytes
     * @return the maximum length for options with this name in bytes
     */
    public int getMaxLength() {
        return maxLength;
    }


    public static OptionName getOptionNameByNumber(int number){
        for(OptionName optionName : OptionName.values()){
            if(optionName.getNumber() == number)
                return optionName;
        }

        log.warn("No OptionName found for number {}.", number);
        return null;
    }

    public OptionType getOptionType() {
        return optionType;
    }
}
