package de.uniluebeck.itm.ncoap;

import org.apache.log4j.*;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 28.06.13
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractCoapTest {

    private static boolean isLoggingConfigured = false;

    public AbstractCoapTest(){
        try{
            if(!isLoggingConfigured){
                initializeLogging();
                setupLogging();
                isLoggingConfigured = true;
            }
        }
        catch (Exception e) {
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

            appender.setBufferSize(100000);

            //Define loglevel
            Logger.getRootLogger().setLevel(Level.ERROR);
            Logger.getLogger("de.uniluebeck.itm.ncoap.communication.AbstractCoapTest")
                    .setLevel(Level.INFO);
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
