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

package de.uniluebeck.itm.spitfire.nCoap.message;

import de.uniluebeck.itm.spitfire.nCoap.application.ResponseCallback;
import de.uniluebeck.itm.spitfire.nCoap.core.MessageIDFactory;
import de.uniluebeck.itm.spitfire.nCoap.core.TokenFactory;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.InvalidHeaderException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Oliver Kleine
 */
public class Request extends Message{

    private static Logger log = Logger.getLogger("nCoap");

    private ResponseCallback callback;

    /**
     * This method creates a new Message object and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     * @param msgType  The message type
     * @param code The message code
     * @param targetUri the recipients URI
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws java.net.URISyntaxException if the URI is not appropriate for a CoAP message
     * @throws ToManyOptionsException if the target URI needs more than the maximum number of options per message
     * @throws InvalidMessageException if the given code is not suitable for a request
     * @return a new Message instance
     */
    public Request(MsgType msgType, Code code, URI targetUri)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {

        super(msgType, code);

        if(!code.isRequest()){
            throw new InvalidMessageException("[Request] Code " + code + " is not for a request.");
        }

        try {
            setMessageId(MessageIDFactory.getInstance().nextMessageID());
        } catch (InvalidHeaderException e) {
            log.fatal("[Request] This should never happen:\n" + e.getStackTrace());
        }

        setTargetURI(targetUri);

        if(log.isDebugEnabled()){
            log.debug("[Request] Created new request instance " +
                "(MsgType: " + msgType + ", Code: " + code + ", TargetURI: " + getTargetUri() + ")");
        }
    }

    /**
     * This method creates a new Message object and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     * @param msgType  The message type
     * @param code The message code
     * @param targetUri the recipients URI
     * @throws InvalidOptionException if one of the target URI options to be created is not valid
     * @throws java.net.URISyntaxException if the URI is not appropriate for a CoAP message
     * @throws ToManyOptionsException if the target URI needs more than the maximum number of options per message
     * @throws InvalidMessageException if the given code is not suitable for a request
     * @return a new Message instance
     */
    public Request(MsgType msgType, Code code, URI targetUri, ResponseCallback callback)
            throws InvalidMessageException, ToManyOptionsException, InvalidOptionException, URISyntaxException {

        this(msgType, code, targetUri);
        setToken(TokenFactory.getInstance().getNextToken());
        this.callback = callback;
    }

    public ResponseCallback getCallback() {
        return callback;
    }
}
