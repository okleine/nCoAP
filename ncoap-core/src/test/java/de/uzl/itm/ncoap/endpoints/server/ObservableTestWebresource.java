/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.endpoints.server;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
* This observable resource changes its status periodically with a delay given as argument for the constructor.
* There are 5 possible states. The internal state representation, i.e. the returned value v of
* {@link #getResourceStatus()} is 1, 2, 3, 4 or 5. The payload contained in a the {@link de.uzl.itm.ncoap.message.CoapResponse}
* produced by {@link #processCoapRequest(SettableFuture, de.uzl.itm.ncoap.message.CoapRequest, InetSocketAddress)} is "Status 1",
* "Status 2", ..., "Status 5".
*
* @author Oliver Kleine
*/
public class ObservableTestWebresource extends ObservableWebresource<Integer> {

    public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private static int NO_AUTOMATIC_UPDATE = 0;

    private static Logger LOG = LoggerFactory.getLogger(ObservableTestWebresource.class.getName());

    private long artificalDelay;
    private long updateInterval;


    /**
     * @param path the absolute path of the service
     * @param initialStatus the initial internal status
     * @param artificalDelay the artificial delay in milliseconds to simulate a longer processing time for inbound
     *                       requests
     * @param updateInterval the time passing between two status updates (in milliseconds)
     */
    public ObservableTestWebresource(String path, int initialStatus, long artificalDelay, final long updateInterval,
                                     ScheduledExecutorService executor) {
        super(path, initialStatus, executor);
        this.artificalDelay = artificalDelay;
        this.updateInterval = updateInterval;

        if (updateInterval != NO_AUTOMATIC_UPDATE) {
            executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    int newStatus = (getResourceStatus() + 1) % 6;
                    if (newStatus == 0)
                        newStatus = 1;
                    setResourceStatus(newStatus, updateInterval / 1000);
                }
            }, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * @param path the absolute path of the service
     * @param initialStatus the initial internal status
     * @param artificalDelay the artificial delay in milliseconds to simulate a longer processing time for inbound
     *                       requests
     */
    public ObservableTestWebresource(String path, int initialStatus, long artificalDelay,
                                     ScheduledExecutorService executor) {
        this(path, initialStatus, artificalDelay, NO_AUTOMATIC_UPDATE, executor);
    }


    @Override
    public byte[] getEtag(long contentFormat) {
        return Ints.toByteArray(Ints.hashCode(getResourceStatus()));
    }

    @Override
    public void updateEtag(Integer resourceStatus) {
        //Nothing to do...
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteSocket) {
        LOG.debug("Process Request from \"{}\".", remoteSocket);
        try{

            Thread.sleep(this.artificalDelay);

            if (coapRequest.getMessageCode() == MessageCode.GET) {
                processGetRequest(responseFuture, coapRequest, remoteSocket);
            }

            else if (coapRequest.getMessageCode() == MessageCode.POST) {
                processPostRequest(responseFuture, coapRequest, remoteSocket);
            }

            else if (coapRequest.getMessageCode() == MessageCode.PUT) {
                processPutRequest(responseFuture, coapRequest, remoteSocket);
            }

            else if (coapRequest.getMessageCode() == MessageCode.DELETE) {
                processDeleteRequest(responseFuture, coapRequest, remoteSocket);
            }

            else{
                LOG.error("This should never happen!");
                responseFuture.setException(new Exception("Something went wrong..."));
            }
        }

        catch(InterruptedException ex) {
            responseFuture.setException(ex);
        }

    }


    private void processGetRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteSocket) {


        Set<Long> acceptedContentFormats = coapRequest.getAcceptedContentFormats();

        WrappedResourceStatus wrappedResourceStatus = null;
        long contentFormat;

        //Use default content format if there was no accept option set in the request
        if (acceptedContentFormats.isEmpty()) {
            contentFormat = DEFAULT_CONTENT_FORMAT;
            wrappedResourceStatus = getWrappedResourceStatus(contentFormat);
        }

        //Try all accepted content formats (if accept option was set in the request)
        else{
            for(long acceptedContentFormat : acceptedContentFormats) {
                wrappedResourceStatus = getWrappedResourceStatus(acceptedContentFormat);

                if (wrappedResourceStatus != null)
                    break;
            }
        }

        if (wrappedResourceStatus == null) {
            CoapResponse coapResponse =
                    new CoapResponse(coapRequest.getMessageType(), MessageCode.BAD_REQUEST_400);

            String content = "Resource status is not available in any of the accepted content formats!";
            coapResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);

            responseFuture.set(coapResponse);
        }

        else{
            CoapResponse coapResponse =
                    new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);

            coapResponse.setContent(wrappedResourceStatus.getContent(), wrappedResourceStatus.getContentFormat());
            coapResponse.setEtag(wrappedResourceStatus.getEtag());
            coapResponse.setMaxAge(wrappedResourceStatus.getMaxAge());

            if (coapRequest.getObserve() == 0) {
                coapResponse.setObserve();
            }

            responseFuture.set(coapResponse);
        }
    }


    private void processPostRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                  InetSocketAddress remoteAddress) {
        responseFuture.setException(new Exception("Something went wrong..."));
    }


    private void processPutRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                  InetSocketAddress remoteAddress) {
        responseFuture.setException(new Exception("Something went wrong..."));
    }


    private void processDeleteRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                  InetSocketAddress remoteAddress) {
        responseFuture.setException(new Exception("Something went wrong..."));
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {

        switch((int) contentFormat) {

            case (int) ContentFormat.TEXT_PLAIN_UTF8:
                return ("Status #" + getResourceStatus()).getBytes(CoapMessage.CHARSET);

            case (int) ContentFormat.APP_XML:
                return ("<status>" + getResourceStatus() + "</status>").getBytes(CoapMessage.CHARSET);

            default:
                return null;
        }
    }


    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteSocket) {
        return true;
    }

    @Override
    public void removeObserver(InetSocketAddress remoteSocket) {
        // nothing to do
    }
}
