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

package de.uniluebeck.itm.ncoap.communication.dispatching.client;

import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * The TokenFactory generates tokens to match incoming responses with open requests and enable the
 * {@link de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallbackManager} to invoke the correct callback method.
 *
 * The CoAP specification makes no assumptions how to interpret the bytes returned by {@link Token#getBytes()()}, i.e.
 * a {@link Token} instances where {@link Token#getBytes()} returns an empty byte array is different from a
 * {@link Token} where {@link Token#getBytes()} returns a byte array containing one "zero byte" (all bits set to 0).
 * Furthermore, both of these {@link Token}s differ from another {@link Token} that is backed by a byte array
 * containing two "zero bytes" and so on...
 *
 * This leads to 257 (<code>(2^8) + 1</code>) different tokens for a maximum token length of 1 or 65793 different
 * tokens (<code>(2^16) + (2^8) + 1</code>) for a maximum token length of 2 and so on and so forth...
 *
 * @author Oliver Kleine
 */
public class TokenFactory {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private int maxTokenLength;

    private SortedSetMultimap<InetSocketAddress, Token> activeTokens;

    /**
     * Creates a new instance of {@link TokenFactory} producing {@link Token}s where the length of
     * {@link Token#getBytes()} is not longer than the given maximum length.
     *
     * @param maxTokenLength the maximum length of {@link Token#getBytes()} for {@link Token}s produced by this
     *                       factory.
     */
    public TokenFactory(int maxTokenLength){
        this.maxTokenLength = maxTokenLength;

        Supplier<TreeSet<Token>> factory = new Supplier<TreeSet<Token>>() {
            @Override
            public TreeSet<Token> get() {
                return new TreeSet<>();
            }
        };

        activeTokens = Multimaps.newSortedSetMultimap(new HashMap<InetSocketAddress, Collection<Token>>(), factory);
    }


    /**
     * Returns a {@link Token} to be used with an outgoing {@link CoapRequest} to relate incoming
     * {@link CoapResponse}(s) with that request. This {@link Token} is ensured to be unique in combination with the
     * given {@link InetSocketAddress}, i.e. as long as there are any other ongoing communications with the same
     * CoAP server, the returned {@link ListenableFuture<Token>} will be set with a {@link Token} that is different
     * from all other {@link Token}s in use with the given remote CoAP endpoints.
     *
     * @param remoteEndpoint the {@link InetSocketAddress} of the CoAP server this token is supposed to be used
     *                            to communicate with.
     *
     * @return a {@link ListenableFuture<Token>} that will be set with a {@link Token} that is not already in use to
     * for ongoing communications with the given {@link InetSocketAddress}.
     */
    public synchronized Token getNextToken(InetSocketAddress remoteEndpoint) {

        Token nextToken = getNextTokenInOrder(remoteEndpoint);

        if(nextToken != null){
            activeTokens.put(remoteEndpoint, nextToken);
        }

        return nextToken;
    }


    private Token getNextTokenInOrder(InetSocketAddress remoteEndpoint){

        if(!activeTokens.containsKey(remoteEndpoint)){
            return  new Token(new byte[1]);
        }

        else{
            return this.getSuccessor(activeTokens.get(remoteEndpoint).last());
        }
    }


    /**
     * This method is invoked by the framework to pass back a {@link Token} for the given
     * {@link InetSocketAddress} to make it re-usable for upcoming communication with the same CoAP endpoints.
     *
     * @param token the {@link Token} that is not used anymore
     * @param remoteEndpoint the {@link InetSocketAddress} of the CoAP server, the {@link Token} was used to
     *                            communicate with
     */
    public synchronized boolean passBackToken(InetSocketAddress remoteEndpoint, Token token){

        if(activeTokens.remove(remoteEndpoint, token)){
            log.debug("Passed back token {} (length: {}) from {}. (Now {} tokens in use.)",
                    new Object[]{token, token.getBytes().length, remoteEndpoint,
                            activeTokens.get(remoteEndpoint).size()});

            return true;
        }

        else{
            log.error("Could not pass back token {} (length: {}) from {}. (Still {} tokens in use.)",
                    new Object[]{token, token.getBytes().length, remoteEndpoint,
                            activeTokens.get(remoteEndpoint).size()});

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
            if(token.getBytes().length < maxTokenLength){
                return new Token(new byte[token.getBytes().length + 1]);
            }

            //make [00000000] the successor of the byte array with 8 [11111111] bytes
            else{
                return new Token(new byte[1]);
            }
        }



        long tmp = Longs.fromByteArray(Bytes.concat(new byte[8-token.getBytes().length], token.getBytes())) + 1;
        byte[] result = Longs.toByteArray(tmp);
        return new Token(Arrays.copyOfRange(result, 8 - token.getBytes().length, 8));
    }

}
