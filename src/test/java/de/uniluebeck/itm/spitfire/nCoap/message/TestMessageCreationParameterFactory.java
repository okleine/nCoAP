package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

public class TestMessageCreationParameterFactory {

    private static Logger log = Logger.getLogger("nCoap");

    public static Collection<Object[]> getParams(){
        ArrayList<Object[]> params = new ArrayList<>();
        params.addAll(getValidOptionCombinations());
        params.addAll(getInvalidCriticalOptions());
        return params;
    }

    private static Collection<Object[]> getValidOptionCombinations(){
        ArrayList<Object[]> params = new ArrayList<>();

        try {
            URI targetURI = new URI("coap://[2001:638:70a:c004::c004]:5683/path1/path2?query1=1&query2=2");
            URI proxyURI = new URI("coap://proxy.itm.uni-luebeck.de:5683/path/to/proxy");

            //Combine all allowed options for a GET request
            params.add(new Object[]{
                    MsgType.CON,    //Message Type
                    Code.GET,       //Message Code
                    targetURI,      //Target URI
                    proxyURI,       //Proxy URI
                    null,           //LocationURI
                    null,           //Content Type
                    -1,           //Max age (-1 indicates option not to be set)
                    new byte[][]{new byte[]{1,2,3,4,5}, new byte[]{1,2,3,4,5,6,7,8}},   //Etags
                    new byte[]{3,3,3,3,3},                                              //Token
                    new MediaType[]{MediaType.APP_EXI, MediaType.APP_JSON},             //Accept
                    null,   //If-match
                    false,          //If-non-match
                    true            //Message is valid and creation should not cause an exception
            });

            return params;

        } catch (URISyntaxException e) {
           log.fatal("[TestMessageCreationParameterFactory] This should never happen.", e);
            return null;
        }
    }

    private static Collection<Object[]> getInvalidElectiveOptions(){
        ArrayList<Object[]> params = new ArrayList<>();
        return params;
    }

    private static Collection<Object[]> getInvalidCriticalOptions(){
        ArrayList<Object[]> params = new ArrayList<>();

        try {
            URI targetURI = new URI("coap://[2001:638:70a:c004::c004]:5683/path1/path2?query1=1&query2=2");
            URI proxyURI = new URI("coap://proxy.itm.uni-luebeck.de:5683/path/to/proxy");
            URI locationURI = new URI("/newly/created/resource");

            //Location URI option in a GET request
            params.add(new Object[]{
                    MsgType.CON, Code.GET, targetURI,
                    null, locationURI, null, -1, null, null, null, null, false,
                    false
            });

            //Content type option in a get request
            params.add(new Object[]{
                MsgType.CON, Code.GET, targetURI,
                    null, null, MediaType.APP_EXI, -1, null, null, null, null, false,
                    false
            });

            //

            return params;

        } catch (URISyntaxException e) {
           log.fatal("[TestMessageCreationParameterFactory] This should never happen.", e);
            return null;
        }
    }
}
