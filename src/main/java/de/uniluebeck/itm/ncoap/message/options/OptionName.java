package de.uniluebeck.itm.ncoap.message.options;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 31.10.13
 * Time: 15:05
 * To change this template use File | Settings | File Templates.
 */
public abstract class OptionName {

    //Name          Number      OptionType  MinLength   MaxLength
    public static final int IF_MATCH        = 1;
    public static final int URI_HOST        = 3;
    public static final int ETAG            = 4;
    public static final int IF_NONE_MATCH   = 5;
    public static final int URI_PORT        = 7;
    public static final int LOCATION_PATH   = 8;
    public static final int URI_PATH        = 11;
    public static final int CONTENT_FORMAT  = 12;
    public static final int MAX_AGE         = 14;
    public static final int URI_QUERY       = 15;
    public static final int ACCEPT          = 17;
    public static final int LOCATION_QUERY  = 20;
    public static final int PROXY_URI       = 35;
    public static final int PROXY_SCHEME    = 39;
    public static final int SIZE_1          = 60;
}
