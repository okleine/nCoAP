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
//* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
//* All rights reserved
//*
//* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//* following conditions are met:
//*
//*  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
//*    disclaimer.
//*
//*  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//*    following disclaimer in the documentation and/or other materials provided with the distribution.
//*
//*  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
//*    products derived from this software without specific prior written permission.
//*
//* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
//* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
//* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
//* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//*/
//package de.uniluebeck.itm.ncoap.application.client;
//
//import com.google.common.primitives.Longs;
//import de.uniluebeck.itm.ncoap.toolbox.Token;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.*;
//
///**
//* The TokenFactory generates tokens to match incoming responses with open requests and enable the
//* {@link CoapClientApplication} to invoke the correct callback method.
//*
//*
//* @author Oliver Kleine
//*/
//class TokenFactory {
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//    private Random random = new Random(System.currentTimeMillis());
//    private Set<Long> usedTokens = Collections.synchronizedSet(new HashSet<Long>());
//
//    /**
//     * Returns the next token to be used
//     * @return the next token to be used
//     */
//    Long getNextToken(){
//        //create new token
//        Long nextToken;
//        do
//            nextToken = random.nextLong();
//        while(!usedTokens.add(nextToken));
//
//        Token result = new Token(Longs.toByteArray(nextToken));
//        log.debug("Added token: {} (Now {} tokens in use).", result, usedTokens.size());
//
//        return result;
//    }
//
//    /**
//     * Pass the token back to make it re-usable for upcoming requests
//     * @param token the token not used anymore
//     */
//    void passBackToken(Token token){
//        byte[] byteArray = token.getByteArray();
//        if(byteArray.length < 8){
//            byte[] tmp = new byte[]{0,0,0,0,0,0,0,0};
//            for(int i = 0; i < token.length; i++){
//                tmp[i + 8 - token.length] = token[i];
//            }
//            token = tmp;
//        }
//
//        if(usedTokens.remove(Longs.fromByteArray(token)))
//            log.debug("Passed back token: {} (Now {} tokens in use.)", new Token(token), usedTokens.size());
//        else
//            log.warn("Could not pass back token {}. Still {} tokens in use.", new Token(token),
//                    usedTokens.size());
//    }
//
//}
