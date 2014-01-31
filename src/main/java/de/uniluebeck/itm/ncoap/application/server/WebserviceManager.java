package de.uniluebeck.itm.ncoap.application.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;
import de.uniluebeck.itm.ncoap.communication.observe.InternalObservableResourceRegistrationMessage;
import de.uniluebeck.itm.ncoap.message.*;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.Option;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The {@link WebserviceManager} is the topmost {@link ChannelHandler} of the {@link ChannelPipeline} returned
 * by {@link ServerChannelPipelineFactory}. It is responsible to dispatch incoming {@link CoapRequest}s, i.e.
 *
 * <ul>
 *     <li>invoke the method {@link Webservice#processCoapRequest(SettableFuture, CoapRequest, InetSocketAddress)} of
 *     the addressed {@link Webservice} instance (if it exists) or</li>
 *     <li>invoke the method {@link WebserviceCreator#handlePutRequest(SettableFuture, CoapRequest)} if the
 *     incoming {@link CoapRequest} has {@link MessageCode.Name#PUT} and the addresses service does not (yet) exist.
 *     </li>
 * </ul>
 *
 * Upon invocation of the method it awaits a proper {@link CoapResponse} and  sends that response downstream, i.e.
 * in the direction of the local socket, i.e. to the client that sent the {@link CoapRequest}.
 *
 * However, the {@link WebserviceManager} is aware of all registered {@link Webservice} instances and is thus to be
 * used to register new {@link Webservice} instances, e.g. while processing an incoming {@link CoapRequest} with
 * {@link MessageCode.Name#POST}. That is why all {@link Webservice} instances can reference their
 * {@link WebserviceManager} via {@link Webservice#getWebserviceManager()}.
 *
 * Furthermore, on incoming {@link CoapRequest}s with {@link MessageCode.Name#DELETE} it shuts down the addresses
 * {@link Webservice} if the method {@link Webservice#allowsDelete()} returns <code>true</code>. Otherwise, it sends
 * a proper error message to the client.
 *
 * Last but not least it checks whether the {@link Option.Name#IF_NONE_MATCH} is set on incoming {@link CoapRequest}s
 * and sends a {@link CoapResponse} with {@link MessageCode.Name#PRECONDITION_FAILED_412} if the option was set but the
 * addressed {@link Webservice} already exists.
 *
 * @author Oliver Kleine
 */
public class WebserviceManager extends SimpleChannelUpstreamHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    //This map holds all open, i.e. not yet answered requests to avoid duplicate request processing
    private Multimap<InetSocketAddress, CoapRequest> openRequests;            

    //This map holds all registered webservices (key: URI path, value: Webservice instance)
    private HashMap<String, Webservice> registeredServices;

    private ScheduledExecutorService executorService;
    private WebserviceCreator webServiceCreator;
    private Channel channel;


    /**
     * @param webServiceCreator Instance of {@link WebserviceCreator} to deal with incoming {@link CoapRequest}s with
     *                          {@link MessageCode.Name#PUT} if the addresses {@link Webservice} does not exist.
     *
     * @param executorService the {@link ScheduledExecutorService} to process the task to send a {@link CoapResponse}
     *                        and
     */
    public WebserviceManager(WebserviceCreator webServiceCreator, ScheduledExecutorService executorService){

        this.openRequests = Multimaps.synchronizedMultimap(HashMultimap.<InetSocketAddress, CoapRequest>create());
        this.registeredServices = new HashMap<String, Webservice>();
        this.executorService = executorService;
        this.webServiceCreator = webServiceCreator;
        
        registerService(new WellKnownCoreResource(registeredServices));
    }

    public void setChannel(Channel channel){
        this.channel = channel;
    }

    /**
     * This method is called by the Netty framework whenever a new message is received to be processed by the server.
     * For each incoming request a new Thread is created to handle the request (by invoking the method
     * <code>receiveCoapRequest</code>).
     *
     * @param ctx The {@link org.jboss.netty.channel.ChannelHandlerContext} connecting relating this class (which implements the
     * {@link org.jboss.netty.channel.ChannelUpstreamHandler} interface) to the datagramChannel that received the message.
     * @param me the {@link org.jboss.netty.channel.MessageEvent} containing the actual message
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me){
        log.debug("Incoming (from {}): {}.", me.getRemoteAddress(), me.getMessage());

        if(me.getMessage() instanceof CoapRequest){
            messageReceived(ctx, (CoapRequest) me.getMessage(), (InetSocketAddress) me.getRemoteAddress());
            me.getFuture().setSuccess();
        }
        else {
            log.warn("Server received inadequeate message: {}", me.getMessage());
            ctx.sendUpstream(me);
        }
    }


    private void messageReceived(final ChannelHandlerContext ctx, final CoapRequest coapRequest,
                                final InetSocketAddress remoteSocketAddress){

        //Add incoming request to list of open requests (to avoid duplicates)
        if(!openRequests.put(remoteSocketAddress, coapRequest)){
            log.warn("Received request duplicate from {}: {}", remoteSocketAddress, coapRequest);
            return;
        }


        //Create settable future to wait for response
        final SettableFuture<CoapResponse> responseFuture = SettableFuture.create();

        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try{
                    if(openRequests.remove(remoteSocketAddress, coapRequest))
                        log.info("Removed message {} from {} from list of open requests!",
                                coapRequest, remoteSocketAddress);
                    else
                        log.warn("Could not remove message {} from {} from list of open requests!",
                                coapRequest, remoteSocketAddress);

                    CoapResponse coapResponse = responseFuture.get();
                    coapResponse.setMessageID(coapRequest.getMessageID());
                    coapResponse.setToken(coapRequest.getToken());
                    sendCoapResponse(ctx, remoteSocketAddress, coapResponse);
                }
                catch (Exception e) {
                    log.error("Exception while processing incoming request", e);
                    CoapResponse coapResponse = CoapResponse.createInternalServerErrorResponse(e,
                            coapRequest.getMessageID(), coapRequest.getToken());
                    sendCoapResponse(ctx, remoteSocketAddress, coapResponse);
                }
            }
        }, executorService);


        //Look up web service instance to handle the request
        final Webservice webservice = registeredServices.get(coapRequest.getUriPath());

        try{
            //The requested Webservice does not exist
            if(webservice == null)
                handleRequestForNotExistingService(coapRequest, responseFuture);

            //The IF-NON-MATCH option indicates that the request is only to be processed if the webservice does not
            //(yet) exist. But it does. So send an error response
            else if(coapRequest.isIfNonMatchSet())
                sendPreconditionFailed(coapRequest.getUriPath(), responseFuture);

            //The incoming request intends do delete the addresses service
            else if(coapRequest.getMessageCodeName() == MessageCode.Name.DELETE)
                handleDeleteRequest(webservice, responseFuture);

            //The incoming request is to be handled by the addressed service
            else
                webservice.processCoapRequest(responseFuture, coapRequest, remoteSocketAddress);
        }
        catch (Exception e) {
            log.error("This should never happen.", e);
            responseFuture.setException(e);
        }
    }


    private void sendCoapResponse(final ChannelHandlerContext ctx, final InetSocketAddress remoteAddress,
                                  final CoapResponse coapResponse){
        //Write response
        log.info("Write response to {}: {}", remoteAddress, coapResponse);
        ChannelFuture future = Channels.write(ctx.getChannel(), coapResponse, remoteAddress);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("Response for token {} successfully sent to recipient {}.", coapResponse.getToken(),
                        remoteAddress);
            }
        });

    }


    private void handleDeleteRequest(Webservice webservice, SettableFuture<CoapResponse> responseFuture)
            throws Exception{

        log.info("Webservice to be deleted {}", webservice.getPath());

        MessageCode.Name messageCode;
        String content;

        if(webservice.allowsDelete()){
            if(this.shutdownService(webservice.getPath())){
                webservice.shutdown();
                messageCode = MessageCode.Name.DELETED_202;
                content = "Deleted service " + webservice.getPath();
            }
            else{
                messageCode = MessageCode.Name.INTERNAL_SERVER_ERROR_500;
                content = "Could not delete webservice " + webservice.getPath();
                log.error(content);
            }
        }
        else{
            messageCode = MessageCode.Name.METHOD_NOT_ALLOWED_405;
            content = "Service \"" + webservice.getPath() + "\" does not allow deletion using DELETE requests.";
        }

        CoapResponse coapResponse = new CoapResponse(messageCode);
        coapResponse.setContent(content.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        responseFuture.set(coapResponse);
    }


    private void sendPreconditionFailed(String servicePath, SettableFuture<CoapResponse> responseFuture)
            throws Exception{

        CoapResponse coapResponse = new CoapResponse(MessageCode.Name.PRECONDITION_FAILED_412);
        String message = "IF-NONE-MATCH option was set but service \"" + servicePath + "\" exists.";
        coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        responseFuture.set(coapResponse);
    }


    private void handleRequestForNotExistingService(CoapRequest coapRequest,
                                                    SettableFuture<CoapResponse> responseFuture) throws Exception{

        if(coapRequest.getMessageCodeName() == MessageCode.Name.PUT){
            //Call the WebserviceCreator
            log.info("Incoming PUT request to create resource {}.", coapRequest.getUriPath());
            this.webServiceCreator.handlePutRequest(responseFuture, coapRequest);
        }
        else{
            //Write error response if there is no such webservice instance registered at this server instance
            log.info("Requested service {} not found. Send 404 NOT FOUND response.", coapRequest.getUriPath());
            CoapResponse coapResponse = new CoapResponse(MessageCode.Name.NOT_FOUND_404);
            coapResponse.setContent(("Requested service \"" + coapRequest.getUriPath() +
                    "\" not found.").getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
            responseFuture.set(coapResponse);
        }
    }


    /**
     * This method is invoked by the framework if an exception occured during the reception or sending of a
     * {@link CoapMessage}.
     *
     * Classes extending {@link CoapServerApplication} may override this method to, e.g. deal with different
     * types of exceptions differently. This implementation only logs an the error message (if logging is
     * enabled).
     *
     * @param ctx the {@link ChannelHandlerContext} relating the {@link CoapServerApplication} and the
     * {@link org.jboss.netty.channel.socket.DatagramChannel} on which the exception occured
     *
     * @param e the {@link ExceptionEvent} containing, e.g. the exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
        log.error("Unsupported exception caught! Don't know what to do...", e.getCause());
    }


    /**
     * Shuts the server down by closing the datagramChannel which includes to unbind the datagramChannel from a listening port and
     * by this means free the port. All blocked or bound external resources are released.
     *
     * Prior to doing so this methods removes all registered {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice} instances from the server, i.e.
     * invokes the {@link de.uniluebeck.itm.ncoap.application.server.webservice.Webservice#shutdown()} method of all registered services.
     */
    public void shutdownAllServices() {

        for(String servicePath : registeredServices.keySet()){
            shutdownService(servicePath);
        }

        //some time to send possible update notifications (404_NOT_FOUND) to observers
        try{
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            log.error("Interrupted while shutting down CoapServerApplication!", e);
        }
    }


    /**
     * Shut down the {@link Webservice} instance registered at the given path from the server
     *
     * @param uriPath the path of the {@link Webservice} instance to be shut down
     *
     * @return <code>true</code> if the service was removed succesfully, <code>false</code> otherwise.
     */
    public synchronized boolean shutdownService(String uriPath) {
        Webservice removedService = registeredServices.remove(uriPath);

        if(removedService != null){
            log.info("Service {} removed from server.", uriPath);
            removedService.shutdown();

            if(removedService instanceof ObservableWebservice)
                channel.write(new InternalServiceRemovedFromServerMessage(uriPath));
        }
        else{
            log.error("Service {} could not be removed. Does not exist.", uriPath);
        }

        return removedService == null;
    }


    /**
     * Registers a Webservice instance at the server. After registration the service will be available at the path
     * given as <code>service.getPath()</code>.
     *
     * It is not possible to register multiple webServices at a single path. If a new service is registered at the
     * server with a path from another already registered service, then the new service replaces the old one.
     *
     * @param webservice A {@link Webservice} instance to be registered at the server
     */
    public final void registerService(final Webservice webservice) {
        webservice.setWebserviceManager(this);
        registeredServices.put(webservice.getPath(), webservice);
        log.info("Registered new service at " + webservice.getPath());

        if(webservice instanceof ObservableWebservice){
            InternalObservableResourceRegistrationMessage message =
                    new InternalObservableResourceRegistrationMessage((ObservableWebservice) webservice);

            ChannelFuture future = Channels.write(channel, message);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("Registered {} at observable resource handler.", webservice.getPath());
                }
            });
        }

        webservice.setScheduledExecutorService(executorService);
    }

}
