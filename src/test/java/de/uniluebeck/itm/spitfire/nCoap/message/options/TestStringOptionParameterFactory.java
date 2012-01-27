package de.uniluebeck.itm.spitfire.nCoap.message.options;

import com.google.common.primitives.Bytes;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Oliver Kleine
 */
class TestStringOptionParameterFactory {

    private static Logger log = Logger.getLogger("nCoap");
    static{
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.DEBUG);
    }

    private static int[] string_options = new int[]{3, 5, 6, 8, 9, 15};

    static Collection<Object[]> getParams(){
         ArrayList<Object[]> params = new ArrayList<>();

         params.addAll(createProxyURITestCases());

        //URI-Host (no. 5) must not contain upper case letters and must replace non-ASCII characters with their
        // percent-encoding
        //TODO

        //URI-Path (no. 9) and URI-Query (no. 15) are critical and can contain any character
        //sequence but must not contain "." or ".."
        //TODO

        //Location-Path (no. 6) and Location-Query (no. 8) are elective and can contain any character
        //sequence but must not contain "." or ".."
        //TODO

      return params;
    }

    private static Collection<Object[]> createProxyURITestCases(){
        ArrayList<Object[]> params = new ArrayList<>();

        int optionNumber = 3;
        int maxValueLength = 270;

        //Absolute URI (smaller than 270 bytes)
        String value = "coap://example.org/";
        boolean valid = true;
        byte[] encodedOption = createEncodedOption(value);
        params.add(createObjectArray(optionNumber, value, maxValueLength, valid, encodedOption));

        //Relative URI
        value = "/relative/path";
        valid = false;
        params.add(new Object[]{optionNumber, value, maxValueLength, valid, null});




//        //Encoded Proxy URI must not contain any percent encoding
//        try {
//            uri = new URI("coap://[2a02:2e0:3fe:100::6]:5388/test%7E1/test2/../test3?q1=1&q2=2");
//
//        } catch (URISyntaxException e) {
//            log.fatal(e);
//            //This should never happen!
//        }

        return params;
    }

    private static byte[] createEncodedOption(String value) {
        byte[] encodedValue = value.getBytes(Charset.forName("UTF-8"));
        //option_delta is 1, length is 19 -> "OptionHeader": 00011111(=31) 00000100(=4)
        ByteBuffer buf = ByteBuffer.allocate(2 + encodedValue.length);
        buf.put(new byte[]{31, 4});
        buf.put(encodedValue);
        return buf.array();
    }

    private static Collection<Object[]> createURIHostTestCases(){
        return null;
    }



    //Creates an Object[] containing all parameters for a parameterlist element
    private static Object[] createObjectArray(int opt_number, String value, int max_length,
            boolean valid, byte[] encoded_option){

        ArrayList<Object> tmp = new ArrayList<>();
        //add option number to parameter list for constructor
        tmp.add(opt_number);

        //add value
        tmp.add(value);

        //add option number specific maximum value
        tmp.add(max_length);

//        //add encoded value
//        tmp.add(Bytes.asList(Arrays.copyOfRange(encoded_option, 1, encoded_option.length)));

        //add whether the option is valid or not
        tmp.add(valid);

        //add encoded option
        tmp.add(Bytes.asList(encoded_option));
        //tmp.add(encoded_option);

        return tmp.toArray();
    }
}
