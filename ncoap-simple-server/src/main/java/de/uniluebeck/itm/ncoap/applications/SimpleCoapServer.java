package de.uniluebeck.itm.ncoap.applications;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LinkAttribute;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LongLinkAttribute;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

/**
 * Created by olli on 30.03.14.
 */
public class SimpleCoapServer extends CoapServerApplication {

    public static void main(String[] args) throws Exception {
        LoggingConfiguration.configureDefaultLogging();

        SimpleCoapServer server = new SimpleCoapServer();

        SimpleNotObservableWebservice simpleWebservice = new SimpleNotObservableWebservice("/simple", "Oliver", 5000);
        server.registerService(simpleWebservice);

        SimpleObservableTimeService timeService = new SimpleObservableTimeService("/utc-time");
        server.registerService(timeService);
    }

}
