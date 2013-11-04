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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.ncoap.message;

import de.uniluebeck.itm.ncoap.message.header.Header;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;


/**
 *
 * @author Oliver Kleine
 */
public class InvalidOptionException extends Exception {

    private Header messageHeader;
    private int optionNumber;

    public InvalidOptionException(Header header, int optionNumber, String msg){
        this(optionNumber, msg);
        this.messageHeader = header;
    }

    public InvalidOptionException(int optionNumber, String msg){
        super(msg);
        this.optionNumber = optionNumber;
    }

    /**
     * Returns true if this Exception has been caused by a critical option. Otherwise (in case of elective options)
     * it returns false.
     * @return whether the Exception was caused by a critical option
     */
    public boolean isCritical(){
       return OptionRegistry.isCritical(optionNumber);
    }

    /**
     * Returns the number of the option that caused the exception
     * @return the number of the option that caused the exception
     */
    public int getOptionNumber() {
        return optionNumber;
    }

    /**
     * Returns the OptionName of the option that caused this exception
     * @return the OptionName of the option that caused this exception
     */
    public OptionName getOptionName(){
        return OptionName.getByNumber(optionNumber);
    }

    /**
     * Returns the {@link Header} instance that caused this exception (if available) or null otherwise
     * @return the {@link Header} instance that caused this exception (if available) or null otherwise
     */
    public Header getMessageHeader() {
        return messageHeader;
    }
}
