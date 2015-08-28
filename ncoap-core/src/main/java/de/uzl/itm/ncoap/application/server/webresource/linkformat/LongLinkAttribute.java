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
package de.uzl.itm.ncoap.application.server.webresource.linkformat;

/**
 * A {@link LongLinkAttribute} is a {@link LinkAttribute} with values of type 'Long'
 * ({@link LinkAttribute#LONG_ATTRIBUTE}).
 *
 * @author Oliver Kleine
 */
public class LongLinkAttribute extends LinkAttribute<Long> {

    /**
     * The key of the content type attribute ("ct")
     */
    public static final String CONTENT_TYPE = "ct";

    /**
     * The key of the maximum size estimation attribute ("sz")
     */
    public static final String MAX_SIZE_ESTIMATE = "sz";

    static{
        LinkAttribute.registerAttribute(CONTENT_TYPE, new AttributeProperties(true, LONG_ATTRIBUTE));
        LinkAttribute.registerAttribute(MAX_SIZE_ESTIMATE, new AttributeProperties(false, LONG_ATTRIBUTE));
    }

    /**
     * This is method is just for internal use withiin the nCoAP framework
     */
    public static void initialize(){
        //nothing to do...
    }

    /**
     * Creates a new instance of
     * {@link LongLinkAttribute}
     *
     * @param key the key of the attribute (see static constants for supported keys)
     * @param value the attributes value
     */
    public LongLinkAttribute(String key, Long value) {
        super(key, value);

        if(LinkAttribute.getAttributeType(key) != LinkAttribute.LONG_ATTRIBUTE){
            throw new IllegalArgumentException(
                    "Given attribute key \"" + key + "\" does not refer to a numeric attribute."
            );
        }
    }


    @Override
    public int hashCode() {
        return this.getKey().hashCode() | this.getValue().hashCode();
    }


    @Override
    public boolean equals(Object object) {
        if(!(object instanceof LongLinkAttribute))
            return false;

        LongLinkAttribute other = (LongLinkAttribute) object;

        return this.getKey().equals(other.getKey()) && this.getValue().equals(other.getValue());
    }
}
