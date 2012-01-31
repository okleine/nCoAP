package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestMessageCreation extends TestCase{

    private static Logger log = Logger.getLogger("nCoap");
    static{
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.DEBUG);
    }

    MsgType msgType;
    Code code;
    URI targetURI;
    URI proxyURI;
    URI locationURI;
    MediaType contentType;
    int maxAge;
    byte[][] etags;
    byte[] token;
    MediaType[] accept;
    byte[][] ifMatch;
    boolean ifNonMatch;
    boolean valid;

    public TestMessageCreation(MsgType msgType, Code code, URI targetURI, URI proxyURI, URI locationURI,
                               MediaType contentType, int maxAge, byte[][] etags, byte[] token, MediaType[] accept,
                               byte[][] ifMatch, boolean ifNonMatch, boolean valid){

        this.msgType = msgType;
        this.code = code;
        this.targetURI = targetURI;
        if(!(proxyURI == null)){
            this.proxyURI = proxyURI;
        }
        if(!(locationURI == null)){
            this.locationURI = locationURI;
        }
        if(!(contentType == null)){
            this.contentType = contentType;
        }
        if(maxAge != 0){
            this.maxAge = maxAge;
        }
        if(!(etags == null)){
            this.etags = etags;
        }
        if(!(token == null)){
            this.token = token;
        }
        if(!(accept == null)){
            this.accept = accept;
        }
        if(!(ifMatch == null)){
            this.ifMatch = ifMatch;
        }
        this.ifNonMatch = ifNonMatch;
        this.valid = valid;
    }

    @Parameters
    public static Collection<Object[]> getParams(){
        return TestMessageCreationParameterFactory.getParams();
    }

    @Test
    public void testOptionCombination() throws Exception{
        Message message = new Request(msgType, code, targetURI);

        try{
            if(!(proxyURI == null)){
                message.setProxyURI(proxyURI);
            }

            if(!(locationURI == null)){
                message.setLocationURI(locationURI);
            }

            if(!(contentType == null)){
                message.setContentType(contentType);
            }

            if(!(maxAge == -1)){
                message.setMaxAge(maxAge);
            }

            if(!(etags == null)){
                message.setETAG(etags);
            }

            if(!(token == null)){
                message.setToken(token);
            }

            if(!(accept == null)){
                message.setAccept(accept);
            }

            if(!(ifMatch == null)){
                message.setIfMatch(ifMatch);
            }

            if(ifNonMatch){
                message.setIfNoneMatch();
            }

            assertCorrect(message);
        }
        catch(Exception e){
            if(!(e instanceof InvalidOptionException)){
                throw e;
            }
            if(valid){
                throw e;
            }
            if(log.isDebugEnabled()){
                log.debug("[TestMessageCreation] Expected InvalidOptionException!", e);
            }
        }
    }

    private void assertCorrect(Message message) throws Exception {
        switch(message.getHeader().getCode()){
            case GET: assertCorrectGET(message);


        }
    }

    private void assertCorrectGET(Message message) throws Exception{
        assertTargetUriEquals(message);
    }

    private void assertTargetUriEquals(Message message) throws Exception{
        URI actualURI = message.getTargetUri();

        //Compare schemes
        assertEquals(targetURI.getScheme(), actualURI.getScheme());

        //Compare host
        InetAddress actualHost = InetAddress.getByName(actualURI.getHost());
        InetAddress expectedHost = InetAddress.getByName(targetURI.getHost());

        assertEquals(expectedHost, actualHost);

        //Compare port
        int targetUriPort = targetURI.getPort();
        if(targetUriPort == -1){
            targetUriPort = OptionRegistry.COAP_PORT_DEFAULT;
        }
        assertEquals(targetUriPort, actualURI.getPort());

        //Compare paths
        assertEquals(targetURI.getRawPath(), actualURI.getRawPath());

        //Compare Query
        assertEquals(targetURI.getRawQuery(), actualURI.getRawQuery());
    }


}
