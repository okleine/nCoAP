/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
package de.uniluebeck.itm.ncoap.toolbox;

import java.util.Arrays;

/**
 * This wrapper is necessary since two raw byte arrays don't equal even if they have the same content! A
 * {@link ByteArrayWrapper} can e.g. be used as a key in {@link java.util.Map} instances.
 *
 * @author Oliver Kleine
 */

public class ByteArrayWrapper{

    private final byte[] data;

    public ByteArrayWrapper(byte[] data){
        if (data == null)
            throw new NullPointerException();

        this.data = data;
    }

    /**
     * Returns a new byte array without the leading zero bytes (if any) of the given argument, e.g.
     * <ul>
     *     <li>
     *         argument: [0x00, 0x00, 0x01, 0x00 0x02], return value: [0x01, 0x00 0x02]
     *     </li>
     *     <li>
     *         argument: [0x01, 0x02, 0x03], return value: [0x01, 0x02, 0x03].
     *     </li>
     * </ul>
     *
     * @param array the array to remove leading zeros from
     *
     * @return a new byte array without the leading zero bytes (if any) of the argument
     */
    public static byte[] removeLeadingZerosFromByteArray(byte[] array){
        for(int i = 0; i < array.length; i++){
            if(array[i] != 0){
                return Arrays.copyOfRange(array, i, array.length);
            }
        }
        return new byte[0];
    }

    @Override
    public boolean equals(Object other){
        if(other == null)
            return false;

        if (!(other instanceof ByteArrayWrapper))
            return false;

        return Arrays.equals(data, ((ByteArrayWrapper) other).data);
    }



    @Override
    public int hashCode(){
        return Arrays.hashCode(data);
    }

    /**
     * Returns a hex-string representation of the data in the wrapped byte array.
     * @return a hex-string representation of the data in the wrapped byte array
     */
    @Override
    public String toString(){
        String result = "";
        for (int i=0; i < data.length; i++) {
            result += Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1) + "-";
        }
        return "" + result.subSequence(0, Math.max(result.length() - 1, 0));
    }
}
