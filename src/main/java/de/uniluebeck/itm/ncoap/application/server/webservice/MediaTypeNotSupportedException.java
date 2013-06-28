package de.uniluebeck.itm.ncoap.application.server.webservice;

import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 28.06.13
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
 */
public class MediaTypeNotSupportedException extends Exception {

    private MediaType mediaType;

    public MediaTypeNotSupportedException(MediaType mediaType){
        this.mediaType = mediaType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}
