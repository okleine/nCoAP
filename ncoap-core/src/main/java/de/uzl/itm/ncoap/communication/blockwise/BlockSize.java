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
package de.uzl.itm.ncoap.communication.blockwise;

import de.uzl.itm.ncoap.message.options.UintOptionValue;
import jdk.nashorn.internal.ir.Block;

/**
 * Created by olli on 09.02.16.
 */
public enum BlockSize {
    UNBOUND(-1, 65536),
    SIZE_16(0, 16),
    SIZE_32(1, 32),
    SIZE_64(2, 64),
    SIZE_128(3, 128),
    SIZE_256(4, 256),
    SIZE_512(5, 512),
    SIZE_1024(6, 1024);

    private int encodedSize;
    private int decodedSize;

    public static final int UNDEFINED = -1;
    private static final int SZX_MIN = 0;
    private static final int SZX_MAX = 6;

    BlockSize(int encodedSize, int decodedSize) {
        this.encodedSize = encodedSize;
        this.decodedSize = decodedSize;
    }

    public int getSzx(){
        return this.encodedSize;
    }

    public int getSize() {
        return this.decodedSize;
    }

    public static int getSize(long szx) {
        return getBlockSize(szx).getSize();
    }

    public static boolean isValid(long szx) {
        return !(szx < SZX_MIN) && !(szx > SZX_MAX);
    }


    public static long min(long szx1, long szx2) {
        if(szx1 == BlockSize.UNDEFINED) {
            return szx2;
        } else if (szx2 == BlockSize.UNDEFINED) {
            return szx1;
        } else {
            return Math.min(szx1, szx2);
        }
    }

    public static BlockSize getBlockSize(long szx) throws IllegalArgumentException{
        if (szx == BlockSize.UNDEFINED) {
            return BlockSize.UNBOUND;
        } else if (szx == 0) {
            return SIZE_16;
        } else if(szx == 1) {
            return SIZE_32;
        } else if(szx == 2) {
            return SIZE_64;
        } else if(szx == 3) {
            return SIZE_128;
        } else if(szx == 4) {
            return SIZE_256;
        } else if(szx == 5) {
            return SIZE_512;
        } else if(szx == 6) {
            return SIZE_1024;
        } else {
            throw new IllegalArgumentException("Unsupported SZX value (Block Option): " + szx);
        }
    }
}
