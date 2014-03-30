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
package de.uniluebeck.itm.ncoap.application.server.webservice.linkformat;

import java.util.*;

/**
 * Created by olli on 29.03.14.
 */
public abstract class LinkAttribute<T> {

    public static final String CONTENT_TYPE = "ct";
    public static final String RESOURCE_TYPE = "rt";
    public static final String INTERFACE = "if";
    public static final String MAX_SIZE_ESTIMATE = "sz";

    public static final int LONG_ATTRIBUTE = 1;
    public static final int STRING_ATTRIBUTE = 2;

    private String key;
    private T value;

    private static Map<String, AttributeCharacteristics> characteristics = new HashMap<>();
    static{
        characteristics.put(    CONTENT_TYPE,       new AttributeCharacteristics(true,  LONG_ATTRIBUTE));
        characteristics.put(    RESOURCE_TYPE,      new AttributeCharacteristics(true,  STRING_ATTRIBUTE));
        characteristics.put(    INTERFACE,          new AttributeCharacteristics(false, STRING_ATTRIBUTE));
        characteristics.put(    MAX_SIZE_ESTIMATE,  new AttributeCharacteristics(false, LONG_ATTRIBUTE));
    }


    protected LinkAttribute(String attributeKey, T value) throws IllegalArgumentException{
        this.key = attributeKey;

        if(!characteristics.containsKey(attributeKey))
            throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");

        this.value = value;
    }


    public String getKey() {
        return key;
    }


    public T getValue() {
        return value;
    }


    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object object);


    public static boolean allowsMultipleValues(String attributeKey){
        if(!characteristics.containsKey(attributeKey))
            throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");

        if(characteristics.get(attributeKey).isMultipleValues())
            return true;

        return false;
    }


    public static int getAttributeType(String attributeKey) throws IllegalArgumentException{
        if(characteristics.containsKey(attributeKey))
            return characteristics.get(attributeKey).getAttributeType();

        throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");
    }


    public static LinkAttribute createLinkAttribute(String key, String value) throws IllegalArgumentException{
        return new StringLinkAttribute(key, value);
    }

    public static LinkAttribute createLinkAttribute(String key, Long value) throws IllegalArgumentException{
        return new LongLinkAttribute(key, value);
    }

//    private static boolean allowsMultipleValues(String attributeKey) throws IllegalArgumentException{
//        if(characteristics.containsKey(attributeKey))
//            return characteristics.get(attributeKey).isMultipleValues();
//
//        throw new IllegalArgumentException("Unknown link attribute: \"" + attributeKey + "\"");
//    }


    private static class AttributeCharacteristics{

        private boolean multipleValues;
        private int attributeType;


        private AttributeCharacteristics(boolean multipleValues, int attributeType){
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
