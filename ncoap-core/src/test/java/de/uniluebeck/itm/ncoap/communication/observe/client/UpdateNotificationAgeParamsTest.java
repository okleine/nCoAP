package de.uniluebeck.itm.ncoap.communication.observe.client;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.ncoap.AbstractCoapTest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.apache.log4j.Logger;

import java.util.Collection;

import static org.junit.Assert.assertTrue;

/**
 * Created by olli on 10.09.14.
 */
@RunWith(Parameterized.class)
public class UpdateNotificationAgeParamsTest extends AbstractCoapTest{

    private static Logger log = Logger.getLogger(UpdateNotificationAgeParamsTest.class.getName());

    public static long THRESHOLD = (long) Math.pow(2, 23);

    /*
    V1: Sequence number of previous update notification
    V2: Sequence number of received update notification
    T1: Reception timestamp of previous update notification
    T2: Reception timestamp of received update notification
    Expected: Is received newer than previous update notification?
     */
    @Parameterized.Parameters(name = "v1 = {0}, v2 = {1}, t1 = {2}, t2 = {3}, expected = {4}")
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                //V1 < V2 and V2 - V1 < 2^23 (T1 = T2)
                new Object[]{1, THRESHOLD, 12345, 12345, true},

                //V1 < V2 and V2 - V1 < 2^23 (T1 < T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD - 1, 12344, 12345, true},

                //V1 < V2 and V2 - V1 = 2^23 (T1 = T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD, 12345, 12345, false},

                //V1 < V2 and V2 - V1 = 2^23 (T1 + 128000 = T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD, 12345, 12345 + 128000, false},

                //V1 < V2 and V2 - V1 = 2^23 (T1 + 128001 = T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD, 12345, 12345 + 128001, true},

                //V1 > V2 and V1 - V2 > 2^23 (T1 < T2)
                new Object[]{THRESHOLD + 2, 1, 12344, 12345, true},

                //V1 > V2 and V1 - V2 > 2^23 (T1 < T2)
                new Object[]{THRESHOLD + 1, 1, 12344, 12345, false},

                //V1 > V2 and V1 - V2 > 2^23 (T2 > T1 + 128 sec)
                new Object[]{THRESHOLD + 1, 1, 12344, 12345 + 128001, true}

        );
    }

    private UpdateNotificationAgeParams params1;
    private UpdateNotificationAgeParams params2;
    private boolean expected;


    public UpdateNotificationAgeParamsTest(long v1, long v2, long t1, long t2, boolean expected){

        CoapResponse coapResponse1 = new CoapResponse(MessageType.Name.CON, MessageCode.Name.CONTENT_205);
        coapResponse1.setObserveOption(v1);

        CoapResponse coapResponse2 = new CoapResponse(MessageType.Name.CON, MessageCode.Name.CONTENT_205);
        coapResponse2.setObserveOption(v2);

        long sequenceNo1 = coapResponse1.getObservationSequenceNumber();
        long sequenceNo2 = coapResponse2.getObservationSequenceNumber();
        log.info("Observe Option Values: V1 =  " + sequenceNo1 + ", V2 = " + sequenceNo2);
        log.info("Timestamps: T1 = " + t1 + ", T2 = " + t1 + ", (T2 > T1 + 128 sec [" + (t2 > t1 + 128000) + "])");

        this.params1 = new UpdateNotificationAgeParams(sequenceNo1, t1);
        this.params2 = new UpdateNotificationAgeParams(sequenceNo2, t2);
        this.expected = expected;
    }

    @Override
    public void setupLogging() throws Exception {
        log.setLevel(Level.INFO);
    }

    @Test
    public void compareActuality(){
        String message;
        if(expected){
            message = "Params2 (" + this.params2 + ") should be considered newer than Params1 (" + this.params1 + ")";
        }
        else{
            message = "Params1 (" + this.params1 + ") should be considered newer than Params2 (" + this.params2 + ")";
        }

        assertTrue(message, UpdateNotificationAgeParams.isParams2Newer(params1, params2) == expected);
    }
}
