package de.uniluebeck.itm.ncoap.communication;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 14.11.13
 * Time: 12:28
 * To change this template use File | Settings | File Templates.
 */
public class InternalExceptionMessage {

    private final int messageType;
    private final int messageCode;
    private final int messageID;
    private final long token;
    private Throwable cause;

    public InternalExceptionMessage(int messageType, int messageCode, int messageID, long token, Throwable cause){
        this.messageType = messageType;
        this.messageCode = messageCode;
        this.messageID = messageID;
        this.token = token;
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getMessageID() {
        return messageID;
    }

    public int getMessageCode() {
        return messageCode;
    }

    public long getToken() {
        return token;
    }
}
