package de.uniluebeck.itm.ncoap.examples;

import de.uniluebeck.itm.ncoap.application.client.CoapClientCallback;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by olli on 23.09.14.
 */
public class PerformanceTestClientCallback extends CoapClientCallback {

    private static Logger log = LoggerFactory.getLogger(PerformanceTestClientCallback.class.getName());

    private Map<Long, CoapResponse> updateNotifications;
    private int callbackNo;

    public PerformanceTestClientCallback(int callbackNo){
        updateNotifications = Collections.synchronizedMap(new TreeMap<Long, CoapResponse>());
        this.callbackNo = callbackNo;
    }

    @Override
    public void processCoapResponse(CoapResponse coapResponse) {


        Long sequenceNo = coapResponse.getObservationSequenceNumber();

        if(sequenceNo == null){
            log.error("Callback #{} received (no update-notification): {}", callbackNo, coapResponse);
        }
        else{
            updateNotifications.put(sequenceNo, coapResponse);
            log.info("Received #{}", sequenceNo);
        }
    }

    public int getCallbackNo(){
        return this.callbackNo;
    }

    public Map<Long, CoapResponse> getUpdateNotifications(){
        return this.updateNotifications;
    }
}
