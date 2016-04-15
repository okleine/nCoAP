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

import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.net.URL;

/**
 * Helper class to configure the applications logging
 *
 * @author Oliver Kleine
 */
public abstract class LoggingConfiguration {

    /**
     * Configure the logging with the XML-File (log4j style) located at the given path. If no
     * proper XML file was found at the given path this method invoke
     * {@link #configureDefaultLogging()}.
     *
     * @param path the (absolute or relative) path to the XML-config file.
     *
     * @throws Exception if something went terribly wrong
     */
    public static void configureLogging(String path) throws Exception{

        if (configureLogging(new File(path)))
            return;

        configureDefaultLogging();

    }


    /**
     * Activates a default logging scheme, i.e. {@link org.apache.log4j.Level#INFO} for all
     * loggers and only console output.
     *
     * @throws Exception if something went terribly wrong
     */
    public static void configureDefaultLogging() throws Exception{
        System.out.println("Use default logging configuration, i.e. INFO level...\n");
        URL url = LoggingConfiguration.class.getClassLoader().getResource("log4j.default.xml");
        System.out.println("Use config file " + url);
        DOMConfigurator.configure(url);
    }


    private static boolean configureLogging(File configFile) throws Exception{
        System.out.println("Looking for file \"log4j.xml\" at path: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            System.out.println("File \"log4j.xml\" not found...\n");
            return false;
        }

        System.out.println("File \"log4j.xml\" found...\n");
        DOMConfigurator.configure(configFile.toURI().toURL());
        return true;
    }
}
