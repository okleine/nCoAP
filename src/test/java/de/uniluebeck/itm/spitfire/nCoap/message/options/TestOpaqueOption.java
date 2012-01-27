package de.uniluebeck.itm.spitfire.nCoap.message.options;

import java.nio.charset.Charset;
import java.util.Collection;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Oliver Kleine
 */
public class TestOpaqueOption extends TestCase{

    private static final Charset charset = Charset.forName("UTF-8");

    private int opt_number;
    private byte[] value;
    private int max_length;
    private boolean valid;
    private byte[] expected_encoded_option;

    @Parameters
    public static Collection<Object[]> getParams(){
        //TODO
        return null;
    }

    //Test constructor
    @Test
    public void testObjectCreation1(){
        //TODO
    }



    //Test static object creator
    @Test
    public void testObjectCreation2(){
        //TODO
    }


    //Test static object creator
    @Test
    public void testObjectCreation3(){
        //TODO
    }


    //Method to be called when a InvalidOptionExcpetion has been thrown inexpectedly
    private void testFailed(int opt_number, String value){
        String msg = "\n[TestOpaqueOption] Should have caused an InvalidOptionException, " +
                    "value is too long for option number " + opt_number +
                    " (Length: is: " + value.length() + ", max: " + max_length + ")";
        fail(msg);
    }
}
