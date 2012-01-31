/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.spitfire.nCoap.message.options;

/**
 *
 * @author Oliver Kleine
 */
/*
public class OptionTest extends TestCase{

    private static final Logger log = Logger.getLogger("nCoap");
    static{
        BasicConfigurator.configure();
        log.setLevel(Level.OFF);
    }

    private static Random rand = new Random(System.currentTimeMillis());

    
    @BeforeClass
    public void setup(){
        rand = new Random(System.currentTimeMillis());
        
    }

    @Test
    public void testEncodeDeltaAndLength(){
        for(int i = 1; i < 10000; i++){
            try{
                //Create random number between 1 and 14 (both including) for option delta
                int opt_number;
                do{
                    opt_number = rand.nextInt(14) + 1;
                } while(opt_number == 10);

                //Create random but valid option specific value length of not greater than 14
                int min_length = OptionRegistry.getMinLength(opt_number);
                int max_length = OptionRegistry.getMaxLength(opt_number);
                int value_length = rand.nextInt(max_length + 1 - min_length) + min_length;

                //Expected result for delta and length encoding
                byte[] exp_result = new byte[]{(byte)((opt_number << 4))};
                if(value_length < 15){
                    exp_result[0] = (byte)(exp_result[0] | value_length);
                }
                else{
                    exp_result[0] = (byte)(exp_result[0] | 15);
                    exp_result = Bytes.concat(exp_result, new byte[]{(byte)(value_length - 15)});
                }


                //Create Option and encode its delta and length
                Option opt = Option.createOption(OptionRegistry.getOptionName(opt_number), new byte[value_length]);
                byte[] result = opt.encodeDeltaAndLength(0, value_length);

                String msg = "\n" +
                            "Expected result: " + Option.getHexString(exp_result) + "\n" +
                            "Result: " + Option.getHexString(result);

                assertTrue(msg, Arrays.equals(exp_result, result));
            }
            catch(Exception e){
                e.printStackTrace();
                fail("Unexpected Exception caught (see StackTrace)!");
            }
        }
    }



    @Test
    public void testEncodeUintOptions(){

        for(int i = 1; i < 10000; i++){
            try{
                //Chose option number randomly
                int[] uint_opts = new int[]{1, 2, 7, 12};
                int opt_number = uint_opts[rand.nextInt(4)];

                if

                //Previous option must be smaller or equal to current option number
                int prev_option = rand.nextInt(opt_number) + 1;

                //Create option specific random value

                int max_length = OptionRegistry.getMaxLength(opt_number) - 1;
                long value = rand.nextLong();
                switch(max_length){
                    case 1:
                        value = value >>> 56;
                        break;
                    case 2:
                        value = value >>> 48;
                        break;
                    case 3:
                        value = value >>> 40;
                        break;
                    case 4:
                        value = value >>> 32;
                        break;
                    default:
                        break;


                }
                

                //Ensure to have a value of zero at least once
                if(i == 1){
                    value = 0;
                }

                //Generate expected result with value zero
                byte[] expected_result = new byte[]{(byte)((2 - prev_option) << 4)};

                //If value is not zero its size must be one
                if(value > 0){
                    //Encode the value
                    byte[] encoded_value = Longs.toByteArray(value);
                    //Remove leading bytes of zero (remove left-side-padding)
                    while(encoded_value[0] == 0){
                        encoded_value = Arrays.copyOfRange(encoded_value, 1, encoded_value.length);
                        if(encoded_value.length == 0){
                            break;
                        }
                    }
                    expected_result[0] = (byte)(expected_result[0] | encoded_value.length);

                    //Generate expected result with value
                    expected_result = Bytes.concat(expected_result, encoded_value);
                }

                //Generate UintOption
                Option o = Option.createOption(OptionName.MAX_AGE, value);
                byte[] result = o.encode(prev_option);

                //Check wheter both results are equal
                String msg = "\n" +
                        "Expected result: " + Option.getHexString(expected_result) + "\n" +
                        "Result: " + Option.getHexString(result);

                assertTrue(msg, Arrays.equals(expected_result, result));
            }
            catch (Exception e){
                fail("Creation of option number 2 caused an unexpected Exception: \n" + e.getRequest());
            }
        }
    }

    @Test
    public void testEncodeOption3(){
        for(int i = 1; i < 100; i++){
            try{
                //Create random UTF-8 String of random size in the range of 1 to 270
                int value_length = rand.nextInt(270) + 1;
                char[] characters = new char[value_length];

                String value = String.copyValueOf(characters);

                //Previous option must be in the range 0 to 3 (since it may occur more than once)
                int prev_option = rand.nextInt(4);

                //Create expected result
                byte[] exp_result = new byte[]{(byte)((3 - prev_option) << 4)};
                
                if(value_length < 15){
                    exp_result[0] = (byte)(exp_result[0] | value_length);
                }
                else{
                    exp_result[0] = (byte)(exp_result[0] | 15);
                    exp_result = Bytes.concat(exp_result, new byte[]{(byte)(value_length - 15)});
                }

                exp_result = Bytes.concat(exp_result, utf8_bytes);

                //Create Option object
                Option o = Option.createOption(OptionName.PROXY_URI, new String(utf8_bytes));
                byte[] result = o.encode(prev_option);

                //Check wheter both results are equal
                String msg = "Value length is " + value_length + "\n" +
                        "Exp. result (" + exp_result.length + " ): " + Option.getHexString(exp_result) + "\n" +
                        "Actual result (" + result.length + "): " + Option.getHexString(result);

                assertTrue(msg, Arrays.equals(exp_result, result));
            }
            catch(Exception e){
                e.printStackTrace();
                fail("Creation of option number 3 caused an unexpected Exception.");
            }
        }
    }
}
 * 
 */
