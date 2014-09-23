package de.uniluebeck.itm.ncoap.examples;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapClientCallback;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.xml.DOMConfigurator;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Created by olli on 23.09.14.
 */
public class Main {

    private CoapClientApplication client;
    private Map<Integer, CoapClientCallback> callbacks;
    private CoapServerApplication server;

    private static void configureLogging() throws Exception{
        System.out.println("Use default logging configuration, i.e. INFO level...\n");
        URL url = Main.class.getClassLoader().getResource("log4j.default.xml");
        System.out.println("Use config file " + url);
        DOMConfigurator.configure(url);
    }


    public static void main(String[] args) throws Exception{
        configureLogging();

        Main main = new Main();
        main.client = new CoapClientApplication();
        main.server = new CoapServerApplication();
        InetSocketAddress serverSocket = new InetSocketAddress("localhost", main.server.getPort());

        for(int i = 0; i < 10; i++){
            PerformanceTestWebservice service = new PerformanceTestWebservice(i, 0, 2000);
            main.server.registerService(service);
            service.initialize();
        }

        for(int i = 0; i < 10; i++){
            URI uri = new URI("coap", null, "localhost", -1, "/service/" + String.format("%03d", i), null, null);
            CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, uri);
            coapRequest.setObserve(true);

            if(i%2 == 0){
                coapRequest.setAccept(ContentFormat.TEXT_PLAIN_UTF8);
            }
            else{
                coapRequest.setAccept(ContentFormat.APP_XML);
            }

            CoapClientCallback callback = new PerformanceTestClientCallback(i);

            main.client.sendCoapRequest(coapRequest, callback, serverSocket);
        }

        Thread.sleep(10000);

        main.client.shutdown();
        main.server.shutdown();
    }

}
