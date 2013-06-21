package de.uniluebeck.itm.spitfire.nCoap.application.client;

import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import org.apache.log4j.Logger;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 21.06.13
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class TestCoapReponseProcessor implements CoapResponseProcessor, RetransmissionTimeoutProcessor,
        EmptyAcknowledgementProcessor {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private SortedMap<Long, CoapResponse> responses = new TreeMap<Long, CoapResponse>();

    private SortedMap<Long, EmptyAcknowledgementReceivedMessage> emptyAcknowledgements
                                    = new TreeMap<Long, EmptyAcknowledgementReceivedMessage>();
    private SortedMap<Long, RetransmissionTimeoutMessage> timeoutMessages
                                    = new TreeMap<Long, RetransmissionTimeoutMessage>();


    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
        log.info("Received Response: " + coapResponse);
        responses.put(System.currentTimeMillis(), coapResponse);
    }

    @Override
    public void processRetransmissionTimeout(RetransmissionTimeoutMessage message) {
        log.info("Retransmission Timeout: " + message);
        timeoutMessages.put(System.currentTimeMillis(), message);
    }

    @Override
    public void processEmptyAcknowledgement(EmptyAcknowledgementReceivedMessage message) {
        log.info("Received empty ACK: " + message);
        emptyAcknowledgements.put(System.currentTimeMillis(), message);
    }

    public SortedMap<Long, CoapResponse> getCoapResponses(){
        return this.responses;
    }

    public SortedMap<Long, EmptyAcknowledgementReceivedMessage> getEmptyAcknowledgements() {
        return emptyAcknowledgements;
    }

    public SortedMap<Long, RetransmissionTimeoutMessage> getTimeoutMessages() {
        return timeoutMessages;
    }


}
