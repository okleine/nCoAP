package de.uniluebeck.itm.ncoap.applications;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WrappedResourceStatus;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import java.net.InetSocketAddress;

/**
 * Created by olli on 30.03.14.
 */
public class SimpleNotObservableWebservice extends NotObservableWebservice<String>{

    private static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private int weakEtag;

    protected SimpleNotObservableWebservice(String servicePath, String initialStatus, long lifetimeSeconds) {
        super(servicePath, initialStatus, lifetimeSeconds);
    }


    @Override
    public byte[] getEtag(long contentFormat) {
        return Ints.toByteArray(weakEtag & Longs.hashCode(contentFormat));
    }


    @Override
    public void updateEtag(String resourceStatus) {
        weakEtag = resourceStatus.hashCode();
    }


    @Override
    public void shutdown() {
        //nothing to to
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteEndpoint) throws Exception {

        if(coapRequest.getMessageCodeName() != MessageCode.Name.GET)
            setMethodNotAllowedResponse(responseFuture, coapRequest);

        else
            processCoapGetRequest(responseFuture, coapRequest);

    }


    public void processCoapGetRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest){
        //create resource status
        WrappedResourceStatus resourceStatus;
        if(coapRequest.getAcceptedContentFormats().isEmpty())
            resourceStatus = getWrappedResourceStatus(DEFAULT_CONTENT_FORMAT);

        else
            resourceStatus = getWrappedResourceStatus(coapRequest.getAcceptedContentFormats());

        //create CoAP response
        CoapResponse coapResponse;
        if(resourceStatus == null){
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.NOT_ACCEPTABLE_406);
            coapResponse.setContent("None of the accepted content formats is supported!".getBytes(CoapMessage.CHARSET),
                    ContentFormat.TEXT_PLAIN_UTF8);
        }

        else{
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), resourceStatus.getContentFormat());
            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());
        }

        responseFuture.set(coapResponse);
    }


    public void setMethodNotAllowedResponse(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest)
        throws Exception{

        CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageTypeName(),
                MessageCode.Name.METHOD_NOT_ALLOWED_405);

        coapResponse.setContent("Only method GET is allowed!".getBytes(CoapMessage.CHARSET),
                ContentFormat.TEXT_PLAIN_UTF8);

        responseFuture.set(coapResponse);
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        String result = null;
        if(contentFormat == ContentFormat.TEXT_PLAIN_UTF8)
            result = "The resource status is " + getResourceStatus() + ".";

        else if(contentFormat == ContentFormat.APP_XML)
            result = "<status>" + getResourceStatus() + "</status>";


        if(result == null)
            return null;

        else
            return result.getBytes(CoapMessage.CHARSET);
    }
}
