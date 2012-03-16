package de.uniluebeck.itm.spitfire.nCoap.message.options;

import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName;
import java.util.LinkedList;
import java.util.List;
import org.junit.*;
import static org.junit.Assert.*;
import static de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry.OptionName.*;
import java.util.Arrays;

/**
 * Test of OptionList class.
 * @author Stefan Hueske
 */
public class OptionListTest {
    List<CodeConstraints> codeConstraintsList = new LinkedList<CodeConstraints>();
    
    /**
     * Fill codeConstraintsList with constraints for each code
     */
    @Before
    public void fillCodeConstraintsList() {
        
        codeConstraintsList.add(new CodeConstraints(Code.GET)
                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT)
                .addNotAllowed(CONTENT_TYPE, MAX_AGE, LOCATION_PATH, 
                LOCATION_QUERY, IF_MATCH, IF_NONE_MATCH, FENCEPOST));
        
        codeConstraintsList.add(new CodeConstraints(Code.POST)
                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT)
                .addNotAllowed(CONTENT_TYPE, MAX_AGE, PROXY_URI,
                ETAG, LOCATION_PATH, LOCATION_QUERY, ACCEPT,
                IF_MATCH, FENCEPOST, IF_NONE_MATCH));
        
        codeConstraintsList.add(new CodeConstraints(Code.PUT)
                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT, CONTENT_TYPE, IF_NONE_MATCH)
                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, 
                LOCATION_PATH, LOCATION_QUERY, ACCEPT, FENCEPOST));
        
        codeConstraintsList.add(new CodeConstraints(Code.DELETE)
                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT)
                .addNotAllowed(CONTENT_TYPE, MAX_AGE, PROXY_URI, ETAG, 
                LOCATION_PATH, LOCATION_QUERY, 
                ACCEPT, IF_MATCH, FENCEPOST, IF_NONE_MATCH));
        
        codeConstraintsList.add(new CodeConstraints(Code.CREATED_201)
                .addOnceAllowed(TOKEN, CONTENT_TYPE)
                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, URI_HOST, URI_PORT, 
                URI_PATH, ACCEPT, IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
        
        codeConstraintsList.add(new CodeConstraints(Code.DELETED_202)
                .addOnceAllowed(TOKEN, CONTENT_TYPE)
                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, URI_HOST, 
                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT, 
                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
        
        codeConstraintsList.add(new CodeConstraints(Code.VALID_203)
                .addOnceAllowed(TOKEN, MAX_AGE)
                .addNotAllowed(CONTENT_TYPE, PROXY_URI, URI_HOST, 
                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT, 
                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
        
        codeConstraintsList.add(new CodeConstraints(Code.CHANGED_204)
                .addOnceAllowed(TOKEN, CONTENT_TYPE)
                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, URI_HOST,
                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
        
        codeConstraintsList.add(new CodeConstraints(Code.CONTENT_205)
                .addOnceAllowed(TOKEN, CONTENT_TYPE, MAX_AGE)
                .addNotAllowed(PROXY_URI, URI_HOST,
                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
        
        Code[] code4x5x = new Code[]{
            Code.BAD_REQUEST_400,
            Code.UNAUTHORIZED_401,
            Code.BAD_OPTION_402,
            Code.FORBIDDEN_403,
            Code.NOT_FOUND_404,
            Code.METHOD_NOT_ALLOWED_405,
            Code.PRECONDITION_FAILED_412,
            Code.REQUEST_ENTITY_TOO_LARGE_413,
            Code.UNSUPPORTED_MEDIA_TYPE_415,
            Code.INTERNAL_SERVER_ERROR_500,
            Code.NOT_IMPLEMENTED_501,
            Code.BAD_GATEWAY_502,
            Code.SERVICE_UNAVAILABLE_503,
            Code.GATEWAY_TIMEOUT_504,
            Code.PROXYING_NOT_SUPPORTED_505};
        
        for (Code code : code4x5x) {
            codeConstraintsList.add(new CodeConstraints(code)
                .addOnceAllowed(TOKEN, MAX_AGE)
                .addNotAllowed(CONTENT_TYPE, PROXY_URI, ETAG, URI_HOST, 
                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT, 
                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));       
        }
    }
    
    /**
     * Tests if all expected exceptions will be thrown when trying to add a invalid
     * option - code combination.
     */
    @Test
    public void testAddOption() throws Exception {
        for(CodeConstraints codeConstraints : codeConstraintsList) {
            //once allowed for this code
            for (OptionName optionName : codeConstraints.onceAllowed) {
                OptionList optionList = new OptionList();
                //exception here and test will fail
                optionList.addOption(codeConstraints.code, optionName, getTestOption(optionName));
                try {
                    optionList.addOption(codeConstraints.code, optionName, getTestOption(optionName));
                    //fail if exception does not occur
                    fail("Missing exception for multiple " + optionName 
                            + " options with " + codeConstraints.code + " code.");
                } catch(InvalidOptionException e) { }
            }
            
            //not allowed for this code
            for (OptionName optionName : codeConstraints.notAllowed) {
                 OptionList optionList = new OptionList();
                 try {
                    optionList.addOption(codeConstraints.code, optionName, getTestOption(optionName));
                    //fail if exception does not occur
                    fail("Missing exception for " + optionName 
                            + " option with " + codeConstraints.code + " code.");
                } catch(InvalidOptionException e) { }
            }
        }
    }
    
    private Option getTestOption(OptionName optionName) throws Exception {
        //determine option type
        OptionRegistry.OptionType type = null;
        
        if (optionName == OptionName.CONTENT_TYPE) type = OptionRegistry.OptionType.UINT;
        else if (optionName == OptionName.MAX_AGE) type = OptionRegistry.OptionType.UINT;
        else if (optionName == OptionName.PROXY_URI) type = OptionRegistry.OptionType.STRING;
        else if (optionName == OptionName.ETAG) type = OptionRegistry.OptionType.OPAQUE;
        else if (optionName == OptionName.URI_HOST) type = OptionRegistry.OptionType.STRING;
        else if (optionName == OptionName.LOCATION_PATH) type = OptionRegistry.OptionType.STRING;
        else if (optionName == OptionName.URI_PORT) type = OptionRegistry.OptionType.UINT;
        else if (optionName == OptionName.LOCATION_QUERY) type = OptionRegistry.OptionType.STRING;
        else if (optionName == OptionName.URI_PATH) type = OptionRegistry.OptionType.STRING;
        else if (optionName == OptionName.TOKEN) type = OptionRegistry.OptionType.OPAQUE;
        else if (optionName == OptionName.ACCEPT) type = OptionRegistry.OptionType.UINT;
        else if (optionName == OptionName.IF_MATCH) type = OptionRegistry.OptionType.OPAQUE;
        else if (optionName == OptionName.URI_QUERY) type = OptionRegistry.OptionType.STRING;
        else if (optionName == OptionName.IF_NONE_MATCH) type = OptionRegistry.OptionType.EMPTY;
        
        if (type == OptionRegistry.OptionType.UINT) {
            return new UintOption(optionName, 1); 
        } else if (type == OptionRegistry.OptionType.STRING) {
            return new StringOption(optionName, "test");
        } else if (type == OptionRegistry.OptionType.OPAQUE) {
            return new OpaqueOption(optionName, "tt".getBytes("UTF8"));
        }
        return new EmptyOption(optionName);
    }
 
     class CodeConstraints {
        Code code;

        List<OptionName> onceAllowed = 
                new LinkedList<OptionName>();
        
        List<OptionName> notAllowed = 
                new LinkedList<OptionName>();
        
        public CodeConstraints(Code code) {
            this.code = code;
        }
        
        public CodeConstraints addOnceAllowed(OptionName... optionNames) {
            onceAllowed.addAll(Arrays.asList(optionNames));
            return this;
        }
        
        public CodeConstraints addNotAllowed(OptionName... optionNames) {
            notAllowed.addAll(Arrays.asList(optionNames));
            return this;
        }
    }
}
