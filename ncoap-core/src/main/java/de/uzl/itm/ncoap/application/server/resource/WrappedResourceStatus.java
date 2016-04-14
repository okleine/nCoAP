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
package de.uzl.itm.ncoap.application.server.resource;

/**
 * Instances of {@link WrappedResourceStatus} wrap a particular
 * representation of the actual status with meta data.
 *
 * @author Oliver Kleine
 */
public class WrappedResourceStatus {

    private byte[] content;
    private long contentFormat;
    private byte[] etag;
    private long maxAge;

    /**
     * Creates a new instance of {@link WrappedResourceStatus}
     * @param content the serialized resource status
     * @param contentFormat the number representing the serialization format
     * @param etag the ETAG value of the actual status
     * @param maxAge the number of seconds this status is allowed to be cached
     */
    public WrappedResourceStatus(byte[] content, long contentFormat, byte[] etag, long maxAge) {
        this.content = content;
        this.contentFormat = contentFormat;
        this.etag = etag;
        this.maxAge = maxAge;
    }

    /**
     * Returns the serialized resource status, i.e. a particular representation
     * @return the serialized resource status, i.e. a particular representation
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Returns the number referring to the format of the serialized resource status returned by {@link #getContent()}.
     *
     * @return the number referring to the format of the serialized resource status
     */
    public long getContentFormat() {
        return contentFormat;
    }

    /**
     * Returns the ETAG value of the serialized resource status returned by {@link #getContent()}.
     * @return the ETAG value of the serialized resource status
     */
    public byte[] getEtag() {
        return etag;
    }

    /**
     * Returns the number of seconds a cache is allowed to cache this status
     * @return the number of seconds a cache is allowed to cache this status
     */
    public long getMaxAge() {
        return maxAge;
    }
}
