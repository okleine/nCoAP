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
package de.uzl.itm.ncoap.application.client.linkformat;

import de.uzl.itm.ncoap.application.server.webresource.linkformat.EmptyLinkAttribute;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.LongLinkAttribute;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.StringLinkAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to decode the CoRE link format
 *
 * @author Oliver Kleine
 */
public class LinkFormatDecoder {

    private static Logger LOG = LoggerFactory.getLogger(LinkFormatDecoder.class.getName());

    public static void initialize(){
        LinkAttribute.initialize();
    }

    /**
     * Decodes the given String (e.g. the content of response from a <code>/.well-known/core</code> service) into a
     * {@link java.util.Map} with the services as keys and a set of their respective attributes as value.
     *
     * @param linkFormat a {@link java.lang.String} in CoRE Link Format (according to RFC 6690)
     *
     * @return a {@link java.util.Map} with the services as keys and a set of their respective attributes as value.
     */
    public static Map<String, Set<LinkAttribute>> decode(String linkFormat) throws IllegalArgumentException{
        Map<String, Set<LinkAttribute>> result = new HashMap<>();

        LOG.debug("Link Format String:\n{}", linkFormat);
        linkFormat = linkFormat.replaceAll("[^a-zA-Z0-9<>;,/=]", "");

        String[] services = linkFormat.split(",");

        for(String service : services){
            String serviceName = service.substring(service.indexOf("<") + 1, service.indexOf(">"));
            LOG.info("Found service {}", serviceName);

            int attributesIndex = service.indexOf(";");
            if(attributesIndex == -1){
                LOG.debug("No attributes found for resource {}.", service);
                continue;
            }

            String[] attributes = service.substring(attributesIndex + 1).split(";");

            Set<LinkAttribute> attributesSet = new HashSet<>();

            for(String attribute : attributes){
                LOG.info("Service {} has attribute {}.", serviceName, attribute);
                String key = !attribute.contains("=") ? attribute : attribute.substring(0, attribute.indexOf("="));
                int attributeType = LinkAttribute.getAttributeType(key);

                if(attributeType == LinkAttribute.EMPTY_ATTRIBUTE){
                    attributesSet.add(new EmptyLinkAttribute(key));
                }
                else{
                    if(attribute.length() == attribute.indexOf("=") + 1){
                        LOG.warn("Service {} has attribute {} without any value (IGNORE!)", serviceName, key);
                        continue;
                    }

                    String encodedValues = attribute.substring(attribute.indexOf("=") + 1);

                    if(attributeType == LinkAttribute.STRING_ATTRIBUTE) {

                        //Remove the quotation marks
                        if(encodedValues.startsWith("\"")){
                            encodedValues = encodedValues.substring(1);
                        }
                        if(encodedValues.endsWith("\"")){
                            encodedValues = encodedValues.substring(0, encodedValues.length() - 1);
                        }

                        //Decode attribute values
                        for(String value : encodedValues.split(" ")) {
                            attributesSet.add(new StringLinkAttribute(key, value));
                        }
                    }
                    else if (attributeType == LinkAttribute.LONG_ATTRIBUTE) {
                        String tmp = attribute.substring(attribute.indexOf("=") + 1, attribute.length());
                        String[] values = tmp.split(" ");
                        for(String value : values) {
                            try {
                                attributesSet.add(new LongLinkAttribute(key, Long.valueOf(value)));
                            }
                            catch(NumberFormatException ex){
                                LOG.warn("Value ({}) of link attribute \"{}\" is no number (IGNORE!)", value, key);
                            }
                        }
                    }
                    else{
                        LOG.warn("Found attribute of unknown type ({}) for service {}. IGNORE!", key, serviceName);
                    }
                }
            }

            result.put(serviceName, attributesSet);
        }

        return result;
    }
}
