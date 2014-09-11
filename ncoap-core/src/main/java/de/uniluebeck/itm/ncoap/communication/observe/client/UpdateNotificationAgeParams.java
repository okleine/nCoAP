package de.uniluebeck.itm.ncoap.communication.observe.client;

/**
 * Created by olli on 11.09.14.
 */
public class UpdateNotificationAgeParams{

    public static long THRESHOLD = (long) Math.pow(2, 23);
    
    long sequenceNo;
    long timestamp;

    UpdateNotificationAgeParams(long sequenceNo, long timestamp){
        this.sequenceNo = sequenceNo;
        this.timestamp = timestamp;
    }

    public static boolean isParams2Newer(UpdateNotificationAgeParams params1, UpdateNotificationAgeParams params2){
        if(params1.sequenceNo < params2.sequenceNo && params2.sequenceNo - params1.sequenceNo < THRESHOLD){
            return true;
        }

        if(params1.sequenceNo > params2.sequenceNo && params1.sequenceNo - params2.sequenceNo > THRESHOLD){
            return true;
        }

        if(params2.timestamp > params1.timestamp + 128000L){
            return true;
        }

        return false;
    }

    @Override
    public String toString(){
        return "Sequence No: " + this.sequenceNo + ", Reception Timestamp: " + this.timestamp;
    }
}
