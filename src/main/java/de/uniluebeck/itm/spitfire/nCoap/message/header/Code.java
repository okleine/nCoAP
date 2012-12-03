/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.spitfire.nCoap.message.header;

import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;

import java.util.Arrays;
import java.util.List;

/**
 * This enumeration contains all defined message codes (i.e. methods for requests and status for responses)
 * in CoAPs draft v7
 *
 * @author Oliver Kleine
*/

public enum Code {

    /**
     * corresponds to CoAPs numerical message code 0
     */
    EMPTY(0, new OptionRegistry.OptionName[0]),

    /**
     * corresponds to CoAPs numerical message code 1
     */
    GET(1, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.URI_HOST,
                            OptionRegistry.OptionName.URI_PATH,
                            OptionRegistry.OptionName.URI_PORT,
                            OptionRegistry.OptionName.URI_QUERY,
                            OptionRegistry.OptionName.PROXY_URI,
                            OptionRegistry.OptionName.ACCEPT,
                            OptionRegistry.OptionName.ETAG,
                            OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 2
     */
    POST(2, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.URI_HOST,
                             OptionRegistry.OptionName.URI_PATH,
                             OptionRegistry.OptionName.URI_PORT,
                             OptionRegistry.OptionName.URI_QUERY,
                             OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 3
     */
    PUT(3, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.URI_HOST,
                            OptionRegistry.OptionName.URI_PATH,
                            OptionRegistry.OptionName.URI_PORT,
                            OptionRegistry.OptionName.URI_QUERY,
                            OptionRegistry.OptionName.CONTENT_TYPE,
                            OptionRegistry.OptionName.IF_MATCH,
                            OptionRegistry.OptionName.IF_NONE_MATCH,
                            OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 4
     */
    DELETE(4, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.URI_HOST,
                               OptionRegistry.OptionName.URI_PATH,
                               OptionRegistry.OptionName.URI_PORT,
                               OptionRegistry.OptionName.URI_QUERY,
                               OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 65
     */
    CREATED_201(65, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.CONTENT_TYPE,
                                     OptionRegistry.OptionName.LOCATION_PATH,
                                     OptionRegistry.OptionName.LOCATION_QUERY,
                                     OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 66
     */
    DELETED_202(66, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.CONTENT_TYPE,
                                     OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 67
     */
    VALID_203(67, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.ETAG,
                                   OptionRegistry.OptionName.MAX_AGE,
                                   OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 68
     */
    CHANGED_204(68, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.CONTENT_TYPE,
                                     OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 69
     */
    CONTENT_205(69, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.CONTENT_TYPE,
                                     OptionRegistry.OptionName.MAX_AGE,
                                     OptionRegistry.OptionName.ETAG,
                                     OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 128
     */
    BAD_REQUEST_400(128, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                          OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 129
     */
    UNAUTHORIZED_401(129, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                           OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 130
     */
    BAD_OPTION_402(130, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                         OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 131
     */
    FORBIDDEN_403(131, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                        OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 132
     */
    NOT_FOUND_404(132, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                        OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 133
     */
    METHOD_NOT_ALLOWED_405(133, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                 OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 140
     */
    PRECONDITION_FAILED_412(140, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                  OptionRegistry.OptionName.TOKEN}),
    /**
     * corresponds to CoAPs numerical message code 141
     */
    REQUEST_ENTITY_TOO_LARGE_413(141, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                       OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 143
     */
    UNSUPPORTED_MEDIA_TYPE_415(143, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                     OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 160
     */
    INTERNAL_SERVER_ERROR_500(160, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                    OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 161
     */
    NOT_IMPLEMENTED_501(161, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                              OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 162
     */
    BAD_GATEWAY_502(162, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                          OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 163
     */
    SERVICE_UNAVAILABLE_503(163, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                  OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 164
     */
    GATEWAY_TIMEOUT_504(164, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                              OptionRegistry.OptionName.TOKEN}),

    /**
     * corresponds to CoAPs numerical message code 165
     */
    PROXYING_NOT_SUPPORTED_505(165, new OptionRegistry.OptionName[]{OptionRegistry.OptionName.MAX_AGE,
                                                     OptionRegistry.OptionName.TOKEN});

    /**
     * The corresponding numerical CoAP message code
     */
    public final int number;
    private final List allowedOptions;

    Code(int number, OptionRegistry.OptionName[] allowedOptions){
        this.number = number;
        this.allowedOptions = Arrays.asList(allowedOptions);
    }

    /**
     * This method is to check whether the specified option is meaningful in the context
     * of the current message code
     * @param opt_name
     * @return <code>true</code> if the given is meaningful in the context of the message code,
     * <code>false</false> otherwise
     */
    public boolean isMeaningful(OptionRegistry.OptionName opt_name){
        return allowedOptions.contains(opt_name);
    }

    /**
     * This method indicates wheter the message code refers to a request or a response
     * @return <code>true</code> in case of a request code, <code>false</code> in case of response code
     */
    public boolean isRequest(){
        return (number < 5 && number > 0);
    }

    /**
     * This method indicates whether a message may contain payload
     * @return <code>true</code> if payload is allowed, <code>false</code> otherwise
     */
    public boolean allowsPayload(){
        return !(number == Code.GET.number || number == Code.DELETE.number);
    }

    public static Code getCodeFromNumber(int number) throws InvalidHeaderException {
        for(Code c : Code.values()){
            if(c.number == number){
                return c;
            }
        }
        throw new InvalidHeaderException("Unknown code (no. " + number + ")");
    }
}

