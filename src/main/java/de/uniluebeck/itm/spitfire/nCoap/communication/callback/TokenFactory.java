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

package de.uniluebeck.itm.spitfire.nCoap.communication.callback;

import com.google.common.primitives.Longs;
import de.uniluebeck.itm.spitfire.nCoap.toolbox.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;

/**
 * The TokenFactory generates tokens to match incoming responses with open requests and enable the
 * ReponseCallbackHandler to invoke the correct callback method. Since there are pow(2,64) possibilities for a token
 * and the generation is randomized, it is rather unlikely to get the same token within the usual time to wait for
 * a response. That's why we pass on memorizing tokens currently being in use.
 *
 * @author Oliver Kleine
 */
public class TokenFactory {

    private static Logger log = LoggerFactory.getLogger(TokenFactory.class.getName());

    private static TokenFactory instance = new TokenFactory();
    private Random random;

    private TokenFactory(){
        random = new Random(System.currentTimeMillis());
    }

    public static TokenFactory getInstance(){
        return instance;
    }

    /**
     * Returns the next token to be used
     * @return the next token to be used
     */
    public byte[] getNextToken(){
        byte[] tmp = Longs.toByteArray(random.nextLong());

        for(int i = 0; i < 8; i++){
            if(tmp[i] != 0){
                return Tools.getByteArrayRange(tmp, i, 8);
            }
        }
        return new byte[0];
    }
}
