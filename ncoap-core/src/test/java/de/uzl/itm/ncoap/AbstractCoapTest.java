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
package de.uzl.itm.ncoap;

import org.apache.log4j.*;
import org.junit.BeforeClass;

/**
 * Abstract class to be extended by all nCoAP tests to get proper logging
 *
 * @author Oliver Kleine
 */
public abstract class AbstractCoapTest {

    private static boolean isLoggingConfigured = false;

    protected AbstractCoapTest() {
        try{
            if (!isLoggingConfigured) {
                initializeLogging();
                setupLogging();
                isLoggingConfigured = true;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not create test scenario. ", e);
        }
    }


    protected static void initializeLogging() {

        if (!isLoggingConfigured) {
            Logger.getRootLogger().removeAllAppenders();

            //asynchronous appender
            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setBufferSize(100000);

            //console-appender
            String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-35.35c{1} | %-5p | %m%n";
            PatternLayout patternLayout = new PatternLayout(pattern);
            asyncAppender.addAppender(new ConsoleAppender(patternLayout));

            //add asynchronous appender to root-logger
            Logger.getRootLogger().addAppender(asyncAppender);

            //Define default log-level
            Logger.getRootLogger().setLevel(Level.ERROR);
        }
    }

    /**
     * If the extending test class is supposed to use any other logging level then {@link Level#ERROR} for everything
     * then this can be configured by overriding this method
     *
     * @throws Exception
     */
    public abstract void setupLogging() throws Exception;
}
