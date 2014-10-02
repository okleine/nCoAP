package de.uniluebeck.itm.ncoap.examples;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.xml.DOMConfigurator;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by olli on 23.09.14.
 */
public class Main {

    private static final int PARALLELITY = 200;
    private static final int DURATION = 20;

    private CoapClientApplication client;
    private Map<Integer, PerformanceTestClientCallback> callbacks;
    private CoapServerApplication server;

    private static void configureLogging() throws Exception{
        System.out.println("Use default logging configuration, i.e. INFO level...\n");
        URL url = Main.class.getClassLoader().getResource("log4j.default.xml");
        System.out.println("Use config file " + url);
        DOMConfigurator.configure(url);
    }


    public Main(){
        this.callbacks = new HashMap<>();
        this.client = new CoapClientApplication();
        this.server = new CoapServerApplication();
    }


    private void checkNotifications(){
        for(int i = 0; i < PARALLELITY; i++){
            PerformanceTestClientCallback callback = this.callbacks.get(i);
            int notificationCount = callback.getUpdateNotifications().values().size();
            System.out.println("Callback #" + String.format("%03d", i) + " received " +
                    notificationCount + " notifications:");

            System.out.println("First: " + callback.getFirstStatus());
            System.out.println("Last: " + callback.getLastStatus());

            Set<Integer> missing = callback.getMissingStates();
            System.out.print("Missing (" + missing.size() + "): ");
            for(int status : callback.getMissingStates()){
                System.out.print(status + ", ");
            }
            System.out.println("\n");

        }
    }

    public static void main(String[] args) throws Exception{
        configureLogging();

        Main main = new Main();
        InetSocketAddress serverSocket = new InetSocketAddress("localhost", main.server.getPort());


        for(int i = 0; i < PARALLELITY; i++){
            PerformanceTestWebservice service = new PerformanceTestWebservice(i, 0, 50, main.server.getExecutor());
            main.server.registerService(service);
            service.initialize();
        }

        for(int i = 0; i < PARALLELITY; i++){
            URI uri = new URI("coap", null, "localhost", -1, "/service/" + String.format("%03d", i), null, null);
            CoapRequest coapRequest = new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, uri);
            coapRequest.setObserve(0);

            if(i % 2 == 0){
                coapRequest.setAccept(ContentFormat.TEXT_PLAIN_UTF8);
            }
            else{
                coapRequest.setAccept(ContentFormat.APP_XML);
            }

            PerformanceTestClientCallback callback = new PerformanceTestClientCallback(i);
            main.callbacks.put(i, callback);

            main.client.sendCoapRequest(coapRequest, callback, serverSocket);
        }

        Thread.sleep(DURATION * 1000);

        main.client.shutdown();
        main.server.shutdown();

        Thread.sleep(1000);
        main.checkNotifications();
    }

}
