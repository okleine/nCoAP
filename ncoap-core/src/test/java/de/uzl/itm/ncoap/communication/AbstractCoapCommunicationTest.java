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
package de.uzl.itm.ncoap.communication;

import de.uzl.itm.ncoap.AbstractCoapTest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Ignore;


/**
 * Abstract class to be extended by all tests classes to test communication functionality, i.e. all
 * tests using at least one of
 * {@link de.uzl.itm.ncoap.application.client.CoapClient}, or
 * {@link de.uzl.itm.ncoap.application.server.CoapServer}.
 *
 * @author Oliver Kleine
 */
 @Ignore
public abstract class AbstractCoapCommunicationTest extends AbstractCoapTest{

    private static Logger log = Logger.getLogger(AbstractCoapCommunicationTest.class.getName());

    private static boolean areComponentsSetup = false;
    private static boolean isTestScenarioCreated = false;
    private static boolean areComponentsShutdown = false;


    /**
     * This method is to create all necessary instances of
     * {@link de.uzl.itm.ncoap.application.client.CoapClient},
     * {@link de.uzl.itm.ncoap.application.server.CoapServer} and
     * {@link de.uzl.itm.ncoap.endpoints.DummyEndpoint}.
     *
     * Additionally all necessary instances of webservices are supposed to be created and registered at the
     * {@link de.uzl.itm.ncoap.application.server.CoapServer} instance(s)
     *
     * @throws Exception
     */
    public abstract void setupComponents() throws Exception;

    /**
     * This method is supposed to define all communication to create the scenario the test method do their testing on
     */
    public abstract void createTestScenario() throws Exception;

    /**
     * This method is to shutdown all existing instances of
     * {@link de.uzl.itm.ncoap.application.client.CoapClient},
     * {@link de.uzl.itm.ncoap.application.server.CoapServer}, and
     * {@link de.uzl.itm.ncoap.endpoints.DummyEndpoint}.
     *
     * @throws Exception
     */
    public abstract void shutdownComponents() throws Exception;


    public AbstractCoapCommunicationTest() {

        Logger.getLogger("AbstractCoapCommunicationTest")
                .setLevel(Level.INFO);

        try {
            if (!areComponentsSetup) {
                log.info("START TEST (" + this.getClass().getSimpleName() + ")");
                setupComponents();
                areComponentsSetup = true;
            }

            if (!isTestScenarioCreated) {
                createTestScenario();
                isTestScenarioCreated = true;
                log.info("SCENARIO CREATED (" + this.getClass().getSimpleName() + ")");
            }

            if (!areComponentsShutdown) {
                shutdownComponents();
                areComponentsShutdown = true;
                log.info("COMPONENTS SHUT DOWN (" + this.getClass().getSimpleName() + ")");
            }

        }
        catch (Exception e) {
            throw new RuntimeException("Could not create test scenario. ", e);
        }
    }

    @AfterClass
    public static void reset() {
        areComponentsSetup = false;
        areComponentsShutdown = false;
        isTestScenarioCreated = false;
    }
}
