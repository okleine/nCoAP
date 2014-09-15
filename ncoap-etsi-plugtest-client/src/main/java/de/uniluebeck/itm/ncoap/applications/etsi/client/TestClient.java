package de.uniluebeck.itm.ncoap.applications.etsi.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by olli on 15.09.14.
 */
public class TestClient extends CoapClientApplication{

    private static Logger log = Logger.getLogger(TestClient.class.getName());
    private final Object monitor = new Object();

    private void core01(String serverName) throws Exception{
        URI targetUri = new URI("coap", null, serverName, -1, "/test", null, null);
        final CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetUri);

        CoapResponseProcessor core01ResponseProcessor = new CoapResponseProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                synchronized (monitor){
                    try{
                        log.info("****** TD_COAP_CORE_01 ******");
                        log.info("Received Response: " + coapResponse);

                        //Check for correct response code
                        checkMessageCode(MessageCode.Name.CONTENT_205, coapResponse.getMessageCodeName());

                        //Check message ID
                        checkMessageID(coapRequest.getMessageID(), coapResponse.getMessageID());

                        //Check token
                        checkToken(coapRequest.getToken(), coapResponse.getToken());

                        //Check payload length (must be >0)
                        if(coapResponse.getContent().readableBytes() < 1){
                            throw new Exception("Response did not contain any payload!");
                        }

                        //Check content format option (must be set)
                        long contentFormat = coapResponse.getContentFormat();
                        if(contentFormat == ContentFormat.UNDEFINED){
                            throw new Exception("Response did not contain content format option!");
                        }

                        log.info("Test \"TD_COAP_CORE_01\" successfully passed!\n\n");
                    }

                    catch(Exception ex){
                        log.error("Test \"TD_COAP_CORE_01\" failed: " + ex.getMessage() + "\n\n");
                    }
                }
            }
        };

        InetAddress serverAddress = InetAddress.getByName(serverName);
        this.sendCoapRequest(coapRequest, core01ResponseProcessor, new InetSocketAddress(serverAddress, 5683));
    }


    private void core02(String serverName) throws Exception{
        URI targetUri = new URI("coap", null, serverName, -1, "/test", null, null);
        final CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.DELETE, targetUri);

        CoapResponseProcessor core01ResponseProcessor = new CoapResponseProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                synchronized (monitor){
                    try{
                        log.info("****** TD_COAP_CORE_02 ******");
                        log.info("Received Response: " + coapResponse);
                        //Check for correct response code
                        checkMessageCode(MessageCode.Name.DELETED_202, coapResponse.getMessageCodeName());

                        //Check message ID
                        checkMessageID(coapRequest.getMessageID(), coapResponse.getMessageID());

                        //Check token
                        checkToken(coapRequest.getToken(), coapResponse.getToken());

                        //Check content format option (if there is payload)
                        if(coapResponse.getContent().readableBytes() > 0){
                            long contentFormat = coapResponse.getContentFormat();
                            if(contentFormat == ContentFormat.UNDEFINED){
                                throw new Exception("Response has payload but no content format option!");
                            }
                        }

                        log.info("Test \"TD_COAP_CORE_02\" successfully passed!\n\n");
                    }

                    catch(Exception ex){
                        log.error("Test \"TD_COAP_CORE_02\" failed: " + ex.getMessage() + "\n\n");
                    }
                }
            }
        };

        InetAddress serverAddress = InetAddress.getByName(serverName);
        this.sendCoapRequest(coapRequest, core01ResponseProcessor, new InetSocketAddress(serverAddress, 5683));
    }


    private void core03(String serverName) throws Exception{
        URI targetUri = new URI("coap", null, serverName, -1, "/test", null, null);
        final CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.PUT, targetUri);
        coapRequest.setContent("Arbitrary payload...".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        CoapResponseProcessor responseProcessor = new CoapResponseProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                synchronized (monitor){
                    try{
                        log.info("****** TD_COAP_CORE_03 ******");
                        log.info("Received Response: " + coapResponse);
                        //Check for correct response code
                        try{
                            checkMessageCode(MessageCode.Name.CHANGED_204, coapResponse.getMessageCodeName());
                        }
                        catch(Exception ex){
                            checkMessageCode(MessageCode.Name.CREATED_201, coapResponse.getMessageCodeName());
                        }

                        //Check message ID
                        checkMessageID(coapRequest.getMessageID(), coapResponse.getMessageID());

                        //Check token
                        checkToken(coapRequest.getToken(), coapResponse.getToken());

                        //Check content format option (if there is payload)
                        if(coapResponse.getContent().readableBytes() > 0){
                            long contentFormat = coapResponse.getContentFormat();
                            if(contentFormat == ContentFormat.UNDEFINED){
                                throw new Exception("Response has payload but no content format option!");
                            }
                        }

                        log.info("Test \"TD_COAP_CORE_03\" successfully passed!\n\n");
                    }

                    catch(Exception ex){
                        log.error("Test \"TD_COAP_CORE_03\" failed: " + ex.getMessage() + "\n\n");
                    }
                }
            }
        };

        InetAddress serverAddress = InetAddress.getByName(serverName);
        this.sendCoapRequest(coapRequest, responseProcessor, new InetSocketAddress(serverAddress, 5683));
    }


    private void core04(String serverName) throws Exception{
        URI targetUri = new URI("coap", null, serverName, -1, "/test", null, null);
        final CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.POST, targetUri);
        coapRequest.setContent("Arbitrary payload...".getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

        CoapResponseProcessor responseProcessor = new CoapResponseProcessor() {
            @Override
            public void processCoapResponse(CoapResponse coapResponse) {
                synchronized (monitor){
                    try{
                        log.info("****** TD_COAP_CORE_04 ******");
                        log.info("Received Response: " + coapResponse);
                        //Check for correct response code
                        try{
                            checkMessageCode(MessageCode.Name.CHANGED_204, coapResponse.getMessageCodeName());
                        }
                        catch(Exception ex){
                            checkMessageCode(MessageCode.Name.CREATED_201, coapResponse.getMessageCodeName());
                        }

                        //Check message ID
                        checkMessageID(coapRequest.getMessageID(), coapResponse.getMessageID());

                        //Check token
                        checkToken(coapRequest.getToken(), coapResponse.getToken());

                        //Check content format option (if there is payload)
                        if(coapResponse.getContent().readableBytes() > 0){
                            long contentFormat = coapResponse.getContentFormat();
                            if(contentFormat == ContentFormat.UNDEFINED){
                                throw new Exception("Response has payload but no content format option!");
                            }
                        }

                        log.info("Test \"TD_COAP_CORE_04\" successfully passed!\n\n");
                    }

                    catch(Exception ex){
                        log.error("Test \"TD_COAP_CORE_04\" failed: " + ex.getMessage() + "\n\n");
                    }
                }
            }
        };

        InetAddress serverAddress = InetAddress.getByName(serverName);
        this.sendCoapRequest(coapRequest, responseProcessor, new InetSocketAddress(serverAddress, 5683));
    }


    private void checkMessageCode(MessageCode.Name expected, MessageCode.Name actual) throws Exception{
        String text = "Message Code (expected: " + expected + ", actual: " + actual + ")";
        log.info(text);
        if(actual != expected){
            throw new Exception(text);
        }    
    }


    private void checkMessageID(int expected, int actual) throws Exception {
        String text = "Message ID (expected: " + expected + ", actual: " + actual + ")";
        log.info(text);
        if(actual != expected){
            throw new Exception(text);
        }
    }


    private void checkToken(Token expected, Token actual) throws Exception {
        String text = "Token (expected: " + expected + ", actual: " + actual + ")";
        log.info(text);
        if(!actual.equals(expected)){
            throw new Exception(text);
        }
    }


    public static void configureDefaultLogging() throws Exception{
        System.out.println("Use default logging configuration, i.e. INFO level...\n");
        URL url = TestClient.class.getClassLoader().getResource("log4j.default.xml");
        System.out.println("Use config file " + url);
        DOMConfigurator.configure(url);
    }


    public static void main(String args[]) throws Exception{
        configureDefaultLogging();

        TestClient client = new TestClient();
        String serverName = "coap.me";
//        String serverName = "vs0.inf.ethz.ch";
        for(int i = 0; i < 1; i++){
            //Test CORE 01
            client.core01(serverName);

            //Test CORE 02
            client.core02(serverName);

            //Test CORE 03
            client.core03(serverName);

            //Test CORE 04
            client.core04(serverName);
        }
    }

}
