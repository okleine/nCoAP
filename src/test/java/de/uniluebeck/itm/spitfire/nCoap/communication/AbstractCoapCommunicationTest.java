package de.uniluebeck.itm.spitfire.nCoap.communication;

import de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.application.CoapServerApplication;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestClient;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.CoapTestServer;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.NotObservableDummyWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.ObservableDummyWebService;
import de.uniluebeck.itm.spitfire.nCoap.communication.utils.receiver.CoapMessageReceiver;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.04.13
 * Time: 21:04
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractCoapCommunicationTest {

    protected Logger log = Logger.getLogger(this.getClass().getName());

    protected static String OBSERVABLE_SERVICE_PATH = "/observable";
    protected static String NOT_OBSERVABLE_SERVICE_PATH = "/not/observable";
    protected static String NOT_OBSERVABLE_RESOURCE_CONTENT = "testpayload";

    protected static CoapTestClient testClient;
    protected static CoapTestServer testServer;
    protected static CoapMessageReceiver testReceiver;

    private static boolean loggingInitialized = false;
    private static boolean componentsInitialized = false;
    private static boolean scenarioCreated = false;


    /**
     * This method is supposed to define all communication to create the scenario the test method do their testing on
     */
    public abstract void createTestScenario() throws Exception;

    public AbstractCoapCommunicationTest(){
        if(!scenarioCreated){
            try {
                log.info("******* Starting tests in " + this.getClass().getName() + " **********");
                createTestScenario();
                log.info("******* Test scenario finished: " + this.getClass().getName() + " **********");
                scenarioCreated = true;
            } catch (Exception e) {
                throw new RuntimeException("Could not create test scenario. ", e);
            }
        }
    }

    @BeforeClass
    public static void runCommunication() throws Exception {
        initializeLogging();
        initializeComponents();
        testReceiver.getReceivedMessages().clear();
        testReceiver.getResponsesToSend().clear();
    }

    @AfterClass
    public static void shutDownComponents(){
        Logger.getLogger("JUnit").info("******* shutDownComponents() *******");
        testServer.shutdown();
        testClient.shutdown();
        testReceiver.shutdown();
        componentsInitialized = false;
        scenarioCreated = false;
    }

    private static void initializeComponents(){
        if(!componentsInitialized){
            testClient = new CoapTestClient();
            testServer = new CoapTestServer(0);
            testReceiver = new CoapMessageReceiver();
            componentsInitialized = true;
        }
    }

    public static void initializeLogging(){
        if(!loggingInitialized){
            //Output pattern
            String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-35.35c{1} | %-5p | %m%n";
            PatternLayout patternLayout = new PatternLayout(pattern);

            //Appenders
            ConsoleAppender consoleAppender = new ConsoleAppender(patternLayout);
            Logger.getRootLogger().addAppender(consoleAppender);

            //Define loglevel
            Logger.getRootLogger().setLevel(Level.INFO);
            Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.application").setLevel(Level.DEBUG);
            Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication").setLevel(Level.DEBUG);
            Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication.encoding").setLevel(Level.INFO);

            loggingInitialized = true;
        }
    }

    public void registerObservableDummyService(long pretendedProcessingMillisForRequests, long updateIntervalMinis){
        testServer.registerService(new ObservableDummyWebService(OBSERVABLE_SERVICE_PATH, true,
                pretendedProcessingMillisForRequests, updateIntervalMinis));
    }

    public void registerObservableDummyService(long pretendedProcessingMillisForRequests, long updateIntervalMinis, int
                                               maxAge){
        testServer.registerService(new ObservableDummyWebService(OBSERVABLE_SERVICE_PATH, true,
                pretendedProcessingMillisForRequests, updateIntervalMinis, maxAge));
    }

    public void registerNotObservableDummyService(long pretendedProcessingMillisForRequests){
        testServer.registerService(new NotObservableDummyWebService(NOT_OBSERVABLE_SERVICE_PATH,
                NOT_OBSERVABLE_RESOURCE_CONTENT, pretendedProcessingMillisForRequests));
    }
    
    public void registerObservableDummyService(ObservableDummyWebService observableDummyWebService) {
        testServer.registerService(observableDummyWebService);
    }
}
