///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package de.uniluebeck.itm.spitfire.nCoap.message.options;
//
//import com.google.common.primitives.Bytes;
//import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
//import junit.framework.TestCase;
//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.SimpleLayout;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBuffers;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import org.junit.runners.Parameterized.Parameters;
//
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//
///**
// *
// * @author Oliver Kleine
// */
//@RunWith(Parameterized.class)
//public class TestUintOption extends TestCase{
//    private static Logger log = Logger.getLogger("nCoap");
//    static{
//        log.addAppender(new ConsoleAppender(new SimpleLayout()));
//        log.setLevel(Level.DEBUG);
//    }
//
//    private int optionNumber;
//    private long value;
//    private long maxAllowedValue;
//    private boolean valid;
//    private byte[] encodedValue;
//    private byte[] expectedEncodedOption;
//
//    @Parameters
//    public static Collection<Object[]> getParams(){
//        return TestUintOptionParameterFactory.getParams();
//    }
//
//
//    public TestUintOption(int optionNumber, long value, long maxAllowedValue,
//                List<Byte> encoded_value, List<Byte> encoded_option, boolean valid){
//        this.optionNumber = optionNumber;
//        this.value = value;
//        this.maxAllowedValue = maxAllowedValue;
//
//        if(encoded_value != null){
//            this.encodedValue = Bytes.toArray(encoded_value);
//        }
//
//        if(encoded_option != null){
//            this.expectedEncodedOption = Bytes.toArray(encoded_option);
//        }
//        this.valid = valid;
//    }
//
//
//    //Test constructor
//    @Test
//    public void testCreationAsUintOption() throws Exception{
//        //Since encoded_values are treated as unsigned, this test does only make sense with not negative values
//        if(value > -1){
//            try{
//                //create new UintOption
//                OptionName optionName = OptionRegistry.getOptionName(optionNumber);
//                UintOption opt = Option.createUintOption(optionName, value);
//
//                //encode created UintOption
//                ChannelBuffer buf = ChannelBuffers.dynamicBuffer(0);
//                opt.encode(buf, optionName, optionNumber - 1);
//                byte[] result = new byte[buf.readableBytes()];
//                buf.readBytes(result);
//
//                //Error message in case encoded option is not as expected
//                String msg = "\n[TestUintOption] Encoding failed\n" +
//                        " Actual: " + Option.getHexString(result) +
//                        " Should: " + Option.getHexString(expectedEncodedOption);
//
//                assertTrue(msg, Arrays.equals(result, expectedEncodedOption));
//            }
//            catch(InvalidOptionException e){
//                if(valid == true){
//                    fail(e.getRequest());
//                }
//            }
//        }
//    }
//
//    //Test static object creator
//    @Test
//    public void testGenericCreationWithEncodedValue() throws Exception{
//        //Since encoded_values are treated as unsigned, this test does only make sense with not negative values
//        if(value > -1){
//            try{
//                //create new UintOption
//                OptionName optionName = OptionRegistry.getOptionName(optionNumber);
//                UintOption opt = (UintOption)Option.createOption(optionName, encodedValue);
//
//                //Encode created UintOption
//                ChannelBuffer buf = ChannelBuffers.dynamicBuffer(0);
//                opt.encode(buf, optionName, optionNumber - 1);
//                byte[] result = new byte[buf.readableBytes()];
//                buf.readBytes(result);
//
//                //Error message if encoded option and expected are not equal
//                String msg = "\n[TestUintOption] Encoding failed\n" +
//                        " Actual: " + Option.getHexString(result) +
//                        " Should: " + Option.getHexString(expectedEncodedOption);
//
//                assertTrue(msg, Arrays.equals(result, expectedEncodedOption));
//            }
//            catch(InvalidOptionException e){
//                if(valid == true){
//                    String msg = "[TestUintOption] Unexpected InvalidOptionException thrown: \n" + e.getRequest();
//                    fail(msg);
//                }
//            }
//        }
//    }
//
//    @Test
//    public void testGenericCreationFromEncodedOption() throws Exception{
//        if(!(encodedValue == null)){
//            try{
//                //Create StringOption instance from encoded
//                ChannelBuffer buf = ChannelBuffers.wrappedBuffer(expectedEncodedOption);
//                UintOption option = (UintOption)Option.createOption(buf, optionNumber - 1);
//
//                //Check whether decoded value equals expected
//                String msg = "\n[TestUintOption] Decoding failed\n" +
//                            " Actual value: " +  option.getValue() + "\n" +
//                            " Should value: " + value;
//
//                assertTrue(msg, value == option.getValue());
//
//                //Check whether decoded option number equals expected
//                msg = "\n[TestUintOption] Decoding failed\n" +
//                            " Actual option number: " +  option.getOptionNumber() + "\n" +
//                            " Expected option number: " + optionNumber;
//
//                assertTrue(msg, option.getOptionNumber() == optionNumber);
//            }
//            catch(InvalidOptionException e){
//                if(valid == true){
//                    String msg = "[TestUintOption] Unexpected InvalidOptionException thrown: \n" + e.getRequest();
//                    fail(msg);
//                }
//            }
//        }
//    }
//}
