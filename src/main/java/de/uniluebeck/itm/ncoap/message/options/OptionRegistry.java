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
package de.uniluebeck.itm.ncoap.message.options;

import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Oliver Kleine
 */
public class OptionRegistry {

    private static Logger log = LoggerFactory.getLogger(OptionRegistry.class.getName());

    /**
     * Enumeration containg the available option types according to the CoAP draft
     */
    public static enum OptionType {UINT, STRING, OPAQUE, EMPTY}

    /**
     * Enumeration containing items to represent occurences (names are self-explanatory)
     */
    public static enum OptionOccurence{NONE, ONCE, MULTIPLE}

    /**
     * Enumeration containing the available option names
     */
    public static enum OptionName{
        UNKNOWN(-1),
        CONTENT_TYPE(1),
        MAX_AGE(2),
        PROXY_URI(3),
        ETAG(4),
        URI_HOST(5),
        LOCATION_PATH(6),
        URI_PORT(7),
        LOCATION_QUERY(8),
        URI_PATH(9),
        OBSERVE_REQUEST(10),
        OBSERVE_RESPONSE(-10),
        TOKEN(11),
        ACCEPT(12),
        IF_MATCH(13),
        FENCEPOST(14),
        URI_QUERY(15),
        BLOCK_2(17),
        BLOCK_1(19),
        IF_NONE_MATCH(21);

        private final int number;
        
        OptionName(int number){
            this.number = number;
        }

        /**
         * Returns the option number internally representing the {@link OptionName}
         * @return the option number internally representing the {@link OptionName}
         */
        public int getNumber(){
            return this.number;
        }

        public static OptionName getByNumber(int optionNumber){
            for(OptionName optionName : OptionName.values()){
                if(optionName.getNumber() == optionNumber)
                    return optionName;
            }
            return OptionName.UNKNOWN;
        }
    }

    /**
     * Enumeration containing the available media types
     */
    public static enum MediaType {
        UNKNOWN(-1),
        TEXT_PLAIN_UTF8(0),
        APP_LINK_FORMAT(40),
        APP_XML(41),
        APP_OCTET_STREAM(42),
        APP_EXI(47),
        APP_JSON(50),
        APP_RDF_XML(201),
        APP_TURTLE(202),
        APP_N3(203),
        APP_SHDT(205);

        public final int number;

        MediaType(int number){
            this.number = number;
        }

        /**
         * Returns the corresonding {@link MediaType} for the given number
         *
         * @param number the number to look up the corresponding {@link MediaType}
         * @return the corresonding {@link MediaType} for the given number
         */
        public static MediaType getByNumber(Long number){
            for(MediaType mediaType : MediaType.values()){
                if(mediaType.number == number){
                    return mediaType;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * The default server port for CoAP
     */
    public static int COAP_PORT_DEFAULT = 5683;

    /**
     * The default value of the {@link OptionName#MAX_AGE} option (if not set different in a {@link CoapResponse})
     */
    public static int MAX_AGE_DEFAULT = 60;


    private static final HashMap<OptionName, OptionSyntaxConstraints> syntaxConstraints
            = new HashMap<OptionName, OptionSyntaxConstraints>();
    static{
        syntaxConstraints.put(OptionName.CONTENT_TYPE, new OptionSyntaxConstraints(OptionType.UINT, 0, 2));
        syntaxConstraints.put(OptionName.MAX_AGE, new OptionSyntaxConstraints(OptionType.UINT, 0, 4));
        syntaxConstraints.put(OptionName.PROXY_URI, new OptionSyntaxConstraints(OptionType.STRING, 1, 270));
        syntaxConstraints.put(OptionName.ETAG, new OptionSyntaxConstraints(OptionType.OPAQUE, 1, 8));
        syntaxConstraints.put(OptionName.URI_HOST, new OptionSyntaxConstraints(OptionType.STRING, 1, 270));
        syntaxConstraints.put(OptionName.LOCATION_PATH, new OptionSyntaxConstraints(OptionType.STRING, 1, 270));
        syntaxConstraints.put(OptionName.URI_PORT, new OptionSyntaxConstraints(OptionType.UINT, 0, 2));
        syntaxConstraints.put(OptionName.LOCATION_QUERY, new OptionSyntaxConstraints(OptionType.STRING, 1, 270));
        syntaxConstraints.put(OptionName.URI_PATH, new OptionSyntaxConstraints(OptionType.STRING, 1, 270));
        syntaxConstraints.put(OptionName.TOKEN, new OptionSyntaxConstraints(OptionType.OPAQUE, 1, 8));
        syntaxConstraints.put(OptionName.ACCEPT, new OptionSyntaxConstraints(OptionType.UINT, 0, 2));
        syntaxConstraints.put(OptionName.IF_MATCH, new OptionSyntaxConstraints(OptionType.OPAQUE, 0, 8));
        syntaxConstraints.put(OptionName.URI_QUERY, new OptionSyntaxConstraints(OptionType.STRING, 1, 270));
        syntaxConstraints.put(OptionName.BLOCK_2, new OptionSyntaxConstraints(OptionType.UINT, 1, 3));
        syntaxConstraints.put(OptionName.BLOCK_1, new OptionSyntaxConstraints(OptionType.UINT, 1, 3));
        syntaxConstraints.put(OptionName.IF_NONE_MATCH, new OptionSyntaxConstraints(OptionType.EMPTY, 0, 0));
        syntaxConstraints.put(OptionName.OBSERVE_REQUEST, new OptionSyntaxConstraints(OptionType.EMPTY, 0, 0));
        syntaxConstraints.put(OptionName.OBSERVE_RESPONSE, new OptionSyntaxConstraints(OptionType.UINT, 0, 2));
    }

    private static final HashMap<Code, HashMap<OptionName, OptionOccurence>> allowedOptions
            = new HashMap<Code, HashMap<OptionName, OptionOccurence>>();
    static{
        //GET
        HashMap<OptionName, OptionOccurence> constraintsGET = new HashMap<OptionName, OptionOccurence>();
        constraintsGET.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraintsGET.put(OptionName.URI_HOST, OptionOccurence.ONCE);
        constraintsGET.put(OptionName.URI_PATH, OptionOccurence.MULTIPLE);
        constraintsGET.put(OptionName.URI_PORT, OptionOccurence.ONCE);
        constraintsGET.put(OptionName.URI_QUERY, OptionOccurence.MULTIPLE);
        constraintsGET.put(OptionName.PROXY_URI, OptionOccurence.MULTIPLE);
        constraintsGET.put(OptionName.ACCEPT, OptionOccurence.MULTIPLE);
        constraintsGET.put(OptionName.ETAG, OptionOccurence.MULTIPLE);
        constraintsGET.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraintsGET.put(OptionName.OBSERVE_REQUEST, OptionOccurence.ONCE);
        allowedOptions.put(Code.GET,  constraintsGET);

        //POST
        HashMap<OptionName, OptionOccurence> constraintsPOST = new HashMap<OptionName, OptionOccurence>();
        constraintsPOST.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.URI_HOST, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.URI_PATH, OptionOccurence.MULTIPLE);
        constraintsPOST.put(OptionName.URI_PORT, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.URI_QUERY, OptionOccurence.MULTIPLE);
        constraintsPOST.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        allowedOptions.put(Code.POST, constraintsPOST);

        //PUT
        HashMap<OptionName, OptionOccurence> constraintsPUT = new HashMap<OptionName, OptionOccurence>();
        constraintsPUT.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraintsPUT.put(OptionName.URI_HOST, OptionOccurence.ONCE);
        constraintsPUT.put(OptionName.URI_PATH, OptionOccurence.MULTIPLE);
        constraintsPUT.put(OptionName.URI_PORT, OptionOccurence.ONCE);
        constraintsPUT.put(OptionName.URI_QUERY, OptionOccurence.MULTIPLE);
        constraintsPUT.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraintsPUT.put(OptionName.IF_MATCH, OptionOccurence.MULTIPLE);
        constraintsPUT.put(OptionName.IF_NONE_MATCH, OptionOccurence.ONCE);
        constraintsPUT.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraintsPUT.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        allowedOptions.put(Code.PUT, constraintsPUT);

        //DELETE
        HashMap<OptionName, OptionOccurence> constraintsDELETE = new HashMap<OptionName, OptionOccurence>();
        constraintsDELETE.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraintsDELETE.put(OptionName.URI_HOST, OptionOccurence.ONCE);
        constraintsDELETE.put(OptionName.URI_PATH, OptionOccurence.MULTIPLE);
        constraintsDELETE.put(OptionName.URI_PORT, OptionOccurence.ONCE);
        constraintsDELETE.put(OptionName.URI_QUERY, OptionOccurence.MULTIPLE);
        allowedOptions.put(Code.DELETE, constraintsDELETE);

        //201 CREATED
        HashMap<OptionName, OptionOccurence> constraints201 = new HashMap<OptionName, OptionOccurence>();
        constraints201.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraints201.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraints201.put(OptionName.LOCATION_PATH, OptionOccurence.MULTIPLE);
        constraints201.put(OptionName.LOCATION_QUERY, OptionOccurence.MULTIPLE);
        constraints201.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraints201.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        allowedOptions.put(Code.CREATED_201, constraints201);

        //202 DELETED
        HashMap<OptionName, OptionOccurence> constraints202 = new HashMap<OptionName, OptionOccurence>();
        constraints202.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraints202.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraints202.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraints202.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        allowedOptions.put(Code.DELETED_202, constraints202);

        //203 VALID
        HashMap<OptionName, OptionOccurence> constraints203 = new HashMap<OptionName, OptionOccurence>();
        constraints203.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraints203.put(OptionName.ETAG, OptionOccurence.MULTIPLE);
        constraints203.put(OptionName.MAX_AGE, OptionOccurence.ONCE);
        constraints203.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraints203.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        allowedOptions.put(Code.VALID_203, constraints203);

        //204 CHANGED
        HashMap<OptionName, OptionOccurence> constraints204 = new HashMap<OptionName, OptionOccurence>();
        constraints204.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraints204.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraints204.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraints204.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        allowedOptions.put(Code.CHANGED_204, constraints204);

        //205 CONTENT
        HashMap<OptionName, OptionOccurence> constraints205 = new HashMap<OptionName, OptionOccurence>();
        constraints205.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraints205.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraints205.put(OptionName.ETAG, OptionOccurence.MULTIPLE);
        constraints205.put(OptionName.MAX_AGE, OptionOccurence.ONCE);
        constraints205.put(OptionName.BLOCK_2, OptionOccurence.ONCE);
        constraints205.put(OptionName.BLOCK_1, OptionOccurence.ONCE);
        constraints205.put(OptionName.OBSERVE_RESPONSE, OptionOccurence.ONCE);
        allowedOptions.put(Code.CONTENT_205, constraints205);

        //both, 4x, 5x only allow Max-Age Option
        HashMap<OptionName, OptionOccurence> constraints4x5x = new HashMap<OptionName, OptionOccurence>();
        constraints4x5x.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraints4x5x.put(OptionName.CONTENT_TYPE, OptionOccurence.ONCE);
        constraints4x5x.put(OptionName.MAX_AGE, OptionOccurence.ONCE);


        allowedOptions.put(Code.BAD_REQUEST_400, constraints4x5x);
        allowedOptions.put(Code.UNAUTHORIZED_401, constraints4x5x);
        allowedOptions.put(Code.BAD_OPTION_402, constraints4x5x);
        allowedOptions.put(Code.FORBIDDEN_403, constraints4x5x);
        allowedOptions.put(Code.NOT_FOUND_404, constraints4x5x);
        allowedOptions.put(Code.METHOD_NOT_ALLOWED_405, constraints4x5x);
        allowedOptions.put(Code.PRECONDITION_FAILED_412, constraints4x5x);
        allowedOptions.put(Code.REQUEST_ENTITY_TOO_LARGE_413, constraints4x5x);
        allowedOptions.put(Code.UNSUPPORTED_MEDIA_TYPE_415, constraints4x5x);
        allowedOptions.put(Code.INTERNAL_SERVER_ERROR_500, constraints4x5x);
        allowedOptions.put(Code.NOT_IMPLEMENTED_501, constraints4x5x);
        allowedOptions.put(Code.BAD_GATEWAY_502, constraints4x5x);
        allowedOptions.put(Code.SERVICE_UNAVAILABLE_503, constraints4x5x);
        allowedOptions.put(Code.GATEWAY_TIMEOUT_504, constraints4x5x);
        allowedOptions.put(Code.PROXYING_NOT_SUPPORTED_505, constraints4x5x);
    }

    /**
     * Returns the maximum length (in bytes) for the given {@link OptionName}.
     *
     * @param optionName an {@link OptionName}
     * @return  the maximum length (in bytes) for the given {@link OptionName}.
     */
    public static int getMaxLength(OptionName optionName){
        return syntaxConstraints.get(optionName).max_length;
    }

    /**
     * Returns the minimum length (in bytes) for the given {@link OptionName}.
     *
     * @param optionName an {@link OptionName}
     * @return  the minimum length (in bytes) for the given {@link OptionName}.
     */
    public static int getMinLength(OptionName optionName){
        return syntaxConstraints.get(optionName).min_length;
    }

    /**
     * Returns a Map containing all available {@link OptionName}s as keys and the according allowed
     * {@link OptionOccurence} as values.
     *
     * @param code the {@link Code} to get the allowed option occurences for
     *
     * @return a Map containing all available {@link OptionName}s as keys and the according allowed
     * {@link OptionOccurence} as values
     */
    public static Map<OptionName, OptionOccurence> getAllowedOccurences(Code code){
        EnumMap<OptionName, OptionOccurence> result = new EnumMap<OptionName, OptionOccurence>(OptionName.class);
        for(OptionName optionName : OptionName.values()){
            result.put(optionName, getAllowedOccurence(code, optionName));
        }
        return result;
    }

    /**
     * Returns the allowed {@link OptionOccurence} for the combination of the given {@link Code} and
     * {@link OptionName}.
     *
     * @param code a {@link Code}
     * @param optionName an {@link OptionName}
     *
     * @return the allowed {@link OptionOccurence}
     */
    public static OptionOccurence getAllowedOccurence(Code code, OptionName optionName){
        try{
            OptionOccurence result = allowedOptions.get(code).get(optionName);
            log.debug("Occurence constraint for option " + optionName + " with code " + code + " is: "
                        + result.toString());
            return result != null ? result : OptionOccurence.NONE;
        }
        catch(NullPointerException e){
            return OptionOccurence.NONE;
        }
    }

    /**
     * Returns the {@link OptionType} of the given {@link OptionName}.
     *
     * @param optionName an {@link OptionName}
     * @return the {@link OptionType} of the given {@link OptionName}.
     */
    public static OptionType getOptionType(OptionName optionName){
        return syntaxConstraints.get(optionName).opt_type;
    }

//    /**
//     * Returns the {@link OptionName} represented by the given number (option names are serialized using numbers
//     * as unique identifier).
//     *
//     * @param number a number
//     * @return the {@link OptionName} of the given number
//     */
//    public static OptionName getOptionName(int number){
//        for(OptionName o :OptionName.values()){
//            if(o.number == number){
//                return o;
//            }
//        }
//        return OptionName.UNKNOWN;
//    }

    /**
     * Returns whether the option represented by the given option number represents a critical or an elective
     * option. Options with even numbers are critial, odd numbers refer to elective options.
     *
     * @param optionNumber a number representing an option
     * @return <code>true</code> if the given number is even, <code>false</code> if the given number is odd
     */
    static boolean isCritical(int optionNumber){
        //Options with even numbers are critial, odd numbers refer to elective options
        return (optionNumber % 2 == 0);
    }


    private static class OptionSyntaxConstraints{

        public OptionType opt_type;
        public int min_length;
        public int max_length;

        public OptionSyntaxConstraints(OptionType opt_type, int min_length, int max_length){
            this.opt_type = opt_type;
            this.min_length = min_length;
            this.max_length = max_length;
        }
    }
}
