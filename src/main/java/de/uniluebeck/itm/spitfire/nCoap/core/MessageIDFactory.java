/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.spitfire.nCoap.core;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is to create and manage message IDs for outgoing messages. The usage of this class to create
 * new message IDs ensures that a message ID is not used twice within 60 seconds.
 *
 * @author Oliver Kleine
 */
public class MessageIDFactory {

    private static Logger log = Logger.getLogger(MessageIDFactory.class);

    //Message ID and time to deallocate message ID (Free it for next usage)
    private ConcurrentHashMap<Integer, Long> allocatedIDs = new ConcurrentHashMap<>();
    private int nextMessageID = 0;

    private static MessageIDFactory instance = new MessageIDFactory();

    /**
     * Returns the one and only instance of the message ID factory
     * @return the one and only instance of the message ID factory
     */
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
                        log.fatal("[MessageIDFactory] This should never happen:\n" + e.getStackTrace());
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

        //Allocate message ID for 60 seconds
        allocatedIDs.put(nextMessageID, System.currentTimeMillis() + 60000);

        return nextMessageID;
    }
}
