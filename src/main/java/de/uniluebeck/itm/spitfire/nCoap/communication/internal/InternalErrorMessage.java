//package de.uniluebeck.itm.spitfire.nCoap.communication.internal;
//
//import de.uniluebeck.itm.spitfire.nCoap.communication.callback.ResponseCallbackHandler;
//
///**
//* An {@link InternalErrorMessage} is sent upstream if an error occurs during the message processing. This could e.g.
//* happen if the automatic setting of message ID or token option fails or if the confirmable message times out.
//*
//* For clients InternalErrorMessages are handled by the {@link ResponseCallbackHandler} as the topmost handler in the
//* pipeline by invoking the receiveInternalError method of the client implementation.
//*/
//public class InternalErrorMessage implements InternalMessage{
//
//    String content;
//    byte[] token;
//
//    /**
//     * Contructor for InternalErrorMessages
//     * @param content The message (human readable) to describe the cause of the error
//     * @param token The token of the message the caused the error
//     */
//    public InternalErrorMessage(String content, byte[] token){
//        this.content = content;
//        this.token = token;
//    }
//
//    /**
//     * Returns a human readable message describing the cause of the error
//     * @return a human readable message describing the cause of the error
//     */
//    @Override
//    public String getContent() {
//        return content;
//    }
//
//    /**
//     * Returns the token of the message that caused the error
//     * @return the token of the message that caused the error
//     */
//    public byte[] getToken() {
//        return token;
//    }
//
//    /**
//     * Returns a String representation of the InternalErrorMessage
//     * @return a String representation of the InternalErrorMessage
//     */
//    @Override
//    public String toString(){
//        return "{[" + this.getClass().getName() + "] Token: " + getToken() + ", Message: " + getContent() + "}";
//    }
//}