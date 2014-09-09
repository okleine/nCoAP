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

import java.util.HashMap;

/**
 * This enumeration contains all defined message codes (i.e. methods for requests and status for responses)
 * in CoAPs draft v7
 *
 * @author Oliver Kleine
*/

public abstract class MessageCode {

    private static final HashMap<Integer, Name> validNumbers = new HashMap<Integer, Name>();

    public static enum Name{
        UNKNOWN(-1),

        /**
         * Corresponds to Code 0
         */
        EMPTY(0),

        /**
         * Corresponds to Request Code 1
         */
        GET(1),

        /**
         * Corresponds to Request Code 2
         */
        POST(2),

        /**
         * Corresponds to Request Code 3
         */
        PUT(3),

        /**
         * Corresponds to Request Code 4
         */
        DELETE(4),

        /**
         * Corresponds to Response Code 65
         */
        CREATED_201(65),

        /**
         * Corresponds to Response Code 66
         */
        DELETED_202(66),

        /**
         * Corresponds to Response Code 67
         */
        VALID_203(67),

        /**
         * Corresponds to Response Code 68
         */
        CHANGED_204(68),

        /**
         * Corresponds to Response Code 69
         */
        CONTENT_205(69),

        /**
         * Corresponds to Response Code 128
         */
        BAD_REQUEST_400(128),

        /**
         * Corresponds to Response Code 129
         */
        UNAUTHORIZED_401(129),

        /**
         * Corresponds to Response Code 130
         */
        BAD_OPTION_402(130),

        /**
         * Corresponds to Response Code 131
         */
        FORBIDDEN_403(131),

        /**
         * Corresponds to Response Code 132
         */
        NOT_FOUND_404(132),

        /**
         * Corresponds to Response Code 133
         */
        METHOD_NOT_ALLOWED_405(133),

        /**
         * Corresponds to Response Code 134
         */
        NOT_ACCEPTABLE_406(134),

        /**
         * Corresponds to Response Code 140
         */
        PRECONDITION_FAILED_412(140),

        /**
         * Corresponds to Response Code 141
         */
        REQUEST_ENTITY_TOO_LARGE_413(141),

        /**
         * Corresponds to Response Code 143
         */
        UNSUPPORTED_CONTENT_FORMAT_415(143),

        /**
         * Corresponds to Response Code 160
         */
        INTERNAL_SERVER_ERROR_500(160),

        /**
         * Corresponds to Response Code 161
         */
        NOT_IMPLEMENTED_501(161),

        /**
         * Corresponds to Response Code 162
         */
        BAD_GATEWAY_502(162),

        /**
         * Corresponds to Response Code 163
         */
        SERVICE_UNAVAILABLE_503(163),

        /**
         * Corresponds to Response Code 164
         */
        GATEWAY_TIMEOUT_504(164),

        /**
         * Corresponds to Response Code 165
         */
        PROXYING_NOT_SUPPORTED_505(165);

        private int number;

        private Name(int number){
            this.number = number;
            validNumbers.put(number, this);
        }

        /**
         * Returns the number corresponding to this {@link de.uniluebeck.itm.ncoap.message.MessageCode} instance
         * @return the number corresponding to this {@link de.uniluebeck.itm.ncoap.message.MessageCode} instance
         */
        public int getNumber() {
            return this.number;
        }

        /**
         * Returns the {@link Name} corresponding to the given number or {@link Name#UNKNOWN} if no such {@link Name}
         * exists.
         *
         * @return the {@link Name} corresponding to the given number or {@link Name#UNKNOWN} if no such {@link Name}
         * exists.
         */
        public static Name getName(int number){
            if(validNumbers.containsKey(number))
                return validNumbers.get(number);
            else
                return Name.UNKNOWN;
        }

        /**
         * Returns <code>true</code> if the given number corresponds to a valid
         * {@link de.uniluebeck.itm.ncoap.message.MessageCode} and <code>false</code> otherwise
         *
         * @param number the number to check for being a valid {@link de.uniluebeck.itm.ncoap.message.MessageCode}
         *
         * @return <code>true</code> if the given number corresponds to a valid
         * {@link de.uniluebeck.itm.ncoap.message.MessageCode} and <code>false</code> otherwise
         */
        public static boolean isMessageCode(int number){
            return validNumbers.containsKey(number);
        }
    }

    /**
     * This method indicates whether the given number refers to a {@link MessageCode} for {@link CoapRequest}s.
     *
     * <b>Note:</b> Messages with {@link MessageCode.Name#EMPTY} are considered neither a response nor a request
     *
     * @return <code>true</code> in case of a request code, <code>false</code> otherwise.
     *
     */
    public static boolean isRequest(int codeNumber){
        return (codeNumber > 0 && codeNumber < 5);
    }

    /**
     * This method indicates whether the given {@link MessageCode.Name} indicates a {@link CoapRequest}.
     *
     * <b>Note:</b> Messages with {@link MessageCode.Name#EMPTY} are considered neither a response nor a request
     *
     * @return <code>true</code> in case of a request code, <code>false</code> otherwise.
     *
     */
    public static boolean isRequest(MessageCode.Name messageCode){
        return isRequest(messageCode.getNumber());
    }

    /**
     * This method indicates whether the given number refers to a {@link MessageCode} for {@link CoapResponse}s.
     *
     * <b>Note:</b> Messages with {@link MessageCode.Name#EMPTY} are considered neither a response nor a request
     *
     * @return <code>true</code> in case of a response code, <code>false</code> otherwise.
     *
     */
    public static boolean isResponse(int codeNumber){
        return codeNumber >= 5;
    }

    /**
     * This method indicates whether the given {@link MessageCode.Name} indicates a {@link CoapResponse}.
     *
     * <b>Note:</b> Messages with {@link MessageCode.Name#EMPTY} are considered neither a response nor a request
     *
     * @return <code>true</code> in case of a response code, <code>false</code> otherwise.
     *
     */
    public static boolean isResponse(MessageCode.Name messageCode){
        return isResponse(messageCode.getNumber());
    }

    /**
     * This method indicates whether the given number refers to a {@link MessageCode} for {@link CoapResponse}s
     * indicating an error.
     *
     * @return <code>true</code> in case of an error response code, <code>false</code> otherwise.
     *
     */
    public static boolean isErrorMessage(int codeNumber){
        return (codeNumber >= 128);
    }

    /**
     * This method indicates whether a message may contain payload
     * @return <code>true</code> if payload is allowed, <code>false</code> otherwise
     */
    public static boolean allowsContent(int codeNumber){
        return !(codeNumber == Name.GET.getNumber() || codeNumber == Name.DELETE.getNumber());
    }


}

