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
package de.uniluebeck.itm.ncoap.communication.blockwise;

/**
 * Enumeration containing the available blocksizes for blockwise transfer. The bloksizes refers to the number of bytes
 * transfered per message during blockwise transfer.
 *
 * @author Oliver Kleine
 */
public enum Blocksize {

    /**
     * Blocksize is not defined
     */
    UNDEFINED(-1),
    /**
     * Blocksize 16 bytes
     */
    SIZE_16(0),
    /**
     * Blocksize 32 bytes
     */
    SIZE_32(1),
    /**
     * Blocksize 64 bytes
     */
    SIZE_64(2),
    /**
     * Blocksize 128 bytes
     */
    SIZE_128(3),
    /**
     * Blocksize 256 bytes
     */
    SIZE_256(4),
    /**
     * Blocksize 512 bytes
     */
    SIZE_512(5),
    /**
     * Blocksize 1024 bytes
     */
    SIZE_1024(6);

    /**
     * The size exponent defining the blocksize
     */
    public final int szx;

    Blocksize(int szx){
        this.szx = szx;
    }

    /**
     * Returns the number of bytes contained in a block of this {@link Blocksize}
     * @return the number of bytes contained in a block of this {@link Blocksize}
     */
    public int length(){
        return (int) Math.pow(2, szx + 4);
    }

    /**
     * Return a {@link Blocksize} instance representing the blocksize defined by the given exponent
     *
     * @param szx the exponent to define the blocksize, i.e. actual exponent - 4
     *
     * @return a {@link Blocksize} instance representing the blocksize defined by the given exponent or
     * <code>null</code> if no corresponding blocksize exists.
     */
    public static Blocksize getByExponent(long szx){
        if (szx == 0){
            return Blocksize.SIZE_16;
        }
        else if (szx == 1){
            return Blocksize.SIZE_32;
        }
        else if (szx == 2){
            return Blocksize.SIZE_64;
        }
        else if (szx == 3){
            return Blocksize.SIZE_128;
        }
        else if (szx == 4){
            return Blocksize.SIZE_256;
        }
        else if (szx == 5){
            return Blocksize.SIZE_512;
        }
        else if (szx == 6){
            return Blocksize.SIZE_1024;
        }
        else{
            return null;
        }
    }
}
