package de.uniluebeck.itm.ncoap.examples;

import org.kohsuke.args4j.Option;

/**
 * Created by olli on 25.03.14.
 */
public class CmdLineArgumentsWrapper {

    @Option(name = "--host",
            usage="Sets IP address or DNS name of the target URI (default = localhost)")
    private String uriHost = "localhost";

    @Option(name = "--port",
            usage = "Sets port of the target URI (default = 5683)")
    private int uriPort = 5683;

    @Option(name = "--path",
            usage = "Sets the path of the target URI (default = null")
    private String uriPath = null;

    @Option(name = "--query",
            usage = "Sets the query of the target URI (default = null")
    private String uriQuery = null;

    @Option(name = "--proxyAddress",
            usage = "Sets the IP address or DNS name of a proxy the request is to be sent to (default = null)")
    private String proxyAddress = null;

    @Option(name = "--proxyPort",
            usage = "Sets the port of a proxy the request is to be sent to (default = 5683)")
    private int proxyPort = 5683;

    @Option(name = "--non",
            usage = "Empty argument that causes the request to be sent non-confirmable")
    private boolean non = false;

    @Option(name = "--updates",
            usage = "Sets the number of update notifications before shutdown (default = 5)")
    private int updates = 5;

    @Option(name = "--duration",
            usage = "Sets the maximum duration (in seconds) before shutdown (default = 60)\" ")
    private int duration = 60;

    @Option(name = "--help",
            usage = "Prints this help")
    private boolean help = false;


    public String getUriHost() {
        return uriHost;
    }

    public int getUriPort() {
        return uriPort;
    }

    public String getUriPath() {
        return uriPath;
    }

    public String getUriQuery() {
        return uriQuery;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean isNon() {
        return non;
    }

    public int getUpdates() {
        return updates;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isHelp() {
        return help;
    }
}
