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
package de.uniluebeck.itm.ncoap.application.client;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLongs;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 19.11.13
 * Time: 18:35
 * To change this template use File | Settings | File Templates.
 */
public class Token implements Comparable<Token>{

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private byte[] token;

    public Token(byte[] token){
        this.token = token;
    }

    public byte[] getBytes(){
        return this.token;
    }


    /**
     * Returns a String representation of the token in form of a hex-String
     *
     * @return a String representation of the token in form of a hex-String
     */
    @Override
    public String toString(){
        return "0x" + bytesToHex(getBytes());
    }


    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public boolean equals(Object object){
        if(object == null || (!(object instanceof Token)))
            return false;

        Token other = (Token) object;
        return Arrays.equals(this.getBytes(), other.getBytes());
    }

    @Override
    public int hashCode(){
        return Ints.fromByteArray(Bytes.concat(token, new byte[4]));
    }

    @Override
    public int compareTo(Token other) {

        if(other.equals(this))
            return 0;

        if(this.getBytes().length < other.getBytes().length)
            return -1;

        if(this.getBytes().length > other.getBytes().length)
            return 1;

        return UnsignedLongs.compare(Longs.fromByteArray(Bytes.concat(this.getBytes(), new byte[8])),
                                     Longs.fromByteArray(Bytes.concat(other.getBytes(), new byte[8])));
    }
}
