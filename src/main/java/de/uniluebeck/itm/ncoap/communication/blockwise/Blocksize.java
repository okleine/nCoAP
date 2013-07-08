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
