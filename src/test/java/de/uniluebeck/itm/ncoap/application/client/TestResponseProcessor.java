package de.uniluebeck.itm.ncoap.application.client;

import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.*;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 21.06.13
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class TestResponseProcessor implements CoapResponseProcessor, RetransmissionTimeoutProcessor,
        EmptyAcknowledgementProcessor, RetransmissionProcessor {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private ArrayList<Object[]> responses = new ArrayList<Object[]>();

    private ArrayList<Object[]> emptyAcknowledgements = new ArrayList<Object[]>();

    private ArrayList<Object[]>  timeoutMessages = new ArrayList<Object[]>();

    private ArrayList<Long> requestSentTimes = new ArrayList<Long>();

    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
       responses.add(new Object[]{coapResponse, System.currentTimeMillis()});
       log.info("Received Response #" + responses.size() + ": " + coapResponse);
    }

    @Override
    public void requestSent() {
        requestSentTimes.add(System.currentTimeMillis());
        log.info("Attempt #" + requestSentTimes.size() + " to sent request.");
    }

    @Override
    public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage message) {
        log.info("Retransmission Timeout: " + message);
        timeoutMessages.add(new Object[]{message, System.currentTimeMillis()});
    }

    @Override
    public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) {
        log.info("Received empty ACK: " + message);
        emptyAcknowledgements.add(new Object[]{message, System.currentTimeMillis()});
    }

    public List<CoapResponse> getCoapResponses(){
        List<CoapResponse> result = new ArrayList<CoapResponse>();
        for(Object[] object : responses){
            result.add((CoapResponse) object[0]);
        }
        return result;
    }
    
    public Long getCoapResponseReceptionTime(int index){
        return (Long) (responses.get(index))[1];   
    }

    public CoapResponse getCoapResponse(int index){
        return (CoapResponse) (responses.get(index))[0];
    }


    public List<InternalEmptyAcknowledgementReceivedMessage> getEmptyAcknowledgementReceivedMessages(){
        List<InternalEmptyAcknowledgementReceivedMessage> result = new ArrayList<InternalEmptyAcknowledgementReceivedMessage>();
        for(Object[] object : emptyAcknowledgements){
            result.add((InternalEmptyAcknowledgementReceivedMessage) object[0]);
        }
        return result;
    }

    public Long getEmptyAcknowledgementeReceptionTime(int index){
        return (Long) (emptyAcknowledgements.get(index))[1];
    }

    public InternalEmptyAcknowledgementReceivedMessage getEmptyAcknowledgementReceivedMessage(int index){
        return (InternalEmptyAcknowledgementReceivedMessage) (responses.get(index))[0];
    }

    public List<InternalRetransmissionTimeoutMessage> getRetransmissionTimeoutMessages(){
        List<InternalRetransmissionTimeoutMessage> result = new ArrayList<InternalRetransmissionTimeoutMessage>();
        for(Object[] object : timeoutMessages){
            result.add((InternalRetransmissionTimeoutMessage) object[0]);
        }
        return result;
    }

    public Long getRetransmissionTimeoutTime(int index){
        return (Long) (timeoutMessages.get(index))[1];
    }

    public InternalRetransmissionTimeoutMessage getRetransmissionTimeoutMessage(int index){
        return (InternalRetransmissionTimeoutMessage) (timeoutMessages.get(index))[0];
    }

    public List<Long> getRequestSentTimes() {
        return requestSentTimes;
    }
}
