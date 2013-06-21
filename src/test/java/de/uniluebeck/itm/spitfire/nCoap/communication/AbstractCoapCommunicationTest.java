package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.endpoint.CoapTestEndpoint;
import org.apache.log4j.*;
import org.junit.AfterClass;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.04.13
 * Time: 21:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractCoapCommunicationTest {

    protected static Logger log = Logger.getLogger(AbstractCoapCommunicationTest.class.getName());

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
                initializeLogging();
                setupLogging();
                isLoggingConfigured = true;
            }

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
            AsyncAppender appender = new AsyncAppender();
            appender.addAppender(new ConsoleAppender(patternLayout));
            Logger.getRootLogger().addAppender(appender);

            appender.setBufferSize(1000);

            //Define loglevel
            Logger.getRootLogger().setLevel(Level.ERROR);
            Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.AbstractCoapCommunicationTest")
                  .setLevel(Level.DEBUG);
        }
    }
}
