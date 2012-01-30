package de.uniluebeck.itm.spitfire.nCoap.communication.reliability;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: olli
 * Date: 30.01.12
 * Time: 20:03
 * To change this template use File | Settings | File Templates.
 */
public class MessageIDFactory {

    private static Logger log = Logger.getLogger("nCoap");

    //MessageID and time to deallocate used message IDs
    private ConcurrentHashMap<Integer, Long> allocatedIDs = new ConcurrentHashMap<>();
    private int nextMessageID = 0;

    private static MessageIDFactory instance = new MessageIDFactory();

    public static MessageIDFactory getInstance(){
        return instance;
    }

    private MessageIDFactory(){
        new Thread(){
            @Override
            public void run(){
                while(true){
                    Enumeration<Integer> keys = allocatedIDs.keys();
                    while(keys.hasMoreElements()){
                        int messageID = keys.nextElement();
                        if(System.currentTimeMillis() > allocatedIDs.get(messageID)){
                            allocatedIDs.remove(messageID);
                            log.debug("[MessageIDFactory] Deallocated message ID " + messageID);
                        }
                    }
                    try{
                        sleep(10000);
                    } catch (InterruptedException e) {
                        log.debug("[MessageIDFactory] This should never happen:\n" + e.getStackTrace());
                    }
                }
            }
        }.start();
    }

    /**
     * Returns the next available message ID within range 1 to (2^16)-1
     * @return the next available message ID within range 1 to (2^16)-1
     */
    public synchronized int nextMessageID(){
        do{
            nextMessageID = (nextMessageID + 1) & 0x0000FFFF;
        }
        while(allocatedIDs.containsKey(nextMessageID));

        allocatedIDs.put(nextMessageID, System.currentTimeMillis() + 30000);

        return nextMessageID;
    }
}
