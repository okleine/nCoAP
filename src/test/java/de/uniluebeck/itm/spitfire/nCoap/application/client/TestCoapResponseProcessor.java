package de.uniluebeck.itm.spitfire.nCoap.application.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 21.06.13
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class TestCoapResponseProcessor implements CoapResponseProcessor, RetransmissionTimeoutProcessor,
        EmptyAcknowledgementProcessor {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private HashMultimap <Long, CoapResponse> responses = HashMultimap.create();

    private HashMultimap<Long, EmptyAcknowledgementReceivedMessage> emptyAcknowledgements = HashMultimap.create();

    private HashMultimap<Long, RetransmissionTimeoutMessage> timeoutMessages = HashMultimap.create();

    private long requestSendTime;

    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
        log.info("Received Response: " + coapResponse);
        responses.put(System.currentTimeMillis(), coapResponse);
    }

    @Override
    public void messageSuccesfullySent() {
        requestSendTime = System.currentTimeMillis();
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

    public Multimap<Long, CoapResponse> getCoapResponses(){
        return this.responses;
    }

    public Multimap<Long, EmptyAcknowledgementReceivedMessage> getEmptyAcknowledgements() {
        return emptyAcknowledgements;
    }

    public Multimap<Long, RetransmissionTimeoutMessage> getTimeoutMessages() {
        return timeoutMessages;
    }


    public long getRequestSendTime() {
        return requestSendTime;
    }
}
