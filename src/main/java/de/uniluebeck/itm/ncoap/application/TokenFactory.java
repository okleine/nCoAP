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

package de.uniluebeck.itm.ncoap.application;

import com.google.common.base.Supplier;
import com.google.common.collect.*;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

/**
* The TokenFactory generates tokens to match incoming responses with open requests and enable the
* {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication} to invoke the correct callback method.
*
*
* @author Oliver Kleine
*/
public class TokenFactory {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private int maxTokenLength;

    private Multimap<InetSocketAddress, SettableFuture<Token>> waitingTokenFutures =
            LinkedHashMultimap.create();

    private SortedSetMultimap<InetSocketAddress, Token> usedTokens;
    private Multimap<InetSocketAddress, Token> releasedTokens;


    public TokenFactory(){
        this(8);
    }


    public TokenFactory(int maxTokenLength){
        this.maxTokenLength = maxTokenLength;

        Supplier<TreeSet<Token>> factory = new Supplier<TreeSet<Token>>() {
            @Override
            public TreeSet<Token> get() {
                return new TreeSet<>();
            }
        };

        usedTokens = Multimaps.newSortedSetMultimap(new HashMap<InetSocketAddress, Collection<Token>>(), factory);
        releasedTokens = HashMultimap.create();
    }



    public synchronized ListenableFuture<Token> getNextToken(InetSocketAddress remoteSocketAddress){

        SettableFuture<Token> tokenFuture = SettableFuture.create();
        Token nextToken = getNextReleasedToken(remoteSocketAddress);

        if(nextToken != null){
            tokenFuture.set(nextToken);
        }
        else{
            nextToken = getNextTokenInOrder(remoteSocketAddress);

            if(nextToken != null){
                tokenFuture.set(nextToken);
            }
        }

        if(nextToken == null){
            waitingTokenFutures.put(remoteSocketAddress, tokenFuture);
        }
        else{
            usedTokens.put(remoteSocketAddress, nextToken);
        }

        log.debug("Future was set: {}", tokenFuture.isDone());

        return tokenFuture;
    }


    private Token getNextReleasedToken(InetSocketAddress remoteSocketAddress){
        try{
            Token result = releasedTokens.get(remoteSocketAddress).iterator().next();
            releasedTokens.remove(remoteSocketAddress, result);
            log.debug("Token {} for {} was released and will now be re-used.", result, remoteSocketAddress);
            return result;
        }
        catch(NoSuchElementException e){
            log.debug("No released token found for {}", remoteSocketAddress);
            return null;
        }
    }


    private Token getNextTokenInOrder(InetSocketAddress remoteSocketAddress){
        try{
            Token result = getSuccessor(usedTokens.get(remoteSocketAddress).last());
            if(!usedTokens.containsEntry(remoteSocketAddress, result)){
                log.debug("Token {} for {} is the smallest available and will now be used.",
                        result, remoteSocketAddress);
                return result;
            }

            log.debug("All tokens ({}) for {} are in use. WAIT...",
                    usedTokens.get(remoteSocketAddress).size(), remoteSocketAddress);
            return null;
        }
        catch(NoSuchElementException e1){
            Token nextToken = new Token(new byte[0]);
            log.debug("Use token {} for {} as the first one.", nextToken, remoteSocketAddress);
            return nextToken;
        }
    }


    /**
     * Pass the token back to make it re-usable for upcoming requests
     * @param token the token not used anymore
     */
    public synchronized boolean passBackToken(InetSocketAddress remoteSocketAddress, Token token){

        if(usedTokens.remove(remoteSocketAddress, token)){
            log.debug("Passed back token {} (length: {}) from {}. (Now {} tokens in use.)",
                    new Object[]{token, token.getBytes().length, remoteSocketAddress,
                            usedTokens.get(remoteSocketAddress).size()});

            Collection<SettableFuture<Token>> futures = waitingTokenFutures.get(remoteSocketAddress);

            //If there is a future waiting for this token immediately re-use the released token
            if(!futures.isEmpty()){
                SettableFuture<Token> tokenFuture = futures.iterator().next();
                waitingTokenFutures.remove(remoteSocketAddress, tokenFuture);

                usedTokens.put(remoteSocketAddress, token);
                log.debug("Reuse token {} (length: {}) for {}. (Now {} tokens in use.)",
                        new Object[]{token, token.getBytes().length, remoteSocketAddress,
                                usedTokens.get(remoteSocketAddress).size()});

                tokenFuture.set(token);
            }

            //If there is no future waiting for a token put it on the list of released tokens
            else{
                log.debug("Put token {} for {} on list of released tokens.", token, remoteSocketAddress);
                releasedTokens.put(remoteSocketAddress, token);
            }

            return true;
        }
        else{
            log.error("Could not pass back token {} (length: {}) from {}. (Still {} tokens in use.)",
                    new Object[]{token, token.getBytes().length, remoteSocketAddress,
                            usedTokens.get(remoteSocketAddress).size()});
            return false;
        }
    }


    private Token getSuccessor(Token token){

        boolean allBitsSet = true;


        //Check if all bits in the given byte array are set to 1
        for(byte b : token.getBytes()){
            if(b != -1){
                allBitsSet = false;
                break;
            }
        }

        if(allBitsSet){
            //make e.g. ([00000000], [00000000]) the successor of ([11111111])
            if(token.getBytes().length < maxTokenLength)
                return new Token(new byte[token.getBytes().length + 1]);
                //make the empty byte array the successor of the byte array with 8 [11111111] bytes
            else
                return new Token(new byte[0]);
        }

        long tmp = Longs.fromByteArray(Bytes.concat(new byte[8-token.getBytes().length], token.getBytes())) + 1;
        byte[] result = Longs.toByteArray(tmp);
        return new Token(Arrays.copyOfRange(result, 8 - token.getBytes().length, 8));
    }

}
