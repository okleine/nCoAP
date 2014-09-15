package de.uniluebeck.itm.ncoap.etsi.client;

import org.apache.log4j.xml.DOMConfigurator;

import java.net.URL;

/**
 * Created by olli on 15.09.14.
 */
public class LoggingConfiguration {

    private static boolean configured = false;

    public synchronized static void configure(){
        if(!configured){
            URL url = LoggingConfiguration.class.getClassLoader().getResource("log4j.default.xml");
            DOMConfigurator.configure(url);
            configured = true;
        }
    }
}
