package de.uniluebeck.itm.spitfire.nCoap.communication.internal;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 28.08.12
 * Time: 14:05
 * To change this template use File | Settings | File Templates.
 */
public class InternalErrorMessage implements InternalMessage{

    String content;
    byte[] token;

    public InternalErrorMessage(String content, byte[] token){
        this.content = content;
        this.token = token;
    }

    @Override
    public String getContent() {
        return content;
    }

    public byte[] getToken() {
        return token;
    }

    public String toString(){
        return "{[" + this.getClass().getName() + "] Token: " + getToken() + ", Message: " + getContent() + "}";
    }
}
