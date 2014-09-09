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
/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.ncoap.message;

import java.util.HashMap;


/**
 * This class is a container of useful helpers to deal with message types.
 *
 * @author Oliver Kleine
 */
public abstract class MessageType {

    private static final HashMap<Integer, Name> validNumbers = new HashMap<Integer, Name>();

    public static enum Name{

        UNKNOWN(-1),

        /**
         * Corresponds to Message Type 0
         */
        CON(0),

        /**
         * Corresponds to Message Type 1
         */
        NON(1),

        /**
         * Corresponds to Message Type 2
         */
        ACK(2),

        /**
         * Corresponds to Message Type 3
         */
        RST(3);

        private int number;

        private Name(int number){
            this.number = number;
            validNumbers.put(number, this);
        }

        /**
         * Returns the number that corresponds to the given {@link de.uniluebeck.itm.ncoap.message.MessageType.Name}
         * @return the number that corresponds to the given {@link de.uniluebeck.itm.ncoap.message.MessageType.Name}
         */
        public int getNumber(){
            return this.number;
        }

        /**
         * Returns the {@link Name} corresponding to the given number or {@link Name#UNKNOWN} if no such {@link Name}
         * exists.
         *
         * @return the {@link Name} corresponding to the given number or {@link Name#UNKNOWN} if no such {@link Name}
         * exists.
         */
        public static Name getName(int number){
            if(validNumbers.containsKey(number))
                return validNumbers.get(number);
            else
                return Name.UNKNOWN;
        }

        /**
         * Returns <code>true</code> if and only if the given number corresponds to a valid message type. Otherwise it
         * returns <code>false</code>.
         *
         * @param number the number to check whether it corresponds to a valid message type.
         *
         * @return whether the given number corresponds to a valid message type.
         */
        public static boolean isMessageType(int number){
            return validNumbers.keySet().contains(number);
        }
    }
}
