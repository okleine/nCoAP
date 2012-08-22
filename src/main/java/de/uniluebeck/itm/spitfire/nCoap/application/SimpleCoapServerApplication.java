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

package de.uniluebeck.itm.spitfire.nCoap.application;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * This is a very simple server application providing a .well-known/core resource and a simple resource both only
 * allowing {@link Code#GET} requests.
 *
* @author Oliver Kleine
*/
public class SimpleCoapServerApplication extends CoapServerApplication {

    private static Logger log = LoggerFactory.getLogger(SimpleCoapServerApplication.class.getName());

    /**
     * This method makes the server to process incoming request and decide whether the requested resource is
     * available or not
     *
     * @param coapRequest The incoming {@link CoapRequest} object
     * @return
     */
    @Override
    public CoapResponse receiveCoapRequest(CoapRequest coapRequest, InetSocketAddress senderSocketAddress) {
        
        log.debug("Received a request for " + coapRequest.getTargetUri());
        log.debug("Path: " + coapRequest.getTargetUri().getPath());
        
        String resource = coapRequest.getTargetUri().getPath();
        
        if(resource.equals("/.well-known/core")){
            if(coapRequest.getCode() == Code.GET){
                return getWellKnownCore();
            }
            else return new CoapResponse(Code.METHOD_NOT_ALLOWED_405);
        }
        else if(resource.equals("/simple")){
            if(coapRequest.getCode() == Code.GET){
                return getSimple();
            }
            else return new CoapResponse(Code.METHOD_NOT_ALLOWED_405);
            
        }
        else{
            log.debug("Request for unknown resource.");
            return new CoapResponse(Code.NOT_FOUND_404);
        }
    }

    private CoapResponse getSimple(){
        CoapResponse coapResponse = new CoapResponse(Code.CONTENT_205);
        
        try{
            coapResponse.setContentType(OptionRegistry.MediaType.TEXT_PLAIN_UTF8);
        } catch (InvalidOptionException e) {
            log.error("[" + this.getClass().getName() + "] " + e.getClass().getName(), e);
        } catch (ToManyOptionsException e) {
            log.error("[" + this.getClass().getName() + "] " + e.getClass().getName(), e);
        }

        try {
            coapResponse.setPayload((new String("Content of simple resource").getBytes(Charset.forName("UTF-8"))));
        } catch (MessageDoesNotAllowPayloadException e) {
            log.error(" Error while setting payload for response.");
        }
        
        return coapResponse;
        
    }
    
    private CoapResponse getWellKnownCore(){
        CoapResponse coapResponse = new CoapResponse(Code.CONTENT_205);
        try {
            coapResponse.setContentType(OptionRegistry.MediaType.APP_LINK_FORMAT);
        } catch (InvalidOptionException e) {
            log.error("[" + this.getClass().getName() + "] " + e.getClass().getName(), e);
        } catch (ToManyOptionsException e) {
            log.error("[" + this.getClass().getName() + "] " + e.getClass().getName(), e);
        }
        try {
            coapResponse.setPayload((new String("</simple>").getBytes(Charset.forName("UTF-8"))));
        } catch (MessageDoesNotAllowPayloadException e) {
            log.error(" Error while setting payload for response.");
        }

        return coapResponse;  
    }
    
    
    public static void main(String[] args){
        SimpleCoapServerApplication serverApplication = new SimpleCoapServerApplication();
        //serverApplication.shutdown();
    }
}
