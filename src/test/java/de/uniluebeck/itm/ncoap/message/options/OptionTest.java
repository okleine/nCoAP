package de.uniluebeck.itm.ncoap.message.options;

import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.toolbox.ByteTestTools;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;
import static org.junit.Assert.fail;


/**
 * Test of Option class.
 * Tests for "URI" to "CoAP URI Options" and
 * "CoAP URI Options" to "URI" encoding.
 * @author Stefan Hueske
 */
public class OptionTest extends AbstractCoapTest{

    @Override
    public void setupLogging() throws Exception {
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    /**
     * Test of createTargetURIOptions method, of class Option.
     */
    @Test
    public void testCreateTargetURIOptions() throws Exception {
        List<UriToCoAPUriOptionsTest> list = new LinkedList<UriToCoAPUriOptionsTest>();
        
        list.add(new UriToCoAPUriOptionsTest("coap://[2001:db8::2:1]", 
                new Option[]{}));
        
        list.add(new UriToCoAPUriOptionsTest("coap://[2001:db8::2:1]:5555/testpath/",
                new Option[]{ //expected encoding
                    new UintOption(URI_PORT, 5555),
                    new StringOption(URI_PATH, "testpath")
                }));
        
        list.add(new UriToCoAPUriOptionsTest("coap://host.com:5555/testpath/test%5Dpath",
                new Option[]{ //expected encoding
                    new StringOption(URI_HOST, "host.com"),
                    new UintOption(URI_PORT, 5555),
                    new StringOption(URI_PATH, "testpath"),
                    new StringOption(URI_PATH, "test]path")
                }));
        
        list.add(new UriToCoAPUriOptionsTest("coaps://host.com/testpath/test%5Dpath?testq%5F",
                new Option[]{ //expected encoding
                    new StringOption(URI_HOST, "host.com"),
                    new StringOption(URI_PATH, "testpath"),
                    new StringOption(URI_PATH, "test]path"),
                    new StringOption(URI_QUERY, "testq_")
                }));        
        
        UriToCoAPUriOptionsTest.test(list, new TestRunner<Collection<Option>, URI>() {

            @Override
            Collection<Option> runTest(URI input) throws Exception {
                return Option.createTargetURIOptions(input);
            }
        });
    }

    /**
     * Test of createLocationUriOptions method, of class Option.
     */
    @Test
    public void testCreateLocationUriOptions() throws Exception {
        List<UriToCoAPUriOptionsTest> list = new LinkedList<UriToCoAPUriOptionsTest>();
        
        list.add(new UriToCoAPUriOptionsTest("/testpath/",
                new Option[]{ //expected encoding
                    new StringOption(LOCATION_PATH, "testpath")
                }));
        
        list.add(new UriToCoAPUriOptionsTest("/testpath/test%5Dpath",
                new Option[]{ //expected encoding
                    new StringOption(LOCATION_PATH, "testpath"),
                    new StringOption(LOCATION_PATH, "test]path")
                }));
        
        list.add(new UriToCoAPUriOptionsTest("testpath/test%5Dpath?testq%5F",
                new Option[]{ //expected encoding
                    new StringOption(LOCATION_PATH, "testpath"),
                    new StringOption(LOCATION_PATH, "test]path"),
                    new StringOption(LOCATION_QUERY, "testq_")
                }));        
        
        UriToCoAPUriOptionsTest.test(list, new TestRunner<Collection<Option>, URI>() {

            @Override
            Collection<Option> runTest(URI input) throws Exception {
                return Option.createLocationUriOptions(input);
            }
        });
    }
    
    /**
     * Tests the decoding of URI options to a URI object
     */
    @Test
    public void testCoAPTargetUriOptionsToUriEncoding() throws Exception {
        
        List<CoAPUriOptionsToUriTest> list = new LinkedList<CoAPUriOptionsToUriTest>();
        
        list.add(new CoAPUriOptionsToUriTest("coap://host.com:5555/testpath", //expected encoding
                new Option[]{ 
                    new StringOption(URI_HOST, "host.com"),
                    new UintOption(URI_PORT, 5555),
                    new StringOption(URI_PATH, "testpath")
                }));
        
        //TODO uncomment this when percent encoding is fixed
        //See issue #14 on https://github.com/okleine/nCoAP/issues/14
        
        list.add(new CoAPUriOptionsToUriTest("coap://host.com:5555/testpath/test%5Dpath", //expected encoding
                new Option[]{
                    new StringOption(URI_HOST, "host.com"),
                    new UintOption(URI_PORT, 5555),
                    new StringOption(URI_PATH, "testpath"),
                    new StringOption(URI_PATH, "test]path")
                }));

        list.add(new CoAPUriOptionsToUriTest("coap://host.com/testpath/test%5Dpath?testq_", //expected encoding
                new Option[]{
                    new StringOption(URI_HOST, "host.com"),
                    new StringOption(URI_PATH, "testpath"),
                    new StringOption(URI_PATH, "test]path"),
                    new StringOption(URI_QUERY, "testq_")
                }));
        
        CoAPUriOptionsToUriTest.test(list, new TestRunner<URI, Option[]>() {

            @Override
            URI runTest(Option[] input) throws Exception {
                Header header = new Header(Code.GET);
                OptionList optionList = new OptionList();
                for (Option option : input) {
                    optionList.addOption(Code.GET, OptionName.getByNumber(option.optionNumber), option);
                }
                InetAddress inetAddress = InetAddress.getLocalHost();
                CoapRequest coapRequest = new CoapRequest(header, optionList, ChannelBuffers.EMPTY_BUFFER);
                coapRequest.setRcptAdress(inetAddress);
                return coapRequest.getTargetUri();
            }
        });
    }



    static class UriToCoAPUriOptionsTest {
        URI inputUri;
        Option[] expectedOptions;

        public UriToCoAPUriOptionsTest(String inputUriAsString, Option[] expectedOptions) throws URISyntaxException {
            this.inputUri = new URI(inputUriAsString);
            this.expectedOptions = expectedOptions;
        }
        
        public static void test(List<UriToCoAPUriOptionsTest> tests,
                TestRunner<Collection<Option>, URI> testRunner) throws Exception {
            for (UriToCoAPUriOptionsTest test : tests) {
                Collection<Option> encodedOptions = testRunner.runTest(test.inputUri);
                if (!encodedOptions.equals(Arrays.asList(test.expectedOptions))) {
                    fail(String.format("\nEncoding of \"%s\" failed.\nExpected options:\n%s\n"
                            + "Encoded options:\n%s", test.inputUri.toString(),
                            optionListToString(Arrays.asList(test.expectedOptions)),
                            optionListToString(encodedOptions)));
                }
            }
        }
    }
    
    static abstract class TestRunner<ResultType, InputType> {
        abstract ResultType runTest(InputType input) throws Exception;
    }
    
    static class CoAPUriOptionsToUriTest {
        URI expectedUri;
        Option[] inputOptions;

        public CoAPUriOptionsToUriTest(String expectedUriAsString, Option[] inputOptions) 
                throws URISyntaxException {
            this.expectedUri = new URI(expectedUriAsString);
            this.inputOptions = inputOptions;
        }
        
        public static void test(List<CoAPUriOptionsToUriTest> tests,
                TestRunner<URI, Option[]> testRunner) throws Exception {
            for (CoAPUriOptionsToUriTest test : tests) {
                URI encodedUri = testRunner.runTest(test.inputOptions);
                if (!encodedUri.equals(test.expectedUri)) {
                    fail(String.format("Encoding of CoAP options \n%s failed.\nExpected URI: %s\n"
                            + "Encoded URI:  %s", optionListToString(Arrays.asList(test.inputOptions)),
                            test.expectedUri,
                            encodedUri));
                }
            }
        }
    }
    
    public static String optionListToString(Collection<Option> list) throws InvalidOptionException {
        StringBuilder res = new StringBuilder();
        for (Option option : list) {
            res.append(String.format("%-15s = %s\n",
                    OptionName.getByNumber(option.optionNumber).toString(), getOptionValueAsString(option)));
        }
        return res.toString();
    }
    
    public static String getOptionValueAsString(Option option) {
        //needed because .toString() implementations are missing
        if (option instanceof UintOption) {
            return String.valueOf(((UintOption) option).getDecodedValue());
        } else if (option instanceof StringOption) {
            return String.valueOf(((StringOption) option).getDecodedValue());
        } else if (option instanceof EmptyOption) {
            return "(empty)";
        } else if (option instanceof OpaqueOption) {
            return "0x" + ByteTestTools.getBytesAsString(((OpaqueOption) option).getValue());
        }
        throw new InternalError("Is there a new option? Please add it here.");
    }
}