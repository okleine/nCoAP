package de.uniluebeck.itm.spitfire.nCoap.communication.utils;

import de.uniluebeck.itm.spitfire.nCoap.application.server.webservice.NotObservableWebService;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static org.junit.Assert.fail;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 09.04.13
 * Time: 15:50
 * To change this template use File | Settings | File Templates.
 */
public class NotObservableDummyWebService extends NotObservableWebService<String>{

    private static Logger log = LoggerFactory.getLogger(NotObservableDummyWebService.class.getName());

    private long pretendedProcessingTimeForRequests;

    public NotObservableDummyWebService(String path, String initialStatus, long pretendedProcessingTimeForRequests){
        super(path, initialStatus);
        this.pretendedProcessingTimeForRequests = pretendedProcessingTimeForRequests;
    }

    @Override
    public CoapResponse processMessage(CoapRequest request, InetSocketAddress remoteAddress) {
        log.debug("Incoming request for resource " + getPath());
        //Simulate a potentially long processing time
        try {
            Thread.sleep(pretendedProcessingTimeForRequests);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //TODO GET, POST, PUT, DELETE
        //create response
        CoapResponse response = new CoapResponse(Code.CONTENT_205);

        try {
            response.setPayload(ChannelBuffers.wrappedBuffer(getResourceStatus().getBytes(Charset.forName("UTF-8"))));
        } catch (MessageDoesNotAllowPayloadException e) {
            log.error("This should never happen.", e);
        }
        log.debug("Response for resource " + getPath() + ": " + response);
        return response;
    }




}
