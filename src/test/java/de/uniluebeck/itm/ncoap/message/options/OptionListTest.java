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
///**
// * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
// * All rights reserved
// *
// * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
// * following conditions are met:
// *
// *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
// *    disclaimer.
// *
// *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
// *    following disclaimer in the documentation and/or other materials provided with the distribution.
// *
// *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
// *    products derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
// * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uniluebeck.itm.ncoap.message.options;
//
//import de.uniluebeck.itm.ncoap.message.*;
//import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
//import java.util.LinkedList;
//import java.util.List;
//import org.junit.*;
//import static org.junit.Assert.*;
//import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName.*;
//import java.util.Arrays;
//
//
///**
// * Test of OptionList class.
// * @author Stefan Hueske
// */
//public class OptionListTest {
//    List<CodeConstraints> codeConstraintsList = new LinkedList<CodeConstraints>();
//
//    /**
//     * Fill codeConstraintsList with constraints for each messageCode
//     */
//    @Before
//    public void fillCodeConstraintsList() {
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.GET)
//                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT, OBSERVE_REQUEST)
//                .addNotAllowed(CONTENT_TYPE, MAX_AGE, LOCATION_PATH,
//                LOCATION_QUERY, IF_MATCH, IF_NONE_MATCH, FENCEPOST));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.POST)
//                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT, CONTENT_TYPE)
//                .addNotAllowed(MAX_AGE, PROXY_URI,
//                ETAG, LOCATION_PATH, LOCATION_QUERY, ACCEPT,
//                IF_MATCH, FENCEPOST, IF_NONE_MATCH));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.PUT)
//                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT, CONTENT_TYPE, IF_NONE_MATCH)
//                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG,
//                LOCATION_PATH, LOCATION_QUERY, ACCEPT, FENCEPOST));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.DELETE)
//                .addOnceAllowed(TOKEN, URI_HOST, URI_PORT)
//                .addNotAllowed(CONTENT_TYPE, MAX_AGE, PROXY_URI, ETAG,
//                LOCATION_PATH, LOCATION_QUERY,
//                ACCEPT, IF_MATCH, FENCEPOST, IF_NONE_MATCH));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.CREATED_201)
//                .addOnceAllowed(TOKEN, CONTENT_TYPE)
//                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, URI_HOST, URI_PORT,
//                URI_PATH, ACCEPT, IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.DELETED_202)
//                .addOnceAllowed(TOKEN, CONTENT_TYPE)
//                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, URI_HOST,
//                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
//                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.VALID_203)
//                .addOnceAllowed(TOKEN, MAX_AGE)
//                .addNotAllowed(CONTENT_TYPE, PROXY_URI, URI_HOST,
//                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
//                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.CHANGED_204)
//                .addOnceAllowed(TOKEN, CONTENT_TYPE)
//                .addNotAllowed(MAX_AGE, PROXY_URI, ETAG, URI_HOST,
//                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
//                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
//
//        codeConstraintsList.add(new CodeConstraints(MessageCode.CONTENT_205)
//                .addOnceAllowed(TOKEN, CONTENT_TYPE, MAX_AGE, OBSERVE_RESPONSE)
//                .addNotAllowed(PROXY_URI, URI_HOST,
//                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
//                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
//
//        MessageCode[] messageCode4x5x = new MessageCode[]{
//            MessageCode.BAD_REQUEST_400,
//            MessageCode.UNAUTHORIZED_401,
//            MessageCode.BAD_OPTION_402,
//            MessageCode.FORBIDDEN_403,
//            MessageCode.NOT_FOUND_404,
//            MessageCode.METHOD_NOT_ALLOWED_405,
//            MessageCode.PRECONDITION_FAILED_412,
//            MessageCode.REQUEST_ENTITY_TOO_LARGE_413,
//            MessageCode.UNSUPPORTED_CONTENT_FORMAT_415,
//            MessageCode.INTERNAL_SERVER_ERROR_500,
//            MessageCode.NOT_IMPLEMENTED_501,
//            MessageCode.BAD_GATEWAY_502,
//            MessageCode.SERVICE_UNAVAILABLE_503,
//            MessageCode.GATEWAY_TIMEOUT_504,
//            MessageCode.PROXYING_NOT_SUPPORTED_505};
//
//        for (MessageCode messageCode : messageCode4x5x) {
//            codeConstraintsList.add(new CodeConstraints(messageCode)
//                .addOnceAllowed(TOKEN, MAX_AGE)
//                .addNotAllowed(PROXY_URI, ETAG, URI_HOST,
//                LOCATION_PATH, URI_PORT, LOCATION_QUERY, URI_PATH, ACCEPT,
//                IF_MATCH, FENCEPOST, URI_QUERY, IF_NONE_MATCH));
//        }
//    }
//
//    /**
//     * Tests if all expected exceptions will be thrown when trying to add a invalid
//     * option - messageCode combination.
//     */
//    @Test
//    public void testAddOption() throws Exception {
//        for(CodeConstraints codeConstraints : codeConstraintsList) {
//            //once allowed for this messageCode
//            for (OptionName optionName : codeConstraints.onceAllowed) {
//                OptionList optionList = new OptionList();
//                //exception here and test will fail
//                optionList.addOption(codeConstraints.messageCode, optionName, getTestOption(optionName));
//                try {
//                    optionList.addOption(codeConstraints.messageCode, optionName, getTestOption(optionName));
//                    //fail if exception does not occur
//                    fail("Missing exception for multiple " + optionName
//                            + " options with " + codeConstraints.messageCode + " messageCode.");
//                } catch(InvalidOptionException e) { }
//            }
//
//            //not allowed for this messageCode
//            for (OptionName optionName : codeConstraints.notAllowed) {
//                 OptionList optionList = new OptionList();
//                 try {
//                    optionList.addOption(codeConstraints.messageCode, optionName, getTestOption(optionName));
//                    //fail if exception does not occur
//                    fail("Missing exception for " + optionName
//                            + " option with " + codeConstraints.messageCode + " messageCode.");
//                } catch(InvalidOptionException e) { }
//            }
//        }
//    }
//
//    private Option getTestOption(OptionName optionName) throws Exception {
//        //determine option type
//        OptionRegistry.OptionType type = null;
//
//        if (optionName == OptionName.CONTENT_TYPE) type = OptionRegistry.OptionType.UINT;
//        else if (optionName == OptionName.MAX_AGE) type = OptionRegistry.OptionType.UINT;
//        else if (optionName == OptionName.PROXY_URI) type = OptionRegistry.OptionType.STRING;
//        else if (optionName == OptionName.ETAG) type = OptionRegistry.OptionType.OPAQUE;
//        else if (optionName == OptionName.URI_HOST) type = OptionRegistry.OptionType.STRING;
//        else if (optionName == OptionName.LOCATION_PATH) type = OptionRegistry.OptionType.STRING;
//        else if (optionName == OptionName.URI_PORT) type = OptionRegistry.OptionType.UINT;
//        else if (optionName == OptionName.LOCATION_QUERY) type = OptionRegistry.OptionType.STRING;
//        else if (optionName == OptionName.URI_PATH) type = OptionRegistry.OptionType.STRING;
//        else if (optionName == OptionName.TOKEN) type = OptionRegistry.OptionType.OPAQUE;
//        else if (optionName == OptionName.ACCEPT) type = OptionRegistry.OptionType.UINT;
//        else if (optionName == OptionName.IF_MATCH) type = OptionRegistry.OptionType.OPAQUE;
//        else if (optionName == OptionName.URI_QUERY) type = OptionRegistry.OptionType.STRING;
//        else if (optionName == OptionName.IF_NONE_MATCH) type = OptionRegistry.OptionType.EMPTY;
//
//        if (type == OptionRegistry.OptionType.UINT) {
//            return new UintOption(optionName, 1);
//        } else if (type == OptionRegistry.OptionType.STRING) {
//            return new StringOption(optionName, "test");
//        } else if (type == OptionRegistry.OptionType.OPAQUE) {
//            return new OpaqueOption(optionName, "tt".getBytes("UTF8"));
//        }
//        return new EmptyOption(optionName);
//    }
//
//     class CodeConstraints {
//        MessageCode messageCode;
//
//        List<OptionName> onceAllowed =
//                new LinkedList<OptionName>();
//
//        List<OptionName> notAllowed =
//                new LinkedList<OptionName>();
//
//        public CodeConstraints(MessageCode messageCode) {
//            this.messageCode = messageCode;
//        }
//
//        public CodeConstraints addOnceAllowed(OptionName... optionNames) {
//            onceAllowed.addAll(Arrays.asList(optionNames));
//            return this;
//        }
//
//        public CodeConstraints addNotAllowed(OptionName... optionNames) {
//            notAllowed.addAll(Arrays.asList(optionNames));
//            return this;
//        }
//    }
//}
