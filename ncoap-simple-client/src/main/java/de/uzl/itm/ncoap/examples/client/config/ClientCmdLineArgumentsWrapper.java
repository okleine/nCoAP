/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uzl.itm.ncoap.examples.client.config;

import de.uzl.itm.ncoap.examples.client.SimpleCoapClient;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Wrapper class for the possible command line options for {@link SimpleCoapClient}
 *
 * @author Oliver Kleine
 */
public class ClientCmdLineArgumentsWrapper {

    @Option(name = "--host",
            usage="Sets IP address or DNS name of the target URI (default = localhost)")
    private String uriHost = "localhost";

    @Option(name = "--port",
            usage = "Sets port of the target URI (default = 5683)")
    private int uriPort = 5683;

    @Option(name = "--path",
            usage = "Sets the path of the target URI (default = null)")
    private String uriPath = null;

    @Option(name = "--query",
            usage = "Sets the query of the target URI (default = null)")
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

    @Option(name = "--duration",
            usage = "Sets the maximum duration (in seconds) before shutdown (default = 60)")
    private int duration = 60;

    @Option(name = "--observing",
            usage = "Empty argument that causes the addressed webresource to be observed")
    private boolean observe = false;

    @Option(name = "--maxUpdates",
            usage = "Sets the number of update notifications before shutdown (default = 1)")
    private int maxUpdates = 1;

    @Option(name = "--help",
            usage = "Prints this help")
    private boolean help = false;

    @Option(name = "--log4jConfig",
            usage = "Sets the path to the log4j (XML) configuration file")
    private String log4jConfigPath = null;

    /**
     * Creates a new instance of {@link ClientCmdLineArgumentsWrapper}.
     *
     * @param args the array of command line parameters (forwarded arguments from
     *             <code>public static void main(String[] args)</code>
     *
     * @throws CmdLineException if some error occurred while reading the given command line arguments
     */
    public ClientCmdLineArgumentsWrapper(String[] args) throws CmdLineException {
        CmdLineParser parser = new CmdLineParser(this);

        try{
            parser.parseArgument(args);
        }
        catch(CmdLineException ex) {
            System.err.println(ex.getMessage());
            parser.printUsage(System.err);
            throw ex;
        }

        if (this.isHelp()) {
            parser.printUsage(System.out);
            System.exit(0);
        }
    }

    /**
     * Returns the host of the URI the request is to be sent to
     * @return the host of the URI the request is to be sent to
     */
    public String getUriHost() {
        return uriHost;
    }

    /**
     * Returns the port of the URI the request is to be sent to
     * @return the port of the URI the request is to be sent to
     */
    public int getUriPort() {
        return uriPort;
    }

    /**
     * Returns the path of the URI the request is to be sent to or <code>null</code> if not set
     * @return the path of the URI the request is to be sent to or <code>null</code> if not set
     */
    public String getUriPath() {
        return uriPath;
    }

    /**
     * Returns the query of the URI the request is to be sent to or <code>null</code> if not set
     * @return the query of the URI the request is to be sent to or <code>null</code> if not set
     */
    public String getUriQuery() {
        return uriQuery;
    }

    /**
     * Returns the address of the proxy the request is to be sent to or <code>null</code> if not set
     * @return the address of the proxy the request is to be sent to or <code>null</code> if not set
     */
    public String getProxyAddress() {
        return proxyAddress;
    }

    /**
     * Returns the port of the proxy the request is to be sent to or <code>5683</code> if not set
     * @return the port of the proxy the request is to be sent to or <code>5683</code> if not set
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Returns <code>true</code> if the request is to be sent non-confirmable or <code>false</code> otherwise
     * @return <code>true</code> if the request is to be sent non-confirmable or <code>false</code> otherwise
     */
    public boolean isNon() {
        return non;
    }

    /**
     * Returns the number of seconds to wait before the client is shut down
     * @return the number of seconds to wait before the client is shut down
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Returns <code>true</code> if <code>--observing</code> was set as command line parameter
     * and <code>false</code> otherwise.
     * @return <code>true</code> if <code>--observing</code> was set as command line parameter
     * and <code>false</code> otherwise.
     */
    public boolean isObserve() {
        return observe;
    }

    /**
     * Returns the maximum of update notifications to wait for before the client is shut down.
     * This parameter is only used if {@link #isObserve()} returns <code>true</code>.
     * @return the maximum of update notifications to wait for before the client is shut down
     */
    public int getMaxUpdates() {
        return maxUpdates;
    }

    /**
     * Returns <code>true</code> if --help was given as console parameter or <code>false</code> otherwise
     * @return <code>true</code> if --help was given as console parameter or <code>false</code> otherwise
     */
    public boolean isHelp() {
        return help;
    }

    /**
     * Returns the path to the log4j (XML-)configuration file or <code>null</code> if not set
     * @return the path to the log4j (XML-)configuration file or <code>null</code> if not set
     */
    public String getLog4jConfigPath() {
        return log4jConfigPath;
    }
}
