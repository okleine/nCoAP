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
package de.uniluebeck.itm.ncoap.message.options;

import de.uniluebeck.itm.ncoap.message.CoapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * @author Oliver Kleine
 */
public class StringOption extends Option<String>{

    private static Logger log = LoggerFactory.getLogger(StringOption.class.getName());


//    public StringOption(OptionName optionName, String value) throws InvalidOptionException{
//        super(optionName,
//                optionName == URI_HOST ?
//                        value.toLowerCase(Locale.ENGLISH).getBytes(CHARSET) :
//                            convertToByteArrayWithoutPercentEncoding(optionName, value));
//    }

    public StringOption(int optionNumber, byte[] value) throws InvalidOptionException, UnknownOptionException {
        super(optionNumber, value);
    }

    public StringOption(int optionNumber, String value) throws InvalidOptionException, UnknownOptionException {
        this(optionNumber, optionNumber == Option.Name.URI_HOST ?
                convertToByteArrayWithoutPercentEncoding(optionNumber, value.toLowerCase(Locale.ENGLISH)) :
                convertToByteArrayWithoutPercentEncoding(optionNumber, value));
    }

    @Override
    public String getDecodedValue() {
        return new String(value, CoapMessage.CHARSET);
    }

//    @Override
//    public boolean equals(Object object) {
//        if(!(object instanceof StringOption))
//            return false;
//
//        StringOption other = (StringOption) object;
//        return this.getDecodedValue().equals(other.getDecodedValue());
//    }


//    @Override
//    public boolean equals(Object o) {
//        if(!(o instanceof StringOption)){
//            return false;
//        }
//        StringOption opt = (StringOption) o;
//        if((this.optionNumber == opt.optionNumber) && Arrays.equals(this.encodedValue, opt.encodedValue)){
//            return true;
//        }
//        return false;
//    }

//    /**
//     * Returns the options value as decoded String assuming the value to be UTF-8 encoded
//     * @return the options value as decoded String assuming the value to be UTF-8 encoded
//     */
//    public String getDecodedValue() {
//        String result = null;
//        try {
//            result = new String(encodedValue, CHARSET);
//        } catch (UnsupportedEncodingException e) {
//           log.debug("This should never happen:\n", e);
//        }
//        return result;
//    }

    //Replaces percent-encoding from ASCII Strings with UTF-8 encoding
    public static byte[] convertToByteArrayWithoutPercentEncoding(int optionNumber, String s)
            throws InvalidOptionException {

        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes(CoapMessage.CHARSET));;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i;
        do{
            i = in.read();
            //-1 indicates end of stream
            if(i == -1)
                break;

            //0x25 = '%'
            if(i == 0x25){
                //Character.digit returns the integer value encoded as in.read(). Since we know that percent encoding
                //uses bytes from 0x0 to 0xF (i.e. 0 to 15) the radix must be 16.
                int d1 = Character.digit(in.read(), 16);
                int d2 = Character.digit(in.read(), 16);

                if(d1 == -1 || d2 == -1){
                    //Unexpected end of stream (at least one byte missing after '%')
                    throw new InvalidOptionException(optionNumber, "Invalid percent encoding in: " + s);
                }

                //Write decoded value to output stream (e.g. sequence [0x02, 0x00] results into byte 0x20
                out.write((d1 << 4) | d2);
            }
            else{
                out.write(i);
            }
        } while(true);

        return out.toByteArray();
    }
}
