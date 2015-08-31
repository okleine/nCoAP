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

import java.util.*;

/**
 * Each {@link de.uzl.itm.ncoap.application.server.webresource.Webresource} can be enriched with
 * {@link LinkAttribute}s to provide additional information about its
 * capabilities in terms of offered content formats, observability, etc...
 *
 * However, the attached {@link LinkAttribute}s are accessible to interested clients via the
 * <code>.well-known/core</code> resource which is provided by every CoAP server.
 *
 * @author Oliver Kleine
 */
public abstract class LinkAttribute<T> {

    /**
     * The integer value (0) representing the internal type for empty attributes, i.e. attributes without value
     */
    public static final int EMPTY_ATTRIBUTE = 0;

    /**
     * The integer value (1) representing the internal type for long attributes, i.e. attributes with numeric values
     */
    public static final int LONG_ATTRIBUTE = 1;

    /**
     * The integer value (2) representing the internal type for string attributes, i.e. attributes with string values
     */
    public static final int STRING_ATTRIBUTE = 2;

    private String key;
    private T value;

    private static Map<String, AttributeProperties> ATTRIBUTES = new HashMap<>();

    private static boolean initialized = false;

    public static void initialize(){
        EmptyLinkAttribute.initialize();
        ATTRIBUTES.putAll(EmptyLinkAttribute.getAttributes());
        LongLinkAttribute.initialize();
        ATTRIBUTES.putAll(LongLinkAttribute.getAttributes());
        StringLinkAttribute.initialize();
        ATTRIBUTES.putAll(StringLinkAttribute.getAttributes());

        initialized = true;
    }


    protected LinkAttribute(String attributeKey, T value) throws IllegalArgumentException{
        if(!initialized){
            initialize();
        }

        this.key = attributeKey;

        if(!ATTRIBUTES.containsKey(attributeKey))
            throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");

        this.value = value;
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

        return ATTRIBUTES.get(attributeKey).isMultipleValues();
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
    public static int getAttributeType(String attributeKey) throws IllegalArgumentException{
        if(ATTRIBUTES.containsKey(attributeKey))
            return ATTRIBUTES.get(attributeKey).getAttributeType();

        return -1;
    }

    @Override
    public String toString(){
        return "Link Attribute (key: " + this.getKey() + ", value: " + this.getValue() + ")";
    }

    static class AttributeProperties {

        private boolean multipleValues;
        private int attributeType;


        AttributeProperties(boolean multipleValues, int attributeType){
            this.multipleValues = multipleValues;
            this.attributeType = attributeType;
        }

        public boolean isMultipleValues(){
            return multipleValues;
        }

        public int getAttributeType(){
            return this.attributeType;
        }
    }
}
