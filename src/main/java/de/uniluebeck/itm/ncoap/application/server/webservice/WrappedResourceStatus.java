package de.uniluebeck.itm.ncoap.application.server.webservice;

/**
 * Created by olli on 17.03.14.
 */
public class WrappedResourceStatus {

    private byte[] content;
    private long contentFormat;
    private byte[] etag;
    private long maxAge;

    public WrappedResourceStatus(byte[] content, long contentFormat, byte[] etag, long maxAge) {
        this.content = content;
        this.contentFormat = contentFormat;
        this.etag = etag;
        this.maxAge = maxAge;
    }


    public byte[] getContent() {
        return content;
    }

    public long getContentFormat() {
        return contentFormat;
    }

    public byte[] getEtag() {
        return etag;
    }

    public long getMaxAge() {
        return maxAge;
    }
}
