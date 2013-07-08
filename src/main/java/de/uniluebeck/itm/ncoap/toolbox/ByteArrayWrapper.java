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
