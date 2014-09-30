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
package de.uniluebeck.itm.ncoap.communication.observing.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by olli on 11.09.14.
 */
public class ResourceStatusAge {

    public static long THRESHOLD = (long) Math.pow(2, 23);
    private static Logger log = LoggerFactory.getLogger(ResourceStatusAge.class.getName());

    long sequenceNo;
    long timestamp;

    ResourceStatusAge(long sequenceNo, long timestamp){
        this.sequenceNo = sequenceNo;
        this.timestamp = timestamp;
    }

    public static boolean isReceivedStatusNewer(ResourceStatusAge params1, ResourceStatusAge params2){
        if(params1.sequenceNo < params2.sequenceNo && params2.sequenceNo - params1.sequenceNo < THRESHOLD){
            log.debug("Criterion 1 matches: Params2 ({}) is newer than Params1 ({}).", params2, params1);
            return true;
        }

        if(params1.sequenceNo > params2.sequenceNo && params1.sequenceNo - params2.sequenceNo > THRESHOLD){
            log.debug("Criterion 2 matches: Params2 ({}) is newer than Params1 ({}).", params2, params1);
            return true;
        }

        if(params2.timestamp > params1.timestamp + 128000L){
            log.debug("Criterion 3 matches: Params2 ({}) is newer than Params1 ({}).", params2, params1);
            return true;
        }

        log.debug("No criterion matches: Params2 ({}) is older than Params1 ({}).", params2, params1);
        return false;
    }

    @Override
    public String toString(){
        return "Sequence No: " + this.sequenceNo + ", Reception Timestamp: " + this.timestamp;
    }
}
