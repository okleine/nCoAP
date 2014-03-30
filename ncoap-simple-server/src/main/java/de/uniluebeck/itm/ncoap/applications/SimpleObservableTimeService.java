package de.uniluebeck.itm.ncoap.applications;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.client.Token;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WrappedResourceStatus;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LinkAttribute;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LongLinkAttribute;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Kleine
 */
public class SimpleObservableTimeService extends ObservableWebservice<Long> {

    private Logger log = Logger.getLogger(SimpleObservableTimeService.class.getName());
    private ScheduledFuture periodicUpdateFuture;

    public static int RESOURCE_UPDATE_INTERVAL_MILLIS = 5000;
    public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private Map<Long, String> templates;




    public SimpleObservableTimeService(String path) {
        super(path, System.currentTimeMillis());

        this.templates = new HashMap<>();

        //add support for utf-8 plain-text content
        addContentFormat(
                ContentFormat.TEXT_PLAIN_UTF8,
                "The current time is %02d:%02d:%02d"
        );

        //add support for xml content
        addContentFormat(
                ContentFormat.APP_XML,
                "<time>\n" + "\t<hour>%02d</hour>\n" + "\t<minute>%02d</minute>\n" + "\t<second>%02d</second>\n</time>"
        );
    }


    private void addContentFormat(long contentFormat, String template){
        this.templates.put(contentFormat, template);
        this.putLinkAttribute(new LongLinkAttribute(LinkAttribute.CONTENT_TYPE, contentFormat));
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService executorService){
        super.setScheduledExecutorService(executorService);
        schedulePeriodicResourceUpdate();
    }

    @Override
    public MessageType.Name getMessageTypeForUpdateNotification(InetSocketAddress remoteEndpoint, Token token) {
        return MessageType.Name.CON;
    }


    @Override
    public byte[] getEtag(long contentFormat) {
        return Longs.toByteArray(getResourceStatus() & (contentFormat << 56));
    }


    @Override
    public void updateEtag(Long resourceStatus) {
        //nothing to do here...
    }


    private void schedulePeriodicResourceUpdate(){
        this.periodicUpdateFuture = getScheduledExecutorService().scheduleAtFixedRate(new Runnable(){

            @Override
            public void run() {
                try{
                    setResourceStatus(System.currentTimeMillis(), RESOURCE_UPDATE_INTERVAL_MILLIS / 1000);
                    log.info("New status of resource " + getPath() + ": " + getResourceStatus());
                }
                catch(Exception ex){
                    log.error("Exception while updating actual time...", ex);
                }
            }
        },0, RESOURCE_UPDATE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) {
        try{
            if(coapRequest.getMessageCodeName() == MessageCode.Name.GET){
                processGet(responseFuture, coapRequest);
            }

            else {
                CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageTypeName(),
                        MessageCode.Name.METHOD_NOT_ALLOWED_405);
                String message = "Service does not allow " + coapRequest.getMessageCodeName() + " requests.";
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
                responseFuture.set(coapResponse);
            }
        }
        catch(Exception ex){
            responseFuture.setException(ex);
        }
    }


    private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest)
            throws Exception {

        //Retrieve the accepted content formats from the request
        Set<Long> contentFormats = coapRequest.getAcceptedContentFormats();

        //If accept option is not set in the request, use the default (TEXT_PLAIN_UTF8)
        if(contentFormats.isEmpty())
            contentFormats.add(DEFAULT_CONTENT_FORMAT);

        //Generate the payload of the response (depends on the accepted content formats, resp. the default
        WrappedResourceStatus resourceStatus = null;
        Iterator<Long> iterator = contentFormats.iterator();
        long contentFormat = DEFAULT_CONTENT_FORMAT;

        while(resourceStatus == null && iterator.hasNext()){
            contentFormat = iterator.next();
            resourceStatus = getWrappedResourceStatus(contentFormat);
        }

        //generate the CoAP response
        CoapResponse coapResponse;

        //if the payload could be generated, i.e. at least one of the accepted content formats (according to the
        //requests accept option(s)) is offered by the Webservice then set payload and content format option
        //accordingly
        if(resourceStatus != null){
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), contentFormat);

            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());

            if(coapRequest.isObserveSet())
                coapResponse.setObserveOption(0);
        }

        //if no payload could be generated, i.e. none of the accepted content formats (according to the
        //requests accept option(s)) is offered by the Webservice then set the code of the response to
        //400 BAD REQUEST and set a payload with a proper explanation
        else{
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.NOT_ACCEPTABLE_406);

            StringBuilder payload = new StringBuilder();
            payload.append("Requested content format(s) (from requests ACCEPT option) not available: ");
            for(long acceptedContentFormat : coapRequest.getAcceptedContentFormats())
                payload.append("[").append(acceptedContentFormat).append("]");

            coapResponse.setContent(payload.toString()
                    .getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        }

        //Set the response future with the previously generated CoAP response
        responseFuture.set(coapResponse);

    }


    @Override
    public void shutdown() {
        log.info("Shutdown service " + getPath() + ".");
        boolean futureCanceled = this.periodicUpdateFuture.cancel(true);
        log.info("Future canceled: " + futureCanceled);
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        log.debug("Try to create payload (content format: " + contentFormat + ")");

        long time = getResourceStatus() % 86400000;
        long hours = time / 3600000;
        long remainder = time % 3600000;
        long minutes = remainder / 60000;
        long seconds = (remainder % 60000) / 1000;

        String template = templates.get(contentFormat);

        if(template == null)
            return null;

        else
            return String.format(template, hours, minutes, seconds).getBytes(CoapMessage.CHARSET);
    }
}
