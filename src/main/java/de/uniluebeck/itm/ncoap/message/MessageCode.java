/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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

package de.uniluebeck.itm.ncoap.message;

import de.uniluebeck.itm.ncoap.message.header.InvalidHeaderException;

/**
 * This enumeration contains all defined message codes (i.e. methods for requests and status for responses)
 * in CoAPs draft v7
 *
 * @author Oliver Kleine
*/

public enum MessageCode {

    /**
     * corresponds to CoAPs numerical message code 0
     */
    EMPTY(0),

    /**
     * corresponds to CoAPs numerical message code 1
     */
    GET(1),

    /**
     * corresponds to CoAPs numerical message code 2
     */
    POST(2),

    /**
     * corresponds to CoAPs numerical message code 3
     */
    PUT(3),

    /**
     * corresponds to CoAPs numerical message code 4
     */
    DELETE(4),

    /**
     * corresponds to CoAPs numerical message code 65
     */
    CREATED_201(65),

    /**
     * corresponds to CoAPs numerical message code 66
     */
    DELETED_202(66),

    /**
     * corresponds to CoAPs numerical message code 67
     */
    VALID_203(67),

    /**
     * corresponds to CoAPs numerical message code 68
     */
    CHANGED_204(68),

    /**
     * corresponds to CoAPs numerical message code 69
     */
    CONTENT_205(69),

    /**
     * corresponds to CoAPs numerical message code 128
     */
    BAD_REQUEST_400(128),

    /**
     * corresponds to CoAPs numerical message code 129
     */
    UNAUTHORIZED_401(129),

    /**
     * corresponds to CoAPs numerical message code 130
     */
    BAD_OPTION_402(130),

    /**
     * corresponds to CoAPs numerical message code 131
     */
    FORBIDDEN_403(131),

    /**
     * corresponds to CoAPs numerical message code 132
     */
    NOT_FOUND_404(132),

    /**
     * corresponds to CoAPs numerical message code 133
     */
    METHOD_NOT_ALLOWED_405(133),

    /**
     * corresponds to CoAPs numerical message code 134
     */
    NOT_ACCEPTABLE(134),

    /**
     * corresponds to CoAPs numerical message code 140
     */
    PRECONDITION_FAILED_412(140),
    /**
     * corresponds to CoAPs numerical message code 141
     */
    REQUEST_ENTITY_TOO_LARGE_413(141),

    /**
     * corresponds to CoAPs numerical message code 143
     */
    UNSUPPORTED_MEDIA_TYPE_415(143),

    /**
     * corresponds to CoAPs numerical message code 160
     */
    INTERNAL_SERVER_ERROR_500(160),

    /**
     * corresponds to CoAPs numerical message code 161
     */
    NOT_IMPLEMENTED_501(161),

    /**
     * corresponds to CoAPs numerical message code 162
     */
    BAD_GATEWAY_502(162),

    /**
     * corresponds to CoAPs numerical message code 163
     */
    SERVICE_UNAVAILABLE_503(163),

    /**
     * corresponds to CoAPs numerical message code 164
     */
    GATEWAY_TIMEOUT_504(164),

    /**
     * corresponds to CoAPs numerical message code 165
     */
    PROXYING_NOT_SUPPORTED_505(165);

    /**
     * The corresponding numerical CoAP message code
     */
    private final int codeNumber;

    private MessageCode(int codeNumber){
        this.codeNumber = codeNumber;
    }

    /**
     * This method indicates wheter the message code refers to a request.
     *
     * <b>Note:</b> Messages of MessageCode {@link MessageCode#EMPTY} are considered neither a response nor a request
     *
     * @return <code>true</code> in case of a request code, <code>false</code> otherwise.
     *
     */
    public boolean isRequest(){
        return (codeNumber > 0 && codeNumber < 5);
    }

    /**
     * This method indicates wheter the message code refers to a response.
     *
     * <b>Note:</b> Messages of MessageCode {@link MessageCode#EMPTY} are considered neither a response nor a request.
     *
     * @return <code>true</code> in case of a request code, <code>false</code> in case of response code
     */
    public boolean isResponse(){
        return codeNumber >= 5;
    }

    /**
     * This method indicates wheter the message code refers to an error message
     * @return <code>true</code> in case of an error <code>false</code> otherwise
     */
    public boolean isErrorMessage(){
        return (codeNumber >= 128);
    }

    /**
     * This method indicates whether a message may contain payload
     * @return <code>true</code> if payload is allowed, <code>false</code> otherwise
     */
    public boolean allowsPayload(){
        return !(codeNumber == MessageCode.GET.codeNumber || codeNumber == MessageCode.DELETE.codeNumber);
    }


    public static MessageCode getCodeFromNumber(int number) throws InvalidHeaderException {
        for(MessageCode c : MessageCode.values()){
            if(c.codeNumber == number){
                return c;
            }
        }
        throw new InvalidHeaderException("Unknown code (no. " + number + ")");
    }
}

