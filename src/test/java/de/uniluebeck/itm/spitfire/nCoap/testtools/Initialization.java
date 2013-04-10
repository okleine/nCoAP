package de.uniluebeck.itm.spitfire.nCoap.testtools;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 09.04.13
 * Time: 14:42
 * To change this template use File | Settings | File Templates.
 */
public class Initialization {
    public static boolean initialized = false;

    public static void init(){
        if(!initialized){
            //Output pattern
            String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-30.30c{1} | %-5p | %m%n";
            PatternLayout patternLayout = new PatternLayout(pattern);

            //Appenders
            ConsoleAppender consoleAppender = new ConsoleAppender(patternLayout);
            Logger.getRootLogger().addAppender(consoleAppender);

            //Define loglevel
            Logger.getRootLogger().setLevel(Level.ERROR);
            Logger.getLogger("de.uniluebeck.itm.spitfire.nCoap.communication").setLevel(Level.DEBUG);

            initialized = true;
        }
    }
}
