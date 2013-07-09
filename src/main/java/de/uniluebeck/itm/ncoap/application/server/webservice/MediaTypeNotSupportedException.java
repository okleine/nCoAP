package de.uniluebeck.itm.ncoap.application.server.webservice;

import de.uniluebeck.itm.ncoap.communication.core.CoapException;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;

/**
 * Exception to {@link MediaTypeNotSupportedException} indicate that a requested {@link MediaType} is not provided by a
 * {@link WebService} instance.
 *
 * @author Oliver Kleine
 */
public class MediaTypeNotSupportedException extends CoapException {

    private MediaType mediaType;

    /**
     * @param mediaType the {@link MediaType} that caused the exception
     */
    public MediaTypeNotSupportedException(MediaType mediaType){
        super("Mediatype not suported: " + mediaType);
        this.mediaType = mediaType;
    }

    /**
     * Returns the {@link MediaType} that caused the exception
     * @return the {@link MediaType} that caused the exception
     */
    public MediaType getMediaType() {
        return mediaType;
    }
}
