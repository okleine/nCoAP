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

/**
 * This enumeration contains all defined message codes (i.e. methods for requests and status for responses)
 * in CoAPs draft v7
 *
 * @author Oliver Kleine
*/

public abstract class MessageCode {

    public static final int EMPTY                           = 0;
    public static final int GET                             = 1;
    public static final int POST                            = 2;
    public static final int PUT                             = 3;
    public static final int DELETE                          = 4;
    public static final int CREATED_201                     = 65;
    public static final int DELETED_202                     = 66;
    public static final int VALID_203                       = 67;
    public static final int CHANGED_204                     = 68;
    public static final int CONTENT_205                     = 69;
    public static final int BAD_REQUEST_400                 = 128;
    public static final int UNAUTHORIZED_401                = 129;
    public static final int BAD_OPTION_402                  = 130;
    public static final int FORBIDDEN_403                   = 131;
    public static final int NOT_FOUND_404                   = 132;
    public static final int METHOD_NOT_ALLOWED_405          = 133;
    public static final int NOT_ACCEPTABLE_406              = 134;
    public static final int PRECONDITION_FAILED_412         = 140;
    public static final int REQUEST_ENTITY_TOO_LARGE_413    = 141;
    public static final int UNSUPPORTED_CONTENT_FORMAT_415  = 143;
    public static final int INTERNAL_SERVER_ERROR_500       = 160;
    public static final int NOT_IMPLEMENTED_501             = 161;
    public static final int BAD_GATEWAY_502                 = 162;
    public static final int SERVICE_UNAVAILABLE_503         = 163;
    public static final int GATEWAY_TIMEOUT_504             = 164;
    public static final int PROXYING_NOT_SUPPORTED_505      = 165;

    /**
     * This method indicates wheter the message code refers to a request.
     *
     * <b>Note:</b> Messages of MessageCode {@link MessageCode#EMPTY} are considered neither a response nor a request
     *
     * @return <code>true</code> in case of a request code, <code>false</code> otherwise.
     *
     */
    public static boolean isRequest(int codeNumber){
        return (codeNumber > 0 && codeNumber < 5);
    }

    /**
     * This method indicates wheter the message code refers to a response.
     *
     * <b>Note:</b> Messages of MessageCode {@link MessageCode#EMPTY} are considered neither a response nor a request.
     *
     * @return <code>true</code> in case of a request code, <code>false</code> in case of response code
     */
    public static boolean isResponse(int codeNumber){
        return codeNumber >= 5;
    }

    /**
     * This method indicates wheter the message code refers to an error message
     * @return <code>true</code> in case of an error <code>false</code> otherwise
     */
    public static boolean isErrorMessage(int codeNumber){
        return (codeNumber >= 128);
    }

    /**
     * This method indicates whether a message may contain payload
     * @return <code>true</code> if payload is allowed, <code>false</code> otherwise
     */
    public static boolean allowsContent(int codeNumber){
        return !(codeNumber == GET || codeNumber == DELETE);
    }


}

