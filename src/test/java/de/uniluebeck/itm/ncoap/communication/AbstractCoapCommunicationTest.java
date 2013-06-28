package de.uniluebeck.itm.ncoap.communication;

import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.endpoint.CoapTestEndpoint;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableTestWebService;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableTestWebService;
import org.apache.log4j.*;
import org.junit.AfterClass;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.04.13
 * Time: 21:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractCoapCommunicationTest extends AbstractCoapTest{

    protected static Logger log = Logger.getLogger(AbstractCoapCommunicationTest.class.getName());


    private static boolean areComponentsSetup = false;
    private static boolean isTestScenarioCreated = false;
    private static boolean areComponentsShutdown = false;


    /**
     * This method is to instanciate all necessary instances of {@link CoapClientApplication},
     * {@link CoapServerApplication} and {@link CoapTestEndpoint}.
     *
     * Additionally all necessary instances {@link NotObservableTestWebService} and
     * {@link ObservableTestWebService} are supposed to be created and registered at the {@link CoapServerApplication}
     * instance(s)
     *
     * @throws Exception
     */
    public abstract void setupComponents() throws Exception;

    /**
     * This method is to shutdown all existing instances of {@link CoapClientApplication},
     * {@link CoapServerApplication} and {@link CoapTestEndpoint}.
     *
     * @throws Exception
     */
    public abstract void shutdownComponents() throws Exception;

    /**
     * This method is supposed to define all communication to create the scenario the test method do their testing on
     */
    public abstract void createTestScenario() throws Exception;


    public AbstractCoapCommunicationTest(){

        try {
            if(!areComponentsSetup){
                log.info("Start: " + this.getClass().getName());
                setupComponents();
                areComponentsSetup = true;
            }

            if(!isTestScenarioCreated){
                createTestScenario();
                isTestScenarioCreated = true;
                log.info("Scenario created for: " + this.getClass().getName());
            }

            if(!areComponentsShutdown){
                shutdownComponents();
                areComponentsShutdown = true;
                log.info("Components shutdown for: " + this.getClass().getName());
            }

        }
        catch (Exception e) {
            throw new RuntimeException("Could not create test scenario. ", e);
        }
    }

    @AfterClass
    public static void reset(){
        areComponentsSetup = false;
        areComponentsShutdown = false;
        isTestScenarioCreated = false;
    }
}
