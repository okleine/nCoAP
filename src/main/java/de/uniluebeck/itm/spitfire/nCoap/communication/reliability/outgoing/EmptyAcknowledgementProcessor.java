package de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 21.06.13
 * Time: 13:12
 * To change this template use File | Settings | File Templates.
 */
public interface EmptyAcknowledgementProcessor {

    /**
     * This method is invoked for an incoming empty
     * acknowledgement. If the client application is e.g. a browser, one could e.g. display a message in the
     * browser windows telling the user that the server has received the request but needs some time to
     * process it.
     */
    public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message);
}
