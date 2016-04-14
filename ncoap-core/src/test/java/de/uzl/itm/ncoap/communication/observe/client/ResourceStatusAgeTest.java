/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
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
package de.uzl.itm.ncoap.communication.observe.client;

import com.google.common.collect.Lists;
import de.uzl.itm.ncoap.AbstractCoapTest;
import de.uzl.itm.ncoap.communication.observing.ResourceStatusAge;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import org.apache.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.apache.log4j.Logger;

import java.util.Collection;

import static org.junit.Assert.assertTrue;

/**
 * Created by olli on 10.09.14.
 */
@RunWith(Parameterized.class)
public class ResourceStatusAgeTest extends AbstractCoapTest {

    private static Logger LOG = Logger.getLogger(ResourceStatusAgeTest.class.getName());

    public static long THRESHOLD = (long) Math.pow(2, 23);

    /*
    V1: Sequence number of previous update notification
    V2: Sequence number of received update notification
    T1: Reception timestamp of previous update notification
    T2: Reception timestamp of received update notification
    Expected: Is received newer than previous update notification?
     */
    @Parameterized.Parameters(name = "v1 = {0}, v2 = {1}, t1 = {2}, t2 = {3}, expected = {4}")
    public static Collection<Object[]> data() throws Exception {
        return Lists.newArrayList(
                //V1 < V2 and V2 - V1 < 2^23 (T1 = T2)
                new Object[]{1, THRESHOLD, 12345, 12345, true},

                //V1 < V2 and V2 - V1 < 2^23 (T1 < T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD - 1, 12344, 12345, true},

                //V1 < V2 and V2 - V1 = 2^23 (T1 = T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD, 12345, 12345, false},

                //V1 < V2 and V2 - V1 = 2^23 (T1 + 128000 = T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD, 12345, 12345 + 128000, false},

                //V1 < V2 and V2 - V1 = 2^23 (T1 + 128001 = T2)
                new Object[]{THRESHOLD, 2 * THRESHOLD, 12345, 12345 + 128001, true},

                //V1 > V2 and V1 - V2 > 2^23 (T1 < T2)
                new Object[]{THRESHOLD + 2, 1, 12344, 12345, true},

                //V1 > V2 and V1 - V2 > 2^23 (T1 < T2)
                new Object[]{THRESHOLD + 1, 1, 12344, 12345, false},

                //V1 > V2 and V1 - V2 > 2^23 (T2 > T1 + 128 sec)
                new Object[]{THRESHOLD + 1, 1, 12344, 12345 + 128001, true}

        );
    }

    private ResourceStatusAge params1;
    private ResourceStatusAge params2;
    private boolean expected;


    public ResourceStatusAgeTest(long v1, long v2, long t1, long t2, boolean expected) {

        CoapResponse coapResponse1 = new CoapResponse(MessageType.CON, MessageCode.CONTENT_205);
        coapResponse1.setObserve(v1);

        CoapResponse coapResponse2 = new CoapResponse(MessageType.CON, MessageCode.CONTENT_205);
        coapResponse2.setObserve(v2);

        long sequenceNo1 = coapResponse1.getObserve();
        long sequenceNo2 = coapResponse2.getObserve();
        LOG.info("Observe Option Values: V1 =  " + sequenceNo1 + ", V2 = " + sequenceNo2);
        LOG.info("Timestamps: T1 = " + t1 + ", T2 = " + t1 + ", (T2 > T1 + 128 sec [" + (t2 > t1 + 128000) + "])");

        this.params1 = new ResourceStatusAge(sequenceNo1, t1);
        this.params2 = new ResourceStatusAge(sequenceNo2, t2);
        this.expected = expected;
    }

    @Override
    public void setupLogging() throws Exception {
        LOG.setLevel(Level.DEBUG);
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    @Test
    public void compareActuality() {
        String message;
        if (expected) {
            message = "Params2 (" + this.params2 + ") should be considered newer than Params1 (" + this.params1 + ")";
        }
        else{
            message = "Params1 (" + this.params1 + ") should be considered newer than Params2 (" + this.params2 + ")";
        }

        assertTrue(message, ResourceStatusAge.isReceivedStatusNewer(params1, params2) == expected);
    }
}
