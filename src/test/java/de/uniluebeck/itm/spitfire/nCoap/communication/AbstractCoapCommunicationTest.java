package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.NotObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableTestWebService;
import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.04.13
 * Time: 21:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractCoapCommunicationTest {

    protected static Logger log = Logger.getLogger(AbstractCoapCommunicationTest.class.getName());

    protected static String OBSERVABLE_SERVICE_PATH = "/observable";
    protected static String NOT_OBSERVABLE_SERVICE_PATH = "/not/observable";
    protected static String NOT_OBSERVABLE_RESOURCE_CONTENT = "testpayload";


    private static boolean isLoggingConfigured = false;
    private static boolean areComponentsSetup = false;
    private static boolean isTestScenarioCreated = false;


    /**
     * This method is supposed to define all communication to create the scenario the test method do their testing on
     */
    public abstract void createTestScenario() throws Exception;

    /**
     * This method is to instanciate all necessary instances of {@link CoapTestClient}, {@link CoapTestServer} and
     * {@link CoapTestEndpoint}.
     *
     * Additionally all necessary instances {@link NotObservableTestWebService} and
     * {@link ObservableTestWebService} are supposed to be created and registered at the {@link CoapTestServer}
     * instance(s)
     *
     * @throws Exception
     */
    public abstract void setupComponents() throws Exception;

    /**
     * This method is to shutdown all existing instances of {@link CoapTestClient}, {@link CoapTestServer} and
     * {@link CoapTestEndpoint}.
     *
     * @throws Exception
     */
    public abstract void shutdownComponents() throws Exception;

    /**
     * If the extending test class is supposed to use any other logging level then {@link Level#ERROR} for everything
     * then this can be configured by overriding this method
     *
     * @throws Exception
     */
    public abstract void setupLogging() throws Exception;


    public AbstractCoapCommunicationTest(){
        try {
            if(!isLoggingConfigured){
                log.info("Start: " + this.getClass().getName());
                initializeLogging();
                setupLogging();
                isLoggingConfigured = true;
            }

            if(!areComponentsSetup){
                setupComponents();
                areComponentsSetup = true;
            }

            if(!isTestScenarioCreated){
                createTestScenario();
                isTestScenarioCreated = true;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not create test scenario. ", e);
        }

    }


    public static void initializeLogging(){
        if(!isLoggingConfigured){
            //Output pattern
            String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-35.35c{1} | %-5p | %m%n";
            PatternLayout patternLayout = new PatternLayout(pattern);

            //Appenders
            ConsoleAppender consoleAppender = new ConsoleAppender(patternLayout);
            Logger.getRootLogger().addAppender(consoleAppender);

            //Define loglevel
            Logger.getRootLogger().setLevel(Level.ERROR);
        }
    }
}
