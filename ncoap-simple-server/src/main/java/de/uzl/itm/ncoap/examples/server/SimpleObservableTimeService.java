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
package de.uzl.itm.ncoap.examples.server;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.application.linkformat.LongLinkAttribute;
import de.uzl.itm.ncoap.application.linkformat.StringLinkAttribute;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Oliver Kleine
 */
public class SimpleObservableTimeService extends ObservableWebresource<Long> {

    public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;
    private static Logger log = Logger.getLogger(SimpleObservableTimeService.class.getName());

    private static HashMap<Long, String> payloadTemplates = new HashMap<>();
    static{
        //Add template for plaintext UTF-8 payload
        payloadTemplates.put(
                ContentFormat.TEXT_PLAIN_UTF8,
                "The current time is %02d:%02d:%02d"
        );

        //Add template for XML payload
        payloadTemplates.put(
                ContentFormat.APP_XML,
                "<time>\n" + "\t<hour>%02d</hour>\n" + "\t<minute>%02d</minute>\n" + "\t<second>%02d</second>\n</time>"
        );
    }

    private ScheduledFuture periodicUpdateFuture;
    private int updateInterval;

    /**
     * Creates a new instance of {@link SimpleObservableTimeService}
     * @param path the path of the URI of this servicenew
     * @param updateInterval the interval (in millis) for resource status updates (e.g. 5000 for every 5 seconds).
     */
    public SimpleObservableTimeService(String path, int updateInterval, ScheduledExecutorService executor) {
        super(path, System.currentTimeMillis(), executor);

        //Set the update interval, i.e. the frequency of resource updates
        this.updateInterval = updateInterval;
        schedulePeriodicResourceUpdate();

        //Sets the link attributes for supported content types ('ct')
        this.setLinkAttribute(new LongLinkAttribute(LongLinkAttribute.CONTENT_TYPE, ContentFormat.TEXT_PLAIN_UTF8));
        this.setLinkAttribute(new LongLinkAttribute(LongLinkAttribute.CONTENT_TYPE, ContentFormat.APP_XML));

        //Sets the link attribute for the resource type ('rt')
        String attributeValue = "The actual UTC time (updated every " + updateInterval + " millis)";
        this.setLinkAttribute(new StringLinkAttribute(StringLinkAttribute.RESOURCE_TYPE, attributeValue));

        //Sets the link attribute for max-size estimation ('sz')
        this.setLinkAttribute(new LongLinkAttribute(LongLinkAttribute.MAX_SIZE_ESTIMATE, 100L));

        //Sets the link attribute for interface description ('if')
        this.setLinkAttribute(new StringLinkAttribute(StringLinkAttribute.INTERFACE, "CoAP GET"));
    }


    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteSocket) {
        return false;
    }


    @Override
    public byte[] getEtag(long contentFormat) {
        return Longs.toByteArray(getResourceStatus() | (contentFormat << 56));
    }


    @Override
    public void updateEtag(Long resourceStatus) {
        //nothing to do here as the ETAG is constructed on demand in the getEtag(long contentFormat) method
    }


    private void schedulePeriodicResourceUpdate() {
        this.periodicUpdateFuture = this.getExecutor().scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try{
                    setResourceStatus(System.currentTimeMillis(), updateInterval / 1000);
                    log.info("New status of resource " + getUriPath() + ": " + getResourceStatus());
                }
                catch(Exception ex) {
                    log.error("Exception while updating actual time...", ex);
                }
            }
        }, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) {
        try{
            if (coapRequest.getMessageCode() == MessageCode.GET) {
                processGet(responseFuture, coapRequest);
            } else {
                CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
                        MessageCode.METHOD_NOT_ALLOWED_405);
                String message = "Service does not allow " + coapRequest.getMessageCodeName() + " requests.";
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
                responseFuture.set(coapResponse);
            }
        }
        catch(Exception ex) {
            responseFuture.setException(ex);
        }
    }


    private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest)
            throws Exception {

        //Retrieve the accepted content formats from the request
        Set<Long> contentFormats = coapRequest.getAcceptedContentFormats();

        //If accept option is not set in the request, use the default (TEXT_PLAIN_UTF8)
        if (contentFormats.isEmpty())
            contentFormats.add(DEFAULT_CONTENT_FORMAT);

        //Generate the payload of the response (depends on the accepted content formats, resp. the default
        WrappedResourceStatus resourceStatus = null;
        Iterator<Long> iterator = contentFormats.iterator();
        long contentFormat = DEFAULT_CONTENT_FORMAT;

        while(resourceStatus == null && iterator.hasNext()) {
            contentFormat = iterator.next();
            resourceStatus = getWrappedResourceStatus(contentFormat);
        }

        //generate the CoAP response
        CoapResponse coapResponse;

        //if the payload could be generated, i.e. at least one of the accepted content formats (according to the
        //requests accept option(s)) is offered by the Webservice then set payload and content format option
        //accordingly
        if (resourceStatus != null) {
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), contentFormat);

            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());

            if (coapRequest.getObserve() == 0)
                coapResponse.setObserve();
        }

        //if no payload could be generated, i.e. none of the accepted content formats (according to the
        //requests accept option(s)) is offered by the Webservice then set the code of the response to
        //400 BAD REQUEST and set a payload with a proper explanation
        else{
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);

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
        log.info("Shutdown service " + getUriPath() + ".");
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

        String template = payloadTemplates.get(contentFormat);

        if (template == null)
            return null;

        else
            return String.format(template, hours, minutes, seconds).getBytes(CoapMessage.CHARSET);
    }
}
