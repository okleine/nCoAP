//package de.uniluebeck.itm.spitfire.coap7.message.options;
//
//import com.google.common.primitives.Bytes;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
//import junit.framework.TestCase;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import org.junit.runners.Parameterized.Parameters;
//
//import java.nio.charset.Charset;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//
///**
//*
//* @author Oliver Kleine
//*/
//@RunWith(Parameterized.class)
//public class TestStringOption extends TestCase{
//
//    private static final Charset charset = Charset.forName("UTF-8");
//
//    private int optionNumber;
//    private String value;
//    private int maxValueLength;
//    private boolean valid;
//    private byte[] expectedEncodedOption;
//
//    @Parameters
//    public static Collection<Object[]> getParams(){
//        return TestStringOptionParameterFactory.getParams();
//    }
//
//    public TestStringOption(int optionNumber, String value, int maxValueLength,
//                            boolean valid, List<Byte> expectedEncodedOption){
//
//        this.optionNumber = optionNumber;
//        this.value = value;
//        this.maxValueLength = maxValueLength;
//        this.valid = valid;
//
//        if(expectedEncodedOption != null){
//            this.expectedEncodedOption = Bytes.toArray(expectedEncodedOption);
//        }
//    }
//
//    @Test
//    public void testCreationAsStringOption() throws Exception{
//        try{
//            //Create the StringOption instance
//            OptionName optionName = OptionRegistry.getOptionName(optionNumber);
//            StringOption opt = Option.createStringOption(optionName, value);
//
//            //Decode created StringOption
//            //This test assumes the previous option to have the number optionNumber - 1
//            ChannelBuffer buf = ChannelBuffers.dynamicBuffer(0);
//            opt.encode(buf, optionName, optionNumber - 1);
//            byte[] result = new byte[buf.readableBytes()];
//            buf.readBytes(result);
//
//            //Error message if encoded option is not equal to expected
//            String msg = "\n[TestStringOption] Encoding failed\n" +
//                    " Actual: " + Option.getHexString(result) +
//                    " Should: " + Option.getHexString(expectedEncodedOption);
//
//            assertTrue(msg, Arrays.equals(result, expectedEncodedOption));
//        }
//        catch(InvalidOptionException e){
//            if(valid == true){
//                fail(e.getMessage());
//            }
//        }
//    }
//
//    @Test
//    public void testGenericCreationWithDecodedValue() throws Exception{
//        try{
//            //Create StringOptionInstance
//            OptionName optionName = OptionRegistry.getOptionName(optionNumber);
//            StringOption option = Option.createStringOption(optionName, value);
//
//            //Decode created StringOption
//            //This test assumes the previous option to have the number optionNumber - 1
//            ChannelBuffer buf = ChannelBuffers.dynamicBuffer(0);
//            option.encode(buf, optionName, optionNumber - 1);
//            byte[] result = new byte[buf.readableBytes()];
//            buf.readBytes(result);
//
//            //Error message if encoded option is not equal to expected
//            String msg = "\n[StringOption] Encoding failed\n" +
//                    " Actual: " + Option.getHexString(result) +
//                    " Should: " + Option.getHexString(expectedEncodedOption);
//
//            assertTrue(msg, Arrays.equals(result, this.expectedEncodedOption));
//        }
//        catch(InvalidOptionException e){
//            if(valid == true){
//                fail(e.getMessage());
//            }
//        }
//    }
//
//
//    @Test
//    public void testGenericCreationFromEncodedOption() throws Exception{
//        try{
//            //Create StringOption instance from encoded
//            ChannelBuffer buf = ChannelBuffers.wrappedBuffer(expectedEncodedOption);
//            StringOption option = (StringOption)Option.createOption(buf, optionNumber - 1);
//
//            //Check whether decoded value equals expected
//            String msg = "\n[TestStringOption] Decoding failed\n" +
//                        " Actual value: " +  option.getValue() + "\n" +
//                        " Should value: " + value;
//
//            assertTrue(msg, value.equals(option.getValue()));
//
//            //Check whether decoded option number equals expected
//            msg = "\n[TestStringOption] Decoding failed\n" +
//                        " Actual option number: " +  option.getOptionNumber() + "\n" +
//                        " Expected option number: " + optionNumber;
//
//            assertTrue(msg, option.optionNumber == optionNumber);
//        }
//        catch(InvalidOptionException e){
//            if(valid == true){
//                fail(e.getMessage());
//            }
//        }
//    }
//
////    @Test
////    public void testURIResolution(){
////        //
////    }
//
//
//    //Method to be called when a InvalidOptionExcpetion has been thrown inexpectedly
//    private void testFailed(int opt_number, String value){
//        String msg = "\n[TestStringOption] Should have caused an InvalidOptionException, " +
//                    "value is too long for option number " + opt_number +
//                    " (Length: is: " + value.length() + ", max: " + maxValueLength + ")";
//        fail(msg);
//    }
//}
