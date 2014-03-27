/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uniluebeck.itm.ncoap.simpleclient;

import org.kohsuke.args4j.Option;

/**
 * Wrapper class for the possible command line options for {@link SimpleObservingCoapClient}
 *
 * @author Oliver Kleine
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
            usage = "Sets the maximum duration (in seconds) before shutdown (default = 60)")
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
