package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 *
 * @author Oliver Kleine
 */
public class OptionRegistry {

    private static Logger log = LoggerFactory.getLogger(OptionRegistry.class.getName());

    public static enum OptionType {UINT, STRING, OPAQUE, EMPTY}

    public static enum OptionOccurence{NONE, ONCE, MULTIPLE}

    public static enum OptionName{
        CONTENT_TYPE(1),
        MAX_AGE(2),
        PROXY_URI(3),
        ETAG(4),
        URI_HOST(5),
        LOCATION_PATH(6),
        URI_PORT(7),
        LOCATION_QUERY(8),
        URI_PATH(9),
        TOKEN(11),
        ACCEPT(12),
        IF_MATCH(13),
        FENCEPOST(14),
        URI_QUERY(15),
        BLOCK_2(17),
        BLOCK_1(19),
        IF_NONE_MATCH(21);

        public final int number;
        
        OptionName(int number){
            this.number = number;
        }
    }

    public static enum MediaType {
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
         * @param number the number to look up the corresponding {@link MediaType}
         * @return  the corresonding {@link MediaType} for the given number or null if there is no.
         */
        public static MediaType getByNumber(int number){
            for(MediaType mediaType : MediaType.values()){
                if(mediaType.number == number){
                    return mediaType;
                }
            }
            
            return null;
        }
    }

    /**
     * The default server port for CoAP messages is 5683
     */
    public static int COAP_PORT_DEFAULT = 5683;

    /**
     * The default max age option value is 60
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
        allowedOptions.put(Code.GET,  constraintsGET);

        //POST
        HashMap<OptionName, OptionOccurence> constraintsPOST = new HashMap<OptionName, OptionOccurence>();
        constraintsPOST.put(OptionName.TOKEN, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.URI_HOST, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.URI_PATH, OptionOccurence.MULTIPLE);
        constraintsPOST.put(OptionName.URI_PORT, OptionOccurence.ONCE);
        constraintsPOST.put(OptionName.URI_QUERY, OptionOccurence.MULTIPLE);
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
        allowedOptions.put(Code.CONTENT_205, constraints205);

        //both, 4x, 5x only allow Max-Age Option
        HashMap<OptionName, OptionOccurence> constraints4x5x = new HashMap<OptionName, OptionOccurence>();
        constraints4x5x.put(OptionName.TOKEN, OptionOccurence.ONCE);
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

    public static int getMaxLength(OptionName opt_name){
        return syntaxConstraints.get(opt_name).max_length;
    }

    public static int getMinLength(OptionName opt_name){
        return syntaxConstraints.get(opt_name).min_length;
    }

    public static OptionOccurence getAllowedOccurence(Code code, OptionName opt_name){
        try{
            OptionOccurence result = allowedOptions.get(code).get(opt_name);
            log.debug("Occurence constraint for option " + opt_name + " with code " + code + " is: "
                        + result.toString());
            return result != null ? result : OptionOccurence.NONE;
        }
        catch(NullPointerException e){
            return OptionOccurence.NONE;
        }
    }

    public static OptionType getOptionType(OptionName opt_name){
        return syntaxConstraints.get(opt_name).opt_type;
    }

    public static OptionName getOptionName(int number) throws InvalidOptionException{
        for(OptionName o :OptionName.values()){
            if(o.number == number){
                return o;
            }
        }
        String msg = "[OptionRegistry] Option number " + number + " is not registered in the OptionRegistry";
        throw new InvalidOptionException(number, msg);
    }

    public static boolean isCritial(OptionName opt_name){
        return isCritical(opt_name.number);
    }

    static boolean isCritical(int opt_number){
        //Options with even numbers are critial, odd numbers refer to elective options
        return (opt_number % 2 == 0);
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
