package de.uniluebeck.itm.spitfire.nCoap.communication.blockwise;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 12.09.12
 * Time: 18:19
 * To change this template use File | Settings | File Templates.
 */
public enum Blocksize {

    UNDEFINED(-1),
    SIZE_16(0),
    SIZE_32(1),
    SIZE_64(2),
    SIZE_128(3),
    SIZE_256(4),
    SIZE_512(5),
    SIZE_1024(6);

    public final int szx;

    Blocksize(int szx){
        this.szx = szx;
    }

    public int length(){
        return (int) Math.pow(2, szx + 4);
    }

    public static Blocksize getByExponent(long exponent){
        if (exponent == 0){
            return Blocksize.SIZE_16;
        }
        else if (exponent == 1){
            return Blocksize.SIZE_32;
        }
        else if (exponent == 2){
            return Blocksize.SIZE_64;
        }
        else if (exponent == 3){
            return Blocksize.SIZE_128;
        }
        else if (exponent == 4){
            return Blocksize.SIZE_256;
        }
        else if (exponent == 5){
            return Blocksize.SIZE_512;
        }
        else if (exponent == 6){
            return Blocksize.SIZE_1024;
        }
        else{
            return null;
        }
    }
}
