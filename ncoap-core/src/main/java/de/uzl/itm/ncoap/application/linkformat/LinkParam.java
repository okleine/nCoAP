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
package de.uzl.itm.ncoap.application.linkformat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <p>A {@link LinkParam} is a representation of an attribute to describe a resource, resp. its representations. A
 * {@link LinkParam} instance consists of a key and a value.</p>
 *
 * @author Oliver Kleine
 */
public class LinkParam {

    //*******************************************************************************************
    // static fields, enums, and methods
    //*******************************************************************************************

    private static Logger LOG = LoggerFactory.getLogger(LinkParam.class.getName());

    /**
     * The enumeration {@link Key} contains all link-param-keys that are supported
     */
    public enum Key {
        /**
         * Corresponds to link-param-key "rel"
         */
        REL("rel", ValueType.RELATION_TYPE, ValueType.DQUOTED_RELATION_TYPES),

        /**
         * Corresponds to link-param-key "anchor"
         */
        ANCHOR("anchor", ValueType.DQUOTED_URI_REFERENCE),

        /**
         * Corresponds to link-param-key "rev"
         */
        REV("rev", ValueType.RELATION_TYPE),

        /**
         * Corresponds to link-param-key "hreflang"
         */
        HREFLANG("hreflang", ValueType.LANGUAGE_TAG),

        /**
         * Corresponds to link-param-key "media"
         */
        MEDIA("media", ValueType.MEDIA_DESC, ValueType.DQUOTED_MEDIA_DESC),

        /**
         * Corresponds to link-param-key "title"
         */
        TITLE("title", ValueType.DQUOTED_STRING),

        /**
         * Corresponds to link-param-key "title*"
         */
        TITLE_STAR("title*", ValueType.EXT_VALUE),

        /**
         * Corresponds to link-param-key "type"
         */
        TYPE("type", ValueType.MEDIA_TYPE, ValueType.DQUOTED_MEDIA_TYPE),

        /**
         * Corresponds to link-param-key "rt"
         */
        RT("rt", ValueType.RELATION_TYPE),

        /**
         * Corresponds to link-param-key "if"
         */
        IF("if", ValueType.RELATION_TYPE),

        /**
         * Corresponds to link-param-key "sz"
         */
        SZ("sz", ValueType.CARDINAL),

        /**
         * Corresponds to link-param-key "ct"
         */
        CT("ct", ValueType.CARDINAL, ValueType.DQUOTED_CARDINALS),

        /**
         * Corresponds to link-param-key "obs"
         */
        OBS("obs", ValueType.EMPTY),

        /**
         * Used internally for unknown link-param-keys
         */
        UNKNOWN(null, ValueType.UNKNOWN);

        private final String keyName;
        private final Set<ValueType> valueTypes;

        Key(String keyName, ValueType... valueType) {
            this.keyName = keyName;
            this.valueTypes = new HashSet<>(valueType.length);
            this.valueTypes.addAll(Arrays.asList(valueType));
        }

        /**
         * Returns the name of this link-param-key (i.e. "ct")
         * @return the name of this link-param-key (i.e. "ct")
         */
        public String getKeyName() {
            return this.keyName;
        }

        /**
         * Returns the {@link ValueType}s that are allowed for values of this key
         * @return the {@link ValueType}s that are allowed for values of this key
         */
        public Set<ValueType> getValueTypes() {
            return this.valueTypes;
        }
    }


    /**
     * The enumeration {@link ValueType} contains all value types that are supported
     */
    public enum ValueType {

        /**
         * Corresponds to the empty type, i.e. no value
         */
        EMPTY (false, false),

        /**
         * Corresponds to a single value of type "relation-types"
         */
        RELATION_TYPE (false, false),

        /**
         * Corresponds to one or more values of type "relation-types" enclosed in double-quotes (<code>DQUOTE</code>)
         */
        DQUOTED_RELATION_TYPES(true, true),

        /**
         * Corresponds to a single value of type "URI reference"
         */
        DQUOTED_URI_REFERENCE(true, false),

        /**
         * Corresponds to a single value of type "Language-Tag"
         */
        LANGUAGE_TAG (false, false),

        /**
         * Corresponds to a single value of type "Media Desc"
         */
        MEDIA_DESC (false, false),

        /**
         * Corresponds to a single value of type "Media Desc" enclosed in double-quotes (<code>DQUOTE</code>)
         */
        DQUOTED_MEDIA_DESC(true, false),

        /**
         * Corresponds to a single value of type "quoted-string", i.e. a string value enclosed in double-quotes
         * (<code>DQUOTE</code>)
         */
        DQUOTED_STRING(true, false),

        /**
         * Corresponds to a single value of type "ext-value"
         */
        EXT_VALUE (false, false),

        /**
         * Corresponds to a single value of type "media-type"
         */
        MEDIA_TYPE (false, false),

        /**
         * Corresponds to a single value of type "media-type" enclosed in double-quotes (<code>DQUOTE</code>)
         */
        DQUOTED_MEDIA_TYPE(true, false),

        /**
         * Corresponds to a single value of type "cardinal", i.e. digits
         */
        CARDINAL (false, false),

        /**
         * Values of this type consist of multiple cardinal values, divided by white spaces and  enclosed in
         * double-quotes (<code>DQUOTE</code>)
         */
        DQUOTED_CARDINALS(true, true),

        /**
         * Internally used to represent all other types
         */
        UNKNOWN(false, false);

        private boolean doubleQuoted;
        private boolean multipleValues;

        ValueType(boolean doubleQuoted, boolean multipleValues) {
            this.doubleQuoted = doubleQuoted;
            this.multipleValues = multipleValues;
        }

        /**
         * Returns <code>true</code> if this {@link ValueType} allows multiple values divided by white spaces and
         * <code>false</code> otherwise
         *
         * @return <code>true</code> if this {@link ValueType} allows multiple values divided by white spaces and
         * <code>false</code> otherwise
         */
        public boolean isMultipleValues() {
            return this.multipleValues;
        }

        /**
         * Returns <code>true</code> if values of this {@link ValueType} are enclosed in double-quotes
         * (<code>DQUOTE</code>) and <code>false</code> otherwise
         *
         * @return <code>true</code> if values of this {@link ValueType} are enclosed in double-quotes
         * (<code>DQUOTE</code>) and <code>false</code> otherwise
         */
        public boolean isDoubleQuoted() {
            return this.doubleQuoted;
        }
    }


    /**
     * Returns the {@link Key} corresponding to the given name or {@link Key#UNKNOWN} if no such {@link Key} exists
     *
     * @param keyName the name of the {@link Key} to lookup
     * @return the {@link Key} corresponding to the given name or {@link Key#UNKNOWN} if no such {@link Key} exists
     */
    public static Key getKey(String keyName) {
        for(Key key : Key.values()) {
            if (key.getKeyName().equals(keyName)) {
                return key;
            }
        }
        return null;
    }

    public static ValueType getValueType(Key key, String value) {
        // determine possible value types
        Set<ValueType> valueTypes = key.getValueTypes();

        // check if link param value is quoted and if there is quoted type
        ValueType result = ValueType.UNKNOWN;

        if (valueTypes.size() == 1) {
            result = valueTypes.iterator().next();
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            for (ValueType valueType : valueTypes) {
                if(valueType.isDoubleQuoted()) {
                    result = valueType;
                    break;
                }
            }
        } else {
            for (ValueType valueType : valueTypes) {
                if(!valueType.isDoubleQuoted()) {
                    result = valueType;
                    break;
                }
            }
        }

        return result;
    }

    public static LinkParam decode(String linkParam) {

        if (!linkParam.contains("=")) {
            // link param has empty value
            return new LinkParam(linkParam, ValueType.EMPTY, null);
        } else {
            // link param has non-empty value
            String keyName = linkParam.substring(0, linkParam.indexOf("="));
            LinkParam.Key key = LinkParam.getKey(keyName);
            String value = linkParam.substring(linkParam.indexOf("=") + 1, linkParam.length());
            if (key == null) {
                return new LinkParam(keyName, ValueType.UNKNOWN, value);
            } else {
                LinkParam.ValueType valueType = LinkParam.getValueType(key, value);

                LOG.debug("Value: {}, Type: {}", value, valueType);

                // remove double quotes if necessary
                if (valueType.isDoubleQuoted()) {
                    value = value.substring(1, value.length() - 1);
                }

                return new LinkParam(keyName, valueType, value);
            }
        }
    }


    //******************************************************************************************
    // instance related fields and methods
    //******************************************************************************************

    private String key;
    private ValueType valueType;
    private String value;

    protected LinkParam(String key, ValueType valueType, String value) {
        this.key = key;
        this.valueType = valueType;
        this.value = value;
        LOG.debug("LinkParam created: {}", this.toString());
    }

    /**
     * Returns the name of the key (e.g. "ct" or "rt")
     * @return the name of the key (e.g. "ct" or "rt")
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the {@link ValueType} of the value returned by {@link #getValue()}
     * @return the {@link ValueType} of the value returned by {@link #getValue()}
     */
    public ValueType getValueType() {
        return this.valueType;
    }

    /**
     * Returns the value of this {@link LinkParam}
     * @return the value of this {@link LinkParam}
     */
    public String getValue() {
        if (this.valueType.isDoubleQuoted()) {
            return "\"" + this.value + "\"";
        } else {
            return this.value;
        }
    }

    /**
     * <p>Returns <code>true</code> if the given value is contained in the value returned by {@link #getValue()} and
     * <code>false</code> otherwise. The exact behaviour depends on whether there are multiple values allowed in a
     * single param (see: {@link ValueType#isMultipleValues()}).</p>
     *
     * <p>Example: If the {@link LinkParam} corresponds to <code>ct="0 41"</code> then both, <code>contains("0")</code>
     * and <code>contains("41")</code> return <code>true</code> but <code>contains("0 41")</code> returns
     * <code>false</code>.</p>
     *
     * @param value the value to check
     *
     * @return <code>true</code> if the given value is contained in the value returned by {@link #getValue()} and
     * <code>false</code> otherwise.
     */
    public boolean contains(String value) {
        if (this.valueType.isMultipleValues()){
            return Arrays.asList(this.value.split(" ")).contains(value);
        } else {
            return this.value.equals(value);
        }
    }

    /**
     * Returns a string representation of this {@link LinkParam}
     * @return a string representation of this {@link LinkParam}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.key);
        if (this.valueType != ValueType.EMPTY) {
            builder.append("=");
            if (this.valueType.doubleQuoted) {
                builder.append("\"");
            }
            builder.append(this.value);
            if (this.valueType.doubleQuoted) {
                builder.append("\"");
            }
        }
        return builder.toString();
    }
}
