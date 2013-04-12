package de.uniluebeck.itm.spitfire.nCoap.application.webservice;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
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
     * @param path may be null, is ignored anyway since the path is defined in the CoAP draft
     * @param initialStatus the Map containing all available path
     */
    public WellKnownCoreResource(String path, Map<String, WebService> initialStatus) {
        super("/.well-known/core", initialStatus);
    }

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
