/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.message.options;

import com.google.common.primitives.Bytes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author Oliver Kleine
 */
class TestUintOptionParameterFactory {

    private static int[] uintOptionNumbers = new int[]{1, 2, 7, 12};

    static Collection<Object[]> getParams(){
        ArrayList<Object[]> params = new ArrayList<>();

        for(int optionNumber : uintOptionNumbers){
            params.addAll(TestUintOptionParameterFactory.getParamsForNegativeValue(optionNumber));
            params.addAll(TestUintOptionParameterFactory.getParamsForTooLargeValue(optionNumber));
            params.addAll(TestUintOptionParameterFactory.getParamsForValidValues(optionNumber));
        }

        return params;
    }

    /*
     * Creates option test parameters with negative value (4 parameter sets)
     */
    static Collection<Object[]> getParamsForNegativeValue(int opt_number){
        ArrayList<Object[]> params = new ArrayList<>();

        long value = -1;
        long max_value;
        boolean isValid = false;


        if(opt_number == 2){
            //maximum value for option number 2
            max_value = (long)Math.pow(2, 32) - 1;
        }
        else{
            //maximum value for option numbers 1, 7, and 12
            max_value = (long)Math.pow(2, 16) - 1;
        }

        params.add(new Object[]{opt_number, value, max_value, null, null, isValid});

        //return the collection containing only one Object[]
        return params;
    }

    /*
     * Creates option parameters for each option with a too large value (4 parameter sets)
     */
    static Collection<Object[]> getParamsForTooLargeValue(int opt_number){
        ArrayList<Object[]> params = new ArrayList<>();

        long value;
        long max_value;
        byte[] encoded_option;
        boolean isValid = false;

        switch(opt_number){
            case 1:
            case 7:
            case 12:
                max_value = (long)Math.pow(2, 16) - 1;
                value = max_value + 1;

                //option_delta is 1, length is 3 -> power(2,4) +  power(2,1) + power(2,0) = 19 = 00010011
                //encoded value treated as unsigned = 00000001 00000000 00000000
                encoded_option = new byte[]{19, 1, 0, 0};
                break;

            case 2:
                max_value = (long)Math.pow(2, 32) - 1;
                value = max_value + 1;

                //option_delta is 1, length is 5 -> power(2,4) +  power(2,2) + power(2,0) = 21 = 00010101
                //encoded value treated as unsigned = 00000001 00000000 00000000 00000000 00000000
                encoded_option = new byte[]{21, 1, 0, 0, 0, 0};
                break;

            default:
                //This will never happen but the compiler cannot know...
                value = 0;
                max_value = 0;
                encoded_option = new byte[0];
                break;
        }

        //Add to param list
        params.add(createObjectArray(opt_number, value, max_value, encoded_option, isValid));

        return params;
    }


    /*
     * Creates a set of valid option parameters
     * Options 1, 7, 12: 5 each
     * Option 2: 9
     * Sum: 3 * 5 + 9 = 24
     */
    static Collection<Object[]> getParamsForValidValues(int opt_number){
        ArrayList<Object[]> params = new ArrayList<>();

        long value;
        long max_value;

        byte[] encoded_option;

        if(opt_number == 2){
            max_value = (long)Math.pow(2, 32) - 1;

            //largest value with encoded length 4 -> power(2, 32)-1 = 11111111 11111111 11111111 11111111
            value = (long)Math.pow(2, 32) - 1;
            //option_delta is 1, length is 4 -> 00010100 = 20
            encoded_option = new byte[]{20, -1, -1, -1, -1};
            params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

            //smallest value with encoded length 4 -> power(2, 24) = 00000001 00000000 00000000 00000000
            value = (long)Math.pow(2, 24);
            encoded_option = new byte[]{20, 1, 0, 0, 0};
            params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

            //largest value with length 3 -> power(2, 24)-1 = 11111111 11111111 11111111
            value = (long)Math.pow(2, 24) - 1;
            //option_delta is 1, length is 3 -> 00010011 = 19
            encoded_option = new byte[]{19, -1, -1, -1};
            params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

            //smallest value with length 3 -> 65536 = 00000001 00000000 00000000
            value = 65536;
            encoded_option = new byte[]{19, 1, 0, 0};
            params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));
        }

        max_value = (long)Math.pow(2, 16) - 1;
        //largest value with encoded length 2 -> 65535 = 11111111 11111111
        value = 65535;
        //option_delta is 1, length is 2 -> 00010010 = 18;
        encoded_option = new byte[]{18, -1, -1};
        params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

        //smallest value with length 2 -> 256 = 00000001 00000000
        value = 256;
        encoded_option = new byte[]{18, 1, 0};
        params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

        //largest value with length 1 -> 255 = 11111111
        value = 255;
        //option_delta is 1, length is 1 -> 00010001 = 17
        encoded_option = new byte[]{17, -1};
        params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

        //smallest value with length 1
        value = 1;
        encoded_option = new byte[]{17, 1};
        params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

        //value with encoded length of zero
        value = 0;
        //option_delta is 1, length is 0 -> power(2,4) = 16 = 00010000
        encoded_option = new byte[]{16};
        params.add(createObjectArray(opt_number, value, max_value, encoded_option, true));

        return params;
    }

    //Creates an Object[] containing all parameters for a parameterlist element
    private static Object[] createObjectArray(int opt_number, long value, long max_value,
            byte[] encoded_option, boolean valid){

        ArrayList<Object> tmp = new ArrayList<>();
        //add option number to parameter list for constructor
        tmp.add(opt_number);

        //add value
        tmp.add(value);

        //add option number specific maximum value
        tmp.add(max_value);

        //add encoded value
        tmp.add(Bytes.asList(Arrays.copyOfRange(encoded_option, 1, encoded_option.length)));

        //add encoded option
        tmp.add(Bytes.asList(encoded_option));

        //add whether the option is valid or not
        tmp.add(valid);

        return tmp.toArray();
    }
}
