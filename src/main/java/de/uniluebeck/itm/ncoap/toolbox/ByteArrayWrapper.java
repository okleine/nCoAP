package de.uniluebeck.itm.ncoap.toolbox;

import java.util.Arrays;

///This wrapper is necessary since two raw byte arrays don't equal even if they have the same content!
public class ByteArrayWrapper{
    private final byte[] data;

    public ByteArrayWrapper(byte[] data){
        if (data == null)
            throw new NullPointerException();

        this.data = data;
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

    @Override
    public String toString(){
        return Tools.toHexString(data);
    }
}
