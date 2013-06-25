package de.uniluebeck.itm.spitfire.nCoap.application.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.EmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutMessage;
import de.uniluebeck.itm.spitfire.nCoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import org.apache.log4j.Logger;

import java.util.*;

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

    private ArrayList<Object[]> responses = new ArrayList<Object[]>();

    private ArrayList<Object[]> emptyAcknowledgements = new ArrayList<Object[]>();

    private ArrayList<Object[]>  timeoutMessages = new ArrayList<Object[]>();

    private long requestSendTime;

    @Override
    public void processCoapResponse(CoapResponse coapResponse) {
       responses.add(new Object[]{coapResponse, System.currentTimeMillis()});
       log.info("Received Response #" + responses.size() + ": " + coapResponse);
    }

    @Override
    public void messageSuccesfullySent() {
        requestSendTime = System.currentTimeMillis();
    }

    @Override
    public void processRetransmissionTimeout(RetransmissionTimeoutMessage message) {
        log.info("Retransmission Timeout: " + message);
        timeoutMessages.add(new Object[]{message, System.currentTimeMillis()});
    }

    @Override
    public void processEmptyAcknowledgement(EmptyAcknowledgementReceivedMessage message) {
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


    public List<EmptyAcknowledgementReceivedMessage> getEmptyAcknowledgementReceivedMessages(){
        List<EmptyAcknowledgementReceivedMessage> result = new ArrayList<EmptyAcknowledgementReceivedMessage>();
        for(Object[] object : emptyAcknowledgements){
            result.add((EmptyAcknowledgementReceivedMessage) object[0]);
        }
        return result;
    }

    public Long getEmptyAcknowledgementeReceptionTime(int index){
        return (Long) (emptyAcknowledgements.get(index))[1];
    }

    public EmptyAcknowledgementReceivedMessage getEmptyAcknowledgementReceivedMessage(int index){
        return (EmptyAcknowledgementReceivedMessage) (responses.get(index))[0];
    }

    public List<RetransmissionTimeoutMessage> getRetransmissionTimeoutMessages(){
        List<RetransmissionTimeoutMessage> result = new ArrayList<RetransmissionTimeoutMessage>();
        for(Object[] object : timeoutMessages){
            result.add((RetransmissionTimeoutMessage) object[0]);
        }
        return result;
    }

    public Long getRetransmissionTimeoutTime(int index){
        return (Long) (timeoutMessages.get(index))[1];
    }

    public RetransmissionTimeoutMessage getRetransmissionTimeoutMessage(int index){
        return (RetransmissionTimeoutMessage) (timeoutMessages.get(index))[0];
    }

    public long getRequestSendTime() {
        return requestSendTime;
    }
}
