package de.uzl.itm.ncoap.examples;

import de.uzl.itm.ncoap.application.server.CoapServer;

/**
 * Created by olli on 11.09.15.
 */
public class PerformanceTestServer extends CoapServer{

    public static void main(String[] args) {
        PerformanceTestServer server = new PerformanceTestServer();
        server.registerWebresource(
            new PerformanceTestResource("/performance", "Performance", 1000, server.getExecutor())
        );
    }
}
