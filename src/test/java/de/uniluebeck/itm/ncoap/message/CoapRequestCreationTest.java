package de.uniluebeck.itm.ncoap.message;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 10.11.13
 * Time: 23:34
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Parameterized.class)
public class CoapRequestCreationTest extends AbstractCoapTest{

    @Parameterized.Parameters(name = "[{index}] Message Type: {0}, Message Code: {1}")
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                new Object[]{MessageType.Name.CON,
                             MessageCode.Name.GET,
                             new URI("coap", null, "www.example.org", -1, "/path/to/service", null, null),
                             null,
                             12345,
                             new byte[]{1, 2, 3, 4, 5, 6, 7, 8}},

                new Object[]{MessageType.Name.CON,
                             MessageCode.Name.GET,
                             new URI("coap", null, "www.example.org", -1, "/path/to/service", null, null),
                             InetAddress.getByName("2001:1:2:3:4:5:6:7"),
                             65535,
                             new byte[0]}
        );
    }

    private Logger log = Logger.getLogger(this.getClass().getName());

    private final MessageType.Name messageType;
    private final MessageCode.Name messageCode;
    private final URI targetUri;
    private final InetAddress proxyAddress;
    private final int messageID;
    private final byte[] token;
    private CoapRequest coapRequest;

    public CoapRequestCreationTest(MessageType.Name messageType, MessageCode.Name messageCode, URI targetUri,
                                   InetAddress proxyAddress, int messageID, byte[] token) throws Exception {

        this.messageType = messageType;
        this.messageCode = messageCode;
        this.targetUri = targetUri;
        this.proxyAddress = proxyAddress;
        this.messageID = messageID;
        this.token = token;

        log.debug("Create CoAP Request: (Type) " + messageType + ", (Code) " + messageCode);
        log.debug("Create CoAP Request: (Type) " + messageType.getNumber() + ", (Code) " + messageCode.getNumber());
        this.coapRequest = new CoapRequest(messageType, messageCode, targetUri);


    }

    @Test
    public void testMessageTypeName(){
        assertEquals("Message types do not match, ", coapRequest.getMessageTypeName(), messageType);
    }

    @Test
    public void testMessageType(){
        assertEquals("Message type numbers do not match, ", coapRequest.getMessageType(), messageType.getNumber());
    }

    @After
    public void justWaitSomeTimeToCompleteLogging() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Override
    public void setupLogging() throws Exception {
        Logger.getLogger("de.uniluebeck.itm.ncoap.message").setLevel(Level.DEBUG);
    }
}
