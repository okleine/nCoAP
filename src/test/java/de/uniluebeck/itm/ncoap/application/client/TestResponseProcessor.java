/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.application.client;
//
//import com.google.common.util.concurrent.SettableFuture;
//import de.uniluebeck.itm.ncoap.communication.observe.ObservationTimeoutProcessor;
//import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.*;
//import de.uniluebeck.itm.ncoap.message.CoapRequest;
//import de.uniluebeck.itm.ncoap.message.CoapResponse;
//import org.apache.log4j.Logger;
//
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Instance of {@link CoapResponseProcessor} additionally implementing {@link RetransmissionTimeoutProcessor},
// * {@link EmptyAcknowledgementProcessor}, and {@link RetransmissionTimeoutProcessor} to handle all possible
// * events related to a sent {@link CoapRequest}.
// */
//public class TestResponseProcessor implements CoapResponseProcessor, RetransmissionTimeoutProcessor,
//        EmptyAcknowledgementProcessor, RetransmissionProcessor, ObservationTimeoutProcessor {
//
//    private Logger log = Logger.getLogger(this.getClass().getName());
//
//    private ArrayList<Object[]> responses = new ArrayList<Object[]>();
//
//    private ArrayList<Object[]> emptyAcknowledgements = new ArrayList<Object[]>();
//
//    private ArrayList<Object[]>  timeoutMessages = new ArrayList<Object[]>();
//
//    private ArrayList<Long> requestSentTimes = new ArrayList<Long>();
//
//    private boolean observationTimedOut = false;
//
//    @Override
//    public void processCoapResponse(CoapResponse coapResponse) {
//       responses.add(new Object[]{coapResponse, System.currentTimeMillis()});
//       log.info("Received Response #" + responses.size() + ": " + coapResponse);
//    }
//
//    @Override
//    public void requestSent() {
//        requestSentTimes.add(System.currentTimeMillis());
//        log.info("Attempt #" + requestSentTimes.size() + " to sent request.");
//    }
//
//    @Override
//    public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage message) {
//        log.info("Retransmission Timeout: " + message);
//        timeoutMessages.add(new Object[]{message, System.currentTimeMillis()});
//    }
//
//    @Override
//    public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) {
//        log.info("Received empty ACK: " + message);
//        emptyAcknowledgements.add(new Object[]{message, System.currentTimeMillis()});
//    }
//
//    @Override
//    public void processObservationTimeout(InetSocketAddress remoteAddress) {
//        log.info("Observation timed out!");
//        setObservationTimedOut(true);
//        //continueObservation.set(null);
//    }
//
//    protected void setObservationTimedOut(boolean observationTimedOut){
//        this.observationTimedOut = observationTimedOut;
//    }
//
//    public boolean isObservationTimedOut(){
//        return this.observationTimedOut;
//    }
//
//    /**
//     * Returns a {@link List} containing all received {@link CoapResponse} instances. The elements in the list are
//     * ordered according to the time of reception (first element = first received {@link CoapResponse}).
//     *
//     * @return all received {@link CoapResponse} instances
//     */
//    public List<CoapResponse> getCoapResponses(){
//        List<CoapResponse> result = new ArrayList<CoapResponse>();
//        for(Object[] object : responses){
//            result.add((CoapResponse) object[0]);
//        }
//        return result;
//    }
//
//    /**
//     * Returns the time at which a particular {@link CoapResponse} was received
//     *
//     * @param index the index of the {@link CoapResponse} the time should be returned for. The index is given
//     *              by the position in the list of responses returned by {@link #getCoapResponses()}.
//     *
//     * @return the time at which a particular {@link CoapResponse} was received
//     */
//    public Long getCoapResponseReceptionTime(int index){
//        return (Long) (responses.get(index))[1];
//    }
//
//    /**
//     * Returns a particular {@link CoapResponse} according to its index
//     *
//     * @param index the index of the {@link CoapResponse} the time should be returned for. The index is given
//     *              by the position in the list of responses returned by {@link #getCoapResponses()}.
//     *
//     * @return a particular {@link CoapResponse} according to its index
//     */
//    public CoapResponse getCoapResponse(int index){
//        return (CoapResponse) (responses.get(index))[0];
//    }
//
//    /**
//     * Returns a {@link List} containing all received {@link InternalEmptyAcknowledgementReceivedMessage}s.
//     * The elements in the list are ordered according to the time of reception (first element = first received
//     * {@link InternalEmptyAcknowledgementReceivedMessage}).
//     *
//     * @return a {@link List} containing all received {@link InternalEmptyAcknowledgementReceivedMessage}s.
//     */
//    public List<InternalEmptyAcknowledgementReceivedMessage> getEmptyAcknowledgementReceivedMessages(){
//        List<InternalEmptyAcknowledgementReceivedMessage> result = new ArrayList<InternalEmptyAcknowledgementReceivedMessage>();
//        for(Object[] object : emptyAcknowledgements){
//            result.add((InternalEmptyAcknowledgementReceivedMessage) object[0]);
//        }
//        return result;
//    }
//
//    /**
//     * Returns the time at which a particular {@link InternalEmptyAcknowledgementReceivedMessage} was received
//     *
//     * @param index the index of the {@link InternalEmptyAcknowledgementReceivedMessage} the time should be returned
//     *              for. The index is given by the position in the list of responses returned by
//     *              {@link #getEmptyAcknowledgementReceivedMessages()}).
//     *
//     * @return the time at which a particular {@link InternalEmptyAcknowledgementReceivedMessage} was received
//     */
//    public Long getEmptyAcknowledgementeReceptionTime(int index){
//        return (Long) (emptyAcknowledgements.get(index))[1];
//    }
//
//    /**
//     * Returns a particular {@link InternalEmptyAcknowledgementReceivedMessage} according to its index
//     *
//     * @param index the index of the {@link InternalEmptyAcknowledgementReceivedMessage} the time should be returned
//     *              for. The index is given by the position in the list of responses returned by
//     *              {@link #getEmptyAcknowledgementReceivedMessages()}.
//     *
//     * @return a particular {@link InternalEmptyAcknowledgementReceivedMessage} according to its index
//     */
//    public InternalEmptyAcknowledgementReceivedMessage getEmptyAcknowledgementReceivedMessage(int index){
//        return (InternalEmptyAcknowledgementReceivedMessage) (responses.get(index))[0];
//    }
//
//    /**
//     * Returns a {@link List} containing all received {@link InternalRetransmissionTimeoutMessage} instances. The
//     * elements in the list are ordered according to the time of reception (first element = first received
//     * {@link InternalRetransmissionTimeoutMessage}).
//     *
//     * @return all received {@link InternalRetransmissionTimeoutMessage} instances
//     */
//    public List<InternalRetransmissionTimeoutMessage> getRetransmissionTimeoutMessages(){
//        List<InternalRetransmissionTimeoutMessage> result = new ArrayList<InternalRetransmissionTimeoutMessage>();
//        for(Object[] object : timeoutMessages){
//            result.add((InternalRetransmissionTimeoutMessage) object[0]);
//        }
//        return result;
//    }
//
//    protected void resetRetransmissionCounter(){
//        requestSentTimes.clear();
//    }
//
//    /**
//     * Returns the time at which a particular {@link InternalRetransmissionTimeoutMessage} was received
//     *
//     * @param index the index of the {@link InternalRetransmissionTimeoutMessage} the time should be returned
//     *              for. The index is given by the position in the list of responses returned by
//     *              {@link #getRetransmissionTimeoutMessages()}).
//     *
//     * @return the time at which a particular {@link InternalRetransmissionTimeoutMessage} was received
//     */
//    public Long getRetransmissionTimeoutTime(int index){
//        return (Long) (timeoutMessages.get(index))[1];
//    }
//
//    /**
//     * Returns a particular {@link InternalRetransmissionTimeoutMessage} according to its index
//     *
//     * @param index the index of the {@link InternalRetransmissionTimeoutMessage} the time should be returned for.
//     *              The index is given by the position in the list of responses returned by
//     *              {@link #getRetransmissionTimeoutMessages()}.
//     *
//     * @return a particular {@link InternalRetransmissionTimeoutMessage} according to its index
//     */
//    public InternalRetransmissionTimeoutMessage getRetransmissionTimeoutMessage(int index){
//        return (InternalRetransmissionTimeoutMessage) (timeoutMessages.get(index))[0];
//    }
//
//    /**
//     * Returns a {@link List} containing all times the {@link CoapRequest} related to the {@link CoapResponse}(s) this
//     * {@link TestResponseProcessor} is supposed to handle was sent (incl. retransmission times).
//     *
//     * @return a {@link List} containing all times the {@link CoapRequest} related to the {@link CoapResponse}(s) this
//     * {@link TestResponseProcessor} is supposed to handle was sent (incl. retransmission times).
//     */
//    public List<Long> getRequestSentTimes() {
//        return requestSentTimes;
//    }
//
//
//}
