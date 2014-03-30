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

        SimpleNotObservableWebservice webservice = new SimpleNotObservableWebservice("/path", "Oliver", 5000);
        webservice.putLinkAttribute(new LongLinkAttribute(LinkAttribute.CONTENT_TYPE, ContentFormat.TEXT_PLAIN_UTF8));
        webservice.putLinkAttribute(new LongLinkAttribute(LinkAttribute.CONTENT_TYPE, ContentFormat.APP_XML));

        SimpleNotObservableWebservice webservice2 = new SimpleNotObservableWebservice("/path2", "Oliver", 5000);
        webservice2.putLinkAttribute(new LongLinkAttribute(LinkAttribute.CONTENT_TYPE, ContentFormat.TEXT_PLAIN_UTF8));

        server.registerService(webservice);
        server.registerService(webservice2);
    }

}
