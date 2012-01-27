package de.uniluebeck.itm.spitfire.nCoap.message.header;

/**
 * @author Oliver Kleine
 */
public enum MsgType {

    /**
     * corresponds to CoAPs numerical message type 0 (Confirmable message)
     */
    CON(0),

    /**
     * corresponds to CoAPs numerical message type 1 (Nonconfirmable message)
     */
    NON(1),

    /**
     * corresponds to CoAPs numerical message type 2 (Acknowledgement)
     */
    ACK(2),

    /**
     * corresponds to CoAPs numerical message type 3 (Reset message)
     */
    RST(3);

    /**
     * The corresponding numerical CoAP message type
     */
    public final int number;

    MsgType(int number){
        this.number = number;
    }

    public static MsgType getMsgTypeFromNumber(int number){
        for(MsgType t : MsgType.values()){
            if(t.number == number){
                return t;
            }
        }
        return null;
    }
}
