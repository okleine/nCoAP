package de.uniluebeck.itm.spitfire.nCoap.application.webservice;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 10.04.13
 * Time: 15:47
 * To change this template use File | Settings | File Templates.
 */
public class WellKnownCoreResource extends NotObservableWebService<Map<String, WebService>> {

    private static Logger log = LoggerFactory.getLogger(WellKnownCoreResource.class.getName());

    /**
     * Creates the well-known/core resource at path /.well-known/core as defined in the CoAP draft
     * @param initialStatus the Map containing all available path
     */
    public WellKnownCoreResource(Map<String, WebService> initialStatus) {
        super("/.well-known/core", initialStatus);
    }

    /**
     * The .well-known/core resource is a standard webservice to be provided by every CoAP webserver as defined in
     * the CoAP protocol draft. It only allows requests with {@link Code#GET}. Any other out of {@link Code#POST},
     * {@link Code#PUT} or {@link Code#DELETE} returns a {@link CoapResponse} with {@link Code#METHOD_NOT_ALLOWED_405}.
     *
     * In case of a request with {@link Code#GET} it returns a {@link CoapResponse} with {@link Code#CONTENT_205} and
     * with a payload listing all paths to the available resources (i.e. {@link WebService} instances}). The
     * payload is always formatted in {@link MediaType#APP_LINK_FORMAT}. If the request contains an
     * {@link OptionName#ACCEPT} option requesting another payload format, this option is ignored.
     *
     * @param request The {@link CoapRequest} to be processed by the {@link WebService} instance
     * @param remoteAddress The address of the sender of the request
     * @return
     */
    @Override
    public CoapResponse processMessage(CoapRequest request, InetSocketAddress remoteAddress) {
        if(request.getCode() != Code.GET){
            return new CoapResponse(Code.METHOD_NOT_ALLOWED_405);
        }

        CoapResponse response = new CoapResponse(Code.CONTENT_205);
        StringBuffer buffer = new StringBuffer();

        //TODO make this CoRE link format
        for(Map.Entry<String, WebService> entry : getResourceStatus().entrySet()){
            buffer.append("<" + entry.getKey() + ">,\n");
        }
        buffer.deleteCharAt(buffer.length()-2);

        try {
            response.setPayload(ChannelBuffers.wrappedBuffer(buffer.toString().getBytes(Charset.forName("UTF-8"))));
            response.setContentType(OptionRegistry.MediaType.APP_LINK_FORMAT);

        } catch (MessageDoesNotAllowPayloadException e) {
            log.error("This should never happen.", e);
        } catch (InvalidOptionException e) {
            log.error("This should never happen.", e);
        } catch (ToManyOptionsException e) {
            log.error("This should never happen.", e);
        }

        return response;
    }
}
