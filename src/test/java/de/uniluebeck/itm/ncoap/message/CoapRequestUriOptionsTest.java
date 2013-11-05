package de.uniluebeck.itm.ncoap.message;

import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 05.11.13
 * Time: 14:10
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Parameterized.class)
public class CoapRequestUriOptionsTest extends AbstractCoapTest{

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception{
        return Arrays.asList(new Object[][] {
                {new URI("coap", null, "[2001:638::1234]", -1, "/path/to/service", "param1=1234&param2=test2", null)}
        });
    }

    @Parameterized.Parameter(value = 0)
    public URI uri;

//    @Parameterized.Parameter(value = 1)
//    public String uriHost;
//
//    @Parameterized.Parameter(value = 2)
//    public int uriPort;
//
//    @Parameterized.Parameter(value = 3)
//    public String uriPath;
//
//    @Parameterized.Parameter(value = 4)
//    public String uriQuery;


    @Override
    public void setupLogging() throws Exception {

        Logger.getLogger("de.uniluebeck.itm.ncoap.message").setLevel(Level.DEBUG);

    }


    @Test
    public void testUriHostOption() throws Exception{

        //URI targetURI = new URI(uriScheme, null, uriHost, uriPort, uriPath, uriQuery, null);
        CoapRequest coapRequest = new CoapRequest(MessageTypeNames.CON, MessageCodeNames.GET, uri);
        assertEquals(uri.getHost(), coapRequest.getUriHost());


//        assertEquals(uri.getHost(),
//                ((StringOption) coapRequest.getOptions(OptionName.URI_HOST).iterator().next()).getDecodedValue());

//        assertEquals(Option.URI_PORT_DEFAULT, coapRequest.getUriPort());
//        assertEquals(uri.getPath(), coapRequest.getUriPath());
//        assertEquals(uri.getQuery(), coapRequest.getUriQuery());
//
//        assertTrue(coapRequest.getOptions(OptionName.URI_PORT).isEmpty());

    }


}
