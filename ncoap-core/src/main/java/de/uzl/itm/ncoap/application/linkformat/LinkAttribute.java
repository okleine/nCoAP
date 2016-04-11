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
package de.uzl.itm.ncoap.application.linkformat;

import com.google.common.collect.ImmutableMap;

import java.util.*;
import static de.uzl.itm.ncoap.application.linkformat.LinkAttribute.Type.*;
import static de.uzl.itm.ncoap.application.linkformat.LongLinkAttribute.*;
import static de.uzl.itm.ncoap.application.linkformat.EmptyLinkAttribute.*;
import static de.uzl.itm.ncoap.application.linkformat.StringLinkAttribute.*;

/**
 * Each {@link de.uzl.itm.ncoap.application.server.resource.Webresource} can be enriched with
 * {@link LinkAttribute}s to provide additional information about its
 * capabilities in terms of offered content formats, observability, etc...
 *
 * However, the attached {@link LinkAttribute}s are accessible to interested clients via the
 * <code>.well-known/core</code> resource which is provided by every CoAP server.
 *
 * @author Oliver Kleine
 */
public abstract class LinkAttribute<T> {

    public static enum Type {
        UNKNOWN, EMPTY, LONG, STRING
    }

    private static Map<String, AttributeProperties> ATTRIBUTES = new HashMap<>();
    static {
        ATTRIBUTES.putAll(ImmutableMap.<String, AttributeProperties>builder()
            .put(CONTENT_TYPE, new AttributeProperties(true, LONG))
            .put(MAX_SIZE_ESTIMATE, new AttributeProperties(false, LONG))
            .put(OBSERVABLE, new AttributeProperties(false, EMPTY))
            .put(RESOURCE_TYPE, new AttributeProperties(true, STRING))
            .put(INTERFACE, new AttributeProperties(false, STRING))
            .build()
        );
    }

    private String key;
    private T value;


    protected LinkAttribute(String attributeKey, T value) throws IllegalArgumentException{

        this.key = attributeKey;

        if(!ATTRIBUTES.containsKey(attributeKey)) {
            throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");
        } else {
            this.value = value;
        }
    }


    /**
     * Returns the key of the link attribute
     * @return the key of the link attribute
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value of the link attribute
     * @return the value of the link attribute
     */
    public T getValue() {
        return value;
    }


    @Override
    public abstract int hashCode();


    @Override
    public abstract boolean equals(Object object);


    public static boolean allowsMultipleValues(String attributeKey){
        if(!ATTRIBUTES.containsKey(attributeKey))
            throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");

        return ATTRIBUTES.get(attributeKey).permitMultipleValues();
    }

    /**
     * Creates an instance of {@link de.uzl.itm.ncoap.application.linkformat.LinkAttribute} from a URI query
     * parameter, i.e. <code>key=value</code>.
     *
     * @param queryParameter e.g. "<code>ct=0</code>" for content-format "plain text"
     *
     * @return an instance of {@link de.uzl.itm.ncoap.application.linkformat.LinkAttribute}
     *
     * @throws IllegalArgumentException
     */
    public static LinkAttribute createFromUriQuery(String queryParameter) throws IllegalArgumentException {

        if(!queryParameter.equals("")){
            String[] param = queryParameter.split("=");

            if(param.length != 2) {
                throw new IllegalArgumentException("Could not parse query " + queryParameter);
            }

            LinkAttribute linkAttribute;
            LinkAttribute.Type attributeType = LinkAttribute.getAttributeType(param[0]);

            if(STRING.equals(attributeType)) {
                linkAttribute = new StringLinkAttribute(param[0], param[1]);
            } else if(LONG.equals(attributeType)) {
                linkAttribute = new LongLinkAttribute(param[0], Long.parseLong(param[1]));
            } else if(EMPTY.equals(attributeType)) {
                linkAttribute = new EmptyLinkAttribute(param[0]);
            } else {
                throw new IllegalArgumentException("Unknown link attribute key (\"" + param[0] + "\")");
            }

            return linkAttribute;
        }

        return null;
    }

    /**
     * Returns the number corresponding to the given attribute key, i.e. 0, 1, or 2 (see static constants) or
     * -1 for unknown attributes.
     *
     * @param attributeKey the key to retrieve the attribute type for
     *
     * @return the number corresponding to the given attribute key, i.e. 0, 1, or 2 (see static constants) or
     * -1 for unknown attributes.
     */
    public static Type getAttributeType(String attributeKey) throws IllegalArgumentException{
        if(ATTRIBUTES.containsKey(attributeKey)) {
            return ATTRIBUTES.get(attributeKey).getAttributeType();
        } else {
            return Type.UNKNOWN;
        }
    }

    @Override
    public String toString(){
        return "Link Attribute (key: " + this.getKey() + ", value: " + this.getValue() + ")";
    }

    static class AttributeProperties {

        private boolean multipleValues;
        private Type attributeType;


        AttributeProperties(boolean multipleValues, Type attributeType){
            this.multipleValues = multipleValues;
            this.attributeType = attributeType;
        }

        public boolean permitMultipleValues(){
            return multipleValues;
        }

        public Type getAttributeType(){
            return this.attributeType;
        }
    }
}
